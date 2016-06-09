import org.opengis.feature.simple.SimpleFeature;

/**
 * SHAPE que que será el contorno los subshapes de las construcciones en 3d
 * Los nuevos tags de OSM para 3D recomiendan usar building:part
 * para partes de distintas alturas y luego crear una relación con
 * el contorno que contenga el building:yes
 */

public class ShapeConstruExterior extends ShapeParent {

	// Variable autoincremental que se concatena al shapeId
	private volatile static long URID = (long) 0;
	private volatile static long RUID = (long) 0;


	public ShapeConstruExterior(SimpleFeature f, String tipo) {
		
		super(f, tipo);

		shapeId = "CONSTRUEXT" + tipo + (tipo.equals("UR") ? (URID = super.newShapeId(URID)) : (RUID = super.newShapeId(RUID)));
		
		// Para agrupar geometrias segun su codigo de masa
		codigoMasa = ((String) f.getAttribute("MASA")).replaceAll("[^\\p{L}\\p{N}]", "")+"-";

		// Los demas atributos son metadatos y de ellos sacamos 
		referenciaCatastral = (String) f.getAttribute("REFCAT");
	}


	public boolean isValid(){
		return true;
	}


	@Override
	public void createAttributesFromUsoDestino() {	
	}
	
}


