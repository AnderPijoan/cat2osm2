import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.PolygonExtracter;


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
		if (nodes != null && nodes.size() > pos)
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
		if(attributes != null){
			for (String key : attributes.getKeys()){
				if (!key.equals("source") && 
						!key.equals("source:date") && 
						!key.equals("type")){
					return true;
				}
			}
		}
		return false;
	}
	
	
	/**
	 * A la hora de imprimir, las masas no tienen que ser imprimidas
	 */
	public boolean hasRelevantAttributesForPrinting(){
		if(attributes != null){
			for (String key : attributes.getKeys()){
				if (	!key.equals("catastro:ref") &&
						!key.equals("source") &&
						!key.equals("source:date") &&
						!key.equals("masa") &&
						!key.equals("type")){
					return true;
				}
			}
		}
		return false;
	}
	
	
	public String getTtggss(){
		return null;
	}


	public boolean toOSM(Cat2OsmUtils utils, ShapeParent parent){

		if (!this.getGeometry().isEmpty()){
			
			// Transformar a OSM
			// Obtenemos las coordenadas de cada punto del shape
			if (this.geometry instanceof Polygon || this.geometry instanceof MultiPolygon){

					int numPolygons = 0;
					for (int x = 0; x < this.getGeometry().getNumGeometries(); x++){
						Polygon p = (Polygon) this.getGeometry().getGeometryN(x);
	
						// Outer
						Coordinate[] coors = p.getExteriorRing().getCoordinates();
						// Miramos por cada punto si existe un nodo, si no lo creamos
						for (Coordinate coor : coors){
							// Insertamos en la lista de nodos del shape, los ids de sus nodos
							this.addNode(numPolygons, utils.generateNodeId(this.getCodigoMasa(), coor, null));
						}
						numPolygons++;
	
						// Posibles Inners
						for (int y = 0; y < p.getNumInteriorRing(); y++){
	
							// Comprobar que los agujeros tengan cierto tamano, sino pueden ser fallose
							// de union de parcelas mal dibujadas en catastro
							if (p.getInteriorRingN(y).getArea() != 0){
								coors = p.getInteriorRingN(y).getCoordinates();
	
								// Miramos por cada punto si existe un nodo, si no lo creamos
								for (Coordinate coor : coors){
									// Insertamos en la lista de nodos del shape, los ids de sus nodos
									this.addNode(numPolygons, utils.generateNodeId(this.getCodigoMasa(), coor, null));
								}
								numPolygons++;
							}
						}
					}

					// Por cada poligono creamos su way
					for (int y = 0; y < numPolygons; y++){
						// Con los nodos creamos un way
						List <Long> nodeList = this.getNodesIds(y);
						this.addWay(y, utils.generateWayId(this.getCodigoMasa(), nodeList, null));
					}
	
					// Creamos una relation para el shape, metiendoe en ella todos los members
					List <Long> ids = new ArrayList<Long>(); // Ids de los members
					List <String> types = new ArrayList<String>(); // Tipos de los members
					List <String> roles = new ArrayList<String>(); // Roles de los members
					for (int y = 0; y < this.getWays().size(); y++){
						long wayId = this.getWays().get(y);
						if (!ids.contains(wayId)){
							ids.add(wayId);
							types.add("way");
							if (y == 0)roles.add("outer");
							else roles.add("inner");
						}
					}
					this.setRelation(utils.generateRelationId(this.getCodigoMasa(), ids, types, roles, this));
					return true;
				} else {
				System.out.println("["+new Timestamp(new Date().getTime())+"]\tGeometría en formato desconocido : " + this.getGeometry().getGeometryType().toString());
				return false;
			}
		}
		return false;
	}
}
