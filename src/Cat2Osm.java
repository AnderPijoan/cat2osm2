import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
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
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;


public class Cat2Osm {

	public static final String VERSION = "2013-01-16";
	public static Cat2OsmUtils utils;

	private final double MINDIST = 0.00008; // Distancia minima para busqueda de portales ~ 80 metros

	/** Constructor
	 * @param utils Clase utils en la que se almacenan los nodos, ways y relaciones 
	 * y tiene funciones para manejarlos
	 */
	public Cat2Osm (Cat2OsmUtils utils){
		Cat2Osm.utils = utils;
	}


	/** Busca en la lista de shapes los que coincidan con la ref catastral
	 * @param codigo Codigo de masa
	 * @param ref referencia catastral a buscar
	 * @returns List<Shape> lista de shapes que coinciden                    
	 */
	private static List<Shape> buscarRefCat(List<Shape> shapes, String ref){

		List<Shape> shapeList = null;

		for(Shape shape : shapes) 
			if (shape != null && shape.getRefCat() != null && shape.getRefCat().equals(ref)){
				if (shapeList == null)
					shapeList = new ArrayList<Shape>();
				shapeList.add(shape);
			}

		return shapeList;
	}


	/** Busca en la lista de shapes los que coincidan con el codigo de subparce
	 * @param shapes lista de shapes que han coincidido con la refCat para buscar en ella las subparcelas
	 * @param subparce Codigo de subparcela para obtener la que corresponda
	 * @returns List<Shape> lista de shapes que coinciden                    
	 */
	private static List<Shape> buscarSubparce(List<Shape> shapes, String subparce){
		List<Shape> shapeList = new ArrayList<Shape>();

		for(Shape shape : shapes) 
			if (shape != null && shape instanceof ShapeSubparce)
				if (((ShapeSubparce) shape).getSubparce().equals(subparce))
					shapeList.add(shape);

		return shapeList;
	}


	/** Busca en la lista de shapes los que sean parcelas
	 * @param shapes lista de shapes
	 * @return List<Shape> lista de shapes que coinciden  
	 */
	private static List<Shape> buscarParce(List<Shape> shapes){
		List<Shape> shapeList = new ArrayList<Shape>();

		for(Shape shape : shapes) 
			if (shape != null && shape instanceof ShapeParcela)
				shapeList.add((ShapeParcela) shape);

		return shapeList;
	}


	/** Los elementos textuales traen informacion con los numeros de portal pero sin datos de la parcela ni unidos a ellas
	 * Con esto, sabiendo el numero de portal buscamos la parcela mas cercana con ese numero y le pasamos los tags al elemento
	 * textual que es un punto
	 * @param shapes Lista de shapes original
	 * @return lista de shapes con los tags modificados, si es null es que no habia portales
	 */
	@SuppressWarnings("unchecked")
	public HashMap <String, List<Shape>> calcularEntradas(HashMap <String, List<Shape>> shapes){

		// Si no se ha leido ningun portal
		if (shapes.get("ELEMTEX-189401") == null)
			return null;

		// Creamos la factoria para crear objetos de GeoTools (hay otra factoria pero falla)
		// com.vividsolutions.jts.geom.GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
		GeometryFactory gf = new GeometryFactory();

		// Variabbles para el calculo del tiempo estimado
		System.out.print("["+new Timestamp(new Date().getTime())+"]    Progreso = 0%. Estimando tiempo restante...\r");
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

				System.out.print("["+new Timestamp(new Date().getTime())+"]    Progreso = "+progress+"%. Tiempo restante estimado = "+hor+" horas, "+min+" minutos, "+seg+" segundos.\r");
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

			double minDist = MINDIST; // Distancia minima ~ 80 metros

			// Creamos el punto de busqueda con la coordenada del punto y la expandimos
			// en la distancia minima para obtener
			// una linea de desplazamiento para tocar la parcela
			Envelope search = new Envelope(point.getCoordinate());
			search.expandBy(MINDIST);

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

			minDist = MINDIST; // Distancia minima ~ 80 metros

			// Creamos el punto de busqueda con la coordenada del punto y la expandimos
			// en la distancia minima para obtener
			// una linea de desplazamiento para tocar la parcela
			search = new Envelope(point.getCoordinate());
			search.expandBy(MINDIST);

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
						String housenumber = tempParcela.getAttribute("addr:housenumber");		

						if (housenumber != null && esNumero(housenumber) && esNumero(((ShapeElemtex)shapeTex).getRotulo().trim()) &&
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

			minDist = MINDIST; // Distancia minima ~ 80 metros

			// Creamos el punto de busqueda con la coordenada del punto y la expandimos
			// en la distancia minima para obtener
			// una linea de desplazamiento para tocar la parcela
			search = new Envelope(point.getCoordinate());
			search.expandBy(MINDIST);

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
						String housenumber = tempParcela.getAttribute("addr:housenumber");		

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
				shapeTex.addAttribute("FIXME", "FIXME");
			}
		}

		System.out.println("["+new Timestamp(new Date().getTime())+"] Terminado.");
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
	 * estaban simplificadas pensando que estas relaciones tenian tags
	 * @param utils Clase Utils de Cat2Osm
	 */
	public void simplificarShapesSinTags(String key, List<Shape> shapes){

		Iterator<Shape> it = shapes.iterator();
		while(it.hasNext()){

			Shape shape = it.next();

			if(shape instanceof ShapeParcela){
				boolean hasData = false;
				if (null != ((ShapeParcela) shape).getSubshapes())
					for(ShapePolygonal subshape : ((ShapeParcela) shape).getSubshapes()){
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
		System.out.print("["+new Timestamp(new Date().getTime())+"]    Progreso = 0%. Estimando tiempo restante...\r");
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

				System.out.print("["+new Timestamp(new Date().getTime())+"]    Progreso = "+progress+"%. Tiempo restante estimado = "+hor+" horas, "+min+" minutos, "+seg+" segundos.\r");
				bar = progress;
				time = System.currentTimeMillis();
			}

			Shape shape1 = shapes.get(x);

			Iterator<Shape> it = shapes.iterator();
			while(it.hasNext()){

				Shape shape2 = it.next();

				// Geometria resultante de la union
				Geometry newGeom = null;
				boolean touches = true;

				// Comprobamos si se tocan
				if(shape1 != shape2){

					switch(Cat2OsmUtils.areConnected(shape1.getGeometry(), shape2.getGeometry())){
					case 1: newGeom = shape1.getGeometry().union(shape2.getGeometry()); break;
					case 2: newGeom = shape2.getGeometry().union(shape1.getGeometry()); break;
					case 3: newGeom = shape1.getGeometry().reverse().union(shape2.getGeometry()); break;
					case 4: newGeom = shape1.getGeometry().union(shape2.getGeometry().reverse()); break;
					default : touches = false;
					}

					if (touches){
						// Actualizamos el shape1
						shape1.setGeometry(newGeom);
						shape1.addAttributes(shape2.getAttributes());
						shape1.setFechaConstru(shape2.getFechaConstru());

						// Borramos el shape2
						it.remove();
						x = Math.max(0, x--);
					}
				}
			}
		}
		System.out.println("["+new Timestamp(new Date().getTime())+"]    Terminado.");
		return shapes;
	}


	/** Concatena los 3 archivos, Nodos + Ways + Relations y lo deja en el OutOsm.
	 * @param oF Ruta donde esta el archivo final
	 * @param tF Ruta donde estan los archivos temporadles (nodos, ways y relations)
	 * @throws IOException
	 */
	public void juntarFilesTemporales(String key, String folder, String filename, BufferedWriter outOsmGlobal) throws IOException{

		String path = Config.get("ResultPath") + "/" + Config.get("ResultFileName");

		// Borrar archivo con el mismo nombre si existe, porque sino concatenaria el nuevo
		new File(path + "/" + folder + "/" + filename +".osm").delete();
		new File(path + "/" + folder + "/" + filename +".osm.gz").delete();

		// Archivo al que se le concatenan los nodos, ways y relations
		String fstreamOsm = path + "/" + folder + "/" + filename + ".osm.gz";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outOsm = new BufferedWriter( new OutputStreamWriter (new GZIPOutputStream(new FileOutputStream(fstreamOsm)), "UTF-8"));

		// Juntamos los archivos en uno, al de los nodos le concatenamos el de ways y el de relations
		// Cabecera del archivo Osm
		outOsm.write("<?xml version='1.0' encoding='UTF-8'?>");outOsm.newLine();
		outOsm.write("<osm version=\"0.6\" generator=\"cat2osm-"+VERSION+"\">");outOsm.newLine();	

		// Concatenamos todos los archivos
		String str;

		if (new File(path + "/" + folder + "/" + Config.get("ResultFileName") + "-" + key + "tempNodes.osm").exists())
		{
			BufferedReader inNodes = new BufferedReader(new FileReader(path + "/" + folder + "/" +Config.get("ResultFileName") + "-" + key + "tempNodes.osm"));
			while ((str = inNodes.readLine()) != null){
				outOsm.write(str);
				outOsm.newLine();

				outOsmGlobal.write(str);
				outOsmGlobal.newLine();
			}
			inNodes.close();
		}

		if (new File(path + "/" + folder + "/" + Config.get("ResultFileName") + "-" + key + "tempWays.osm").exists())
		{
			BufferedReader inWays = new BufferedReader(new FileReader(path + "/" + folder + "/" + Config.get("ResultFileName") + "-" + key + "tempWays.osm"));
			while ((str = inWays.readLine()) != null){
				outOsm.write(str);
				outOsm.newLine();

				outOsmGlobal.write(str);
				outOsmGlobal.newLine();
			}
			inWays.close();
		}

		if (new File(path + "/" + folder + "/" + Config.get("ResultFileName") + "-" + key + "tempRelations.osm").exists())
		{
			BufferedReader inRelations = new BufferedReader(new FileReader(path + "/" + folder + "/" + Config.get("ResultFileName") + "-" + key + "tempRelations.osm"));
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
		borrado = borrado && (new File(path + "/" + folder + "/" + Config.get("ResultFileName") + "-" + key + "tempNodes.osm")).delete();
		borrado = borrado && (new File(path + "/" + folder + "/" + Config.get("ResultFileName") + "-" + key + "tempWays.osm")).delete();
		borrado = borrado && (new File(path + "/" + folder + "/" + Config.get("ResultFileName") + "-" + key + "tempRelations.osm")).delete();

		if (!borrado)
			System.out.println("["+new Timestamp(new Date().getTime())+"] NO se pudo borrar alguno de los archivos temporales." +
					" Estos estarán en la carpeta "+ path + "/" + folder +".");

	}


	public void printResults(String key, String folder, List<Shape> shapes) throws IOException{

		// Comprobar si existe el directorio para guardar los archivos
		File dir = new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + folder);
		if (!dir.exists()) 
		{
			try                { dir.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}

		// Archivo temporal para escribir relations
		String fstreamRelations = Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + folder + "/" + Config.get("ResultFileName") + "-" + key + "tempRelations.osm";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outRelations = new BufferedWriter(new OutputStreamWriter (new FileOutputStream(fstreamRelations), "UTF-8"));

		// Archivo temporal para escribir los ways
		String fstreamWays = Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + folder + "/" + Config.get("ResultFileName") + "-" + key +"tempWays.osm";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outWays = new BufferedWriter(new OutputStreamWriter (new FileOutputStream(fstreamWays), "UTF-8"));

		// Archivo temporal para escribir los nodos
		String fstreamNodes = Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + folder + "/" + Config.get("ResultFileName") + "-" + key+ "tempNodes.osm";
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

		case "ShapeParcela":
		case "ShapeMasa":{

			if( null != ((ShapeParent)shape).getSubshapes())
				for(Shape subshape : ((ShapeParent)shape).getSubshapes())
					printShape(key, subshape, outNodes, outWays, outRelations);

			// Continua
		}

		case "ShapeConstru":
		case "ShapeSubparce":{

			if(!shape.checkBuildingDate(Long.parseLong(Config.get("FechaConstruDesde")), Long.parseLong(Config.get("FechaConstruHasta"))))
				break;

			// Empezamos comprobando la relation
			RelationOsm relation = (RelationOsm) utils.getKeyFromValue( (Map<String, Map <Object, Long>>) ((Object)utils.getTotalRelations()), key, shape.getRelationId());

			if (relation != null){

				relation.getIds().remove(null);

				// Si no tiene ids no se imprime
				if (relation.getIds().size()<1){
					System.out.println("["+new Timestamp(new Date().getTime())+"]    Relation id="+ shape.getRelationId() +" con menos de un way. No se imprimirá.");
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
			System.out.println("["+new Timestamp(new Date().getTime())+"] Nombre de clase inesperado al imprimir" +
					"shapes : "+shape.getClass().getName());
		}
	}


	/** Lee linea a linea el archivo cat, coge los shapes q coincidan 
	 * con esa referencia catastral y les anade los tags de los registros .cat
	 * @param cat Archivo cat del que lee linea a linea
	 * @param List<Shape> Lista de los elementos shp parseados ya en memoria
	 * @throws IOException
	 */
	public void catParser(String tipo, File cat, HashMap <String, List<Shape>> shapesTotales) throws IOException{

		BufferedReader bufRdr = createCatReader(cat);
		String line = null; // Para cada linea leida del archivo .cat

		int tipoRegistrosBuscar = Integer.parseInt(Config.get("TipoRegistro"));

		// Lectura del archivo .cat
		while((line = bufRdr.readLine()) != null)
		{
			// Parsear la linea leida
			Cat c = catLineParser(line);
			String key = "";

			if (tipo.equals("UR") && c.getRefCatastral() != null) // El codigo de masa son los primeros 5 caracteres
				key = c.getRefCatastral().substring(0, 5).replaceAll("[^\\p{L}\\p{N}]", "") + "-";
			if (tipo.equals("RU") && c.getRefCatastral() != null) // El codigo de masa son los caracteres 6, 7 y 8
				key = c.getRefCatastral().substring(6, 9).replaceAll("[^\\p{L}\\p{N}]", "") + "-";

			if (shapesTotales.get(key) != null && (c.getTipoRegistro() == tipoRegistrosBuscar || tipoRegistrosBuscar == 0)){

				// Obtenemos los shape que coinciden con la referencia catastral de la linea leida
				List <Shape> matches = buscarRefCat(shapesTotales.get(key), c.getRefCatastral());

				if (matches != null)
					switch (c.getTipoRegistro()){

					// El registro 11 solo se tiene que asignar a parcelas
					case 11:
						matches = buscarParce(matches);
						break;

						// El registro 13 es para bienes inmuebles e incluye la fecha de construccion. Como no
						// hay forma de asociarlo con cada bien inmueble, se la asociamos a toda su referencia
						// catastral, cogiendo la menor de todas, es decir la mas antigua.

						// El registro 14 es para bienes inmuebles pero como no hay forma de relacionarlo
						// se ha hecho que la parcela acumule todos los destinos y usos y al final elija el que mayor
						// area tiene. Ese dato solo se almacena en el shape, luego habra que llamar al metodo
						// que calcule los destinos para pasarlos a las relaciones.
					case 14: 
						matches = buscarParce(matches);
						for (Shape match : matches){
							((ShapeParcela) match).addDestino(c.getUsoDestino(), c.getArea());
						}
						break;

						// El registro 15 es para bienes inmuebles pero como no hay forma de relacionarlo
						// se ha hecho que la parcela acumule todos los destinos y usos y al final elija el que mayor
						// area tiene. Ese dato solo se almacena en el shape, luego habra que llamar al metodo
						// que calcule los destinos para pasarlos a las relaciones.
					case 15:
						matches = buscarParce(matches);
						for (Shape match : matches){
							((ShapeParcela) match).addUso(c.getUsoDestino(), c.getArea());
						}
						break;

						// Para los tipos de registro de subparcelas, buscamos la subparcela concreta para
						// anadirle los atributos
					case 17:
						matches = buscarSubparce(matches, c.getSubparce());
						break;

					}

				// Insertamos los atributos leidos, la fecha de construccion y le indicamos que parsee
				// los usos y destinos para que cree los tags relevantes
				if (matches != null)
					for (Shape shape : matches)
						if (shape != (null) && c.getAttributes() != null){
							shape.addAttributes(c.getAttributes());
							shape.setFechaConstru(c.getFechaConstru());
						}
			}
		}
		bufRdr.close();
	}


	/** Lee linea a linea el archivo cat, coge los registros 14 que son los que tienen uso
	 * de inmuebles y con el punto X e Y del centroide de la parcela que coincide con su referencia
	 * catastral crea nodos con los usos
	 * de los bienes inmuebles
	 * @param cat Archivo cat del que lee linea a linea
	 * @param List<Shape> Lista de los elementos shp parseados ya en memoria
	 * @param t solo sirve para diferenciar del otro metodo
	 * @throws IOException
	 */
	public void catUsosParser(String tipo, File cat, HashMap <String, List<Shape>> shapesTotales) throws IOException{

				BufferedReader bufRdr  = createCatReader(cat);
				String line = null; // Para cada linea leida del archivo .cat
		
				// Lectura del archivo .cat
				while((line = bufRdr.readLine()) != null)
				{
					Cat c = catLineParser(line);
					String key = "";
		
					if (tipo.equals("UR") && c.getRefCatastral() != null) // El codigo de masa son los primeros 5 caracteres
						key = c.getRefCatastral().substring(0, 5).replaceAll("[^\\p{L}\\p{N}]", "") + "-";
					if (tipo.equals("RU") && c.getRefCatastral() != null) // El codigo de masa son los caracteres 6, 7 y 8
						key = c.getRefCatastral().substring(6, 9).replaceAll("[^\\p{L}\\p{N}]", "") + "-";
		
					// Si es registro 14
					if (!"".equals(key) && null != shapesTotales.get(key) && esNumero(line.substring(0,2)) && line.substring(0,2).equals("14")){
		
						// Cogemos las geometrias con esa referencia catastral.
						List<Shape> matches = buscarRefCat(shapesTotales.get(key), c.getRefCatastral());
		
						// Puede que no haya shapes para esa refCatastral
						if (matches != null)
							for (Shape shape : matches)
								if (shape != null && shape instanceof ShapeParcela && shape.getGeometry() != null && !shape.getGeometry().isEmpty()){
		
									List<ShapeAttribute> tags = new ArrayList<ShapeAttribute>();
									
									// Metemos los tags de uso de inmuebles con el numero de inmueble por delante
									for(String[] s : ShapeParcela.destinoParser(c.getUsoDestino())){
										tags.add(new ShapeAttribute(c.getRefCatastral()+":"+c.getNumOrdenConstru()+":"+s[0], s[1]));
									}

									// Anadimos la referencia catastral
									//tags.add(new String[] {"catastro:ref", c.getRefCatastral() + line.substring(44,48)});
									tags.add(new ShapeAttribute(c.getRefCatastral()+":"+c.getNumOrdenConstru()+":addr:floor", line.substring(64,67).trim()));
									
									shape.addAttributes(tags);
								}
					}
				}
				bufRdr.close();
	}


	/** Parsea el archivo .cat y crea los elementos en memoria en un List
	 * @param f Archivo a parsear
	 * @returns List<Cat> Lista de los elementos parseados
	 * @throws IOException 
	 * @see http://www.catastro.meh.es/pdf/formatos_intercambio/catastro_fin_cat_2006.pdf
	 */
	public List<Cat> catParser(File f) throws IOException{

		BufferedReader bufRdr = createCatReader(f);
		String line = null;

		List<Cat> l = new ArrayList<Cat>();

		int tipoRegistro = Integer.parseInt(Config.get("tipoRegistro"));

		while((line = bufRdr.readLine()) != null)
		{
			Cat c = catLineParser(line);
			// No todos los tipos de registros de catastro tienen FechaAlta y FechaBaja
			// Los que no tienen, pasan el filtro
			if ((c.getTipoRegistro() == tipoRegistro || tipoRegistro == 0) )
				l.add(c);
		}
		bufRdr.close();
		return l;
	}

	private BufferedReader createCatReader(File archivoCat) throws IOException {
		InputStream inputStream = new FileInputStream(archivoCat);
		if (archivoCat.getName().toLowerCase().endsWith(".gz")){
			inputStream = new GZIPInputStream(inputStream);
		}
		return new BufferedReader(new InputStreamReader(inputStream, "ISO-8859-15"));
	}


	/** De la lista de shapes con la misma referencia catastral devuelve la mas actual
	 * del periodo indicado.
	 * @param shapes Lista de shapes
	 * @returns El shape que hay para el periodo indicado porque hay shapes que 
	 * tienen distintas versiones a lo largo de los anos
	 */
	public static Shape buscarShapesParaFecha(List<Shape> shapes){

		// Lo habitual es que venga ordenado de mas reciente a mas antiguo
		Shape s = shapes.get(shapes.size()-1);
		long fA = 00000101;
		long fechaHasta = Long.parseLong(Config.get("FechaHasta"));

		for (int x = shapes.size()-1 ; x >= 0 ; x--){

			if (shapes.get(x).getFechaAlta() >= fA && shapes.get(x).getFechaAlta() < fechaHasta && shapes.get(x).getFechaBaja() >= fechaHasta){
				s = shapes.get(x);
				fA = s.getFechaAlta();
			}
		}
		return s;
	}


	/** Parsea la linea del archivo .cat y devuelve un elemento Cat
	 * @param line Linea del archivo .cat
	 * @returns Cat Elemento Cat con todos los campos leidos en la linea
	 * @throws IOException 
	 * @see http://www.catastro.meh.es/pdf/formatos_intercambio/catastro_fin_cat_2006.pdf
	 */
	private static Cat catLineParser(String line) throws IOException{

		Cat c = null;

		if (esNumero(line.substring(0,2)))
			c = new Cat(Integer.parseInt(line.substring(0,2)));
		else
			c = new Cat(0);

		switch(c.getTipoRegistro()){ // Formato de los tipos distintos de registro .CAT
		case 01: {

			/*System.out.println("\nTIPO DE REGISTRO 01: REGISTRO DE CABECERA\n");
			System.out.println("TIPO DE ENTIDAD GENERADORA                  :"+line.substring(2,3));
			System.out.println("CODIGO DE LA ENTIDAD GENERADORA             :"+line.substring(3,12));
			System.out.println("NOMBRE DE LA ENTIDAD GENERADORA             :"+line.substring(12,39));
			System.out.println("FECHA DE GENERACION (AAAAMMDD)              :"+line.substring(39,47)); 
			System.out.println("HORA DE GENERACION (HHMMSS)                 :"+line.substring(47,53));
			System.out.println("TIPO DE FICHERO                             :"+line.substring(53,57));
			System.out.println("DESCRIPCION DEL CONTENIDO DEL FICHERO       :"+line.substring(57,96));
			System.out.println("NOMBRE DE FICHERO                           :"+line.substring(96,117));
			System.out.println("CODIGO DE LA ENTIDAD DESTINATARIA           :"+line.substring(117,120));
			System.out.println("FECHA DE INICIO DEL PERIODO (AAAAMMDD)      :"+line.substring(120,128));
			System.out.println("FECHA DE FIN DEL PERIODO (AAAAMMDD)         :"+line.substring(128,136));
			 */
			break;}
		case 11: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2)); 
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(25,28)));
			//c.addAttribute("BLANCO EXCEPTO INMUEBLES ESPECIALES",line.substring(28,30));
			c.addAttribute("catastro:special",eliminarComillas(line.substring(28,30)));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44)); 
			//c.addAttribute("CODIGO DE PROVINCIA",line.substring(50,52));
			//c.addAttribute("catastro:ref:province",eliminarCerosString(line.substring(50,52)));
			//c.addAttribute("NOMBRE DE PROVINCIA",line.substring(52,77));
			//c.addAttribute("is_in:province",line.substring(52,77));
			//c.addAttribute("CODIGO DE MUNICIPIO",line.substring(77,80));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(77,80)));
			//c.addAttribute("CODIGO DE MUNICIPIO (INE). EXCLUIDO ULTIMO DIGITO DE CONTROL:",line.substring(80,83));
			//c.addAttribute("ine:ref:municipality",eliminarCerosString(line.substring(80,83)));
			//c.addAttribute("NOMBRE DE MUNICIPIO",line.substring(83,123));
			//c.addAttribute("is_in:municipality",eliminarComillas(line.substring(83,123)));
			//c.addAttribute("NOMBRE DE LA ENTIDAD MENOR EN CASO DE EXISTIR",line.substring(123,153));
			//c.addAttribute("CODIGO DE VIA PUBLICA",line.substring(153,158));
			//c.addAttribute("catastro:ref:way",eliminarCerosString(line.substring(153,158)));
			//c.addAttribute("TIPO DE VIA O SIGLA PUBLICA",line.substring(158,163));
			//c.addAttribute("NOMBRE DE VIA PUBLICA",line.substring(163,188));
			c.addAttribute("addr:street",nombreTipoViaParser(line.substring(158,163).trim())+" "+formatearNombreCalle(eliminarComillas(line.substring(163,188).trim())));
			//c.addAttribute("PRIMER NUMERO DE POLICIA",line.substring(188,192));
			//c.addAttribute("PRIMERA LETRA (CARACTER DE DUPLICADO)",line.substring(192,193));
			c.addAttribute("addr:housenumber",eliminarCerosString(line.substring(188,192))+line.substring(192,193).trim());
			//c.addAttribute("SEGUNDO NUMERO DE POLICIA",line.substring(193,197));
			//c.addAttribute("SEGUNDA LETRA (CARACTER DE DUPLICADO)",line.substring(197,198));
			//c.addAttribute("KILOMETRO (3enteros y 2decimales)",line.substring(198,203));
			//c.addAttribute("BLOQUE",line.substring(203,207));
			//c.addAttribute("TEXTO DE DIRECCION NO ESTRUCTURADA",line.substring(215,240));
			c.addAttribute("name",eliminarComillas(line.substring(215,240)));
			//c.addAttribute("CODIGO POSTAL",line.substring(240,245));
//			if (!line.substring(240,245).equals("00000"))
//				c.addAttribute("addr:postcode", line.substring(240,245));
//			if (!line.substring(240,245).isEmpty() && !line.substring(240,245).equals("00000"))
//				c.addAttribute("addr:country","ES");
			//c.addAttribute("DISTRITO MUNICIPAL",line.substring(245,247));
			//c.addAttribute("CODIGO DEL MUNICIPIO ORIGEN EN CASO DE AGREGACION",line.substring(247,250));
			//c.addAttribute("CODIGO DE LA ZONA DE CONCENTRACION",line.substring(250,252));
			//c.addAttribute("CODIGO DE POLIGONO",line.substring(252,255));
			//c.addAttribute("catastro:ref:polygon",eliminarCerosString(line.substring(252,255)));
			//c.addAttribute("CODIGO DE PARCELA",line.substring(255,260));
			//c.addAttribute("CODIGO DE PARAJE",line.substring(260,265));
			//c.addAttribute("NOMBRE DEL PARAJE",line.substring(265,295));
			//c.addAttribute("SUPERFICIE CATASTRAL (metros cuadrados)",line.substring(295,305));
			//c.addAttribute("catastro:surface",eliminarCerosString(line.substring(295,305)));
			//c.addAttribute("SUPERFICIE CONSTRUIDA TOTAL",line.substring(305,312));
			//if (!eliminarCerosString(line.substring(295,305)).equals(eliminarCerosString(line.substring(305,312))))
			//c.addAttribute("catastro:surface:built",eliminarCerosString(line.substring(305,312)));
			//c.addAttribute("SUPERFICIE CONSTRUIDA SOBRE RASANTE",line.substring(312,319));
			//if (!eliminarCerosString(line.substring(295,305)).equals(eliminarCerosString(line.substring(312,319))))
			//c.addAttribute("catastro:surface:overground",eliminarCerosString(line.substring(312,319)));
			//c.addAttribute("SUPERFICIE CUBIERTA",line.substring(319,333));
			//c.addAttribute("COORDENADA X (CON 2 DECIMALES Y SIN SEPARADOR)",line.substring(333,342));
			//c.addAttribute("COORDENADA Y (CON 2 DECIMALES Y SIN SEPARADOR)",line.substring(342,352));
			//c.addAttribute("REFERENCIA CATASTRAL BICE DE LA FINCA",line.substring(581,601));
			//c.addAttribute("catastro:ref:bice",eliminarCerosString(line.substring(581,601)));
			//c.addAttribute("DENOMINACION DEL BICE DE LA FINCA",line.substring(601,666));
			//c.addAttribute("HUSO GEOGRAFICO SRS",line.substring(666,676));

			return c;}
		case 13: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DE MUNICIPIO",line.substring(25,28));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(25,28)));
			//c.addAttribute("CLASE DE LA UNIDAD CONSTRUCTIVA",line.substring(28,30));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("CODIGO DE LA UNIDAD CONSTRUCTIVA",line.substring(44,48));
			if (esNumero(line.substring(44,48)) && Integer.parseInt(line.substring(44,48)) != 0)
				c.setNumOrdenConstru(Integer.parseInt(line.substring(44,48)));
			else
				c.setSubparce(line.substring(44,48));
			//c.addAttribute("CODIGO DE PROVINCIA",line.substring(50,52));
			//c.addAttribute("catastro:ref:province",eliminarCerosString(line.substring(50,52)));
			//c.addAttribute("NOMBRE PROVINCIA",line.substring(52,77));
			//c.addAttribute("is_in_province",line.substring(52,77));
			//c.addAttribute("CODIGO DEL MUNICIPIO DGC",line.substring(77,80));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(77,80)));
			//c.addAttribute("CODIGO DE MUNICIPIO (INE) EXCLUIDO EL ULTIMO DIGITO DE CONTROL",line.substring(80,83));
			//c.addAttribute("ine:ref:municipality",eliminarCerosString(line.substring(80,83)));
			//c.addAttribute("NOMBRE DEL MUNICIPIO",line.substring(83,123));
			//c.addAttribute("is_in:municipality",eliminarComillas(line.substring(83,123)));
			//c.addAttribute("NOMBRE DE LA ENTIDAD MENOR EN CASO DE EXISTIR",line.substring(123,153));
			//c.addAttribute("CODIGO DE VIA PUBLICA DGC",line.substring(153,158));
			//c.addAttribute("catastro:ref:way",eliminarCerosString(line.substring(153,158)));
			//c.addAttribute("TIPO DE VIA O SIBLA PUBLICA",line.substring(158,163));
			//c.addAttribute("NOMBRE DE VIA PUBLICA",line.substring(163,188));
			//c.addAttribute("addr:street",nombreTipoViaParser(line.substring(158,163).trim())+" "+formatearNombreCalle(eliminarComillas(line.substring(163,188).trim())));
			//c.addAttribute("PRIMER NUMERO DE POLICIA",line.substring(188,192));
			//c.addAttribute("addr:housenumber",eliminarCerosString(line.substring(188,192)));
			//c.addAttribute("PRIMERA LETRA (CARACTER DE DUPLICADO)",line.substring(192,193));
			//c.addAttribute("SEGUNDO NUMERO DE POLICIA",line.substring(193,197));
			//c.addAttribute("SEGUNDA LETRA (CARACTER DE DUPLICADO)",line.substring(197,198));
			//c.addAttribute("KILOMETRO (3enteros y 2decimales)",line.substring(198,203));
			//c.addAttribute("TEXTO DE DIRECCION NO ESTRUCTURADA",line.substring(215,240));
			//c.addAttribute("name",eliminarComillas(line.substring(215,240).trim()));
			//c.addAttribute("ANO DE CONSTRUCCION (AAAA)",line.substring(295,299));
			c.setFechaConstru(Long.parseLong(line.substring(295,299)+"0101"));
			//c.addAttribute("INDICADOR DE EXACTITUD DEL ANO DE CONTRUCCION",line.substring(299,300));
			//c.addAttribute("SUPERFICIE DE SUELO OCUPADA POR LA UNIDAD CONSTRUCTIVA",line.substring(300,307));
			//c.addAttribute("catastro:surface",eliminarCerosString(line.substring(300,307)));
			//c.addAttribute("LONGITUD DE FACHADA",line.substring(307,312));
			//c.addAttribute("CODIGO DE UNIDAD CONSTRUCTIVA MATRIZ",line.substring(409,413));

			return c; }

		case 14: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2)); 
			//c.addAttribute("CODIGO DE DELEGACION DEL MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(25,28)));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("NUMERO DE ORDEN DEL ELEMENTO DE CONSTRUCCION",line.substring(44,48));
			if (esNumero(line.substring(44,48)) && Integer.parseInt(line.substring(44,48)) != 0)
				c.setNumOrdenConstru(Integer.parseInt(line.substring(44,48)));
			else
				c.setSubparce(line.substring(44,48));
			//c.addAttribute("NUMERO DE ORDEN DEL BIEN INMUEBLE FISCAL",line.substring(50,54));
			//c.addAttribute("CODIGO DE LA UNIDAD CONSTRUCTIVA A LA QUE ESTA ASOCIADO EL LOCAL",line.substring(54,58));
			//c.addAttribute("BLOQUE",line.substring(58,62));
			//c.addAttribute("ESCALERA",line.substring(62,64));
			//c.addAttribute("PLANTA",line.substring(64,67));
			//c.addAttribute("PUERTA",line.substring(67,70));
			//c.addAttribute("CODIGO DE DESTINO SEGUN CODIFICACION DGC",line.substring(70,73));
			c.setUsoDestino(line.substring(70,73).trim());
			//c.addAttribute("INDICADOR DEL TIPO DE REFORMA O REHABILITACION",line.substring(73,74));
			//c.addAttribute("ANO DE REFORMA EN CASO DE EXISTIR",line.substring(74,78));
			//c.addAttribute("ANO DE ANTIGUEDAD EFECTIVA EN CATASTRO",line.substring(78,82)); 
			//c.addAttribute("INDICADOR DE LOCAL INTERIOR (S/N)",line.substring(82,83));
			//c.addAttribute("SUPERFICIE TOTAL DEL LOCAL A EFECTOS DE CATASTRO",line.substring(83,90));
			if (esNumero(line.substring(83,90).trim()))
				c.setArea(Double.parseDouble(line.substring(83,90).trim()));
			else
				c.setArea((double) 10);
			//c.addAttribute("SUPERFICIA DE PORCHES Y TERRAZAS DEL LOCAL",line.substring(90,97));
			//c.addAttribute("SUPERFICIE IMPUTABLE AL LOCAL SITUADA EN OTRAS PLANTAS",line.substring(97,104));
			//c.addAttribute("TIPOLOGIA CONSTRUCTIVA SEGUN NORMAS TECNICAS DE VALORACION",line.substring(104,109));
			//c.addAttribute("CODIGO DE MODALIDAD DE REPARTO",line.substring(111,114));

			return c;}

		case 15: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(25,28)));
			//c.addAttribute("CLASE DE BIEN INMUEBLE (UR, RU, BI)",line.substring(28,30));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("NUMERO SECUENCIAL DEL BIEN INMUEBLE DENTRO DE LA PARCELA",line.substring(44,48));
			if (esNumero(line.substring(44,48)) && Integer.parseInt(line.substring(44,48)) != 0)
				c.setNumOrdenConstru(Integer.parseInt(line.substring(44,48)));
			else
				c.setSubparce(line.substring(44,48));
			//c.addAttribute("PRIMER CARACTER DE CONTROL",line.substring(48,49));
			//c.addAttribute("SEGUNDO CARACTER DE CONTROL",line.substring(49,50));
			//c.addAttribute("NUMERO FIJO DEL BIEN INMUEBLE",line.substring(50,58));
			//c.addAttribute("CAMPO PARA LA IDENTIFICACION DEL BIEN INMUEBLE ASIGNADO POR EL AYTO",line.substring(58,73));
			//c.addAttribute("NUMERO DE FINCA REGISTRAL",line.substring(73,92));
			//c.addAttribute("CODIGO DE PROVINCIA",line.substring(92,94));
			//c.addAttribute("catastro:ref:province",eliminarCerosString(line.substring(92,94)));
			//c.addAttribute("NOMBRE DE PROVINCIA",line.substring(94,119));
			//c.addAttribute("is_in:province",eliminarComillas(line.substring(94,119)));
			//c.addAttribute("CODIGO DE MUNICIPIO",line.substring(119,122));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(119,122)));
			//c.addAttribute("CODIGO DE MUNICIPIO (INE) EXCLUIDO EL ULTIMO DIGITO DE CONTROL",line.substring(122,125));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(122,125)));
			//c.addAttribute("NOMBRE DE MUNICIPIO",line.substring(125,165));
			//c.addAttribute("is_in:municipality",eliminarComillas(line.substring(125,165)));
			//c.addAttribute("NOMBRE DE LA ENTIDAD MENOR EN CASO DE EXISTIR",line.substring(165,195));
			//c.addAttribute("CODIGO DE VIA PUBLICA",line.substring(195,200));
			//c.addAttribute("catastro:ref:way",eliminarCerosString(line.substring(195,200)));
			//c.addAttribute("TIPO DE VIA O SIGLA PUBLICA",line.substring(200,205));
			//c.addAttribute("NOMBRE DE VIA PUBLICA",line.substring(205,230));
			c.addAttribute("addr:street",nombreTipoViaParser(line.substring(200,205).trim())+" "+formatearNombreCalle(eliminarComillas(line.substring(205,230).trim())));
			//c.addAttribute("PRIMER NUMERO DE POLICIA",line.substring(230,234));
			//c.addAttribute("PRIMERA LETRA (CARACTER DE DUPLICADO)",line.substring(234,235));
			c.addAttribute("addr:housenumber",eliminarCerosString(line.substring(230,234))+line.substring(234,235).trim());
			//c.addAttribute("SEGUNDO NUMERO DE POLICIA",line.substring(235,239));
			//c.addAttribute("SEGUNDA LETRA (CARACTER DE DUPLICADO)",line.substring(239,240));
			//c.addAttribute("KILOMETRO (3enteros y 2decimales)",line.substring(240,245));
			//c.addAttribute("BLOQUE",line.substring(245,249));
			//c.addAttribute("ESCALERA",line.substring(249,251));
			//c.addAttribute("PLANTA",line.substring(251,254));
			//c.addAttribute("PUERTA",line.substring(254,257));
			//c.addAttribute("TEXTO DE DIRECCION NO ESTRUCTURADA",line.substring(257,282));
			c.addAttribute("name",eliminarComillas(line.substring(257,282).trim()));
			//c.addAttribute("CODIGO POSTAL",line.substring(282,287));
//			if (!line.substring(282,287).equals("00000"))
//				c.addAttribute("addr:postcode",line.substring(282,287));
//			if (!line.substring(282,287).isEmpty() && !line.substring(282,287).equals("00000"))
//				c.addAttribute("addr:country" ,"ES");
			//c.addAttribute("DISTRITO MUNICIPAL",line.substring(287,289));
			//c.addAttribute("CODIGO DEL MUNICIPIO DE ORIGEN EN CASO DE AGREGACION",line.substring(289,292));
			//c.addAttribute("CODIGO DE LA ZONA DE CONCENTRACION",line.substring(292,294));
			//c.addAttribute("CODIGO DE POLIGONO",line.substring(294,297));
			//c.addAttribute("CODIGO DE PARCELA",line.substring(297,302));
			//c.addAttribute("CODIGO DE PARAJE",line.substring(302,307));
			//c.addAttribute("NOMBRE DEL PARAJE",line.substring(307,337));
			//c.addAttribute("NUMERO DE ORDEN DEL INMUEBLE EN LA ESCRITURA DE DIVISION HORIZONTAL",line.substring(367,371));
			//c.addAttribute("ANO DE ANTIGUEDAD DEL BIEN INMUEBLE",line.substring(371,375)); 
			//c.addAttribute("CLAVE DE GRUPO DE LOS BIENES INMUEBLES DE CARAC ESPECIALES",line.substring(427,428));
			c.setUsoDestino(line.substring(427,428).trim());
			//c.addAttribute("SUPERFICIE DEL ELEMENTO O ELEMENTOS CONSTRUCTIVOS ASOCIADOS AL INMUEBLE",line.substring(441,451));
			if (esNumero(line.substring(441,451).trim()))
				c.setArea(Double.parseDouble(line.substring(441,451).trim()));
			else
				c.setArea((double) 10);
			//c.addAttribute("SUPERFICIE ASOCIADA AL INMUEBLE",line.substring(451,461));
			//c.addAttribute("COEFICIENTE DE PROPIEDAD (3ent y 6deci)",line.substring(461,470));


			return c;}

		case 16: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("NUMERO DE ORDEN DEL ELEMENTO CUYO VALOR SE REPARTE",line.substring(44,48));
			if (esNumero(line.substring(44,48)) && Integer.parseInt(line.substring(44,48)) != 0)
				c.setNumOrdenConstru(Integer.parseInt(line.substring(44,48)));
			else
				c.setSubparce(line.substring(44,48));
			//c.addAttribute("CALIFICACION CATASTRAL DE LA SUBPARCELA",line.substring(48,50));
			//c.addAttribute("BLOQUE REPETITIVO HASTA 15 VECES",line.substring(50,999));


			return c;}

		case 17: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("NATURALEZA DEL SUELO OCUPADO POR EL CULTIVO (UR, RU)",line.substring(28,30));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("CODIGO DE LA SUBPARCELA",line.substring(44,48));
			c.setSubparce(line.substring(44,48));
			//c.addAttribute("NUMERO DE ORDEN DEL BIEN INMUEBLE FISCAL",line.substring(50,54));
			//c.addAttribute("TIPO DE SUBPARCELA (T, A, D)",line.substring(54,55));
			//c.addAttribute("SUPERFICIE DE LA SUBPARCELA (m cuadrad)",line.substring(55,65));
			//c.addAttribute("catastro:surface",eliminarCerosString(line.substring(55,65)));
			//if (esNumero(line.substring(55,65).trim()))
			//c.setArea(Double.parseDouble(line.substring(55,65).trim()));
			//else
			//c.setArea((double) 10);
			//c.addAttribute("CALIFICACION CATASTRAL/CLASE DE CULTIVO",line.substring(65,67));
			//c.addAttribute("DENOMINACION DE LA CLASE DE CULTIVO",line.substring(67,107));
			//c.addAttribute("INTENSIDAD PRODUCTIVA",line.substring(107,109));
			//c.addAttribute("CODIGO DE MODALIDAD DE REPARTO",line.substring(126,129));


			return c;}
		case 90: {

			/*System.out.println("\nTIPO DE REGISTRO 90: REGISTRO DE COLA\n"); 
			System.out.println("Numero total de registros tipo 11           :"+line.substring(9,16));
			System.out.println("Numero total de registros tipo 13           :"+line.substring(23,30));
			System.out.println("Numero total de registros tipo 14           :"+line.substring(30,37));
			System.out.println("Numero total de registros tipo 15           :"+line.substring(37,44));
			System.out.println("Numero total de registros tipo 16           :"+line.substring(44,51));
			System.out.println("Numero total de registros tipo 17           :"+line.substring(51,58));
			 */
			break;}
		}
		return c;
	}

	/** Elimina ceros a la izquierda en un String
	 * @param s String en el cual eliminar los ceros de la izquierda
	 * @return String sin los ceros de la izquierda
	 */
	public static String eliminarCerosString(String s){
		String temp = s.trim();
		if (esNumero(temp) && !temp.isEmpty()){
			Integer i = Integer.parseInt(temp);
			if (i != 0)
				temp = i.toString();
			else 
				temp = "";
		}
		return temp;
	}


	/** Comprueba si solo contiene caracteres numericos
	 * @param str String en el cual comprobar
	 * @return boolean de si es o no
	 */
	public static boolean esNumero(String s)
	{
		if (s.isEmpty() || s == null)
			return false;

		for (int x = 0; x < s.length(); x++) {
			if (!Character.isDigit(s.charAt(x)))
				return false;
		}
		return true;
	}


	/** Eliminar las comillas '"' de los textos, sino al leerlo JOSM devuelve error
	 * pensando que ha terminado un valor antes de tiempo.
	 * @param s String al que quitar las comillas
	 * @return String sin las comillas
	 */
	public static String eliminarComillas(String s){
		String ret = new String();
		for (int x = 0; x < s.length(); x++)
			if (s.charAt(x) != '"') ret += s.charAt(x);
		return ret;
	}


	public static String nombreTipoViaParser(String codigo){

		switch(codigo){
		case "CL":return "Calle";
		case "AL":return "Aldea/Alameda";
		case "AR":return "Area/Arrabal";
		case "AU":return "Autopista";
		case "AV":return "Avenida";
		case "AY":return "Arroyo";
		case "BJ":return "Bajada";
		case "BO":return "Barrio";
		case "BR":return "Barranco";
		case "CA":return "Cañada";
		case "CG":return "Colegio/Cigarral";
		case "CH":return "Chalet";
		case "CI":return "Cinturon";
		case "CJ":return "Calleja/Callejón";
		case "CM":return "Camino";
		case "CN":return "Colonia";
		case "CO":return "Concejo/Colegio";
		case "CP":return "Campa/Campo";
		case "CR":return "Carretera/Carrera";
		case "CS":return "Caserío";
		case "CT":return "Cuesta/Costanilla";
		case "CU":return "Conjunto";
		case "DE":return "Detrás";
		case "DP":return "Diputación";
		case "DS":return "Diseminados";
		case "ED":return "Edificios";
		case "EM":return "Extramuros";
		case "EN":return "Entrada, Ensanche";
		case "ER":return "Extrarradio";
		case "ES":return "Escalinata";
		case "EX":return "Explanada";
		case "FC":return "Ferrocarril";
		case "FN":return "Finca";
		case "GL":return "Glorieta";
		case "GR":return "Grupo";
		case "GV":return "Gran Vía";
		case "HT":return "Huerta/Huerto";
		case "JR":return "Jardines";
		case "LD":return "Lado/Ladera";
		case "LG":return "Lugar";
		case "MC":return "Mercado";
		case "ML":return "Muelle";
		case "MN":return "Municipio";
		case "MS":return "Masias";
		case "MT":return "Monte";
		case "MZ":return "Manzana";
		case "PB":return "Poblado";
		case "PD":return "Partida";
		case "PJ":return "Pasaje/Pasadizo";
		case "PL":return "Polígono";
		case "PM":return "Paramo";
		case "PQ":return "Parroquia/Parque";
		case "PR":return "Prolongación/Continuación";
		case "PS":return "Paseo";
		case "PT":return "Puente";
		case "PZ":return "Plaza";
		case "QT":return "Quinta";
		case "RB":return "Rambla";
		case "RC":return "Rincón/Rincona";
		case "RD":return "Ronda";
		case "RM":return "Ramal";
		case "RP":return "Rampa";
		case "RR":return "Riera";
		case "RU":return "Rua";
		case "SA":return "Salida";
		case "SD":return "Senda";
		case "SL":return "Solar";
		case "SN":return "Salón";
		case "SU":return "Subida";
		case "TN":return "Terrenos";
		case "TO":return "Torrente";
		case "TR":return "Travesía";
		case "UR":return "Urbanización";
		case "VR":return "Vereda";
		case "CY":return "Caleya";
		}

		return codigo;
	}


	/** Pasa todo el nombre de la calle a minusculas y luego va poniendo en mayusculas las primeras
	 * letras de todas las palabras a menos que sean DE|DEL|EL|LA|LOS|LAS
	 * @param s El nombre de la calle
	 * @return String con el nombre de la calle pasando los articulos a minusculas.
	 */
	public static String formatearNombreCalle(String c){

		String[] l = c.toLowerCase().split(" ");
		String ret = "";

		for (String s : l){
			if (!s.isEmpty() && !s.equals("de") && !s.equals("del") && !s.equals("la") && !s.equals("las") && !s.equals("el") && !s.equals("los")){
				char mayus = Character.toUpperCase(s.charAt(0));
				ret += mayus + s.substring(1, s.length())+" ";
			}
			else
				ret += s+" ";
		}

		return ret.trim();
	}

	/** Simplifica las geometrias
	 * @param key
	 * @param shapes
	 */
	public void simplifyGeometries(List<Shape> shapes, double threshold) {

		// Recorremos todos los shapes
		for(Shape shape : shapes){
			// Hay 2 tipos de simplificadores
			//
			// DouglasPeuckerSimplifier
			// TopologyPreservingSimplifier
			// 

			TopologyPreservingSimplifier tps = new TopologyPreservingSimplifier(shape.getGeometry());
			tps.setDistanceTolerance(threshold);
			shape.setGeometry(tps.getResultGeometry());
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

					// Si una coincide
					if(x != y && subshape != null && 
							(subshape instanceof ShapeSubparce || subshape instanceof ShapeConstru) && 
							subshape.getRefCat().equals(refCat)){

						// Se mete dentro de su parcela padre
						((ShapeParent) shape).addSubshape((ShapePolygonal) subshape);

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

								// Si una coincide
								if(x != y && y != z && null != subsubshape && 
										subsubshape instanceof ShapeConstru && 
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

							// Intenta unir todos los subshapes con los mismos tags en uno
							((ShapeParent) subshape).joinSubshapes(false);

							// Una vez unidos y transferidos los datos de la parcela a sus construcciones
							// las parcelas no se van a usar, por lo que pasamos las construcciones de la
							// parcela a la masa
							if(null != ((ShapeParent) subshape).getSubshapes())
								for(ShapePolygonal constru : ((ShapeParent) subshape).getSubshapes())
									((ShapeParent) shape).addSubshape(constru);

							// Eliminamos la parcela
							// INTRODUCIMOS NULL PARA NO ALTERAR EL ORDEN DE LOS ELEMENTOS DE LA LISTA
							// DESPUES LOS BORRAMOS
							shapes.set(y, null);
						}

						// Si una subparcela coincide
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
					// Intenta unir todos los subshapes con los mismos tags en uno
					((ShapeParent) shape).joinSubshapes(false);
				}					
		}

		// Eliminar los null que hemos introducido
		Iterator<Shape> it = shapes.iterator();
		while(it.hasNext())
			if(null == it.next())
				it.remove();
	}


	// Convertir los shapes a elementos de OSM. De esta manera luego a la hora de imprimir se
	// reutilizan elementos que se compartan entre varios shapes
	public void convertShapes2OSM(List<Shape> shapes) {

		Iterator<Shape> it = shapes.iterator();
		while(it.hasNext()){
			Shape shape = it.next();
			switch(shape.getClass().getName()){

			case "ShapeConstru":
			case "ShapeMasa":
			case "ShapeParcela":
			case "ShapeSubparce":

				if(!utils.mPolygonShapeParser(shape))
					it.remove();
				break;

			case "ShapeEjes":
			case "ShapeElemlin":
				if(!utils.mLineStringShapeParser(shape))
					it.remove();
				break;

			case "ShapeElempun":
			case "ShapeElemtex":
				if(!utils.pointShapeParser(shape))
					it.remove();
				break;

			default:
				System.out.println("["+new Timestamp(new Date().getTime())+"] Nombre de clase inesperado al convertir" +
						"shapes a OSM: "+shape.getClass().getName());	
			}
		}

	}

}
