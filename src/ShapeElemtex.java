import java.sql.Timestamp;
import java.util.Date;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

import org.apache.commons.lang.WordUtils;

public class ShapeElemtex extends ShapePoint {

	// Variable autoincremental que se concatena al shapeId
	private volatile static long URID = (long) 0;
	private volatile static long RUID = (long) 0;
	private String rotulo; // Campo Rotulo solo en Elemtex.shp

	public ShapeElemtex(SimpleFeature f, String tipo) {

		super(f, tipo);

		shapeId = "ELEMTEX" + tipo + (tipo.equals("UR") ? (URID = super.newShapeId(URID)) : (RUID = super.newShapeId(RUID)));

		// Elemtex trae la geometria en formato MultiLineString
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiLineString")){

			geometry = (Point)((MultiLineString) f.getDefaultGeometry()).getCentroid();
			geometry.normalize();
			
		}
		else {
			System.out.println("["+new Timestamp(new Date().getTime())+"]\tFormato geometrico "+ 
					f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile ELEMTEX");
		}

		// Los demas atributos son metadatos y de ellos sacamos 
		ttggss = (String) f.getAttribute("TTGGSS");

		//		try {
		//			rotulo = new String(f.getAttribute("ROTULO").toString().getBytes(), "UTF-8");
		//			rotulo = eliminarComillas(rotulo);			
		//		} catch (UnsupportedEncodingException e) {e.printStackTrace();}

		rotulo = eliminarComillas(f.getAttribute("ROTULO").toString());
		if (rotulo.equals(rotulo.toUpperCase())) {
			char[] delim = {' ','.'};
			rotulo = WordUtils.capitalizeFully(rotulo, delim);
		}

		// Se agregan tags dependientes del rotulo
		getAttributes().addAll(Rules.getTags(rotulo));

		// Dependiendo del ttggss se usa o no
		if (ttggss != null && rotulo != null)
			getAttributes().addAll(ttggssParser(ttggss, rotulo));

		// Para agrupar geometrias segun su codigo de masa que como en este caso no existe se
		// asigna el del nombre del fichero shapefile
		codigoMasa = "ELEMTEX-" + ttggss;
	}


	public void setCoor(Coordinate c){
			
		// Creamos la factoria para crear objetos de GeoTools (hay otra factoria pero falla)
		// com.vividsolutions.jts.geom.GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
		GeometryFactory gf = new GeometryFactory(geometry.getPrecisionModel());
		geometry = gf.createPoint(c);
	}

	
	public boolean isAligned(Coordinate a, Coordinate b){
		return (getGeometry().getCoordinate().y - a.y)*(b.x-a.x) ==
				(b.y-a.y)*(getGeometry().getCoordinate().x-a.x);
	}

	
	public String getRotulo() {
		return rotulo;
	}

	public void setRotulo(String rotulo) {
		this.rotulo = rotulo;
	}


	/** Dependiendo del ttggss desechamos algunos Elemtex que no son necesarios.
	 */
	public boolean isValid (){

		if (!Cat2OsmUtils.getOnlyEntrances()){
			// Modo todos los elemtex de Parajes y Comarcas, Informacion urbana 
			// y rustica y Vegetacion y Accidentes demograficos
			if (ttggss.equals("189203"))
				return Rules.isValid(rotulo);
			else if (ttggss.equals("189300"))
				return Rules.isValid(rotulo);
			else if (ttggss.equals("189700"))
				return Rules.isValid(rotulo);
			else
				return false;
		}
		else{
			// Modo solo portales, solamente sacar los portales
			if (ttggss.equals("189401"))
				return true;
			else
				return false;
		}
	}
}
