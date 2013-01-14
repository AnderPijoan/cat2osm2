import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;


public abstract class ShapePolygonal extends Shape {

	// Estas geometrias se descomponen en sus poligonos 
	// El primero o posicion [0] es el poligono de afuera o outer
	// Los siguientes son subpoligonos o agujeros que puedan existir
	
	// Geometria descompuesta en elementos de OSM
	protected List<List<Long>> nodes; //[0] Outer, [1..N] inner
	protected List<Long> ways; //[0] Outer, [1..N] inner
	protected Long relation; // Relacion de sus ways
	protected String referenciaCatastral = null; // Referencia catastral
	
	
	public ShapePolygonal(SimpleFeature f, String tipo) {
		super(f, tipo);
	}
	
	
	public String getRefCat(){
		return referenciaCatastral;
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
		}

		if (nodes.size() <= pos){
			nodes.add(nodes.size(), new ArrayList<Long>());
		}
			nodes.get(pos).add(nodeId);
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
		ways = waysId;
	}

	
	public void addWay(int pos, long wayId){
		
		if(ways == null){
			ways = new ArrayList<Long>();
		}
		
		if (!ways.contains(wayId)){
			ways.add(pos,wayId);
		}
	}
	
	
	/** Devuelve la lista de ids de ways del poligono 
	 * @return Lista de ids de ways del poligono
	 */
	public synchronized List<Long> getWays() {
		return ways;
	}
	
	
	/** Devuelve el tamano de la lista de ways para saber de cuantos
	 * poligonos se compone
	 * @return Tamano de la lista de ways
	 */
	public synchronized int getWaysSize() {
		return ways.size();
	}
	
	
	
	public synchronized void deleteWay(long wayId){
			ways.remove(wayId);
	}
	
	
	public void setRelation(long relationId){
		relation = relationId;
	}
	
	
	public synchronized Long getRelationId(){
		return relation;
	}
	
	
	public boolean hasRelevantAttributesInternally(){
		if(attributes != null)
			for (ShapeAttribute atr : attributes)
				if (	!atr.getKey().equals("source") &&
						!atr.getKey().equals("source:date") &&
						!atr.getKey().equals("type"))
					return true;
		
		return false;
	}
	
	
	public boolean hasRelevantAttributesForPrinting(){
		if(attributes != null)
			for (ShapeAttribute atr : attributes)
				if (!atr.getKey().equals("addr:postcode") &&
						!atr.getKey().equals("addr:country") &&
						!atr.getKey().equals("source") &&
						!atr.getKey().equals("source:date") &&
						!atr.getKey().equals("masa") &&
						!atr.getKey().equals("type"))
					return true;
		
		return false;
	}
	
	
	public String getTtggss(){
		return null;
	}
}
