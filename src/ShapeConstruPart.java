import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.util.PolygonExtracter;


public class ShapeConstruPart extends ShapePolygonal {

	// Variable autoincremental que se concatena al shapeId
	private volatile static long URID = (long) 0;
	private volatile static long RUID = (long) 0;
	private String constru; // Campo Constru solo en Constru.shp


	public ShapeConstruPart(SimpleFeature f, String tipo) {
		
		super(f, tipo);

		shapeId = "CONSTRU" + tipo + (tipo.equals("UR") ? (URID = super.newShapeId(URID)) : (RUID = super.newShapeId(RUID)));
		
		// Para agrupar geometrias segun su codigo de masa
		codigoMasa = ((String) f.getAttribute("MASA")).replaceAll("[^\\p{L}\\p{N}]", "")+"-";

		// Constru.shp trae la geometria en formato MultiPolygon
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiPolygon")){

			// Poligono, trae el primer punto de cada poligono repetido al final.
			geometry = (MultiPolygon) f.getDefaultGeometry();

			// Eliminamos posibles poligonos multiples
			List<?> polys = PolygonExtracter.getPolygons(geometry.union());
			geometry = geometry.getFactory().buildGeometry(polys);
			geometry.normalize();
		}
		else
			System.out.println("["+new Timestamp(new Date().getTime())+"]\tFormato geometrico "
					+ f.getDefaultGeometry().getClass().getName() +" desconocido del shapefile CONSTRU");

		// Los demas atributos son metadatos y de ellos sacamos 
		referenciaCatastral = (String) f.getAttribute("REFCAT");
		constru = (String) f.getAttribute("CONSTRU");
		
		if (constru != null){
			getAttributes().addAll(new ConstruParser().parseConstru(constru));
		}
	}


	public String getConstru() {
		return constru;
	}
	

	public boolean isValid(){
		
		if (Cat2OsmUtils.getOnlyConstru())
			return true;
		
		switch(constru){

		case "B":
		case "T":
		case "TZA":
		case "POR":
		case "SOP":
		case "PJE":
		case "MAR":
		case "P":
			return false;
		case "CO":
			return true;
		case "EPT":
		case "SS":
		case "ALT":
			return false;
		case "PI":
		case "TEN":
		case "ETQ":
		case "SILO":
		case "SUELO":
		case "TERRENY":
		case "SOLAR":
			return true;
		case "PRG":
		case "ESC":
			return false;
		case "DEP":
		case "TRF":
		case "JD":
		case "YJD":
		case "FUT":
			return true;
		case "VOL":
			return false;
		case "ZD":
		case "RUINA":
		case "CONS":
		case "PRESA":
		case "ZBE":
			return true;
		case "ZPAV":
			return false;
		case "GOLF":
		case "CAMPING":
			return true;
		case "HORREO":
			return false;
		case "PTLAN":
		case "DARSENA":
			return true;
		case "LV":
			return false;
		}
		return true;
	}
}


