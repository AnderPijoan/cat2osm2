import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

import org.apache.commons.lang3.text.WordUtils;

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
			
		}
		else {
			System.out.println("["+new Timestamp(new Date().getTime())+"] Formato geometrico "+ 
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
		addAttributesAsStringArray(Rules.getTags(rotulo));

		// Dependiendo del ttggss se usa o no
		if (ttggss != null)
			addAttributesAsStringArray(ttggssParser(ttggss));

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


	public String getRotulo() {
		return rotulo;
	}

	public void setRotulo(String rotulo) {
		this.rotulo = rotulo;
	}


	/** Traduce el atributo ttggss. Los que tengan ttggss = 0 no se tienen en cuenta
	 * ya que hay elementos textuales que no queremos mostrar. Muchos atributos CONSTRU
	 * de construcciones los han metido mal como elementos textuales, esos son los de longitud
	 * menor a 3 que vamos a desechar
	 * @return Lista de tags que genera
	 */
	public List<String[]> ttggssParser(String ttggss){
		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		if (rotulo != null && ttggss.equals("189203") && rotulo.length()>2){ 
			s[0] = "place"; s[1] ="locality";
			l.add(s);
			s = new String[2];
			s[0] = "name"; s[1] = rotulo;
			l.add(s);

			return l;}

		else if (rotulo != null && ttggss.equals("189300") && rotulo.length()>2){ 
			s = new String[2];
			s[0] = "name"; s[1] = rotulo;
			l.add(s);
			return l;}

		else if (rotulo != null && ttggss.equals("189700") && rotulo.length()>2){ 
			s = new String[2];
			s[0] = "name"; s[1] = rotulo;
			l.add(s);
			return l;}

		else if (rotulo != null && ttggss.equals("189401")){ 
			s = new String[2];
			s[0] = "entrance"; s[1] = "yes";
			l.add(s);
			s = new String[2];
			s[0] = "addr:housenumber"; s[1] = rotulo;
			l.add(s);

			return l;}

		else {
			s = new String[2];
			s[0] = "ttggss"; s[1] = "0";
			l.add(s);
			setTtggss("0");
			return l;
		}
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
