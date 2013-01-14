import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;


public abstract class ShapeLinear extends Shape {

	// Estas geometrias se descomponen en su unico poligono lineal
	// que sera el primero o posicion [0] 
	
	// Geometria descompuesta en elementos de OSM
	protected List<List<Long>> nodes; //[0] Outer solo
	protected Long ways; //[0] Outer solo
	
	
	public ShapeLinear(SimpleFeature f, String tipo) {
		super(f, tipo);
	}
	
	
	public void setNodes(List<List<Long>> nodesId) {
		nodes = nodesId;
	}
	
	
	/** Anade el nodo al poligono pos de la lista de nodos de la geometria
	 * @param pos
	 * @param nodeId
	 */
	public void addNode(int pos, long nodeId){
		
		if(nodes == null){
			nodes = new ArrayList<List<Long>>();
			// nodes solo tiene 1 posicion
			nodes.add(new ArrayList<Long>());
		}

		if (geometry.getNumGeometries() > pos){
			nodes.get(pos).add(nodeId);
		}
	}
	
	
	/** Devuelve la lista de ids de nodos del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @return Lista de ids de nodos del poligono en posicion pos
	 */
	public synchronized List<Long> getNodesIds(int pos){
		if (nodes.size() > pos)
			return nodes.get(pos);
		else
			return null;
	}
	
	
	public void setWays(List<Long> waysId) {
		if (!waysId.isEmpty())
		ways = waysId.get(0);
	}

	
	public void addWay(int pos, long wayId){
		if(pos == 0)
		ways = wayId;
	}
	
	
	/** Devuelve la lista de ids de ways
	 * del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @return Lista de ids de ways del poligono en posicion pos
	 */
	public synchronized List<Long> getWays() {
		List<Long> l = new ArrayList<Long>();
		l.add(ways);
			return l;
	}
	
	
	public synchronized void deleteWay(long wayId){
		if (ways == wayId)
			ways = null;
	}
	
	
	public void setRelation(long relationId){
	}
	
	
	public synchronized Long getRelationId(){
		return null;
	}
	
	
	public boolean hasRelevantAttributesInternally(){
		if(attributes != null)
			for (ShapeAttribute atr : attributes)
				if (!atr.getKey().equals("addr:postcode") && 
						!atr.getKey().equals("addr:country") && 
						!atr.getKey().equals("source") && 
						!atr.getKey().equals("source:date") && 
						!atr.getKey().equals("type"))
					return true;
		
		return false;
	}
	
	
	public boolean hasRelevantAttributesForPrinting(){
		return hasRelevantAttributesInternally();
	}
	
	
	public String getRefCat() {
		return null;
	}
}
