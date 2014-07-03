import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opengis.feature.simple.SimpleFeature;

import com.linuxense.javadbf.DBFReader;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.util.PolygonExtracter;

public class ShapeSubparce extends ShapePolygonal {

	// Variable autoincremental que se concatena al shapeId
	private volatile static Long URID = (long) 0;
	private volatile static Long RUID = (long) 0;
	private String subparce; // Clave de Subparcela
	private String cultivo; // Codigo de cultivo de la subparcela
	private long area; // Area para saber si poner landuse allotments (<400m2) o el que sea
	private static final Map<String,Map<String,String>> lSub = new HashMap<String,Map<String,String>>(); // Mapa <RefCat<ClaveSubparce,CodigoCultivo>> (para el Subparce.shp)


	/** Constructor
	 * @throws IOException 
	 */
	public ShapeSubparce(SimpleFeature f, String tipo) throws IOException {

		super(f, tipo);

		shapeId = "SUBPARCE" + tipo + (tipo.equals("UR") ? (URID = super.newShapeId(URID)) : (RUID = super.newShapeId(RUID)));
		
		// Para agrupar geometrias segun su codigo de masa
		codigoMasa = ((String) f.getAttribute("MASA")).replaceAll("[^\\p{L}\\p{N}]", "")+"-";

		if (lSub.isEmpty()){
			readSubparceDetails(tipo);
		}

		// Parcela.shp trae la geometria en formato MultiPolygon
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiPolygon")){

			// Poligono, trae el primer punto de cada poligono repetido al final.
			geometry = (MultiPolygon) f.getDefaultGeometry();
			
			// Eliminamos posibles poligonos multiples
			List<?> polys = PolygonExtracter.getPolygons(geometry.union());
			geometry = geometry.getFactory().buildGeometry(polys);
			geometry.normalize();

		}
		else
			System.out.println("["+new Timestamp(new Date().getTime())+"]\tFormato geometrico "+ 
					f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile SUBPARCE");

		// Los demas atributos son metadatos y de ellos sacamos 
		referenciaCatastral = (String) f.getAttribute("REFCAT");
		
		subparce = (String) f.getAttribute("SUBPARCE");
		if (subparce != null){
			cultivo = getCultivo(referenciaCatastral,subparce);
		}
		
		if (cultivo != null){
			getAttributes().addAll(cultivoParser(cultivo));
		}
		
		// Al campo AREA puede estar en distintos formatos, ogr2ogr cambia el formato
		if (f.getAttribute("AREA") instanceof Double){
			double a = (Double) f.getAttribute("AREA");
			setArea((long) a);
		}
		else if (f.getAttribute("AREA") instanceof Long){
			setArea((Long) f.getAttribute("AREA"));
		}
		else if (f.getAttribute("AREA") instanceof Integer){
			int a = (Integer) f.getAttribute("AREA");
			setArea((long) a);
		}
		else System.out.println("["+new Timestamp(new Date().getTime())+"]\tNo se reconoce el tipo del atributo AREA "
				+ f.getAttribute("AREA").getClass().getName());	
	}
	

	public String getSubparce(){
		return subparce;
	}


	/** Lee el archivo Rusubparcela.dbf y lo almacena para despues relacionar la clave subparce 
	 * de Subparce.shp con la calificacion catastral que trae Rusubparcela.dbf. Con la cc se accedera
	 * al rucultivo.dbf. Se supone que estos archivos solo existen en el caso de subparcelas rusticas,
	 * por si acaso se pasa el tipo para futuras mejoras.
	 * @throws IOException 
	 */
	public void readSubparceDetails(String tipo) throws IOException {

		if (tipo.equals("RU")){
			InputStream inputStream = new FileInputStream(Config.get("RusticoSHPPath") + "/RUSUBPARCELA/RUSUBPARCELA.DBF");
			DBFReader reader = new DBFReader(inputStream);
			Object[] rowObjects;

			while((rowObjects = reader.nextRecord()) != null) {

				// La posicion 2 es la referencia catastral
				// La posicion 6 es la clave de la subparcela dentro de esa parcela
				// La posicion 8 es la calificacion catastral = codigo de cultivo
				if (lSub.get(((String) rowObjects[2]).trim()) == null)
					lSub.put(((String) rowObjects[2]).trim(), new HashMap<String,String>());

				lSub.get(((String) rowObjects[2]).trim()).put(((String) rowObjects[6]).trim(), ((String) rowObjects[8]).trim());
			}
			inputStream.close();
		}
	}  


	/**Relaciona el codigo de subparcela que trae el Subparce.shp con
	 * el codigo de cultivo que trae Rusubparcela.dbf Solo es para subparcelas rurales
	 * @param v Numero de subparcela a buscar
	 * @return String tipo de cultivo
	 */
	public String getCultivo(String refCat, String subparce){

		if (lSub.get(refCat) == null || lSub.get(refCat).isEmpty())
			return "";

		if (lSub.get(refCat).get(subparce) == null || lSub.get(refCat).get(subparce).isEmpty())
			return "";

		return lSub.get(refCat).get(subparce);
	} 


	public boolean isValid (){

		if (cultivo == null)
			return true;
		if (cultivo.equals("VT"))
			return false;
		else
			return true;
	}

	/** Parsea el tipo de cultivo con la nomenclatura de catastro y lo convierte
	 * a los tags de OSM
	 * @param cultivo Cultivo con la nomenclatura de catastro
	 * @return Lista de los tags que genera
	 */
	public Map<String,String> cultivoParser(String cultivo){
		Map<String,String>  l = new HashMap<String,String> ();
		
		switch(cultivo.toUpperCase()){

		case"A-":
		case"A":
			if (this.area <= 400) {
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("crop","rice");
			l.put("irrigated","yes");
			return l;
		case"AB":
		case"AK":
		case"HB":
		case"HK":
		case"HR":
			if (this.area <= 400){
				l.put("landuse","allotments");
				}
			else {
				l.put("landuse","farmland");
			}
			l.put("crop","vegetables");
			l.put("irrigated","yes");
			return l;
		case"AG":
			return l;
		case"AM":
			if (this.area <= 400){
				l.put("landuse","allotments");
				}
			else {
				l.put("landuse","orchard");
			}			
			l.put("trees","almond_trees");
			l.put("irrigated","no");
			return l;
		case"AO":
			if (this.area <= 400){
				l.put("landuse","allotments");
				}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","hazels");
			l.put("irrigated","no");
			return l;
		case"AP":
			if (this.area <= 400){
				l.put("landuse","allotments");
				}
			else {
				l.put("landuse","vineyard");
			}
			l.put("trees","olives");
			return l;
		case"AT":
		case"AY":
		case"AZ":
			if (this.area <= 400) {
				l.put("landuse","allotments");
				}
			else {
				l.put("natural","scrub");
			}			
			l.put("scrub","esparto");
			return l;
		case"AV":
			if (this.area <= 400){
				l.put("landuse","allotments");
				}
			else {
				l.put("landuse","orchard");	
			}
			l.put("trees","hazels");
			l.put("irrigated","yes");
			return l;
		case"BB":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else{
				l.put("landuse","farmyard");
			}
			l.put("livestock","cattle");
			l.put("produce","fighting_bulls");
			return l;
		case"BC":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmyard");
			}
			l.put("livestock","cattle");
			l.put("produce","meat");
			return l;
		case"BL":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmyard");
			}
			l.put("livestock","cattle");
			l.put("produce","milk");
			return l;
		case"BM":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmyard");
			}
			l.put("livestock","cattle");
			l.put("produce","milk,meat");
			return l;
		case"C-":
		case"C":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("irrigated","no");
			return l;
		case"CA":
			l.put("landuse","quarry");
			return l;
		case"CB":
		case"CK":
		case"HT":
		case"HV":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("crop","cereal");
			l.put("irrigated","yes");
			return l;
		case"CC":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("trees","chestnut_trees");
			l.put("irrigated","no");
			return l;
		case"CE":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("trees","holm_oaks");
			l.put("irrigated","no");
			return l;
		case"CF":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("trees","fruit_trees");
			l.put("irrigated","no");
			return l;
		case"CG":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("trees","oaks");
			l.put("irrigated","no");
			return l;
		case"CH":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("trees","prickly_pears");
			l.put("irrigated","no");
			return l;
		case"CL":
			l.put("landuse","farmyard");
			l.put("livestock","goats");
			l.put("produce","milk");
			return l;
		case"CM":
			l.put("landuse","farmyard");
			l.put("livestock","goats");
			l.put("produce","meat");
			return l;
		case"CN":
		case"CQ":
		case"CT":
		case"CV":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("crop","cereal");
			l.put("irrigated","no");
			return l;
		case "CP":
			return l;
		case"CR":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("irrigated","yes");
			return l;
		case"CS":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("trees","cork_oaks");
			l.put("irrigated","no");
			return l;
		case"CX":
			return l;
		case"CZ":
			l.put("tourism","camp_site");
			return l;
		case"E-":
		case"E":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","meadow");
			}
			l.put("meadow","perpetual");
			return l;
		case"EA":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmyard");
			}
			l.put("use","agricultural");
			return l;
		case"EE":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","meadow");
			}
			l.put("meadow","perpetual");
			l.put("trees","holm_oaks");
			return l;
		case"EG":
			l.put("landuse","farmyard");
			l.put("use","livestocking");
			l.put("building","yes");
			return l;
		case"EO":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","meadow");
			}
			l.put("meadow","perpetual");
			l.put("trees","olives");
			return l;
		case"ES":
			return l;
		case"EU":
		case"KI":
			l.put("landuse","forest");
			l.put("type","evergreen");
			l.put("trees","eucalyptus");
			return l;
		case"EV":
			return l;
		case"EX":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmfield");
			}
			return l;
		case"F-":
		case"F":
		case"FY":
		case"FZ":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else{
				l.put("landuse","orchard");
			}
			l.put("irrigated","no");
			return l;
		case"FA":
		case"TF":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			return l;
		case"FB":
		case"FK":
		case"FR":
		case"FV":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("irrigated","yes");
			return l;
		case"FC":
		case"SC":
		case"ZC":
			l.put("landuse","forest");
			l.put("wood","deciduous");
			l.put("type","deciduous");
			l.put("trees","chestnut_trees");
			return l;
		case"FE":
		case"KE":
		case"SE":
		case"ZE":
			l.put("landuse","forest");
			l.put("type","evergreen");
			l.put("trees","holm_oaks");
			return l;
		case"FF":
			l.put("landuse","railway");
			return l;
		case"FG":
		case"KG":
		case"SG":
		case"ZG":
			l.put("landuse","forest");
			l.put("wood","deciduous");
			l.put("trees","oaks");
			return l;
		case"FH":
		case"KH":
		case"SH":
		case"ZH":
			l.put("landuse","forest");
			l.put("wood","deciduous");
			l.put("trees","beeches");
			return l;
		case"FM":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","peach_trees");
			l.put("irrigated","yes");
			return l;
		case"FN":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","apple_trees");
			l.put("irrigated","yes");
			return l;
		case"FO":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","cherry_trees");
			l.put("irrigated","yes");
			return l;
		case"FP":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","pear_trees");
			l.put("irrigated","yes");
			return l;
		case"FQ":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","apricot_trees_trees");
			return l;
		case"FS":
		case"ZS":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("trees","cork_oaks");
			return l;
		case"FU":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","plum_trees");
			l.put("irrigated","yes");
			return l;
		case"G-":
		case"G":
			if (this.area <= 400){
				l.put("landuse","allotments");
				}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","carobs");
			l.put("irrigated","no");
			return l;
		case"GB":
			l.put("landuse","surface_mining");
			return l;
		case"GR":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","carobs");
			l.put("irrigated","yes");
			return l;
		case"HC":
			l.put("landuse","reservoir");
			l.put("fixme","Comprobar si es embalse, acequia o canal...");
			return l;
		case"HE":
		case"HY":
		case"HZ":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("crop","vegetables");
			return l;
		case"HG":
			l.put("waterway","riverbank");
			return l;
		case"HS":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("crop","vegetables");
			l.put("irrigated","no");
			return l;
		case"I-":
		case"I":
			return l;
		case"IF":
		case"IO":
			l.put("landuse","greenhouse_horticulture");
			l.put("crop","flowers");
			return l;
		case"IH":
			l.put("landuse","greenhouse_horticulture");
			l.put("crop","vegetables");
			return l;
		case"IN":
			l.put("landuse","greenhouse_horticulture");
			return l;
		case"IR":
			if (this.area <= 400){
				l.put("landuse","allotments");
				}
			else{
				l.put("landuse","farmland");
			}
			l.put("irrigated","yes");
			return l;
		case"KA":
		case"MA":
			l.put("landuse","forest");
			l.put("wood","coniferous");
			l.put("type","evergreen");
			l.put("trees","firs");
			return l;
		case"KB":
		case"MS":
			l.put("landuse","forest");
			l.put("wood","coniferous");
			l.put("type","evergreen");
			l.put("trees","tetraclinis");
			return l;
		case"KC":
			l.put("landuse","forest");
			l.put("wood","deciduous");
			l.put("type","deciduous");
			l.put("trees","chestnut_trees");
			return l;
		case"KL":
		case"MH":
			l.put("landuse","forest");
			l.put("wood","coniferous");
			l.put("type","evergreen");
			l.put("trees","larches");
			return l;
		case"KN":
		case"ME":
			l.put("landuse","forest");
			l.put("wood","coniferous");
			l.put("type","evergreen");
			l.put("trees","junipers");
			return l;
		case"KP":
		case"KY":
		case"KZ":
			l.put("landuse","forest");
			l.put("wood","coniferous");
			l.put("type","evergreen");
			l.put("trees","pines");
			return l;
		case"KR":
		case"RI":
		case"RY":
		case"RZ":
			l.put("landuse","forest");
			l.put("wood","deciduous");
			l.put("type","deciduous");
			l.put("trees","aspens");
			return l;
		case"KS":
		case"SS":
			l.put("landuse","forest");
			l.put("wood","deciduous");
			l.put("type","deciduous");
			l.put("trees","cork_oaks");
			return l;
		case"KX":
		case"MX":
			l.put("landuse","forest");
			l.put("wood","coniferous");
			l.put("type","evergreen");
			l.put("trees","spanish_firs");
			return l;
		case"LA":
			l.put("landuse","farmyard");
			l.put("livestock","sheep");
			l.put("produce","wool");
			return l;
		case"LE":
			l.put("landuse","farmyard");
			l.put("livestock","sheep");
			l.put("produce","wool");
			l.put("variety","entrefino");
			return l;
		case"LG":
			l.put("landuse","farmyard");
			l.put("livestock","sheep");
			l.put("produce","wool");
			l.put("variety","alcudia");
			return l;
		case"LM":
			l.put("landuse","farmyard");
			l.put("livestock","sheep");
			l.put("produce","wool");
			l.put("variety","manchego");
			return l;
		case"LP":
			l.put("landuse","farmyard");
			l.put("livestock","sheep");
			l.put("produce","wool");
			l.put("variety","monte");
			return l;
		case"LT":
			l.put("landuse","farmyard");
			l.put("livestock","sheep");
			l.put("produce","wool");
			l.put("variety","talaverano");
			return l;
		case"MB":
		case"MT":
			l.put("natural","scrub");
			return l;
		case"MF":
			l.put("landuse","forest");
			l.put("wood","mixed");
			l.put("type","mixed");
			return l;
		case"MI":
			l.put("natural","wetland");
			l.put("wetland","marsh");
			return l;
		case"MM":
			l.put("landuse","forest");
			l.put("wood","coniferous");
			l.put("type","evergreen");
			l.put("trees","pines");
			l.put("produce","wood");
			return l;
		case"MP":
			l.put("landuse","forest");
			l.put("wood","coniferous");
			l.put("type","evergreen");
			l.put("trees","pines");
			l.put("produce","pine_cones");
			return l;
		case"MR":
			l.put("landuse","forest");
			l.put("wood","coniferous");
			l.put("type","evergreen");
			l.put("trees","pines");
			l.put("produce","resin");
			return l;
		case"MY":
		case"MZ":
			return l;
		case"NB":
		case"NK":
		case"NR":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","citrus_trees");
			l.put("irrigated","yes");
			return l;
		case"NJ":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","orange_trees");
			return l;
		case"NL":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","lemon_trees");
			return l;
		case"NM":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else{
				l.put("landuse","orchard");
			}
			l.put("trees","tangerine_trees");
			return l;
		case"NS":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","citrus_trees");
			l.put("irrigated","no");
			return l;
		case"O-":
		case"O":
		case"OY":
		case"TO":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","olives");
			return l;
		case"OB":
		case"OK":
		case"OR":
		case"OS":
		case"OV":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","olives");
			l.put("irrigated","yes");
			return l;
		case"OZ":
		case"VO":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","olives");
			l.put("irrigated","no");
			return l;
		case"PA":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","farmland");
			}
			l.put("crop","herbs");
			return l;
		case"PC":
			l.put("landuse","farmyard");
			l.put("livestock","pigs");
			l.put("variety","fooder-fed");
			return l;
		case"PD":
		case"PH":
		case"PP":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","meadow");
			}
			l.put("meadow","agricultural");
			return l;
		case"PF":
			l.put("landuse","aquaculture");
			return l;
		case"PL":
			l.put("natural","scrub");
			l.put("scrub","palms");
			return l;
		case"PM":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","palm_trees");
			l.put("irrigated","no");
			return l;
		case"PR":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","meadow");
			}
			l.put("meadow","agricultural");
			l.put("irrigated","yes");
			return l;
		case"PV":
			l.put("landuse","farmyard");
			l.put("livestock","pigs");
			return l;
		case"PZ":
			l.put("landuse","pond");
			return l;
		case"R-":
		case"R":
		case"RR":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","fig_trees");
			l.put("irrigated","no");
			return l;
		case"SM":
			l.put("landuse","salt_pond");
			return l;
		case"TA":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","almond_trees");
			return l;
		case"TG":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","carobs");
			return l;
		case"TM":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else{
				l.put("landuse","farmland");
			}
			l.put("crop","tomatoes");
			l.put("irrigated","yes");
			return l;
		case"TN":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","citrus_trees");
			return l;
		case"PT":
			if (this.area <= 400){
				l.put("landuse","allotments");}
			else {
				l.put("landuse","orchard");
			}
			l.put("trees","banana_trees");
			return l;
		case"TR":
			return l;
		case"U-":
		case"U":
			l.put("landuse","residential");
			return l;
		case"V-":
		case"V":
			l.put("landuse","vineyard");
			l.put("irrigated","no");
			return l;
		case"VB":
		case"VK":
		case"VP":
		case"VR":
		case"VY":
		case"VZ":
			l.put("landuse","vineyard");
			l.put("irrigated","yes");
			return l;
		case"VC":
			return l;
		case"VS":
		case"VV":
			l.put("landuse","vineyard");
			return l;
		case"VT":
			return l;
		case"XX":
			return l;
		case"Z-":
			if (this.area <= 400){
				l.put("landuse","allotments");
			}
			else{
				l.put("landuse","farmland");
			}
			l.put("crop","sumac");
			l.put("irrigated","no");
			return l;
		default:
			if (l.isEmpty() && !cultivo.isEmpty()){
				l.put("fixme","Tagear cultivo "+ cultivo +" en http://wiki.openstreetmap.org/w/index.php?title=Traduccion_metadatos_catastro_a_map_features#Tipos_de_cultivo.");
			}
			return l;
		}
	}

	public long getArea() {
		return area;
	}

	public void setArea(long area) {
		this.area = area;
	}
}
