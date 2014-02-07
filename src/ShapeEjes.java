import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opengis.feature.simple.SimpleFeature;

import com.linuxense.javadbf.DBFReader;
import com.vividsolutions.jts.geom.MultiLineString;

public class ShapeEjes extends ShapeLinear {
	
	// Variable autoincremental que se concatena al shapeId
	private volatile static long URID = (long) 0;
	private volatile static long RUID = (long) 0;
	private String via; // Nombre de via, solo en Ejes.shp, lo coge de Carvia.dbf
	private static final Map<Long,String> ejesNames = new HashMap<Long, String>(); // Lista de codigos y nombres de vias (para el Ejes.shp)

	
	public ShapeEjes(SimpleFeature f, String tipo) throws IOException {
		
		super(f, tipo);
		
		shapeId = "EJES"  + tipo + (tipo.equals("UR") ? (URID = super.newShapeId(URID)) : (RUID = super.newShapeId(RUID)));

		if (tipo.equals("UR") && ejesNames.isEmpty()){
			readCarvia(tipo);
		}
		
		// Ejes trae la geometria en formato MultiLineString
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiLineString")){

			geometry = (MultiLineString) f.getDefaultGeometry();
			geometry.normalize();
		}
		else {
			System.out.println("["+new Timestamp(new Date().getTime())+"]\tFormato geometrico "+
		f.getDefaultGeometry().getClass().getName() +" desconocido del shapefile EJES");
		}
		
		if (tipo.equals("UR") && f.getAttribute("VIA") instanceof Double){
			double v = (Double) f.getAttribute("VIA");
			via = getVia((long) v);
		}
		else if (tipo.equals("UR") && f.getAttribute("VIA") instanceof Long){
			via = getVia((long) f.getAttribute("VIA"));
		}
		else if (tipo.equals("UR") && f.getAttribute("VIA") instanceof Integer){
			int v = (Integer) f.getAttribute("VIA");
			via = getVia((long) v);
		}
		else if (tipo.equals("UR") && f.getAttribute("VIA") instanceof String){
			int v = Integer.parseInt((String) f.getAttribute("VIA"));
			via = getVia((long) v);
		}
		else if(tipo.equals("UR"))  System.out.println("["+new Timestamp(new Date().getTime())+"]\tNo se reconoce el tipo del atributo VIA "+ f.getAttribute("VIA").getClass().getName() );	

		// Para agrupar geometrias segun su codigo de masa que como en este caso no existe se
		// asigna el del nombre del fichero shapefile
		// En este caso se anade "EJES" por delante para que luego el proceso al encontrar un key
		// de EJES intente juntar todos los ways con todos los que se toquen
		// (a diferencia de las otros elementos que solo tiene que unir ways si pertenecen
		// a los mismos shapes)
		codigoMasa = (via == null ? "EJES-SINNOMBRE" : "EJES-" + via.trim().replaceAll("[^\\p{L}\\p{N}]", ""));
		
		// Via trae el tipo (substring de 0 a 2) y el nombre (substring de 3 en adelante)
		// Se parsea el tipo para traducirlo al nombre del tipo y para sacar tags extra

		if (via != null && !via.isEmpty()){
			// Nombre de la via
			getAttributes().addAttribute("name", nombreTipoViaParser(via.substring(0, 2)) +" "+ formatearNombreCalle(eliminarComillas(via.substring(3))));
			// En funcion del tipo de via, meter tags que la describan
			getAttributes().addAll(atributosViaParser(via.substring(0, 2)));
		}
		else{
			getAttributes().addAttribute("highway", "unclassified");
		}
		
		if (tipo.equals("UR")){
			getAttributes().addAttribute("highway", "residential");
		}
	}
	
	
	public Map<String, String> atributosViaParser(String codigo){
		Map<String, String> l = new HashMap<String, String>();

		switch(codigo){
		
		case "AU":
			l.put("highway","motorway");
		return l;
		
		case "AY":
			l.put("waterway","stream");
		return l;
		
		case "CG":
			l.put("amenity","school");
		return l;
		
		case "CJ":
			l.put("highway","residential");
		return l;
		
		case "CM":
			l.put("highway","track");
		return l;
		
		case "CR":
			l.put("highway","unclassified");
		return l;
		
		case "ES":
			l.put("highway","steps");
		return l;
		
		case "FC":
			l.put("railway","rail");
		return l;
		
		case "GL":
			l.put("junction","roundabout");
		return l;
		
		case "GV":
			l.put("highway","primary");
		return l;
		
		case "JR":
			l.put("leisure","garden");
		return l;
		
		case "MC":
			l.put("amenity","marketplace");
		return l;
		
		case "ML":
			l.put("waterway","dock");
		return l;
		
		case "PZ":
			l.put("highway","unclassified");
		return l;
		
		case "RD":
			l.put("highway","unclassified");
		return l;
		
		case "RU":
			l.put("highway","residential");
		return l;
		
		case "SD":
			l.put("highway","path");
		return l;
		
		case "TR":
			l.put("highway","path");
		return l;
		
		case "UR":
			l.put("highway","residential");
		return l;
		
		default:
			if (codigo.isEmpty()){
				l.put("fixme","Tagear tipo de via "+ codigo +" en http://wiki.openstreetmap.org/w/index.php?title=Traduccion_metadatos_catastro_a_map_features.");
			}
		}
		return l;
	}

	
	public String getTtggss() {
		return null;
	}
	
	
	public boolean isValid (){
		return true;
	}
	
	
	/** Lee el archivo Carvia.dbf y lo almacena para despues relacionar el numero de via 
	 * de Ejes.shp con los nombres de via que trae Carvia.dbf. El nombre de via trae tambien el tipo
	 * en formato 2caracteres de tipo de via, un espacio en blanco y el nombre de via
	 * @param file Archivo Ejes.shp del para acceder a su Carvia.dbf
	 * @throws IOException 
	 * @throws IOException
	 */
	public void readCarvia(String tipo) throws IOException{
		
		InputStream inputStream = null;
		if (tipo.equals("UR"))
		inputStream = new FileInputStream(Config.get("UrbanoSHPPath") + "/CARVIA/Carvia.DBF");
		else if (tipo.equals("RU"))
		inputStream = new FileInputStream(Config.get("RusticoSHPPath") + "/CARVIA/Carvia.DBF");
		DBFReader reader = new DBFReader(inputStream);
		
		int indiceVia = 0;
		int indiceDenomina = 0;
		for (int i = 0; i < reader.getFieldCount(); i++)
		{
			if (reader.getField(i).getName().equals("VIA"))      indiceVia = i;
			if (reader.getField(i).getName().equals("DENOMINA")) indiceDenomina = i;
		}
		
		Object[] rowObjects;

		while((rowObjects = reader.nextRecord()) != null) {
			
			// La posicion indiceVia es el codigo de via
			// La posicion indiceDenomina es la denominacion de via
			if (rowObjects[indiceVia] instanceof Double){
				double v = (Double) rowObjects[indiceVia];
				ejesNames.put((long) v, (String) rowObjects[indiceDenomina]);
			}
			else if (rowObjects[indiceVia] instanceof Long){
				ejesNames.put((Long) rowObjects[indiceVia], (String) rowObjects[indiceDenomina]);
			}
			else if (rowObjects[indiceVia] instanceof Integer){
				int v = (Integer) rowObjects[indiceVia];
				ejesNames.put((long) v, (String) rowObjects[indiceDenomina]);
			}
		}
	}  
	
	
	/** Relaciona el numero de via de Ejes.shp con los nombres de via que trae Carvia.dbf. El nombre de via trae tambien el tipo
	 * en formato 2caracteres de tipo de via, un espacio en blanco y el nombre de via
	 * @param v Numero de via a buscar
	 * @return String tipo y nombre de via
	 */
	public String getVia(long v){
			return ejesNames.get(v);
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
}
