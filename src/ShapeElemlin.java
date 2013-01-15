import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Date;

import org.opengis.feature.simple.SimpleFeature;

import com.linuxense.javadbf.DBFReader;
import com.vividsolutions.jts.geom.MultiLineString;


public class ShapeElemlin extends ShapeLinear {

	// Variable autoincremental que se concatena al shapeId
	private volatile static long URID = (long) 0;
	private volatile static long RUID = (long) 0;
	private String ttggss; // Campo TTGGSS en Elemlin.shp

	
	public ShapeElemlin(SimpleFeature f, String tipo) {

		super(f, tipo);

		shapeId = "ELEMLIN" + tipo + (tipo.equals("UR") ? (URID = super.newShapeId(URID)) : (RUID = super.newShapeId(RUID)));

		// Elemtex trae la geometria en formato MultiLineString
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiLineString")){

			geometry = (MultiLineString) f.getDefaultGeometry();
			geometry.normalize();
		}
		else {
			System.out.println("["+new Timestamp(new Date().getTime())+"] Formato geometrico "+ 
					f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile ELEMLIN");
		}

		// Los demas atributos son metadatos y de ellos sacamos

		ttggss = (String) f.getAttribute("TTGGSS");
		
		if (ttggss != null){
			addAttributesAsStringArray(ttggssParser(ttggss));
		}

		// Para agrupar geometrias segun su codigo de masa que como en este caso no existe se
		// asigna el del nombre del fichero shapefile
		// En este caso se anade "ELEMLIN" por delante para que luego el proceso al encontrar un key
		// de ELEMLIN intente juntar todos los ways con todos los que se toquen
		// (a diferencia de las otros elementos que solo tiene que unir ways si pertenecen
		// a los mismos shapes)
		codigoMasa = "ELEMLIN-" + ttggss;
	}


	public String getTtggss() {
		return ttggss;
	}


	public boolean isValid (){

		switch(ttggss){
		//case "030202":
			//return true; // Margenes de rios que van a tener natural=water
		//case "030302":
			//return true; // Emborrona mas que ayudar
		case "037101":
			return true;
		case "038101":
			return true;
		case "038102":
			return true;
		case "037102":
			return true;
		case "167111":
			return true;
		default:
			return false;
		}
	}


	/** Lee el archivo Carvia.dbf y relaciona el numero de via que trae el Elemlin.shp con
	 * los nombres de via que trae el Carvia.dbf.
	 * @param v Numero de via a buscar
	 * @return String tipo y nombre de via
	 * @throws IOException
	 */
	public String getVia(long v) throws IOException{
		InputStream inputStream  = new FileInputStream(Config.get("UrbanoSHPDir") + "/CARVIA/CARVIA.DBF");
		DBFReader reader = new DBFReader(inputStream); 

		Object[] rowObjects;

		while((rowObjects = reader.nextRecord()) != null) {

			if ((Double) rowObjects[2] == (v)){
				inputStream.close();
				return ((String) rowObjects[3]);
			}
		}
		return null;
	}
}
