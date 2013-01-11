import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

public abstract class ShapePoint extends Shape {

	// Estas geometrias se descomponen en su unico poligono lineal
	// que sera el primero o posicion [0] 
	
	// Geometria descompuesta en elementos de OSM
	protected Long nodes; //[0] Outer solo
	protected String ttggss; // Campo TTGGSS en Elempun.shp y Elemtex.shp
	// Se usara para desechar algunos Elemtex y para
	// conocer cuales tienen influencia en las parcelas sobre los que estan colocados (para tags landuse)W
	

	public ShapePoint(SimpleFeature f, String tipo) {
		super(f, tipo);
	}
	
	
	public void setNodes(List<List<Long>> nodesId) {
		nodes = nodesId.get(0).get(0);
	}
	
	
	/** Anade el nodo al poligono pos de la lista de nodos de la geometria
	 * @param pos
	 * @param nodeId
	 */
	public void addNode(int pos, long nodeId){
		if (pos == 0){
			nodes = nodeId;
		}
	}
	
	
	/** Devuelve la lista de ids de nodos del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @return Lista de ids de nodos del poligono en posicion pos
	 */
	public synchronized List<Long> getNodesIds(int pos){
		List<Long> l = new ArrayList<Long>();
		l.add(nodes);
		return l;
	}
	
	
	public void setWays(List<Long> waysId) {
	}

	
	public void addWay(int pos, long wayId){
	}
	
	
	/** Devuelve la lista de ids de ways
	 * del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @return Lista de ids de ways del poligono en posicion pos
	 */
	public synchronized List<Long> getWays() {
		return null;
	}
	
	
	public synchronized void deleteWay(long wayId){
	}
	
	
	public void setRelation(long relationId){
	}
	
	
	public synchronized Long getRelationId(){
		return null;
	}
	
	
	public String getRefCat(){
		return null;
	}
	

	public String getTtggss() {
		return ttggss;
	}

	
	public void setTtggss(String t) {
		ttggss = t;
	}
	
	
	public boolean hasRelevantAttributes(){
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

}
