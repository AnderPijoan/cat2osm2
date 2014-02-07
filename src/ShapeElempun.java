import java.sql.Timestamp;
import java.util.Date;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Point;


public class ShapeElempun extends ShapePoint {

	// Variable autoincremental que se concatena al shapeId
	private volatile static long URID = (long) 0;
	private volatile static long RUID = (long) 0;

	public ShapeElempun(SimpleFeature f, String tipo) {

		super(f, tipo);

		shapeId = "ELEMPUN" + tipo + (tipo.equals("UR") ? (URID = super.newShapeId(URID)) : (RUID = super.newShapeId(RUID)));

		// Elempun trae la geometria en formato Point
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.Point")){

			geometry = (Point) f.getDefaultGeometry();
			geometry.normalize();
		}
		else {
			System.out.println("["+new Timestamp(new Date().getTime())+"]\tFormato geometrico "+ 
					f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile ELEMPUN");
		}

		// Los demas atributos son metadatos y de ellos sacamos 

		ttggss = (String) f.getAttribute("TTGGSS");
		
		if (ttggss != null){
			getAttributes().addAll(ttggssParser(ttggss, ""));
		}

		// Para agrupar geometrias segun su codigo de masa que como en este caso no existe se
		// asigna el del nombre del fichero shapefile
		codigoMasa = "ELEMPUN-" + ttggss;
	}

	public boolean isValid (){

		switch(ttggss){
		case "030202":
			return true;
		case "030302":
			return true;
		case "037101":
			return true;
		case "038101":
			return true;
		case "038102":
			return true;
		case "037102":
			return true;
		case "068401":
			return true;
		case "167111":
			return true;
		case "167201":
			return true;
		case "168103":
			return true;
		case "168116":
			return true;
		case "168153":
			return true;
		case "168168":
			return true;
		case "168113":
			return true;
		case "060402":
			return false;
		case "060202":
			return false;
		case "160300":
			return false;
		case "067121":
			return false;
		case "160101":
			return false;
		case "115101":
			return false;
		case "168100":
			return false;
		default:
			return true;
		}
	}

}
