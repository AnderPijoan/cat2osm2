import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.util.PolygonExtracter;

public class ShapeParcela extends ShapeParent {

	// Variable autoincremental que se concatena al shapeId
	private volatile static Long URID = (long) 0;
	private volatile static Long RUID = (long) 0;
	private int numSymbol;

	// Esto se usa para la paralelizacion ya que luego solo se simplificaran
	// geometrias que
	// pertenezcan a las mismas masas. Si alguna geometria no tiene codigo de
	// masa, se le
	// asignara el nombre de tipo de archivo

	// Para definir cual de todos los usos y destinos asignar,
	// se ha llegado a la conclusion de asignar el que mas area tenga
	// Aun y asi, los registros tipo 14 del catastro traen los destinos
	// (especifios, de 3 caracteres)
	// de cada bien inmueble y los tipo 15 los usos, que son mas generales (solo
	// el primer caracter) y que al
	// pertenecer a la parcela tienen mayor area que los de los bienes
	// inmuebles. Es por eso que sucedia que
	// al final se cogia el que menos detalle tenia por ser el uso de la
	// parcela. Para eso vamos a separalos
	// y a coger el uso en caso de que no haya destino.

	private HashMap<String, Double> usos;
	private HashMap<String, Double> destinos;

	// Las parcelas pueden contener otros shapes en su interior
	// El portal que es un elemtex
	private List<ShapeElemtex> entrances;


	/**
	 * Constructor
	 * 
	 * @param f Linea del archivo shp
	 */
	public ShapeParcela(SimpleFeature f, String tipo) {

		super(f, tipo);

		shapeId = "PARCELA" + tipo + (tipo.equals("UR") ? (URID = super.newShapeId(URID)) : (RUID = super.newShapeId(RUID)));

		// Para agrupar geometrias segun su codigo de masa
		codigoMasa = ((String) f.getAttribute("MASA")).replaceAll(
				"[^\\p{L}\\p{N}]", "") + "-";


		// Parcela.shp trae la geometria en formato MultiPolygon
		if (f.getDefaultGeometry().getClass().getName()
				.equals("com.vividsolutions.jts.geom.MultiPolygon")) {

			// Poligono, trae el primer punto de cada poligono repetido al
			// final.
			geometry = (MultiPolygon) f.getDefaultGeometry();
			
			// Eliminamos posibles poligonos multiples
			List<?> polys = PolygonExtracter.getPolygons(geometry.union());
			geometry = geometry.getFactory().buildGeometry(polys);
			geometry.normalize();

		} else
			System.out.println("[" + new Timestamp(new Date().getTime())+ "]\tFormato geometrico "
			+ f.getDefaultGeometry().getClass().getName()
			+ " desconocido dentro del shapefile PARCELA");

		// Los demas atributos son metadatos y de ellos sacamos
		referenciaCatastral = (String) f.getAttribute("REFCAT");
		
		if (f.getAttribute("NUMSYMBOL") instanceof Double) {
			numSymbol = (Integer) f.getAttribute("NUMSYMBOL");
		} else if (f.getAttribute("NUMSYMBOL") instanceof Long) {
			numSymbol = (Integer) f.getAttribute("NUMSYMBOL");
		} else if (f.getAttribute("NUMSYMBOL") instanceof Integer) {
			numSymbol = (Integer) f.getAttribute("NUMSYMBOL");
		}

	}


	public boolean isValid() {
		//////////////////////////////////////////////////////////////////////
		//
		// SE HA COMPROBADO QUE EL NUMSYMBOL 4 PERTENECE A PARCELAS QUE GENERALMENTE
		// NO HAY QUE DIBUJAR COMO PARCELAS BAJO CARRETERAS, PARCELAS RUSTICA QUE 
		// CUBREN TODA UNA ZONA URBANA Y POR TANTO NO TIENEN ATRIBUTOS, ETC
		//
		///////////////////////////////////////////////////////////////////////
		return (numSymbol != 4 ? true : false);
	}


	public HashMap<String, Double> getUsos() {
		return usos;
	}


	public void setUsos(HashMap<String, Double> usos) {
		this.usos = usos;
	}


	public void addUso(String cod, double area) {
		if (usos == null)
			usos = new HashMap<String, Double>();

		if (usos.get(cod) == null)
			usos.put(cod, area);
		else {
			double a = usos.get(cod);
			a += area;
			usos.put(cod, a);
		}
	}


	public String getUsoDestinoMasArea() {

		// Si hay destinos cogemos el de mayor area
		if (destinos != null && !destinos.isEmpty()) {

			String destino = "";
			double area = 0;
			Iterator<Entry<String, Double>> it = destinos.entrySet().iterator();

			// Comparamos las areas de los destinos (son mas especificos)
			while (it.hasNext()) {
				Map.Entry<String, Double> e = it.next();
				if ((Double) e.getValue() >= area) {
					area = (Double) e.getValue();
					destino = (String) e.getKey();
				}
			}
			return destino;
		}

		// Si no lo hay, pasamos a usos que son mas generales y con menos nivel
		// de detalle
		else if (usos != null && !usos.isEmpty()) {

			String uso = "";
			double area = 0;
			Iterator<Entry<String, Double>> it = usos.entrySet().iterator();

			// Comparamos las areas de los destinos (son mas especificos)
			while (it.hasNext()) {
				Map.Entry<String, Double> e = it.next();
				if ((Double) e.getValue() >= area) {
					area = (Double) e.getValue();
					uso = (String) e.getKey();
				}
			}
			return uso;
		}
		return "";
	}


	public void setDestinos(HashMap<String, Double> destinos) {
		this.destinos = destinos;
	}


	public void addDestino(String cod, double area) {
		if (destinos == null)
			destinos = new HashMap<String, Double>();

		if (destinos.get(cod) == null)
			destinos.put(cod, area);
		else {
			double a = destinos.get(cod);
			a += area;
			destinos.put(cod, a);
		}
	}


	public List<ShapeElemtex> getEntrances() {
		return entrances;
	}


	public void addEntrance(ShapeElemtex entrance) {
		if(null == this.entrances)
			entrances = new ArrayList<ShapeElemtex>();
		
		if(!entrances.contains(entrance))
		this.entrances.add(entrance);
	}

	/**
	 * Los nuevos tags de OSM 3d requieren para las construcciones con distintas
	 * alturas crear una relacion con el contorno y sus partes.
	 * Para ello esta la clase ConstruExterior y las ConstruPart.
	 * La Parcela unicamente tendra como subshapes ConstruExteriores y estas seran
	 * las que contengan los ConstruPart como sus subshapes.
	 * Se creara una ConstruExterior de forma automatica para englobar los ConstruPart.
	 * A la hora de anadir los Construs a una parcela, se comprueba si interseca
	 * con alguna ya existente, para unirlas en una. Al crear esta union
	 * se a��adira la nueva parte ampliando la geometr��a de la ConstruExterior
	 */
//	@Override
//	public void addSubshape(ShapePolygonal subshape){
//		if (subshapes == null)
//			subshapes = new ArrayList<Shape>();
//		this.subshapes.add(subshape);
//		
//		ShapePolygonal construPart = subshape;
//		boolean existsConstruPart = false;
//		
//		// Si el subshape (ShapeConstruPart) coincide perfectamente con la parcela
//		// o son practicamente el mismo directamente solo anadimos los tags a la parcela
//		
//		if (construPart.getGeometry().buffer(0).equalsNorm(this.geometry.buffer(0))){
//			getAttributes().addAll(construPart.getAttributes().asHashMap());
//		} else {
//			
//			// Sino, comprobar los subshapes de la parcela para buscar intersecciones
//			// para reutilizar subshapes existentes. Si no existen o no hay interseccion
//			// anadir la construccion a los subshapes de la parcela
//
//			for(Shape construExt : subshapes){
//				
//				// Calcular la interseccion. Hasta ahora si solo se tocaban en un punto 2 geometrias
//				// se creaba un multipoligono que las unia, pero no tiene logica. Para comprobar esos
//				// casos se comprueba que la interseccion tenga mas e 1 punto.
//				Geometry intersection = construPart.getGeometry().intersection(construExt.getGeometry());
//				
//				if ( construPart.getGeometry().overlaps(construExt.getGeometry()) || // Una junto a la otra
//						(construPart.getGeometry().intersects(construExt.getGeometry()) && intersection.getNumPoints() > 1 ) // Interseccionan
//						){
//					existsConstruPart = true;
//					construExt.setGeometry(
//							construExt.getGeometry().union(
//									construPart.getGeometry()));
//					construExt.getGeometry().normalize();
//					((ShapeParent) construExt).addSubshape(construPart);
//				}
//			}
//			if (!existsConstruPart){
//				// Si no existe ninguna parte que interseque
//				// crear una nueva ConstruExterior y meter en ella la ConstruPart
//				SimpleFeatureType type = null;
//				try {
//					// Crear el shape
//					type = DataUtilities.createType("ConstruExterior", "MASA:String,REFCAT:String,FECHAALTA:Double,FECHABAJA:Double");
//					SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
//					builder.add(this.getCodigoMasa());
//					builder.add(this.getRefCat());
//					builder.add(construPart.getFechaAlta());
//					builder.add(construPart.getFechaBaja());
//					ShapeParent nuevoConstruExt = new ShapeConstruExterior(builder.buildFeature(null), this.getTipo());
//
//					// Anadimos como subshape el ConstruPart
//					nuevoConstruExt.addSubshape(construPart);
//					nuevoConstruExt.setGeometry(construPart.getGeometry());
//
//					// Anadimos NO TODOS, sino los atributos importantes a la construccion
//					// exterior, para que esta tenga tags relevantes y asi no sea borrada
//					List<String> nokeys = new ArrayList<String>();
//					nokeys.add("building:levels");
//					nokeys.add("height");
//					for (String key : construPart.getAttributes().getKeys()){
//						if (!nokeys.contains(key))
//						{
//							nuevoConstruExt.getAttributes().addAttribute(key, construPart.getAttributes().getValue(key));
//						}
//					}
//					// Anadimos a la parcela el ConstruExterior
//					this.subshapes.add(nuevoConstruExt);
//				} catch (SchemaException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//	}

	
	@Override
	public void createAttributesFromUsoDestino() {
		ParcelaParser parser = new ParcelaParser();
		parser.parseParcela(this);
	}
	
	/**
	 * Convierte la parcela a OSM Y ANADE EL PUNTO DE LA ENTRADA
	 * a la relacion
	 */
	@Override
	public boolean toOSM(Cat2OsmUtils utils, ShapeParent parent){
		boolean converted = true;
		// ANADIMOS LA ENTRADA A LA GEOMETRIA DE PARCELA Y TODOS LOS SUBSHAPES SOBRE LOS
		// QUE TAMBIEN LA TOQUEN.
		if (this.getEntrances() != null){
			// Recorremos los entrances de la parcela
			for (Shape entrance : this.getEntrances()){
				// Crear un NodeOSM con la entrada
				converted = entrance.toOSM(utils, this);
				if (converted){
					converted = addEntrancePointToGeomIfOnEdge(utils, this, entrance);
				}
			}
		}
		if (converted){
			converted = super.toOSM(utils, parent);
		} 
		return converted;
	}
	
	/**
	 * Dada un shape y una entrada, comprueba si esta ultima esta sobre el shape y en ese
	 * caso lo anade.
	 * @param utils
	 * @param shape
	 * @param entrance
	 * @return
	 */
	public boolean addEntrancePointToGeomIfOnEdge(Cat2OsmUtils utils, ShapePolygonal shape, Shape entrance){	
		// Crear una lista con las coordenadas de la geometria
		List<Coordinate> coors = new ArrayList<Coordinate>();
		for (Coordinate coor : shape.getGeometry().getCoordinates()){
			coors.add(coor);
		}
		
		// Crear una lista con pares de indices de coordenadas entre los que ha dado
		// que la entrada interseca
		List<Coordinate[]> matchCoors = new ArrayList<Coordinate[]>();
		
		for(int x = 1; x < coors.size(); x++){
			Coordinate[] tempCoors = {coors.get(x-1), coors.get(x)};
			LineString line = shape.getGeometry().getFactory().createLineString(tempCoors);
			if (line.isWithinDistance(entrance.getGeometry(), Cat2OsmUtils.GEOM_INTERSECTION_THRESHOLD)) {
				
				Coordinate[] coors2 = {coors.get(x-1), coors.get(x)};
				matchCoors.add(coors2);
				
				// ANADIMOS LA COORDENADA A LA LISTA DE COORDENADAS Y SOBREESCRIBIMOS LA GEOMETRIA
				coors.add(x, entrance.getGeometry().getCoordinates()[0]);
				
				// COMPROBAR QUE HA QUEDADO UNA LINEA CERRADA
				if (coors.get(0).equals(coors.get(coors.size()-1))){
					shape.setGeometry(shape.getGeometry().getFactory().createPolygon(
							shape.getGeometry().getFactory().createLinearRing( (coors.toArray(new Coordinate[coors.size()])) ), null)
							);
				}
				
				// LANZAR EL MISMO PROCESO CON LOS SUBSHAPES
				if(shape instanceof ShapeParent && ((ShapeParent)shape).getSubshapes() != null){
					for(Shape subshape : ((ShapeParent)shape).getSubshapes()){
						addEntrancePointToGeomIfOnEdge(utils, (ShapePolygonal)subshape, entrance);
					}
				}
				return true;
			}
		}
		return false;
	}
}
