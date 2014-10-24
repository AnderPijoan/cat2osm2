import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;


public class Cat2Osm {

	public static final String VERSION = "2014-10-14";
	public static Cat2OsmUtils utils;

	/** Constructor
	 * @param utils Clase utils en la que se almacenan los nodos, ways y relaciones 
	 * y tiene funciones para manejarlos
	 */
	public Cat2Osm (Cat2OsmUtils utils){
		Cat2Osm.utils = utils;
	}

	/** 
	 * Parsea el archivo .CAT y guarda en shapesTotales los resultados
	 * @param tipo
	 * @param cat
	 * @param shapesTotales
	 * @throws IOException
	 */
	public void catParser(String tipo, File cat, HashMap <String, List<Shape>> shapesTotales) throws IOException{
		CatParser parser = new CatParser(utils);
		parser.parseFile(tipo, cat, shapesTotales);
	}
	
	/**
	 * Parsea los usos y destinos del archivo .CAT y guarda en shapesTotales los resultados
	 * @param tipo
	 * @param cat
	 * @param shapesTotales
	 * @throws IOException
	 */
	public void catUsosParser(String tipo, File cat, HashMap <String, List<Shape>> shapesTotales) throws IOException{
		CatParser parser = new CatParser(utils);
		parser.parseUsosFile(tipo, cat, shapesTotales);
	}
	

	/** Los elementos textuales traen informacion con los numeros de portal pero sin datos de la parcela ni unidos a ellas
	 * Con esto, sabiendo el numero de portal buscamos la parcela mas cercana con ese numero y le pasamos los tags al elemento
	 * textual que es un punto
	 * @param shapes Lista de shapes original
	 * @return lista de shapes con los tags modificados, si es null es que no habia portales
	 */
	@SuppressWarnings("unchecked")
	public HashMap <String, List<Shape>> moveEntrancesToParcel(HashMap <String, List<Shape>> shapes){

		// Si no se ha leido ningun portal
		if (shapes.get("ELEMTEX-189401") == null)
			return null;

		// Creamos la factoria para crear objetos de GeoTools (hay otra factoria pero falla)
		// com.vividsolutions.jts.geom.GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
		GeometryFactory gf = new GeometryFactory();

		// Variabbles para el calculo del tiempo estimado
		System.out.print("["+new Timestamp(new Date().getTime())+"]\tProgreso = 0%. Estimando tiempo restante...\r");
		int bar = 0;
		int pos = 0;
		long timeElapsed = 0;
		float size = shapes.get("ELEMTEX-189401").size();
		long time = System.currentTimeMillis();

		// Creamos una cache para meter las geometrias de las parcelas y una lista con los numeros de policia de esas parcelas
		final SpatialIndex index = new STRtree();

		for (String key : shapes.keySet())
			if (!key.startsWith("EJES") && !key.startsWith("ELEM"))
				for (Shape shapeParcela : shapes.get(key)){

					// Si es un shape de parcela y tiene geometria
					if (shapeParcela instanceof ShapeParcela && "UR".equals(shapeParcela.getTipo()) && shapeParcela.getGeometry() != null && !shapeParcela.getGeometry().isEmpty()){

						// Cogemos la geometria exterior de la parcela
						Polygon p = (Polygon) shapeParcela.getGeometry().getGeometryN(0);

						// Outer
						Coordinate[] coors = p.getExteriorRing().getCoordinates();
						Geometry geom = (LineString) gf.createLineString(coors);

						// Para hacer la query es necesario en Envelope
						Envelope env = shapeParcela.getGeometry().getEnvelope().getEnvelopeInternal();

						// Comprobamos que el envelope no sea null y lo metemos
						if (!env.isNull()){
							index.insert(env, new LocationIndexedLine(geom));
						}
					}
				}


		// Buscamos la parcela mas cercana
		// Los shapes con key = "ELEMTEX189401" seran todos los elementos textuales de
		// entradas a parcelas
		Iterator<Shape> it = shapes.get("ELEMTEX-189401").iterator();
		while (it.hasNext()){

			Shape shapeTex = it.next();

			int progress = (int) ((pos++/size)*100);
			if (bar != progress){
				timeElapsed = (timeElapsed+(100-progress)*(System.currentTimeMillis()-time)/1000)/2;
				long hor = Math.round((timeElapsed/3600));
				long min = Math.round((timeElapsed-3600*hor)/60);
				long seg = Math.round((timeElapsed-3600*hor-60*min));

				System.out.print("["+new Timestamp(new Date().getTime())+"]\tProgreso = "+progress+"%. Tiempo restante estimado = "+hor+" horas, "+min+" minutos, "+seg+" segundos.\r");
				bar = progress;
				time = System.currentTimeMillis();
			}

			// Guarderemos 3 parcelas
			ShapeParcela nearestParcela = null; // Parcela mas cercana
			ShapeParcela nearestPairParcela = null; // Parcela par o impar dependiendo del rotulo mas cercana
			ShapeParcela nearestSameNumberParcela = null; // Parcela con el mismo addr:housenumber mas cerana

			// Y 3 coordenadas
			Coordinate nearestSnappedCoor = null; // Coordenada pegada a la parcela mas cercana
			Coordinate nearestPairSnappedCoor = null; // Igual pero para la parcela con par/impar
			Coordinate nearestSameNumberSnappedCoor = null; // Igual pero a la parcela con el mismo addr:housenumber

			// Variables
			Coordinate tempSnappedCoor = new Coordinate(); // Coordenada temporal del elemtex pegado a la geometria de la parcela
			com.vividsolutions.jts.geom.Point point = (Point) shapeTex.getGeometry(); // Geometria del shape del portal

			// Buscamos la parcela mas cercana
			double minDist = Cat2OsmUtils.ENTRANCES_SEARCHDIST;

			// Creamos el punto de busqueda con la coordenada del punto y la expandimos
			// en la distancia minima para obtener
			// una linea de desplazamiento para tocar la parcela
			Envelope search = new Envelope(point.getCoordinate());
			search.expandBy(Cat2OsmUtils.ENTRANCES_SEARCHDIST);

			// Hacemos la query
			List<LocationIndexedLine> lines = index.query(search);

			// Cada linea que nos devuelve representa el desplazamiento
			// que hay que darle a la coordenada para que se situe sobre la linea de la
			// geometria de la parcela
			for (LocationIndexedLine line : lines) {
				LinearLocation here = line.project(point.getCoordinate());
				tempSnappedCoor = line.extractPoint(here);
				double distance = tempSnappedCoor.distance(point.getCoordinate());

				if (distance < minDist) {

					ShapeParcela tempParcela = (ShapeParcela) getUnderlyingParcela(shapes, point.getCoordinate(), tempSnappedCoor);

					// Si hemos encontrado una parcela que cumple, actualizamos
					if (tempParcela != null){

						// Acualizamos la variable minDist y la parcela
						minDist = distance;
						nearestParcela = tempParcela;
						nearestSnappedCoor = tempSnappedCoor;
					}
				}
			}


			// Buscamos la parcela par/impar mas cercana

			tempSnappedCoor = new Coordinate(); // Coordenada del elemtex pegado a la geometria de la parcela
			point = (Point) shapeTex.getGeometry();

			minDist = Cat2OsmUtils.ENTRANCES_SEARCHDIST; // Distancia minima ~ 80 metros

			// Creamos el punto de busqueda con la coordenada del punto y la expandimos
			// en la distancia minima para obtener
			// una linea de desplazamiento para tocar la parcela
			search = new Envelope(point.getCoordinate());
			search.expandBy(Cat2OsmUtils.ENTRANCES_SEARCHDIST);

			// Hacemos la query
			lines = index.query(search);

			// Cada linea que nos devuelve representa el desplazamiento
			// que hay que darle a la coordenada para que se situe sobre la linea de la
			// geometria de la parcela
			for (LocationIndexedLine line : lines) {
				LinearLocation here = line.project(point.getCoordinate());
				tempSnappedCoor = line.extractPoint(here);
				double dist = tempSnappedCoor.distance(point.getCoordinate());

				if (dist < minDist) {

					ShapeParcela tempParcela = (ShapeParcela) getUnderlyingParcela(shapes, point.getCoordinate(), tempSnappedCoor);

					// Si hemos encontrado una parcela que cumple miramos su addr:housenumber
					if (tempParcela != null){

						// Comparamos si su addr:housenumber es igual que el rotulo del elemtex
						String housenumber = tempParcela.getAttributes().getValue("addr:housenumber");		

						if (housenumber != null && utils.esNumero(housenumber) && utils.esNumero(((ShapeElemtex)shapeTex).getRotulo().trim()) &&
								// Aqui se comprueba el par/impar
								Integer.parseInt(housenumber)%2 == Integer.parseInt(((ShapeElemtex) shapeTex).getRotulo())%2 ){

							// Acualizamos la variable minDist y la parcela
							minDist = dist;
							nearestPairParcela = tempParcela;
							nearestPairSnappedCoor = tempSnappedCoor;
						}
					}
				}
			}


			// Buscamos la parcela con el mismo addr:housenumber mas cercana

			tempSnappedCoor = new Coordinate(); // Coordenada del elemtex pegado a la geometria de la parcela
			point = (Point) shapeTex.getGeometry();

			minDist = Cat2OsmUtils.ENTRANCES_SEARCHDIST; // Distancia minima ~ 80 metros

			// Creamos el punto de busqueda con la coordenada del punto y la expandimos
			// en la distancia minima para obtener
			// una linea de desplazamiento para tocar la parcela
			search = new Envelope(point.getCoordinate());
			search.expandBy(Cat2OsmUtils.ENTRANCES_SEARCHDIST);

			// Hacemos la query
			lines = index.query(search);

			// Cada linea que nos devuelve representa el desplazamiento
			// que hay que darle a la coordenada para que se situe sobre la linea de la
			// geometria de la parcela
			for (LocationIndexedLine line : lines) {
				LinearLocation here = line.project(point.getCoordinate());
				tempSnappedCoor = line.extractPoint(here);
				double dist = tempSnappedCoor.distance(point.getCoordinate());

				if (dist < minDist) {

					ShapeParcela tempParcela = (ShapeParcela) getUnderlyingParcela(shapes, point.getCoordinate(), tempSnappedCoor);

					// Si hemos encontrado una parcela que cumple miramos su addr:housenumber
					if (tempParcela != null){

						// Comparamos si su addr:housenumber es igual que el rotulo del elemtex
						String housenumber = tempParcela.getAttributes().getValue("addr:housenumber");		

						if (housenumber != null && 
								// Aqui se compara que sean iguales
								housenumber.trim().equals(((ShapeElemtex) shapeTex).getRotulo().trim())){

							// Acualizamos la variable minDist y la parcela
							minDist = dist;
							nearestSameNumberParcela = tempParcela;
							nearestSameNumberSnappedCoor = tempSnappedCoor;
						}
					}
				}
			}

			ShapeParcela finalParcel = null;
			Coordinate   finalCoord = null;

			// Una vez que ya tenemos las 3 parcelas
			if (nearestParcela != null){

				if (nearestPairParcela != null){

					if (nearestSameNumberParcela != null){

						// Si se han encontrado las 3 pero son la misma
						if (nearestPairParcela.equals(nearestParcela) && nearestPairParcela.equals(nearestSameNumberParcela)){
							finalCoord  = nearestSnappedCoor;
							finalParcel = nearestParcela;
						}
						// Si se han encontrado las 3 pero la par/impar y mismo numero son iguales y la mas cercana es distinta
						// Coger la mismo numero ya que sera que esta mas cerca del otro lado de la calle
						else { 
							if (nearestPairParcela.equals(nearestSameNumberParcela) && !nearestPairParcela.equals(nearestParcela)){
								finalCoord  = nearestSameNumberSnappedCoor;
								finalParcel = nearestSameNumberParcela;
							} else {
								// Si se han encontrado las 3 pero la del numero igual es distinta 
								// Se comprueba si la del numero igual esta muy lejos para que no sea una con un mismo numero de otra calle
								if (nearestPairParcela.equals(nearestParcela) && !nearestPairParcela.equals(nearestSameNumberParcela)){
									// Si esta a menos de 20metros supondremos que es a la que deberia ir
									if (shapeTex.getGeometry().getCoordinate().distance(nearestSameNumberSnappedCoor) <= 0.00002){
										finalCoord  = nearestSameNumberSnappedCoor;										
										finalParcel = nearestSameNumberParcela;
									}
									// Si esta mas lejos quiere decir que no es de esa calle
									// Coger la par/impar ya que sera que no hay parcela con ese numero
									else{
										finalCoord  = nearestPairSnappedCoor;
										finalParcel = nearestPairParcela;
									}
								}
								// Si se han encontrado las 3 y las 3 son distintas
								// Comparamos la distancia a la que esta la del mismo numero y si esta a mas de 20metros cogemos la par/impar
								else{
									if (shapeTex.getGeometry().getCoordinate().distance(nearestSameNumberSnappedCoor) <= 0.00002){
										finalCoord  = nearestSameNumberSnappedCoor;
										finalParcel = nearestSameNumberParcela;
									}
									else {
										finalCoord  = nearestPairSnappedCoor;
										finalParcel = nearestPairParcela;
									}
								}
							}
						}
					}
					// No existe sameNumberParcela
					else{
						// Si se han encontrado solo estas dos pero son la misma
						if (nearestPairParcela.equals(nearestParcela)){
							finalCoord  = nearestSnappedCoor;
							finalParcel = nearestParcela;
						}
						// Si se han encontrado las dos pero son distintas (se deduce que la par/impar estara algo mas lejos)
						// Coger la par/impar ya que la mas cercana sera la de enfrente en la calle
						else{
							finalCoord  = nearestPairSnappedCoor;
							finalParcel = nearestPairParcela;
						}
					}
				}
				// Solo se ha encontrado la mas cercana
				else{
					finalCoord  = nearestSnappedCoor;
					finalParcel = nearestParcela;
				}

				// Actualizamos la coordenada de la geometria del portal
				((ShapeElemtex)shapeTex).setCoor(finalCoord);
				// Anadimos el portal a su parcela
				finalParcel.addEntrance((ShapeElemtex) shapeTex);
				// Modificamos el codigo de masa para que su nodo se cree en el mismo Map que el de
				// su parcela, sino al imprimir no lo encontrara
				shapeTex.setCodigoMasa(finalParcel.getCodigoMasa());
				// Lo borramos de la lista de shapes porque ya esta asignado a una parcela
				it.remove();
			}
			// No se han encontrado parcelas para ese portal
			else{
				shapeTex.getAttributes().addAttribute("FIXME", "FIXME");
			}
		}
		System.out.println("["+new Timestamp(new Date().getTime())+"]\tTerminado.                                         ");
		return shapes;
	}


	/** Devuelve el shape de parcela que toca el punto indicado
	 * @param shapesTotales Lista de shapes original
	 * @param coorPoint coordenada del elemtex del portal tal y como esta en catastro
	 * @param coorSnapped coordenada movida a la linea de la parcela
	 * @return Shape que coincide
	 */
	public Shape getUnderlyingParcela(HashMap <String, List<Shape>> shapesTotales, Coordinate coorPoint, Coordinate coorSnapped){

		// Coordinate[] pts = DistanceOp.nearestPoints(poly, outsidePoint);
		// Creamos la factoria para crear objetos de GeoTools (hay otra factoria pero falla)
		// com.vividsolutions.jts.geom.GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
		GeometryFactory gf = new GeometryFactory();


		// Con el punto original del portal y el punto pegado a la parcela sacamos el simetrico
		// con respecto del borde de la parcela para tener un punto sobre ella
		// Puede ser el punto inicial del portal (que a veces esta sobre la parcela) o
		// el nuevo punto que calculamos al sacar el simetrico
		Coordinate insideCoor = new Coordinate(coorSnapped.x+(coorSnapped.x-coorPoint.x),
				coorSnapped.y+(coorSnapped.y-coorPoint.y),
				0);

		for (String key : shapesTotales.keySet())
			for (Shape s: shapesTotales.get(key))

				if (s instanceof ShapeParcela && s.getGeometry() != null){

					// Cogemos el outer de la parcela que esta en la posicion[0]
					Geometry parcelaOuter = s.getGeometry().getGeometryN(0);

					// Si cumple lo anadimos
					if (parcelaOuter.contains(gf.createPoint(insideCoor)) || parcelaOuter.contains(gf.createPoint(coorPoint))){
						return s;
					}
				}

		return null;
	}


	/** Comprueba si una relacion no tiene datos relevantes y la elimina.
	 * Antes se hacia solo a la hora de imprimir las relaciones pero para entonces las vias ya
	 * estaban simplificadas y con tags cambiados.
	 * Ahora se hace al leer las geometrias (hasRelevantAttributesInternally) y antes de imprimir
	 * (hasRelevantAttributes)
	 * @param utils Clase Utils de Cat2Osm
	 */
	public void deleteNoTagsShapes(String key, List<Shape> shapes){

		Iterator<Shape> it = shapes.iterator();
		while(it.hasNext()){

			Shape shape = it.next();

			if(shape instanceof ShapeParent){
				boolean hasData = false;
				if (null != ((ShapeParent) shape).getSubshapes())
					for(Shape subshape : ((ShapeParent) shape).getSubshapes()){
						hasData = hasData || subshape.hasRelevantAttributesInternally();
					}

				if(!hasData && !shape.hasRelevantAttributesInternally())
					it.remove();

			} else if(shape == null || !shape.hasRelevantAttributesInternally()){
				it.remove();
			}
		}
	}


	/** Une todos los shapes que compartan algun nodo
	 * @param shapes Lista de shapes
	 * @return Lista de shapes
	 * @throws InterruptedException
	 */
	public List<Shape> joinLinearElements(String key, List<Shape> shapes) throws InterruptedException{

		// Variables para el calculo del tiempo estimado
		System.out.print("["+new Timestamp(new Date().getTime())+"]\tProgreso = 0%. Estimando tiempo restante...\r");
		int bar = 0;
		int pos = 0;
		long timeElapsed = 0;
		float size = shapes.size();
		long time = System.currentTimeMillis();


		for(int x = 0; x < shapes.size(); x++){

			// Codigo para el calculo del tiempo estimado
			int progress = (int) ((pos++/size)*100);
			if (bar != progress){
				timeElapsed = (timeElapsed+(100-progress)*(System.currentTimeMillis()-time)/1000)/2;
				long hor = Math.round((timeElapsed/3600));
				long min = Math.round((timeElapsed-3600*hor)/60);
				long seg = Math.round((timeElapsed-3600*hor-60*min));

				System.out.print("["+new Timestamp(new Date().getTime())+"]\tProgreso = "+progress+"%. Tiempo restante estimado = "+hor+" horas, "+min+" minutos, "+seg+" segundos.\r");
				bar = progress;
				time = System.currentTimeMillis();
			}

			Shape shape1 = shapes.get(x);

			Iterator<Shape> it = shapes.iterator();
			while(it.hasNext()){

				Shape shape2 = it.next();

				// Geometria resultante de la union
				Geometry newGeom = null;

				// Comprobamos si se tocan
				if(shape1 != shape2){

					newGeom = Cat2OsmUtils.connectTwoLines(shape1.getGeometry(), shape2.getGeometry());
					if (newGeom != null){
						// Actualizamos el shape1
						shape1.setGeometry(newGeom);
						shape1.getAttributes().addAll(shape2.getAttributes().asHashMap());
						shape1.setFechaConstru(shape2.getFechaConstru());

						// Borramos el shape2
						it.remove();
						x = Math.max(0, x--);
					}
				}
			}
		}
		System.out.println("["+new Timestamp(new Date().getTime())+"]\tTerminado.");
		return shapes;
	}


	/** Concatena los 3 archivos, Nodos + Ways + Relations y lo deja en el OutOsm.
	 * @param oF Ruta donde esta el archivo final
	 * @param tF Ruta donde estan los archivos temporadles (nodos, ways y relations)
	 * @throws IOException
	 */
	public void juntarFilesTemporales(String key, String folder, String filename, BufferedWriter outOsmGlobal) throws IOException{

		String path = Config.get("ResultPath") + File.separatorChar + Config.get("ResultFileName");

		// Borrar archivo con el mismo nombre si existe, porque sino concatenaria el nuevo
		new File(path + File.separatorChar + folder + File.separatorChar + filename +".osm").delete();
		new File(path + File.separatorChar + folder + File.separatorChar + filename +".osm.gz").delete();

		// Archivo al que se le concatenan los nodos, ways y relations
		String fstreamOsm = path + File.separatorChar + folder + File.separatorChar + filename + ".osm.gz";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outOsm = new BufferedWriter( new OutputStreamWriter (new GZIPOutputStream(new FileOutputStream(fstreamOsm)), "UTF-8"));

		// Juntamos los archivos en uno, al de los nodos le concatenamos el de ways y el de relations
		// Cabecera del archivo Osm
		outOsm.write("<?xml version='1.0' encoding='UTF-8'?>");outOsm.newLine();
		outOsm.write("<osm version=\"0.6\" generator=\"cat2osm-"+VERSION+"\">");outOsm.newLine();

		// Concatenamos todos los archivos
		String str;

		if (new File(path + File.separatorChar + folder + File.separatorChar + Config.get("ResultFileName") + "-" + key + "tempNodes.osm").exists())
		{
			BufferedReader inNodes = new BufferedReader(new FileReader(path + File.separatorChar + folder + File.separatorChar +Config.get("ResultFileName") + "-" + key + "tempNodes.osm"));
			while ((str = inNodes.readLine()) != null){
				outOsm.write(str);
				outOsm.newLine();

				outOsmGlobal.write(str);
				outOsmGlobal.newLine();
			}
			inNodes.close();
		}

		if (new File(path + File.separatorChar + folder + File.separatorChar + Config.get("ResultFileName") + "-" + key + "tempWays.osm").exists())
		{
			BufferedReader inWays = new BufferedReader(new FileReader(path + File.separatorChar + folder + File.separatorChar + Config.get("ResultFileName") + "-" + key + "tempWays.osm"));
			while ((str = inWays.readLine()) != null){
				outOsm.write(str);
				outOsm.newLine();

				outOsmGlobal.write(str);
				outOsmGlobal.newLine();
			}
			inWays.close();
		}

		if (new File(path + File.separatorChar + folder + File.separatorChar + Config.get("ResultFileName") + "-" + key + "tempRelations.osm").exists())
		{
			BufferedReader inRelations = new BufferedReader(new FileReader(path + File.separatorChar + folder + File.separatorChar + Config.get("ResultFileName") + "-" + key + "tempRelations.osm"));
			while ((str = inRelations.readLine()) != null){
				outOsm.write(str);
				outOsm.newLine();

				outOsmGlobal.write(str);
				outOsmGlobal.newLine();
			}
			inRelations.close();
		}
		outOsm.write("</osm>");outOsm.newLine();

		outOsm.close();

		boolean borrado = true;
		borrado = borrado && (new File(path + File.separatorChar + folder + File.separatorChar + Config.get("ResultFileName") + "-" + key + "tempNodes.osm")).delete();
		borrado = borrado && (new File(path + File.separatorChar + folder + File.separatorChar + Config.get("ResultFileName") + "-" + key + "tempWays.osm")).delete();
		borrado = borrado && (new File(path + File.separatorChar + folder + File.separatorChar + Config.get("ResultFileName") + "-" + key + "tempRelations.osm")).delete();

		if (!borrado)
			System.out.println("["+new Timestamp(new Date().getTime())+"]\tNO se pudo borrar alguno de los archivos temporales." +
					" Estos estar��n en la carpeta "+ path + File.separatorChar + folder +".");

	}


	public void printResults(String key, String folder, List<Shape> shapes) throws IOException{

		// Comprobar si existe el directorio para guardar los archivos
		File dir = new File(Config.get("ResultPath") + File.separatorChar + Config.get("ResultFileName") + File.separatorChar + folder);
		if (!dir.exists()) 
		{
			try                { dir.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}

		// Archivo temporal para escribir relations
		String fstreamRelations = Config.get("ResultPath") + File.separatorChar + Config.get("ResultFileName") + File.separatorChar + folder + File.separatorChar + Config.get("ResultFileName") + "-" + key + "tempRelations.osm";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outRelations = new BufferedWriter(new OutputStreamWriter (new FileOutputStream(fstreamRelations), "UTF-8"));

		// Archivo temporal para escribir los ways
		String fstreamWays = Config.get("ResultPath") + File.separatorChar + Config.get("ResultFileName") + File.separatorChar + folder + File.separatorChar + Config.get("ResultFileName") + "-" + key +"tempWays.osm";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outWays = new BufferedWriter(new OutputStreamWriter (new FileOutputStream(fstreamWays), "UTF-8"));

		// Archivo temporal para escribir los nodos
		String fstreamNodes = Config.get("ResultPath") + File.separatorChar + Config.get("ResultFileName") + File.separatorChar + folder + File.separatorChar + Config.get("ResultFileName") + "-" + key+ "tempNodes.osm";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outNodes = new BufferedWriter(new OutputStreamWriter (new FileOutputStream(fstreamNodes), "UTF-8"));

		// Recorremos todos los shapes
		for(Shape shape : shapes){
			printShape(key, shape, outNodes, outWays, outRelations);
		}

		outRelations.close();
		outWays.close();
		outNodes.close();

	}

	// Imprimir un shape
	@SuppressWarnings("unchecked")
	public void printShape(String key, Shape shape, BufferedWriter outNodes, BufferedWriter outWays, BufferedWriter outRelations) throws IOException{

		switch(shape.getClass().getName()){

		// Los shapeParcela, imprimen las entradas
		case "ShapeParcela":{
			
			if( ((ShapeParcela)shape).getEntrances() != null)
				for(Shape subshape : ((ShapeParcela)shape).getEntrances())
					printShape(key, subshape, outNodes, outWays, outRelations);
			// Continua
		}
		
		// Los shapes que son padres imprimen sus subshapes y luego su propia geometria si fuese el caso
		case "ShapeConstruExterior":
		case "ShapeMasa":{

			if( ((ShapeParent)shape).getSubshapes() != null)
				for(Shape subshape : ((ShapeParent)shape).getSubshapes())
					printShape(key, subshape, outNodes, outWays, outRelations);
			// Continua
		}

		case "ShapeConstruPart":
		case "ShapeSubparce":{

			if(!shape.checkBuildingDate(Long.parseLong(Config.get("FechaConstruDesde")), Long.parseLong(Config.get("FechaConstruHasta"))))
				break;

			// Empezamos comprobando la relation
			RelationOsm relation = (RelationOsm) utils.getKeyFromValue( (Map<String, Map <Object, Long>>) ((Object)utils.getTotalRelations()), key, shape.getRelationId());

			if (relation != null){

				relation.getIds().remove(null);

				// Si no tiene ids no se imprime
				if (relation.getIds().size()<1){
					System.out.println("["+new Timestamp(new Date().getTime())+"]\tRelation id="+ shape.getRelationId() +" con menos de un way. No se imprimir��.");
					break;
				}

				outRelations.write(relation.printRelation(shape.getRelationId(), key, outNodes, outWays, outRelations, utils, null));
			}		
			break;
		}

		case "ShapeElemlin":
		case "ShapeEjes":

			// Estos elementos no tienen relation
			// solamente uno o varios ways y sus nodos

			if(!shape.checkBuildingDate(Long.parseLong(Config.get("FechaConstruDesde")), Long.parseLong(Config.get("FechaConstruHasta"))))
				break;

			long wayId = shape.getWays().get(0);

			WayOsm way = (WayOsm) utils.getKeyFromValue( (Map<String, Map <Object, Long>>) ((Object)utils.getTotalWays()), key, wayId);

			if (way != null){
				outWays.write(way.printWay(wayId, key, outNodes, utils, null));
			}

			break;

		case "ShapeElempun":
		case "ShapeElemtex":

			// Estos elementos no tienen relation ni way,
			// solamente son un unico punto

			NodeOsm node = (NodeOsm) utils.getKeyFromValue( (Map<String, Map <Object, Long>>) ((Object)utils.getTotalNodes()), key, shape.getNodesIds(0).get(0));

			if (node != null){
				outNodes.write(node.printNode(shape.getNodesIds(0).get(0))); outNodes.newLine();
			}
			break;

		default:
			System.out.println("["+new Timestamp(new Date().getTime())+"]\tNombre de clase inesperado al imprimir" +
					"shapes : "+shape.getClass().getName());
		}
	}


	// Los shapes de parcela urbana van a tener unos subshapes que seran las construcciones
	// Se sacan de la lista de shapes las que coincidan con una parcela y se meten en su parcela.
	// Lo mismo con las masas rusticas, que almacenaran dentro sus subparcelas (ya que de la informacion
	// rustica no interesan las parcelas ni sus direcciones, solo la union de todas las geometrias segun
	// sus cultivos).
	// Esto sirve para luego imprimir los datos, para que elementos hijo no tengan repetidos los
	// tags del padre. (Como seria el caso de que un building tuviese el codigo postal o direccion
	// si ya su parcela los tiene).
	// Las construcciones o subparcelas que no tengan parcela padre quedaran en la lista y se imprimi
	// ran de forma normal como un elemento independiente.
	public void createHyerarchy(String key, List<Shape> shapes) {

		for(int x = 0; x < shapes.size(); x++){

			Shape shape = shapes.get(x);

			// Si encontramos una PARCELA URBANA
			if(shape != null && shape instanceof ShapeParcela && "UR".equals(shape.getTipo())){

				// Leemos su referencia catastral
				String refCat = shape.getRefCat();

				// Comparamos con las demas shapes de la lista
				for(int y = 0; y < shapes.size(); y++){

					Shape subshape = shapes.get(y);

					// Si una que debe ser hija coincide
					// (Las construExterior en este momento no existe aun, estas se crean dentro de la
					// ShapeParcela de forma automatica)
					if(x != y && subshape != null && 
							(subshape instanceof ShapeSubparce || subshape instanceof ShapeConstruPart) && 
							subshape.getRefCat().equals(refCat)){

						// Se creara una ShapeConstruExterior para meterla dentro. Y dentro
						// de la parcela se metera la COnstruExterior
						// Parcela > Exterior > Part
						// Para ello el addSubshape de parcela esta sobreescrito
						((ShapeParcela) shape).addSubshape((ShapePolygonal) subshape);
						// INTRODUCIMOS NULL PARA NO ALTERAR EL ORDEN DE LOS ELEMENTOS DE LA LISTA
						// DESPUES LOS BORRAMOS
						shapes.set(y, null);
					}
				}

				// Coge el destino o uso de mayor area de esa parcela y crea los attributes
				// para ella y sus subshapes
				((ShapeParent) shape).createAttributesFromUsoDestino();

				// Intenta unir todos los subshapes con los mismos tags en uno
				((ShapeParent) shape).joinSubshapes(true);
				
				// Pasar los atributos de direccion postal de la parcela a sus entradas
				if ( ((ShapeParcela) shape).getEntrances() != null){
					for(ShapeElemtex entrance : ((ShapeParcela) shape).getEntrances()){
						entrance.getAttributes().addAttribute("addr:street", shape.getAttributes().getValue("addr:street"));
						entrance.getAttributes().addAttribute("addr:postcode", shape.getAttributes().getValue("addr:postcode"));
					}
					// Y borramos esos tags de la parcela sólo en este caso, si la parcela
                                        // no tiene entradas los dejamos igual^M
					shape.getAttributes().removeAttribute("addr:street");
					shape.getAttributes().removeAttribute("addr:postcode");
				}
				
			} else 
				// Si encontramos una MASA RUSTICA 
				// (deberia haber solo una en la lista de shapes, ya que son por una unica masa)
				if(shape != null && shape instanceof ShapeMasa && "RU".equals(shape.getTipo())){

					// Leemos su codigo de masa
					String masa = shape.getCodigoMasa();

					// Comparamos con las demas shapes de la lista
					for(int y = 0; y < shapes.size(); y++){

						Shape subshape = shapes.get(y);

						// Si encontramos una PARCELA RUSTICA
						if(x != y && null != subshape &&
								subshape instanceof ShapeParcela &&
								"RU".equals(subshape.getTipo())){

							// Leemos su referencia catastral
							String refCat = subshape.getRefCat();

							// Comparamos con las demas shapes de la lista
							for(int z = 0; z < shapes.size(); z++){

								Shape subsubshape = shapes.get(z);

								// Si encontramos una construccion
								if(x != y && y != z && null != subsubshape && 
										subsubshape instanceof ShapeConstruPart && 
										subsubshape.getRefCat().equals(refCat)){

									// Se mete dentro de su parcela padre
									((ShapeParent) subshape).addSubshape((ShapePolygonal) subsubshape);

									// INTRODUCIMOS NULL PARA NO ALTERAR EL ORDEN DE LOS ELEMENTOS DE LA LISTA
									// DESPUES LOS BORRAMOS
									shapes.set(z, null);
								}
							}

							// Coge el destino o uso de mayor area de esa parcela y crea los attributes
							// para ella y sus subshapes
							((ShapeParent) subshape).createAttributesFromUsoDestino();
						}

						// Si encontramos una SUBPARCELA RUSTICA
						if(x != y && null != subshape &&
								subshape instanceof ShapeSubparce &&
								subshape.getCodigoMasa().equals(masa)){

							// Se mete dentro de su parcela padre
							((ShapeParent) shape).addSubshape((ShapePolygonal) subshape);

							// INTRODUCIMOS NULL PARA NO ALTERAR EL ORDEN DE LOS ELEMENTOS DE LA LISTA
							// DESPUES LOS BORRAMOS
							shapes.set(y, null);
						}
					}
					
					// Si no han pedido que separemos las parcelas rusticas
					if(Config.get("SplitRU").equals("0")){
						((ShapeParent) shape).joinSubshapes(false);
					}
				}
		}
		// Eliminar los null que hemos introducido
		Iterator<Shape> it = shapes.iterator();
		while(it.hasNext())
			if(it.next() == null)
				it.remove();
	}


	// Convertir los shapes a elementos de OSM. De esta manera luego a la hora de imprimir se
	// reutilizan elementos OSM que se compartan entre varios shapes
	public void convertShapes2OSM(List<Shape> shapes) {

		Iterator<Shape> it = shapes.iterator();
		while(it.hasNext()){
			Shape shape = it.next();
			shape.toOSM(utils, null);
		}
	}
	
	// Simplificar las geometrias OSM
	// No se pueden simplificar antes porque hay que reutilizar nodos y geometrias superpuestas
	public void simplifyOSM(Cat2OsmUtils utils, List<Shape> shapes, String key) {
	}

}
