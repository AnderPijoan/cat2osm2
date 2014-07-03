import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

public abstract class ShapePoint extends Shape {

	// Estas geometrias se descomponen en su unico poligono lineal
	// que sera el primero o posicion [0] 
	
	// Geometria descompuesta en elementos de OSM
	protected Long nodes; //[0] Outer solo
	protected String ttggss; // Campo TTGGSS en Elempun.shp y Elemtex.shp
	// Se usara para desechar algunos Elemtex y para
	// conocer cuales tienen influencia en las parcelas sobre los que estan colocados (para tags landuse)
	

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
	

	/*public String getTtggss() {
		return ttggss;
	}

	
	public void setTtggss(String t) {
		ttggss = t;
	}*/
	
	
	public boolean hasRelevantAttributesInternally(){
		if(attributes != null){
			for (String key : attributes.getKeys()){
				if (!key.equals("addr:postcode") && 
						!key.equals("addr:country") && 
						!key.equals("source") && 
						!key.equals("source:date") && 
						!key.equals("type")){
					return true;
				}
			}
		}
		return false;
	}


	public boolean hasRelevantAttributesForPrinting(){
		return hasRelevantAttributesInternally();
	}
	

	/**
	 * Una vez leidos todos los datos, pasar el shape a formato OSM y guardarlo 
	 * en la propia shape. Ya se extraera mas adelante.
	 */
	@Override
	public boolean toOSM(Cat2OsmUtils utils, ShapeParent parent){
	
		if (!this.getGeometry().isEmpty()){
			Coordinate coor = this.getGeometry().getCoordinate();

			// Anadimos solo un nodo
			this.addNode(0, utils.generateNodeId(this.getCodigoMasa(), coor, this));

			return true;
		}
		return false;
	}

}
