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
			System.out.println("[" + new Timestamp(new Date().getTime())+ "]\tFormato geometrico "
			+ f.getDefaultGeometry().getClass().getName()
			+ " desconocido dentro del shapefile PARCELA");

		// Los demas atributos son metadatos y de ellos sacamos
		referenciaCatastral = (String) f.getAttribute("REFCAT");

		if (referenciaCatastral != null) {
			getAttributes().addAttribute("catastro:ref", referenciaCatastral);
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

	
	/**
	 * Sobreescribe el atributo a los constru SOLO si existe la clave k con el valor v
	 * @param k Clave
	 * @param v Valor existente
	 * @param newV Valor con el que se sobreescribira
	 */
	public void overwriteAttributeInConstru(String k, String v, String newV){
		if(subshapes != null)
			for(Shape sub : subshapes){
				if (sub instanceof ShapeConstru )
					sub.getAttributes().overwriteAttribute(k, v, newV);
			}
	}
	
	
	/**
	 * Anade el atributo a los constru solo si existe la clave especificada
	 * @param ifKey Clave que tiene que existir
	 * @param k Clave
	 * @param v Valor
	 */
	public void addAttributeInConstruIfKeyValue(String existKey, String existValue, String k, String v){
		if(subshapes != null)
			for(Shape sub : subshapes){
				if (sub instanceof ShapeConstru )
					sub.getAttributes().addAttributeIfKeyValue(existKey, existValue, k, v);
			}
	}

	
	/**
	 * Comprueba si hay destinos o usos, coge el de mayor
	 * area y actualiza sus propiedades y las de los subshapes en consecuencia.
	 * 
	 * Hay tags que vienen mas detallados en los shapefiles por eso puede que
	 * no se sobreescriban o si.
	 */
	public void createAttributesFromUsoDestino() {

			String usodestino = getUsoDestinoMasArea();
			
			switch (usodestino){
			case "A":
			case "B":
				break;
			case "AAL":
			case "BAL":
				overwriteAttributeInConstru("building", "yes", "warehouse");
				break;
			case "AAP":
			case "BAP":
				getAttributes().addAttribute("landuse", "garages");
				overwriteAttributeInConstru("building", "yes", "garage");
				break;
			case"ACR":
			case"BCR":
				break;
			case "ACT":
			case "BCT":
				addAttributeInConstruIfKeyValue("building", "yes", "power", "sub_station");
				overwriteAttributeInConstru("building", "yes", "sub_station");
				break;
			case "AES":
			case "BES":
				overwriteAttributeInConstru("building", "yes", "station");
				addAttributeInConstruIfKeyValue("building", "yes", "public_transport", "station");
				break;
			case "AIG":
			case "BIG":
				getAttributes().addAttribute("landuse","farmyard");
				overwriteAttributeInConstru("building", "yes", "livestock");
				break;
			case "C":
			case "D":
				getAttributes().addAttributeIfNotExistValue("landuse","retail");
				break;
			case "CAT":
			case "DAT":
				addAttributeInConstruIfKeyValue("building", "yes", "shop", "car");
				break;
			case "CBZ":
			case "DBZ":
				addAttributeInConstruIfKeyValue("building", "yes", "shop","electronics");
				break;
			case "CCE":
			case "DCE":
				getAttributes().addAttribute("landuse","retail");
				break;
			case "CCL":
			case "DCL":
				addAttributeInConstruIfKeyValue("building", "yes", "shop","shoes");
				break;
			case "CCR":
			case "DCR":
				addAttributeInConstruIfKeyValue("building", "yes", "shop","butcher");
				break;
			case "CDM":
			case "DDM":
				getAttributes().addAttribute("landuse","retail");
				addAttributeInConstruIfKeyValue("building", "yes", "shop","yes");
				break;
			case "CDR":
			case "DDR":
				addAttributeInConstruIfKeyValue("building", "yes", "shop","chemist");
				break;
			case "CFN":
			case "DFN":;
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","bank");
				break;
			case "CFR":
			case "DFR":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","pharmacy");
				break;
			case "CFT":
			case "DFT":
				addAttributeInConstruIfKeyValue("building", "yes", "craft","plumber");
				break;
			case "CGL":
			case "DGL":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","marketplace");
				break;
			case "CIM":
			case "DIM":
				addAttributeInConstruIfKeyValue("building", "yes", "shop","copyshop");
				break;
			case "CJY":
			case "DJY":
				addAttributeInConstruIfKeyValue("building", "yes", "shop","jewelry");
				break;
			case "CLB":
			case "DLB":
				addAttributeInConstruIfKeyValue("building", "yes", "shop","books");
				break;
			case "CMB":
			case "DMB":
				addAttributeInConstruIfKeyValue("building", "yes", "shop","furniture");
				break;
			case "CPA":
			case "DPA":
				getAttributes().addAttribute("landuse","retail");
				break;
			case "CPR":
			case "DPR":
				addAttributeInConstruIfKeyValue("building", "yes", "shop","chemist");
				break;
			case "CRL":
			case "DRL":
				addAttributeInConstruIfKeyValue("building", "yes", "craft","watchmaker");
				break;
			case "CSP":
			case "DSP":
				addAttributeInConstruIfKeyValue("building", "yes", "shop","clothes");
				break;
			case "CTJ":
			case "DTJ":
				getAttributes().addAttribute("landuse","retail");
				overwriteAttributeInConstru("building","yes", "supermarket");
				addAttributeInConstruIfKeyValue("building", "yes", "shop","supermarket");
				break;
			case "E":
			case "F":
				getAttributes().addAttributeIfNotExistValue("amenity","school");
				addAttributeInConstruIfKeyValue("building","yes","amenity","school");
				overwriteAttributeInConstru("building","yes","school");
				break;
			case "EBL":
			case "FBL":
				overwriteAttributeInConstru("building","yes", "library");
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","library");
				break;
			case "EBS":
			case "FBS":
				getAttributes().addAttribute("amenity","school");
				getAttributes().addAttribute("isced:level","1;2");
				overwriteAttributeInConstru("building","yes", "school");
				addAttributeInConstruIfKeyValue("building", "yes", "amenity", "school");
				addAttributeInConstruIfKeyValue("building", "yes", "isced:level","1;2");
				break;
			case "ECL":
			case "FCL":
				getAttributes().addAttribute("amenity","community_centre");
				overwriteAttributeInConstru("building","yes", "community_centre");
				addAttributeInConstruIfKeyValue("building", "yes", "amenity", "community_centre");
				break;
			case "EIN":
			case "FIN":
				getAttributes().addAttribute("amenity","school");
				getAttributes().addAttribute("isced:level","3;4");
				addAttributeInConstruIfKeyValue("building", "yes", "amenity", "school");
				addAttributeInConstruIfKeyValue("building", "yes", "isced:level","3;4");
				overwriteAttributeInConstru("building","yes","school");
				break;
			case "EMS":
			case "FMS":
				getAttributes().addAttribute("tourism","museum");
				addAttributeInConstruIfKeyValue("building", "yes", "tourism", "museum");
				overwriteAttributeInConstru("building","yes","museum");
				break;
			case "EPR":
			case "FPR":
				getAttributes().addAttribute("amenity","school");
				getAttributes().addAttribute("isced:level","4");
				addAttributeInConstruIfKeyValue("building", "yes", "amenity", "school");
				addAttributeInConstruIfKeyValue("building", "yes", "isced:level","4");
				overwriteAttributeInConstru("building","yes","school");
				break;
			case "EUN":
			case "FUN":
				getAttributes().addAttribute("amenity","university");
				addAttributeInConstruIfKeyValue("building", "yes", "amenity", "university");
				overwriteAttributeInConstru("building","yes","university");
				break;
			case "G":
			case "H":
				getAttributes().addAttribute("tourism","hotel");
				addAttributeInConstruIfKeyValue("building", "yes", "tourism","hotel");
				overwriteAttributeInConstru("building","yes","hotel");
				break;
			case "GC1":
			case "HC1":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","cafe");
				addAttributeInConstruIfKeyValue("building", "yes", "forks","1");
				break;
			case "GC2":
			case "HC2":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","cafe");
				addAttributeInConstruIfKeyValue("building", "yes", "forks","2");
				break;
			case "GC3":
			case "HC3":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","cafe");
				addAttributeInConstruIfKeyValue("building", "yes", "forks","3");
				break;
			case "GC4":
			case "HC4":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","cafe");
				addAttributeInConstruIfKeyValue("building", "yes", "forks","4");
				break;
			case "GC5":
			case "HC5":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","cafe");
				addAttributeInConstruIfKeyValue("building", "yes", "forks","5");
				break;
			case "GH1":
			case "HH1":
				addAttributeInConstruIfKeyValue("building", "yes", "tourism","hotel");
				addAttributeInConstruIfKeyValue("building", "yes", "stars","1");
				overwriteAttributeInConstru("building","yes","hotel");
				break;
			case "GH2":
			case "HH2":
				addAttributeInConstruIfKeyValue("building", "yes", "tourism","hotel");
				addAttributeInConstruIfKeyValue("building", "yes", "stars","2");
				overwriteAttributeInConstru("building","yes","hotel");
				break;
			case "GH3":
			case "HH3":
				addAttributeInConstruIfKeyValue("building", "yes", "tourism","hotel");
				addAttributeInConstruIfKeyValue("building", "yes", "stars","3");
				overwriteAttributeInConstru("building","yes","hotel");
				break;
			case "GH4":
			case "HH4":
				addAttributeInConstruIfKeyValue("building", "yes", "tourism","hotel");
				addAttributeInConstruIfKeyValue("building", "yes", "stars","4");
				overwriteAttributeInConstru("building","yes","hotel");
				break;
			case "GH5":
			case "HH5":
				addAttributeInConstruIfKeyValue("building", "yes", "tourism","hotel");
				addAttributeInConstruIfKeyValue("building", "yes", "stars","5");
				overwriteAttributeInConstru("building","yes","hotel");
				break;
			case "GP1":
			case "HP1":
				addAttributeInConstruIfKeyValue("building", "yes", "tourism","apartments");
				addAttributeInConstruIfKeyValue("building", "yes", "category","1");
				overwriteAttributeInConstru("building","yes","apartments");
				break;
			case "GP2":
			case "HP2":
				addAttributeInConstruIfKeyValue("building", "yes", "tourism","apartments");
				addAttributeInConstruIfKeyValue("building", "yes", "category","2");
				overwriteAttributeInConstru("building","yes","apartments");
				break;
			case "GP3":
			case "HP3":
				addAttributeInConstruIfKeyValue("building", "yes", "tourism","apartments");
				addAttributeInConstruIfKeyValue("building", "yes", "category","3");
				overwriteAttributeInConstru("building","yes","apartments");
				break;
			case "GPL":
			case "HPL":
				addAttributeInConstruIfKeyValue("building", "yes", "tourism","apartments");
				overwriteAttributeInConstru("building","yes","apartments");
				break;
			case "GR1":
			case "HR1":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","restaurant");
				addAttributeInConstruIfKeyValue("building", "yes", "forks","1");
				break;
			case "GR2":
			case "HR2":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","restaurant");
				addAttributeInConstruIfKeyValue("building", "yes", "forks","2");
				break;
			case "GR3":
			case "HR3":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","restaurant");
				addAttributeInConstruIfKeyValue("building", "yes", "forks","3");
				break;
			case "GR4":
			case "HR4":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","restaurant");
				addAttributeInConstruIfKeyValue("building", "yes", "forks","4");
				break;
			case "GR5":
			case "HR5":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","restaurant");
				addAttributeInConstruIfKeyValue("building", "yes", "forks","5");
				break;
			case "GS1":
			case "HS1":
				addAttributeInConstruIfKeyValue("building", "yes", "tourism","hotel");
				addAttributeInConstruIfKeyValue("building", "yes", "stars","1");
				overwriteAttributeInConstru("building","yes","hotel");
				break;
			case "GS2":
			case "HS2":
				addAttributeInConstruIfKeyValue("building", "yes", "tourism","hotel");
				addAttributeInConstruIfKeyValue("building", "yes", "stars","2");
				overwriteAttributeInConstru("building","yes","hotel");
				break;
			case "GS3":
			case "HS3":
				addAttributeInConstruIfKeyValue("building", "yes", "tourism","hotel");
				addAttributeInConstruIfKeyValue("building", "yes", "stars","3");
				overwriteAttributeInConstru("building","yes","hotel");
				break;
			case "GT1":
			case "HT1":
			case "GT2":
			case "HT2":
			case "GT3":
			case "HT3":
			case "GTL":
			case "HTL":
				// Como no sabemos a que se puede referir esto, mejor ponemos un fixme
				getAttributes().addAttribute("fixme","Documentar codificación de destino de los bienes inmuebles en catastro código="+ usodestino +" en http://wiki.openstreetmap.org/wiki/Traduccion_metadatos_catastro_a_map_features#Codificaci.C3.B3n_de_los_destinos_de_los_bienes_inmuebles");
				break;
			case "I":
			case "J":
				getAttributes().addAttributeIfNotExistValue("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IAG":
			case "JAG":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","farming");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IAL":
			case "JAL":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","food");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IAM":
			case "JAM":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","storage_tank");
				getAttributes().addAttribute("content","OMW");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IAR":
			case "JAR":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","agricultural");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IAS":
			case "JAS":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("craft","sawmill");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IBB":
			case "JBB":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","drinks");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IBD":
			case "JBD":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","winery");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IBR":
			case "JBR":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","ceramic");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "ICH":
			case "JCH":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","mushrooms");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "ICN":
			case "JCN":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","building");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "ICT":
			case "JCT":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","quarry");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IEL":
			case "JEL":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","electric");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IGR":
			case "JGR":
				getAttributes().addAttribute("landuse","farmyard");
				break;
			case "IIM":
			case "JIM":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","chemistry");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IIN":
			case "JIN":
				getAttributes().addAttribute("landuse","greenhouse_horticulture");
				overwriteAttributeInConstru("building","yes","greenhouse");
				break;
			case "IMD":
			case "JMD":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","wood");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IMN":
			case "JMN":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","manufacturing");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IMT":
			case "JMT":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","metal");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IMU":
			case "JMU":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","machinery");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IPL":
			case "JPL":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","plastics");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IPP":
			case "JPP":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","paper");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IPS":
			case "JPS":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","fishing");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IPT":
			case "JPT":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","petroleum");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "ITB":
			case "JTB":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","tobacco");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "ITX":
			case "JTX":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","clothing");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "IVD":
			case "JVD":
				getAttributes().addAttribute("landuse","industrial");
				getAttributes().addAttribute("man_made","works");
				getAttributes().addAttribute("works","glass");
				overwriteAttributeInConstru("building","yes","industrial");
				break;
			case "K":
			case "L":
				getAttributes().addAttributeIfNotExistValue("leisure","sports_centre");
				break;
			case "KDP":
			case "LDP":
				getAttributes().addAttribute("leisure","pitch");
				getAttributes().addAttribute("fixme","Codigo="+ usodestino +", afinar sport=X si es posible.");
				break;
			case "KES":
			case "LES":
				getAttributes().addAttribute("leisure","stadium");
				getAttributes().addAttribute("fixme","Codigo="+ usodestino +", afinar sport=X si es posible.");
				break;
			case "KPL":
			case "LPL":
				getAttributes().addAttribute("leisure","sports_centre");
				getAttributes().addAttribute("fixme","Codigo="+ usodestino +", afinar sport=X si es posible.");
				break;
			case "KPS":
			case "LPS":
				getAttributes().addAttribute("leisure","swimming_pool");
				getAttributes().addAttribute("sport","swimming");
				break;
			case "M":
			case "N":
				getAttributes().addAttributeIfNotExistValue("landuse","greenfield");
				break;
			case "O":
			case "X":
				getAttributes().addAttributeIfNotExistValue("landuse","commercial");
				break;
			case "O02":
			case "X02":
				getAttributes().addAttribute("landuse","commercial");
				getAttributes().addAttribute("fixme","Codigo="+ usodestino +", Profesional superior. Afinar office=X si es posible.");
				break;
			case "O03":
			case "X03":
				getAttributes().addAttribute("landuse","commercial");
				getAttributes().addAttribute("fixme","Codigo="+ usodestino +", Profesional medio. Afinar office=X si es posible.");
				break;
			case "O06":
			case "X06":
				getAttributes().addAttribute("landuse","commercial");
				getAttributes().addAttribute("fixme","Codigo="+ usodestino +", Médicos, abogados... Afinar office=X si es posible.");
				break;
			case "O07":
			case "X07":
				getAttributes().addAttribute("landuse","health");
				addAttributeInConstruIfKeyValue("building", "yes", "health_facility:type","office");
				addAttributeInConstruIfKeyValue("building", "yes", "health_person:type","nurse");
				overwriteAttributeInConstru("building","yes","health");
				break;
			case "O11":
			case "X11":
				getAttributes().addAttribute("landuse","commercial");
				getAttributes().addAttribute("fixme","Codigo="+ usodestino +", Profesores Mercant. Afinar office=X si es posible.");
				break;
			case "O13":
			case "X13":
				getAttributes().addAttribute("landuse","commercial");
				getAttributes().addAttribute("fixme","Codigo="+ usodestino +", Profesores Universitarios. Afinar office=X si es posible.");
				break;
			case "O15":
			case "X15":
				getAttributes().addAttribute("landuse","commercial");
				addAttributeInConstruIfKeyValue("building", "yes", "office","writer");
				break;
			case "O16":
			case "X16":
				getAttributes().addAttribute("landuse","commercial");
				addAttributeInConstruIfKeyValue("building", "yes", "craft","painter");
				break;
			case "O17":
			case "X17":
				getAttributes().addAttribute("landuse","commercial");
				addAttributeInConstruIfKeyValue("building", "yes", "office","musician");
				break;
			case "O43":
			case "X43":
				getAttributes().addAttribute("landuse","commercial");
				addAttributeInConstruIfKeyValue("building", "yes", "office","salesman");
				break;
			case "O44":
			case "X44":
				getAttributes().addAttribute("landuse","commercial");
				getAttributes().addAttribute("fixme","Codigo="+ usodestino +", agentes. Afinar office=X si es posible.");
				break;
			case "O75":
			case "X75":
				getAttributes().addAttribute("landuse","commercial");
				addAttributeInConstruIfKeyValue("building", "yes", "craft","weaver");
				break;
			case "O79":
			case "X79":
				getAttributes().addAttribute("landuse","commercial");
				addAttributeInConstruIfKeyValue("building", "yes", "craft","tailor");
				break;
			case "O81":
			case "X81":
				getAttributes().addAttribute("landuse","commercial");
				addAttributeInConstruIfKeyValue("building", "yes", "craft","carpenter");
				break;
			case "O88":
			case "X88":
				getAttributes().addAttribute("landuse","commercial");
				addAttributeInConstruIfKeyValue("building", "yes", "craft","jeweller");
				break;
			case "O99":
			case "X99":
				getAttributes().addAttribute("landuse","commercial");
				getAttributes().addAttribute("fixme","Codigo="+ usodestino +", otras actividades. Afinar office=X si es posible.");
				break;
			case "P":
			case "Q":
				getAttributes().addAttribute("amenity","public_building");
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","public_building");
				overwriteAttributeInConstru("building","yes","public");
				break;
			case "PAA":
			case "QAA":
				getAttributes().addAttribute("amenity","townhall");
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","townhall");
				overwriteAttributeInConstru("building","yes","public");
				break;
			case "PAD":
			case "QAD":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","courthouse");
				addAttributeInConstruIfKeyValue("building", "yes", "operator","autonomous_community");
				overwriteAttributeInConstru("building","yes","public");
				break;
			case "PAE":
			case "QAE":
				getAttributes().addAttribute("amenity","townhall");
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","townhall");
				overwriteAttributeInConstru("building","yes","public");
				break;
			case "PCB":
			case "QCB":
				getAttributes().addAttribute("office","administrative");
				addAttributeInConstruIfKeyValue("building", "yes", "office","administrative");
				overwriteAttributeInConstru("building","yes","public");
				break;
			case "PDL":
			case "QDL":
			case "PGB":
			case "QGB":
				getAttributes().addAttribute("office","government");
				addAttributeInConstruIfKeyValue("building", "yes", "office","government");
				overwriteAttributeInConstru("building","yes","public");
				break;
			case "PJA":
			case "QJA":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","courthouse");
				addAttributeInConstruIfKeyValue("building", "yes", "operator","county");
				overwriteAttributeInConstru("building","yes","public");
				break;
			case "PJO":
			case "QJO":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","courthouse");
				addAttributeInConstruIfKeyValue("building", "yes", "operator","province");
				overwriteAttributeInConstru("building","yes","public");
				break;
			case "R":
			case "S":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","place_of_worship");
				overwriteAttributeInConstru("building","yes","church");
				break;
			case "RBS":
			case "SBS":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","place_of_worship");
				addAttributeInConstruIfKeyValue("building", "yes", "religion","christian");
				addAttributeInConstruIfKeyValue("building", "yes", "denomination","roman_catholic");
				overwriteAttributeInConstru("building","yes","basilica");
				break;
			case "RCP":
			case "SCP":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","place_of_worship");
				addAttributeInConstruIfKeyValue("building", "yes", "religion","christian");
				addAttributeInConstruIfKeyValue("building", "yes", "denomination","roman_catholic");
				overwriteAttributeInConstru("building","yes","chapel");
				break;
			case "RCT":
			case "SCT":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","place_of_worship");
				addAttributeInConstruIfKeyValue("building", "yes", "religion","christian");
				addAttributeInConstruIfKeyValue("building", "yes", "denomination","roman_catholic");
				overwriteAttributeInConstru("building","yes","cathedral");
				break;
			case "RER":
			case "SER":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","place_of_worship");
				addAttributeInConstruIfKeyValue("building", "yes", "religion","christian");
				addAttributeInConstruIfKeyValue("building", "yes", "denomination","roman_catholic");
				overwriteAttributeInConstru("building","yes","hermitage");
				break;
			case "RPR":
			case "SPR":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","place_of_worship");
				addAttributeInConstruIfKeyValue("building", "yes", "religion","christian");
				addAttributeInConstruIfKeyValue("building", "yes", "denomination","roman_catholic");
				overwriteAttributeInConstru("building","yes","parish_church");
				break;
			case "RSN":
			case "SSN":
				getAttributes().addAttribute("landuse","health");
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","hospital");
				overwriteAttributeInConstru("building","yes","hospital");
				break;
			case "T":
			case "U":
				break;
			case "TAD":
			case "UAD":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","auditorium");
				overwriteAttributeInConstru("building","yes","auditorium");
				break;
			case "TCM":
			case "UCM":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","cinema");
				overwriteAttributeInConstru("building","yes","cinema");
				break;
			case "TCN":
			case "UCN":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","cinema");
				overwriteAttributeInConstru("building","yes","cinema");
				break;
			case "TSL":
			case "USL":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","hall");
				overwriteAttributeInConstru("building","yes","hall");
				break;
			case "TTT":
			case "UTT":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","theatre");
				overwriteAttributeInConstru("building","yes","theatre");
				break;
			case "V":
			case "W":
				getAttributes().addAttributeIfNotExistValue("landuse","residential");
				overwriteAttributeInConstru("building","yes","residential");
				break;
			case "Y":
			case "Z":
				break;
			case "YAM":
			case "ZAM":
			case "YCL":
			case "ZCL":
				getAttributes().addAttribute("landuse","health");
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","clinic");
				addAttributeInConstruIfKeyValue("building", "yes", "medical_system:western","yes");
				overwriteAttributeInConstru("building","yes","clinic");
				break;
			case "YBE":
			case "ZBE":
				getAttributes().addAttribute("landuse","pond");
				break;
			case "YCA":
			case "ZCA":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","casino");
				overwriteAttributeInConstru("building","yes","casino");
				break;
			case "YCB":
			case "ZCB":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","club");
				overwriteAttributeInConstru("building","yes","club");
				break;
			case "YCE":
			case "ZCE":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","casino");
				overwriteAttributeInConstru("building","yes","casino");
				break;
			case "YCT":
			case "ZCT":
				getAttributes().addAttribute("landuse","quarry");
				break;
			case "YDE":
			case "ZDE":
				getAttributes().addAttribute("man_made","wastewater_plant");
				break;
			case "YDG":
				getAttributes().addAttribute("man_made","storage_tank");
				getAttributes().addAttribute("content","gas");
				break;
			case "ZDG":
				getAttributes().addAttribute("landuse","farmyard");
				getAttributes().addAttribute("man_made","storage_tank");
				getAttributes().addAttribute("content","gas");
				break;
			case "YDL":
				addAttributeInConstruIfKeyValue("building", "yes", "man_made","storage_tank");
				addAttributeInConstruIfKeyValue("building", "yes", "content","liquid");
				break;
			case "ZDL":
				getAttributes().addAttribute("landuse","farmyard");
				getAttributes().addAttribute("man_made","storage_tank");
				getAttributes().addAttribute("content","liquid");
				break;
			case "YDS":
			case "ZDS":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","pharmacy");
				addAttributeInConstruIfKeyValue("building", "yes", "dispensing","yes");
				break;
			case "YGR":
			case "ZGR":
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","kindergarten");
				break;
			case "YGV":
			case "ZGV":
				getAttributes().addAttribute("landuse","surface_mining");
				getAttributes().addAttribute("mining_resource","gravel");
				break;
			case "YHG":
			case "ZHG":
				// Como no sabemos a que se puede referir esto, mejor ponemos un fixme
				getAttributes().addAttribute("fixme","Documentar codificación de destino de los bienes inmuebles en catastro código="+ usodestino +" en http://wiki.openstreetmap.org/wiki/Traduccion_metadatos_catastro_a_map_features#Codificaci.C3.B3n_de_los_destinos_de_los_bienes_inmuebles");
				break;
			case "YHS":
			case "ZHS":
			case "YSN":
			case "ZSN":
				getAttributes().addAttribute("landuse","health");
				addAttributeInConstruIfKeyValue("building", "yes", "amenity","hospital");
				addAttributeInConstruIfKeyValue("building", "yes", "medical_system:western","yes");
				overwriteAttributeInConstru("building","yes","hospital");
				break;
			case "YMA":
			case "ZMA":
				getAttributes().addAttribute("landuse","surface_mining");
				getAttributes().addAttribute("fixme","Codigo="+ usodestino +", afinar mining_resource=X si es posible.");
				break;
			case "YME":
			case "ZME":
				getAttributes().addAttribute("man_made","pier");
				break;
			case "YPC":
			case "ZPC":
				getAttributes().addAttribute("landuse","aquaculture");
				break;
			case "YRS":
			case "ZRS":
				addAttributeInConstruIfKeyValue("building", "yes", "social_facility","group_home");
				break;
			case "YSA":
			case "ZSA":
			case "YSO":
			case "ZSO":
				addAttributeInConstruIfKeyValue("building", "yes", "office","labour_union");
				break;
			case "YSC":
			case "ZSC":
				getAttributes().addAttribute("landuse","health");
				addAttributeInConstruIfKeyValue("building", "yes", "health_facility:type","first_aid");
				addAttributeInConstruIfKeyValue("building", "yes", "medical_system:western","yes");
				break;
			case "YSL":
				addAttributeInConstruIfKeyValue("building", "yes", "man_made","storage_tank");
				addAttributeInConstruIfKeyValue("building", "yes", "content","solid");
				break;
			case "ZSL":
				getAttributes().addAttribute("landuse","farmyard");
				addAttributeInConstruIfKeyValue("building", "yes", "man_made","storage_tank");
				addAttributeInConstruIfKeyValue("building", "yes", "content","solid");
				break;
			case "YVR":
			case "ZVR":
				getAttributes().addAttribute("landuse","landfill");
				break;
			default:
				if (!usodestino.isEmpty()){
					getAttributes().addAttribute("fixme","Documentar nuevo codificación de destino de los bienes inmuebles en catastro código="+ usodestino +" en http://wiki.openstreetmap.org/wiki/Traduccion_metadatos_catastro_a_map_features#Codificaci.C3.B3n_de_los_destinos_de_los_bienes_inmuebles");
					}
		}
	}
}
