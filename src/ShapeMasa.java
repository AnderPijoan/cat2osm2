import java.sql.Timestamp;
import java.util.Date;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;

public class ShapeMasa extends ShapeParent {
	
	// Variable autoincremental que se concatena al shapeId
	private volatile static Long URID = (long) 0;
	private volatile static Long RUID = (long) 0;
	private String masa; // Codigo de masa, solo en Masa.shp


	/** Constructor
	 * @param f Linea del archivo shp
	 */
	public ShapeMasa (SimpleFeature f, String tipo) {

		super(f, tipo);
		
		shapeId = "MASA" + tipo + (tipo.equals("UR") ? (URID = super.newShapeId(URID)) : (RUID = super.newShapeId(RUID)));
		
		// Para agrupar geometrias segun su codigo de masa
		codigoMasa = ((String) f.getAttribute("MASA")).replaceAll("[^\\p{L}\\p{N}]", "")+"-";

		// Masa.shp trae la geometria en formato MultiPolygon
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiPolygon")){

			// Poligono, trae el primer punto de cada poligono repetido al final.
			geometry = (MultiPolygon) f.getDefaultGeometry();

		}
		else 
			System.out.println("["+new Timestamp(new Date().getTime())+"] Formato geometrico "+ 
		f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile MASA");

		// Los demas atributos son metadatos y de ellos sacamos 
		masa = (String) f.getAttribute("MASA");
		
		if (masa != null){
			addAttribute("masa", masa);
		}
	}
	
	
	public boolean isValid (){
		return true;
	}



	public void createAttributesFromUsoDestino() {		
	}
}
