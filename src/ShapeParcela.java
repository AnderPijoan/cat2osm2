import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opengis.feature.simple.SimpleFeature;

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
			System.out.println("[" + new Timestamp(new Date().getTime())
			+ "] Formato geometrico "
			+ f.getDefaultGeometry().getClass().getName()
			+ " desconocido dentro del shapefile PARCELA");

		// Los demas atributos son metadatos y de ellos sacamos
		referenciaCatastral = (String) f.getAttribute("REFCAT");

		if (referenciaCatastral != null) {
			addAttribute("catastro:ref", referenciaCatastral);
		}

		////////////////////////////////////////////////////////////////////// 
		//
		// SE HA COMPROBADO QUE EL NUMSYMBOL 4 PERTENECE A PARCELAS QUE GENERALMENTE
		// NO HAY QUE DIBUJAR COMO PARCELAS DE CARRETERAS, PARCELA RUSTICA QUE CUBRE
		// TODA LA ZONA URBANA Y ALGUNA MAS
		//
		///////////////////////////////////////////////////////////////////////
		if (f.getAttribute("NUMSYMBOL") instanceof Double) {
			numSymbol = (Integer) f.getAttribute("NUMSYMBOL");
		} else if (f.getAttribute("NUMSYMBOL") instanceof Long) {
			numSymbol = (Integer) f.getAttribute("NUMSYMBOL");
		} else if (f.getAttribute("NUMSYMBOL") instanceof Integer) {
			numSymbol = (Integer) f.getAttribute("NUMSYMBOL");
		}

	}


	public boolean isValid() {
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


	/** Traduce el codigo de destino de cada unidad constructiva de los registros 14 o codigo
	 * de uso del bien inmueble de los registro 15 a sus tags en OSM
	 * Como los cat se leen despues de los shapefiles, hay tags que los shapefiles traen
	 * mas concretos, que esto los machacaria. Es por eso que si al tag le ponemos un '*'
	 * por delante, se comprueba que no exista ese tag antes de meterlo. En caso de existir
	 * dejaria el que ya estaba.
	 * Si al tag le ponemos un '@' por delante, solo se aplica a aquellos shapes que sean edificios.
	 * @param codigo Codigo de uso de inmueble
	 * @return Lista de tags que genera
	 */
	public static List<String[]> destinoParser(String codigo){
		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		switch (codigo){
		case "A":
		case "B":
			return l;

		case "AAL":
		case "BAL":
			s[0] = "@building"; s[1] = "warehouse";
			l.add(s);
			return l;

		case "AAP":
		case "BAP":
			s[0] = "@building"; s[1] = "garage";
			l.add(s);
			s = new String[2];
			s[0] = "landuse"; s[1] = "garages";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Comprobar si es sea parking público o al aire libre. En caso de serlo debería ser amenity= parking.";
			l.add(s);
			return l;

		case"ACR":
		case"BCR":
			s[0] = "@building"; s[1] = "yes";
			l.add(s);
			return l;

		case "ACT":
		case "BCT":
			s[0] = "@building"; s[1] = "yes";
			l.add(s);
			s = new String[2];
			s[0] = "power"; s[1] = "sub_station";
			l.add(s);
			return l;

		case "AES":
		case "BES":
			s[0] = "@building"; s[1] = "yes";
			l.add(s);
			s = new String[2];
			s[0] = "public_transport"; s[1] = "station";
			l.add(s);
			return l;

		case "AIG":
		case "BIG":
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			return l;

		case "C":
		case "D":
			s[0] = "*landuse"; s[1] = "retail";
			l.add(s);
			return l;

		case "CAT":
		case "DAT":
			s[0] = "shop"; s[1] = "car";
			l.add(s);
			return l;

		case "CBZ":
		case "DBZ":
			s[0] = "shop"; s[1] = "electronics";
			l.add(s);
			return l;

		case "CCE":
		case "DCE":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "*";
			l.add(s);
			return l;

		case "CCL":
		case "DCL":
			s[0] = "shop"; s[1] = "shoes";
			l.add(s);
			return l;

		case "CCR":
		case "DCR":;
			s[0] = "shop"; s[1] = "butcher";
			l.add(s);
			return l;

		case "CDM":
		case "DDM":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "*Comercio al por menor";
			l.add(s);
			return l;

		case "CDR":
		case "DDR":
			s[0] = "shop"; s[1] = "chemist";
			l.add(s);
			return l;

		case "CFN":
		case "DFN":;
			s[0] = "amenity"; s[1] = "bank";
			l.add(s);
			return l;

		case "CFR":
		case "DFR":
			s[0] = "amenity"; s[1] = "pharmacy";
			l.add(s);
			return l;

		case "CFT":
		case "DFT":
			s[0] = "craft"; s[1] = "plumber";
			l.add(s);
			return l;

		case "CGL":
		case "DGL":
			s[0] = "amenity"; s[1] = "marketplace";
			l.add(s);
			return l;

		case "CIM":
		case "DIM":
			s[0] = "shop"; s[1] = "copyshop";
			l.add(s);
			return l;

		case "CJY":
		case "DJY":
			s[0] = "shop"; s[1] = "jewelry";
			l.add(s);
			return l;

		case "CLB":
		case "DLB":
			s[0] = "shop"; s[1] = "books";
			l.add(s);
			return l;

		case "CMB":
		case "DMB":
			s[0] = "shop"; s[1] = "furniture";
			l.add(s);
			return l;

		case "CPA":
		case "DPA":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "*Comercio al por mayor";
			l.add(s);
			return l;

		case "CPR":
		case "DPR":
			s[0] = "shop"; s[1] = "chemist";
			l.add(s);
			return l;

		case "CRL":
		case "DRL":
			s[0] = "craft"; s[1] = "watchmaker";
			l.add(s);
			return l;

		case "CSP":
		case "DSP":
			s[0] = "shop"; s[1] = "clothes";
			l.add(s);
			return l;

		case "CTJ":
		case "DTJ":
			s[0] = "shop"; s[1] = "supermarket";
			l.add(s);
			return l;

		case "E":
		case "F":
			s[0] = "*amenity"; s[1] = "school";
			l.add(s);
			return l;

		case "EBL":
		case "FBL":
			s[0] = "amenity"; s[1] = "library";
			l.add(s);
			return l;

		case "EBS":
		case "FBS":
			s[0] = "amenity"; s[1] = "school";
			l.add(s);
			s = new String[2];
			s[0] = "isced:level"; s[1] = "1;2";
			l.add(s);
			return l;

		case "ECL":
		case "FCL":
			s[0] = "amenity"; s[1] = "community_centre";
			l.add(s);
			return l;

		case "EIN":
		case "FIN":
			s[0] = "amenity"; s[1] = "school";
			l.add(s);
			s = new String[2];
			s[0] = "isced:level"; s[1] = "3;4";
			l.add(s);
			return l;

		case "EMS":
		case "FMS":
			s[0] = "tourism"; s[1] = "museum";
			l.add(s);
			return l;

		case "EPR":
		case "FPR":
			s[0] = "amenity"; s[1] = "school";
			l.add(s);
			s = new String[2];
			s[0] = "isced:level"; s[1] = "4";
			l.add(s);
			return l;

		case "EUN":
		case "FUN":
			s[0] = "amenity"; s[1] = "university";
			l.add(s);
			return l;

		case "G":
		case "H":
			s[0] = "tourism"; s[1] = "hotel";
			l.add(s);
			return l;

		case "GC1":
		case "HC1":
			s[0] = "amenity"; s[1] = "cafe";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "1";
			l.add(s);
			return l;

		case "GC2":
		case "HC2":
			s[0] = "amenity"; s[1] = "cafe";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "2";
			l.add(s);
			return l;

		case "GC3":
		case "HC3":
			s[0] = "amenity"; s[1] = "cafe";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "3";
			l.add(s);
			return l;

		case "GC4":
		case "HC4":
			s[0] = "amenity"; s[1] = "cafe";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "4";
			l.add(s);
			return l;

		case "GC5":
		case "HC5":
			s[0] = "amenity"; s[1] = "cafe";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "5";
			l.add(s);
			return l;

		case "GH1":
		case "HH1":
			s[0] = "amenity"; s[1] = "hotel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "1";
			l.add(s);
			return l;

		case "GH2":
		case "HH2":
			s[0] = "amenity"; s[1] = "hotel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "2";
			l.add(s);
			return l;

		case "GH3":
		case "HH3":
			s[0] = "amenity"; s[1] = "hotel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "3";
			l.add(s);
			return l;

		case "GH4":
		case "HH4":
			s[0] = "amenity"; s[1] = "hotel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "4";
			l.add(s);
			return l;

		case "GH5":
		case "HH5":
			s[0] = "amenity"; s[1] = "hotel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "5";
			l.add(s);
			return l;

		case "GP1":
		case "HP1":
			s[0] = "tourism"; s[1] = "apartments";
			l.add(s);
			s = new String[2];
			s[0] = "category"; s[1] = "1";
			l.add(s);
			return l;

		case "GP2":
		case "HP2":
			s[0] = "tourism"; s[1] = "apartments";
			l.add(s);
			s = new String[2];
			s[0] = "category"; s[1] = "2";
			l.add(s);
			return l;

		case "GP3":
		case "HP3":
			s[0] = "tourism"; s[1] = "apartments";
			l.add(s);
			s = new String[2];
			s[0] = "category"; s[1] = "3";
			l.add(s);
			return l;

		case "GPL":
		case "HPL":
			s[0] = "tourism"; s[1] = "apartments";
			l.add(s);
			return l;

		case "GR1":
		case "HR1":
			s[0] = "amenity"; s[1] = "restaurant";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "1";
			l.add(s);
			return l;

		case "GR2":
		case "HR2":
			s[0] = "amenity"; s[1] = "restaurant";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "2";
			l.add(s);
			return l;

		case "GR3":
		case "HR3":
			s[0] = "amenity"; s[1] = "restaurant";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "3";
			l.add(s);
			return l;

		case "GR4":
		case "HR4":
			s[0] = "amenity"; s[1] = "restaurant";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "4";
			l.add(s);
			return l;

		case "GR5":
		case "HR5":
			s[0] = "amenity"; s[1] = "restaurant";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "5";
			l.add(s);
			return l;

		case "GS1":
		case "HS1":
			s[0] = "tourism"; s[1] = "hotel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "1";
			l.add(s);
			return l;

		case "GS2":
		case "HS2":
			s[0] = "tourism"; s[1] = "hotel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "2";
			l.add(s);
			return l;

		case "GS3":
		case "HS3":
			s[0] = "tourism"; s[1] = "hotel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "3";
			l.add(s);
			return l;

		case "GT1":
		case "HT1":
		case "GT2":
		case "HT2":
		case "GT3":
		case "HT3":
		case "GTL":
		case "HTL":
			// Como no sabemos a que se puede referir esto, mejor ponemos un fixme
			s[0] = "fixme"; s[1] = "Documentar codificación de destino de los bienes inmuebles en catastro código="+ codigo +" en http://wiki.openstreetmap.org/wiki/Traduccion_metadatos_catastro_a_map_features#Codificaci.C3.B3n_de_los_destinos_de_los_bienes_inmuebles";
			l.add(s);
			return l;

		case "I":
		case "J":
			s[0] = "*landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			return l;

		case "IAG":
		case "JAG":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "farming";
			l.add(s);
			return l;

		case "IAL":
		case "JAL":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "food";
			l.add(s);
			return l;

		case "IAM":
		case "JAM":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "content"; s[1] = "OMW";
			l.add(s);
			return l;

		case "IAR":
		case "JAR":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "agricultural";
			l.add(s);
			return l;

		case "IAS":
		case "JAS":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "craft"; s[1] = "sawmill";
			l.add(s);
			return l;

		case "IBB":
		case "JBB":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "drinks";
			l.add(s);
			return l;

		case "IBD":
		case "JBD":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "winery";
			l.add(s);
			return l;

		case "IBR":
		case "JBR":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "ceramic";
			l.add(s);
			return l;

		case "ICH":
		case "JCH":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "mushrooms";
			l.add(s);
			return l;

		case "ICN":
		case "JCN":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "building";
			l.add(s);
			return l;

		case "ICT":
		case "JCT":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "quarry";
			l.add(s);
			return l;

		case "IEL":
		case "JEL":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "electric";
			l.add(s);
			return l;

		case "IGR":
		case "JGR":
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			return l;

		case "IIM":
		case "JIM":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "chemistry";
			l.add(s);
			return l;

		case "IIN":
		case "JIN":
			s[0] = "landuse"; s[1] = "greenhouse_horticulture";
			l.add(s);
			return l;

		case "IMD":
		case "JMD":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "wood";
			l.add(s);
			return l;

		case "IMN":
		case "JMN":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "manufacturing";
			l.add(s);
			return l;

		case "IMT":
		case "JMT":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "metal";
			l.add(s);
			return l;

		case "IMU":
		case "JMU":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "machinery";
			l.add(s);
			return l;

		case "IPL":
		case "JPL":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "plastics";
			l.add(s);
			return l;

		case "IPP":
		case "JPP":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "paper";
			l.add(s);
			return l;

		case "IPS":
		case "JPS":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "fishing";
			l.add(s);
			return l;

		case "IPT":
		case "JPT":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "petroleum";
			l.add(s);
			return l;

		case "ITB":
		case "JTB":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "tobacco";
			l.add(s);
			return l;

		case "ITX":
		case "JTX":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "clothing";
			l.add(s);
			return l;

		case "IVD":
		case "JVD":
			s[0] = "landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "glass";
			l.add(s);
			return l;

		case "K":
		case "L":
			s[0] = "*leisure"; s[1] = "sports_centre";
			l.add(s);
			return l;

		case "KDP":
		case "LDP":
			s[0] = "leisure"; s[1] = "pitch";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar sport=X si es posible.";
			l.add(s);
			return l;

		case "KES":
		case "LES":
			s[0] = "leisure"; s[1] = "stadium";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar sport=X si es posible.";
			l.add(s);
			return l;

		case "KPL":
		case "LPL":
			s[0] = "leisure"; s[1] = "sports_centre";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar sport=X si es posible.";
			l.add(s);
			return l;

		case "KPS":
		case "LPS":
			s[0] = "leisure"; s[1] = "swimming_pool";
			l.add(s);
			s = new String[2];
			s[0] = "sport"; s[1] = "swimming";
			l.add(s);
			return l;

		case "M":
		case "N":
			s[0] = "*landuse"; s[1] = "greenfield";
			l.add(s);
			return l;

		case "O":
		case "X":
			s[0] = "*landuse"; s[1] = "commercial";
			l.add(s);
			return l;

		case "O02":
		case "X02":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", Profesional superior. Afinar office=X si es posible.";
			l.add(s);
			return l;

		case "O03":
		case "X03":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", Profesional medio. Afinar office=X si es posible.";
			l.add(s);
			return l;

		case "O06":
		case "X06":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", Médicos, abogados... Afinar office=X si es posible.";
			l.add(s);
			return l;

		case "O07":
		case "X07":
			s[0] = "landuse"; s[1] = "health";
			l.add(s);
			s = new String[2];
			s[0] = "health_facility:type"; s[1] = "office";
			l.add(s);
			s = new String[2];
			s[0] = "health_person:type"; s[1] = "nurse";
			l.add(s);
			return l;

		case "O11":
		case "X11":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", Profesores Mercant. Afinar office=X si es posible.";
			l.add(s);
			return l;

		case "O13":
		case "X13":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", Profesores Universitarios. Afinar office=X si es posible.";
			l.add(s);
			return l;

		case "O15":
		case "X15":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "office"; s[1] = "writer";
			l.add(s);
			return l;

		case "O16":
		case "X16":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "craft"; s[1] = "painter";
			l.add(s);
			return l;

		case "O17":
		case "X17":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "office"; s[1] = "musician";
			l.add(s);
			return l;

		case "O43":
		case "X43":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "office"; s[1] = "salesman";
			l.add(s);
			return l;

		case "O44":
		case "X44":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", agentes. Afinar office=X si es posible.";
			l.add(s);
			return l;

		case "O75":
		case "X75":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "craft"; s[1] = "weaver";
			l.add(s);
			return l;

		case "O79":
		case "X79":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "craft"; s[1] = "tailor";
			l.add(s);
			return l;

		case "O81":
		case "X81":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "craft"; s[1] = "carpenter";
			l.add(s);
			return l;

		case "O88":
		case "X88":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "craft"; s[1] = "jeweller";
			l.add(s);
			return l;

		case "O99":
		case "X99":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", otras actividades. Afinar office=X si es posible.";
			l.add(s);
			return l;

		case "P":
		case "Q":
			s[0] = "*amenity"; s[1] = "public_building";
			l.add(s);
			s = new String[2];
			s[0] = "@building"; s[1] = "public";
			l.add(s);
			return l;

		case "PAA":
		case "QAA":
			s[0] = "amenity"; s[1] = "townhall";
			l.add(s);
			s = new String[2];
			s[0] = "@building"; s[1] = "public";
			l.add(s);
			return l;

		case "PAD":
		case "QAD":
			s[0] = "amenity"; s[1] = "courthouse";
			l.add(s);
			s = new String[2];
			s[0] = "operator"; s[1] = "autonomous_community";
			l.add(s);
			s = new String[2];
			s[0] = "@building"; s[1] = "public";
			l.add(s);
			return l;

		case "PAE":
		case "QAE":
			s[0] = "amenity"; s[1] = "townhall";
			l.add(s);
			s = new String[2];
			s[0] = "@building"; s[1] = "public";
			l.add(s);
			return l;

		case "PCB":
		case "QCB":
			s[0] = "office"; s[1] = "administrative";
			l.add(s);
			s = new String[2];
			s[0] = "@building"; s[1] = "public";
			l.add(s);
			return l;

		case "PDL":
		case "QDL":
		case "PGB":
		case "QGB":
			s[0] = "office"; s[1] = "government";
			l.add(s);
			s = new String[2];
			s[0] = "@building"; s[1] = "public";
			l.add(s);
			return l;

		case "PJA":
		case "QJA":
			s[0] = "amenity"; s[1] = "courthouse";
			l.add(s);
			s = new String[2];
			s[0] = "operator"; s[1] = "county";
			l.add(s);
			s = new String[2];
			s[0] = "@building"; s[1] = "public";
			l.add(s);
			return l;

		case "PJO":
		case "QJO":
			s[0] = "amenity"; s[1] = "courthouse";
			l.add(s);
			s = new String[2];
			s[0] = "operator"; s[1] = "province";
			l.add(s);
			s = new String[2];
			s[0] = "@building"; s[1] = "public";
			l.add(s);
			return l;

		case "R":
		case "S":
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			return l;

		case "RBS":
		case "SBS":
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			s = new String[2];
			s[0] = "religion"; s[1] = "christian";
			l.add(s);
			s = new String[2];
			s[0] = "denomination"; s[1] = "roman_catholic";
			l.add(s);
			s = new String[2];
			s[0] = "@building"; s[1] = "basilica";
			l.add(s);
			return l;

		case "RCP":
		case "SCP":
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			s = new String[2];
			s[0] = "religion"; s[1] = "christian";
			l.add(s);
			s = new String[2];
			s[0] = "denomination"; s[1] = "roman_catholic";
			l.add(s);
			s = new String[2];
			s[0] = "@building"; s[1] = "chapel";
			l.add(s);
			return l;

		case "RCT":
		case "SCT":
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			s = new String[2];
			s[0] = "religion"; s[1] = "christian";
			l.add(s);
			s = new String[2];
			s[0] = "denomination"; s[1] = "roman_catholic";
			l.add(s);
			s = new String[2];
			s[0] = "@building"; s[1] = "cathedral";
			l.add(s);
			return l;

		case "RER":
		case "SER":
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			s = new String[2];
			s[0] = "religion"; s[1] = "christian";
			l.add(s);
			s = new String[2];
			s[0] = "denomination"; s[1] = "roman_catholic";
			l.add(s);
			s = new String[2];
			s[0] = "@building"; s[1] = "hermitage";
			l.add(s);
			return l;

		case "RPR":
		case "SPR":
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			s = new String[2];
			s[0] = "religion"; s[1] = "christian";
			l.add(s);
			s = new String[2];
			s[0] = "denomination"; s[1] = "roman_catholic";
			l.add(s);
			s = new String[2];
			s[0] = "@building"; s[1] = "parish_church";
			l.add(s);
			return l;

		case "RSN":
		case "SSN":
			s[0] = "amenity"; s[1] = "hospital";
			l.add(s);
			s = new String[2];
			s[0] = "landuse"; s[1] = "health";
			l.add(s);
			return l;

		case "T":
		case "U":
			return l;

		case "TAD":
		case "UAD":
			s[0] = "amenity"; s[1] = "auditorium";
			l.add(s);
			return l;

		case "TCM":
		case "UCM":
			s[0] = "amenity"; s[1] = "cinema";
			l.add(s);
			return l;

		case "TCN":
		case "UCN":
			s[0] = "amenity"; s[1] = "cinema";
			l.add(s);
			return l;

		case "TSL":
		case "USL":
			s[0] = "amenity"; s[1] = "hall";
			l.add(s);
			return l;

		case "TTT":
		case "UTT":
			s[0] = "amenity"; s[1] = "theatre";
			l.add(s);
			return l;

		case "V":
		case "W":
			s[0] = "*landuse"; s[1] = "residential";
			l.add(s);
			return l;

		case "Y":
		case "Z":
			return l;

		case "YAM":
		case "ZAM":
		case "YCL":
		case "ZCL":
			s[0] = "landuse"; s[1] = "health";
			l.add(s);
			s = new String[2];
			s[0] = "amenity"; s[1] = "clinic";
			l.add(s);
			s = new String[2];
			s[0] = "medical_system:western"; s[1] = "yes";
			l.add(s);
			return l;

		case "YBE":
		case "ZBE":
			s[0] = "landuse"; s[1] = "pond";
			l.add(s);
			return l;

		case "YCA":
		case "ZCA":
			s[0] = "amenity"; s[1] = "casino";
			l.add(s);
			return l;

		case "YCB":
		case "ZCB":
			s[0] = "amenity"; s[1] = "club";
			l.add(s);
			return l;

		case "YCE":
		case "ZCE":
			s[0] = "amenity"; s[1] = "casino";
			l.add(s);
			return l;

		case "YCT":
		case "ZCT":
			s[0] = "landuse"; s[1] = "quarry";
			l.add(s);
			return l;

		case "YDE":
		case "ZDE":
			s[0] = "man_made"; s[1] = "wastewater_plant";
			l.add(s);
			return l;

		case "YDG":
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "content"; s[1] = "gas";
			l.add(s);
			return l;

		case "ZDG":
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "content"; s[1] = "gas";
			l.add(s);
			return l;

		case "YDL":
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "content"; s[1] = "liquid";
			l.add(s);
			return l;

		case "ZDL":
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "content"; s[1] = "liquid";
			l.add(s);
			return l;

		case "YDS":
		case "ZDS":
			s[0] = "amenity"; s[1] = "pharmacy";
			l.add(s);
			s = new String[2];
			s[0] = "dispensing"; s[1] = "yes";
			l.add(s);
			return l;

		case "YGR":
		case "ZGR":
			s[0] = "amenity"; s[1] = "kindergarten";
			l.add(s);
			return l;

		case "YGV":
		case "ZGV":
			s[0] = "landuse"; s[1] = "surface_mining";
			l.add(s);
			s = new String[2];
			s[0] = "mining_resource"; s[1] = "gravel";
			l.add(s);
			return l;

		case "YHG":
		case "ZHG":
			// Como no sabemos a que se puede referir esto, mejor ponemos un fixme
			s[0] = "fixme"; s[1] = "Documentar codificación de destino de los bienes inmuebles en catastro código="+ codigo +" en http://wiki.openstreetmap.org/wiki/Traduccion_metadatos_catastro_a_map_features#Codificaci.C3.B3n_de_los_destinos_de_los_bienes_inmuebles";
			l.add(s);
			return l;

		case "YHS":
		case "ZHS":
		case "YSN":
		case "ZSN":
			s[0] = "landuse"; s[1] = "health";
			l.add(s);
			s = new String[2];
			s[0] = "amenity"; s[1] = "hospital";
			l.add(s);
			s = new String[2];
			s[0] = "medical_system:western"; s[1] = "yes";
			l.add(s);;
			return l;

		case "YMA":
		case "ZMA":
			s[0] = "landuse"; s[1] = "surface_mining";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar mining_resource=X si es posible.";
			l.add(s);
			return l;

		case "YME":
		case "ZME":
			s[0] = "man_made"; s[1] = "pier";
			l.add(s);
			return l;

		case "YPC":
		case "ZPC":
			s[0] = "landuse"; s[1] = "aquaculture";
			l.add(s);
			return l;

		case "YRS":
		case "ZRS":
			s[0] = "social_facility"; s[1] = "group_home";
			l.add(s);
			return l;

		case "YSA":
		case "ZSA":
		case "YSO":
		case "ZSO":
			s[0] = "office"; s[1] = "labour_union";
			l.add(s);
			return l;

		case "YSC":
		case "ZSC":
			s[0] = "health_facility:type"; s[1] = "first_aid";
			l.add(s);
			s = new String[2];
			s[0] = "medical_system:western"; s[1] = "yes";
			l.add(s);
			s = new String[2];
			s[0] = "yes"; s[1] = "yes";
			l.add(s);
			return l;

		case "YSL":
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "content"; s[1] = "solid";
			l.add(s);
			return l;

		case "ZSL":
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "content"; s[1] = "solid";
			l.add(s);
			return l;

		case "YVR":
		case "ZVR":
			s[0] = "landuse"; s[1] = "landfill";
			l.add(s);
			return l;

		default:
			if (!codigo.isEmpty()){
				s[0] = "fixme"; s[1] = "Documentar nuevo codificación de destino de los bienes inmuebles en catastro código="+ codigo +" en http://wiki.openstreetmap.org/wiki/Traduccion_metadatos_catastro_a_map_features#Codificaci.C3.B3n_de_los_destinos_de_los_bienes_inmuebles";
				l.add(s);}

			return l;}
	}


	// Crea los attributes del uso o destino con mas area que existe
	// y los inserta en los shapes que sea necesario
	public void createAttributesFromUsoDestino() {
		for(String[] s : destinoParser(getUsoDestinoMasArea())){
			// Si empieza con @ debe aplicarse solo a construcciones
			if(s[0].startsWith("@")){
				if(subshapes != null)
					for(Shape sub : subshapes){
						if (sub instanceof ShapeConstru)
							sub.addAttribute(s[0].replace("@", ""), s[1]);
					}
				// Si empieza con * se debe comprobar que no exista ya ese tag, porque el shapefile
				// indica de forma mas especifica y este nuevo lo machacaria con un valor mas general
			} else if (s[0].startsWith("*")){
				if (getAttribute(s[0].replace("*", "")) == null){
					addAttribute(s[0].replace("*", ""), s[1]);
			}
				// Sino anadir el attribute
			} else {
				addAttribute(s[0], s[1]);
			}
		}
	}
}
