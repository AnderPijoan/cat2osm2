import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.PolygonExtracter;

public abstract class ShapeParent extends ShapePolygonal {

	// Es un ShapePolygonal pero que almacena en su interior otros
	// ShapesPolygonal que le pertenecen, completando la jerarquia
	// MASA > PARCELA > SUBPARCELA/CONSTRU
	
	// Constante para ampliar las geometrias en un buffer
	
	// Los edificios que contendra una parcela urbana
	// o
	// las subparcelas que contendra una masa rustica
	protected List<ShapePolygonal> subshapes;
	
	public ShapeParent(SimpleFeature f, String tipo) {
		super(f, tipo);
	}
	
	
	public List<ShapePolygonal> getSubshapes() {
		return subshapes;
	}


	public void addSubshape(ShapePolygonal subshape){
		if (subshapes == null)
			subshapes = new ArrayList<ShapePolygonal>();
		
		ShapePolygonal s = subshape;
		
		// Comprobamos si se quiere exportar en formato catastro3d
		// En caso de que no se quiera, los subshapes no pueden sobresalir de su shape padre
		// Por ejemplo un edificio no puede sobresalir de su parcela ya que hay casos que
		// los balcones de este si que salen.
		if(Config.get("Catastro3d").equals("0")){
			List polys = PolygonExtracter.getPolygons(getGeometry().intersection(subshape.getGeometry()));
			s.setGeometry(subshape.getGeometry().getFactory().buildGeometry(polys));
		}
		
		// Si el subshape coincide perfectamente con su parcela o
		// son practicamente la misma
		// directamente solo anadimos los tags a su padre
		if (s.getGeometry().equals(this.geometry) || 
				s.getGeometry().equalsExact(this.geometry) ){
			addAttributes(s.getAttributes());
		} else {
			subshapes.add(s);
		}
	}


	/** 
	 * Une todos los subshapes que se toquen y tengan los mismos tags en uno solo
	 * @param removeParentTags Eliminar los tags que coindican con los del shape padre. 
	 * En el caso de las parcelas rusticas como luego no se van a 
	 * imprimir, dejamos los tags en sus edificios.
	 */
	public void joinSubshapes(boolean removeParentTags) {
		if (subshapes == null)
			return;
		
		// En los subshapes eliminamos los tags que coincidan con los de su shape padre
		Iterator<ShapePolygonal> it = subshapes.iterator();
		while(it.hasNext()){
			ShapePolygonal subshape = it.next();
			
		if (removeParentTags && subshape.getAttributes() != null)
			for(ShapeAttribute atr : getAttributes())
				subshape.getAttributes().remove(atr);
		
		// Si se ha quedado sin tags o no tiene, es innecesaria
		if(subshape.getAttributes() == null || subshape.getAttributes().isEmpty())
			it.remove();
		}

		// Comprobamos todos los subshapes con todos
		for(int x = 0; x < subshapes.size(); x++)
			for(int y = x; y < subshapes.size(); y++){
				
				ShapePolygonal subshape1 = subshapes.get(x);
				ShapePolygonal subshape2 = subshapes.get(y);
				
				if(	x != y &&
						subshape1.sameAttributes(subshape2.getAttributes()) &&
					(subshape1.getGeometry().touches(subshape2.getGeometry()) || 
							subshape1.getGeometry().intersects(subshape2.getGeometry()))
							){
			
					subshape1.setGeometry(
							subshape1.getGeometry().union(
									subshape2.getGeometry()));
					subshape1.getGeometry().normalize();
					
					// Comprobamos que la geometria de el ultimo subshape creado
					// no sea la de la parcela
					if (subshape1.getGeometry().equals(this.geometry) ||
							subshape1.getGeometry().equalsExact(this.geometry)){
						addAttributes(subshape1.getAttributes());
						subshapes.remove(subshape1);
						x = 0;
					}
						
					// Actualizamos los indices para seguir buscando
					subshapes.remove(subshape2);
					y = 0;
				}
		}
	}
	
	
	public abstract void createAttributesFromUsoDestino();
	
}
