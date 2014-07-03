import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;


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
	
	
	public String getRefCat() {
		return null;
	}
	
	/**
	 * Una vez leidos todos los datos, pasar el shape a formato OSM y guardarlo 
	 * en la propia shape. Ya se extraera mas adelante.
	 * @param utils
	 * @return
	 */
	@Override
	public boolean toOSM(Cat2OsmUtils utils, ShapeParent parent){
			
		if(!this.getGeometry().isEmpty()){
			
			//Simplificamos la geometria con un threshold 
			TopologyPreservingSimplifier tps = new TopologyPreservingSimplifier(this.getGeometry());
			tps.setDistanceTolerance(Cat2OsmUtils.GEOM_SIMPLIFIER_THRESHOLD);
			this.setGeometry(tps.getResultGeometry());
			
			// Anadimos todos los nodos
			Coordinate[] coor = this.getGeometry().getCoordinates();

			for (int x = 0; x < coor.length; x++){
				this.addNode(0, utils.generateNodeId(this.getCodigoMasa(), coor[x], null));
			}

			// Con los nodos creamos un way
			List <Long> nodeList = this.getNodesIds(0);
			this.addWay(0, utils.generateWayId(this.getCodigoMasa(), nodeList, this));

			return true;
		}
		return false;
		
	}
}
