import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.geotools.geometry.jts.JTSFactoryFinder;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;


public class Cat2OsmUtils {

	private volatile static long idnode = -1;    // Comienzo de id de nodos
	private volatile static long idway = -1;     // Comienzo de id de ways
	private volatile static long idrelation = -1; // Comienzo de id de relations
	
	// CONSTANTES
	static final double GEOM_SIMPLIFIER_THRESHOLD = 0.00001;
	static final double GEOM_INTERSECTION_THRESHOLD = 0.000005;
	static final double GEOM_EQUALS_THRESHOLD = 0.00001;
	static final double GEOM_AREA_THRESHOLD = 0.0000000001;
	static final double ENTRANCES_SEARCHDIST = 0.00008; // Distancia minima para busqueda de portales ~ 80 metros

	// Fecha actual, leida del archivo .cat
	private static long fechaArchivos;

	// Lista de nodos para evitar repetidos y agrupadas por codigos de masa
	private final ConcurrentHashMap <String, ConcurrentHashMap <NodeOsm, Long>> totalNodes = 
			new ConcurrentHashMap <String, ConcurrentHashMap<NodeOsm, Long>>();
	// Listaa de ways para manejar los que se comparten y agrupadas por codigos de masa
	private final ConcurrentHashMap <String,ConcurrentHashMap <WayOsm, Long>> totalWays = 
			new ConcurrentHashMap <String, ConcurrentHashMap <WayOsm, Long>>();
	// Listaa de relations
	private final ConcurrentHashMap <String, ConcurrentHashMap <RelationOsm, Long>> totalRelations = 
			new ConcurrentHashMap <String, ConcurrentHashMap <RelationOsm, Long>>();

	// Booleanos para el modo de calcular las entradas o ver todos los Elemtex y sacar los Usos de los
	// inmuebles que no se pueden asociar
	private static boolean onlyEntrances = true; // Solo utilizara los portales de elemtex, en la ejecucion normal solo se usan esos.
	private static boolean onlyUsos = false; // Para la ejecucion de mostrar usos, se pone a true
	private static boolean onlyConstru = false; // Para la ejecucion de mostrar construs, se pone a true

	public synchronized ConcurrentHashMap <String, ConcurrentHashMap<NodeOsm, Long>> getTotalNodes() {
		return totalNodes;
	}

	public synchronized void addNode(String codigo, NodeOsm n, Long idnode){
		if (totalNodes.get(codigo) == null)
			totalNodes.put(codigo, new ConcurrentHashMap<NodeOsm, Long>());
		totalNodes.get(codigo).put(n, idnode);
	}

	public synchronized ConcurrentHashMap <String, ConcurrentHashMap<WayOsm, Long>> getTotalWays() {
		return totalWays;
	}


	public synchronized void addWay(String codigo, WayOsm w, Long idway){
		if (totalWays.get(codigo) == null)
			totalWays.put(codigo, new ConcurrentHashMap<WayOsm, Long>());
		totalWays.get(codigo).put(w, idway);
	}


	/** A la hora de simplificar, hay ways que se eliminan porque sus nodos se concatenan
	 * a otro way. Borramos los ways que no se vayan a usar de las relaciones que los contenian
	 * @param key Codigo de masa
	 * @param w Way a borrar
	 */
	public synchronized void deleteWayFromRelations(String key, WayOsm w){
		for (RelationOsm relation : totalRelations.get(key).keySet())
			relation.removeMember(totalWays.get(key).get(w));
	}


	/** Metodo para truncar las coordenadas de los shapefiles para eliminar nodos practicamente duplicados
	 * @param d numero
	 * @param decimalPlace posicion a truncar
	 * @return
	 */
	public static double round(double d, int decimalPlace) {
		BigDecimal bd = new BigDecimal(d);
		bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}


	/** Junta dos ways en uno.
	 * @param w1 Way1 Dependiendo del caso se eliminara un way o el otro
	 * @param w2 Way2
	 * @return long Way que hay que eliminar de los shapes, porque sus nodos se han juntado al otro
	 */
	public synchronized WayOsm joinWays(String key, WayOsm w1, WayOsm w2){

		if ( !w1.getNodes().isEmpty() && !w2.getNodes().isEmpty()){

			WayOsm w3;
			long idWay1;
			List<Long> nodes;

			if (totalWays.get(key).get(w1) != null && totalWays.get(key).get(w2) != null)

				switch(areConnected(w1, w2)){

				// Caso1: w1.final = w2.primero
				case 1:	
					// Clonamos el way al que le anadiremos los nodos, w1
					idWay1 = totalWays.get(key).get(w1);
					w3 = new WayOsm(null);
					for (Long lo : w1.getNodes())
						w3.addNode(lo);
					w3.setShapes(w1.getShapes());

					// Copiamos la lista de nodos del way que eliminaremos, w2
					nodes = new ArrayList<Long>();
					for (Long lo : w2.getNodes())
						nodes.add(lo);

					// Eliminamos el nodo que comparten de la lista de nodos
					nodes.remove(w2.getNodes().get(0));

					// Concatenamos al final del way3 (copia del way1) los nodos del way2
					w3.addNodes(nodes);

					// Borramos el w1 del mapa de ways porque se va a meter el w3 (que es el w1 con los nuevos
					// nodos concatenados)
					totalWays.get(key).remove(w1);

					// Borramos el w2 de las relaciones pero no del mapa porque hace falta para el return
					deleteWayFromRelations(key, w2);

					// Guardamos way3 en la lista de ways, manteniendo el id del way1
					totalWays.get(key).put(w3, idWay1);

					return w2;

					// Caso2: w1.primero = w2.final
				case 2:

					// Es igual que el Caso1 pero cambiados de orden.
					return joinWays(key, w2, w1);

					// Caso3: w1.primero = w2.primero
				case 3:

					// Clonamos el way al que le anadiremos los nodos, w1
					idWay1 = totalWays.get(key).get(w1);
					w3 = new WayOsm(null);
					for (Long lo : w1.getNodes())
						w3.addNode(lo);
					w3.setShapes(w1.getShapes());

					// Copiamos la lista de nodos del way que eliminaremos, w2
					nodes = new ArrayList<Long>();
					for (Long lo : w2.getNodes())
						nodes.add(lo);

					// Eliminamos el nodo que comparten de la lista de nodos
					nodes.remove(w2.getNodes().get(0));

					// Damos la vuelta a la lista de nodos que hay que concatenar en la posicion 0 del
					// way que vamos a conservar
					Collections.reverse(nodes);

					// Concatenamos al principio del way3 (copia del way1) los nodos del way2
					w3.addNodes(nodes, 0);

					// Borramos el w1 del mapa de ways porque se va a meter el w3 (que es el w1 con los nuevos
					// nodos concatenados)
					totalWays.get(key).remove(w1);

					// Borramos el w2 de las relaciones pero no del mapa porque hace falta para el return
					deleteWayFromRelations(key, w2);

					// Guardamos way3 en la lista de ways, manteniendo el id del way1
					totalWays.get(key).put(w3, idWay1);

					return w2;

					// Caso4: w1.final = w2.final
				case 4:

					// Es igual que el Caso3 pero invirtiendo las dos vias
					w1.reverseNodes();
					w2.reverseNodes();

					return joinWays(key, w1, w2);

				case 0:
					// Si el id de alguna via ya no esta en el mapa de vias
					if (totalWays.get(key).get(w1) == null){

						// Borramos el way de las relaciones
						deleteWayFromRelations(key, w1);
					}
					else if (totalWays.get(key).get(w2) == null){

						// Borramos el way de las relaciones
						deleteWayFromRelations(key, w2);
					}

				}
		}
		return null;
	}

	public synchronized ConcurrentHashMap< String, ConcurrentHashMap<RelationOsm, Long>> getTotalRelations() {
		return totalRelations;
	} 

	public synchronized void addRelation(String codigo, RelationOsm r, Long idrel){
		if (totalRelations.get(codigo) == null)
			totalRelations.put(codigo, new ConcurrentHashMap<RelationOsm, Long>());
		totalRelations.get(codigo).put(r, idrel);
	}


	/**	Mira si existe un nodo con las mismas coordenadas
	 * de lo contrario crea el nuevo nodo. Despues devuelve el id
	 * @param key Clave en el mapa de masas
	 * @param c Coordenada del nodo para comparar si ya existe otro
	 * @param shape Shape al que pertenece DIRECTAMENTE ese nodo. Solamente para Elemtex y Elempun para que
	 * luego vaya a ellos a coger los tags
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public synchronized long generateNodeId(String key, Coordinate c, Shape shape){

		Coordinate coor = new Coordinate(round(c.x,7), round(c.y,7));

		Long id = null;

		if (totalNodes.get(key) == null)
			totalNodes.put(key, new ConcurrentHashMap<NodeOsm, Long>());

		if (!totalNodes.get(key).isEmpty())
			id = totalNodes.get(key).get(new NodeOsm(coor));

		// Existe el nodo
		if (id != null){

			// Si es un nodo que hemos creado porque depende de un way, entonces no le indicamos shape
			// Por lo contrario si es un nodo de un shape puntual, indicamos a que shape pertenece, para que luego
			// coja los tags de el
			if(shape != null)
				((NodeOsm) getKeyFromValue((Map< String, Map<Object, Long>>) ((Object) totalNodes), key, id)).addShape(shape);

			return id;
		}
		// No existe, por lo que creamos uno
		else{
			idnode--;
			NodeOsm n = new NodeOsm(coor);

			// Si es un nodo que hemos creado porque depende de un way, entonces no le indicamos shape
			// Por lo contrario si es un nodo de un shape puntual, indicamos a que shape pertenece, para que luego
			// coja los tags de el
			if(shape != null)
				n.addShape(shape);

			totalNodes.get(key).putIfAbsent(n, idnode);
			return idnode;
		}
	}


	/** Mira si existe un way con los mismos nodos y en ese caso anade
	 * los tags, de lo contrario crea uno. Despues devuelve el id
	 * @param key Codigo de masa en la que esta el way
	 * @param nodes Lista de nodos
	 * @param shapes Lista de los shapes a los que pertenecera
	 * @return devuelve el id del way creado o el del que ya existia
	 */
	@SuppressWarnings("unchecked")
	public synchronized long generateWayId(String key, List<Long> nodes, Shape shape ){

		Long id = null;

		if (totalWays.get(key) == null)
			totalWays.put(key, new ConcurrentHashMap<WayOsm, Long>());

		if (!totalWays.isEmpty())
			id = totalWays.get(key).get(new WayOsm(nodes));

		// Existe el way
		if (id != null){
			((WayOsm) getKeyFromValue((Map< String, Map<Object, Long>>) ((Object) totalWays), key, id)).addShape(shape);
			return id;
		}
		// No existe el way por lo que lo creamos
		else{
			idway--;
			WayOsm w = new WayOsm(nodes);
			w.addShape(shape);
			totalWays.get(key).putIfAbsent(w, idway);
			return idway;
		}
	}


	/** Mira si existe una relation con los mismos ways y en ese caso anade 
	 * los tags, de lo contrario crea una. Despues devuelve el id
	 * @param key Codigo de masa en la cual esta la relation
	 * @param ids Lista de ids de los members q componen la relacion
	 * @param types Lista de los tipos de los members de la relacion (por lo general ways)
	 * @param roles Lista de los roles de los members de la relacion (inner,outer...)
	 * @param tags Lista de los tags de la relacion
	 * @param shapesId Lista de shapes a los que pertenece
	 * @return devuelve el id de la relacion creada o el de la que ya existia
	 */
	@SuppressWarnings("unchecked")
	public synchronized long generateRelationId(String key, List<Long> ids, List<String> types, List<String> roles, Shape shape){

		Long id = null;

		if (totalRelations.get(key) == null)
			totalRelations.put(key, new ConcurrentHashMap<RelationOsm, Long>());

		if (!totalRelations.isEmpty())
			id = totalRelations.get(key).get(new RelationOsm(ids,types,roles));

		// Ya existe una relation, por lo que anadimos el shape a su lista de shapes
		if (id != null){
			((RelationOsm) getKeyFromValue((Map< String, Map<Object, Long>>) ((Object)totalRelations), key, id)).addShape(shape);
			return id;
		}
		// No existe relation que coincida por lo que la creamos
		else{
			idrelation--;
			RelationOsm r = new RelationOsm(ids,types,roles);
			r.addShape(shape);
			totalRelations.get(key).putIfAbsent(r, idrelation);
			return idrelation;
		}
	}

	/** Dado un Value de un Map devuelve su Key
	 * @param map Mapa
	 * @param codigo Codigo de masa
	 * @param id Value en el map para obtener su Key
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public synchronized Object getKeyFromValue(Map<String, Map <Object, Long>> map, String key, Long id){

		if (map.get(key) == null)
			return null;

		for (Object o: map.get(key).entrySet()) {
			Map.Entry<Object,Long> entry = (Map.Entry<Object, Long>) o;
			if(entry.getValue().equals(id))
				return entry.getKey();
		}
		return null;
	}

	/** Dado el ID de una relation, la devuelve
	 * @param codigo
	 * @param id
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public synchronized RelationOsm getRelation(String codigo, long id){
		return ((RelationOsm) getKeyFromValue((Map< String, Map <Object, Long>>) ((Object)totalRelations), codigo, id));
	}
	
	/** Dado un id de way lo devuelve
	 * @return node NodeOsm
	 */
	@SuppressWarnings("unchecked")
	public synchronized WayOsm getWay(String key, long id){
		return ((WayOsm) getKeyFromValue((Map< String, Map <Object, Long>>) ((Object)totalWays), key, id));
	}
	
	/** Dada una lista de identificadores de ways, devuelve una lista con esos
	 * ways
	 * @return ways lista de WayOsm
	 */
	@SuppressWarnings("unchecked")
	public synchronized List<WayOsm> getWays(String codigo, List<Long> ids){
		List<WayOsm> ways = new ArrayList<WayOsm>();
		for (Long l: ids){
			WayOsm way = getWay(codigo, l);
			if (way != null){
				ways.add(way);
			}
		}
		return ways;
	}

	/** Dado un id de nodo lo devuelve
	 * @return node NodeOsm
	 */
	@SuppressWarnings("unchecked")
	public synchronized NodeOsm getNode(String key, long id){
		return ((NodeOsm) getKeyFromValue((Map< String, Map <Object, Long>>) ((Object)totalNodes), key, id));
	}
	
	/** Dada una lista de identificadores de nodes, devuelve una lista con esos
	 * nodes
	 * @return nodes lista de NodeOsm
	 */
	@SuppressWarnings("unchecked")
	public synchronized List<NodeOsm> getNodes(String key, List<Long> ids){
		List<NodeOsm> nodes = new ArrayList<NodeOsm>();
		for (Long l: ids){
			NodeOsm node = getNode(key, l);
			if (node != null){
				nodes.add(node);
			}
		}
		return nodes;
	}

	/** Dada una lista de identificadores de nodes, borra esos nodes de la lista de nodos y de ways
	 * @param key Codigo de masa en la que estan esos nodes
	 * @param ids Lista de nodos
	 */
	@SuppressWarnings("unchecked")
	public synchronized void deleteNodes(String key, List<Long> ids){

		for (Long id : ids){

			NodeOsm node = ((NodeOsm) getKeyFromValue((Map< String, Map <Object, Long>>) ((Object)totalNodes), key, id));
			totalNodes.get(key).remove(node);

			for (WayOsm w : totalWays.get(key).keySet())
				w.getNodes().remove(id);
		}
	}
	
	public static boolean getOnlyEntrances() {
		return onlyEntrances;
	}

	public void setOnlyEntrances(boolean entrances) {
		Cat2OsmUtils.onlyEntrances = entrances;
	}

	public static boolean getOnlyUsos() {
		return onlyUsos;
	}

	public void setOnlyUsos(boolean usos) {
		Cat2OsmUtils.onlyUsos = usos;
	}

	public static boolean getOnlyConstru() {
		return onlyConstru;
	}

	public void setOnlyConstru(boolean constru) {
		Cat2OsmUtils.onlyConstru = constru;
	}

	/** Busca si el nodo esta sobre el way indicado
	 * @param key
	 * @param node
	 * @param way
	 * @return
	 */
	public boolean nodeOnWay(String key, long nodeId, long wayId) {
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		NodeOsm node = this.getNode(key, nodeId);
		WayOsm way = this.getWay(key, wayId);
		Point point = geometryFactory.createPoint(node.getCoor());
		for (int i = 1; i < way.getNodes().size(); i++) {
			Coordinate[] coors = {this.getNode(key, way.getNodes().get(i-1)).getCoor(), this.getNode(key, way.getNodes().get(i)).getCoor()};
			LineString line = geometryFactory.createLineString(coors);
			if (line.isWithinDistance(point, Cat2OsmUtils.GEOM_INTERSECTION_THRESHOLD)) {
				return true;
			}
		}
		return false;
	}
	
	/** Busca si el nodo esta sobre el way indicado y en ese caso lo anade al way
	 * @param key
	 * @param node
	 * @param way
	 * @return
	 */
	public boolean nodeToWayIfOnEdge(String key, long nodeId, long wayId) {
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		NodeOsm node = this.getNode(key, nodeId);
		WayOsm way = this.getWay(key, wayId);
		Point point = geometryFactory.createPoint(node.getCoor());
		for (int i = 1; i < way.getNodes().size(); i++) {
			Coordinate[] coors = {this.getNode(key, way.getNodes().get(i-1)).getCoor(), this.getNode(key, way.getNodes().get(i)).getCoor()};
			LineString line = geometryFactory.createLineString(coors);
			if (line.isWithinDistance(point, Cat2OsmUtils.GEOM_INTERSECTION_THRESHOLD)) {
				way.getNodes().add(i, nodeId);
				return true;
			}
		}
		return false;
	}
	
	public static long getFechaArchivos() {
		return fechaArchivos;
	}

	public static void setFechaArchivos(long fechaArchivos) {
		Cat2OsmUtils.fechaArchivos = fechaArchivos;
	}


	/** Calcula si 2 ways estan conectados y devuelve de que forma estan conectados
	 * @param way1 WayOsm
	 * @param way2 WayOsm
	 * @return Codigo de como estan conectados
	 * -1 = alguna via nula
	 * 0 = no conectados
	 * 1 = Caso1: w1.final = w2.primero
	 * 2 = Caso2: w1.primero = w2.final
	 * 3 = Caso3: w1.primero = w2.primero
	 * 4 = Caso4: w1.final = w2.final
	 */
	public static int areConnected(WayOsm w1, WayOsm w2){

		if(w1 == null || w2 == null)
			return -1;
		if(w1.getNodes().get(w1.getNodes().size()-1).equals(w2.getNodes().get(0)))
			return 1;
		if(w1.getNodes().get(0).equals(w2.getNodes().get(w2.getNodes().size()-1)))
			return 2;
		if(w1.getNodes().get(0).equals(w2.getNodes().get(0)))
			return 3;
		if(w1.getNodes().get(w1.getNodes().size()-1).equals(w2.getNodes().get(w2.getNodes().size()-1)))
			return 4;
		return 0;	
	}

	/**
	 * Comprueba si dos geometrias lineales estan conectadas por el principio o final
	 * @param g1
	 * @param g2
	 * @return
	 */
	public static int areConnected(Geometry g1, Geometry g2){

		if(g1 == null || g2 == null)
			return -1;
		if(g1.getCoordinates()[g1.getCoordinates().length-1].equals(g2.getCoordinates()[0]))
			return 1;
		if(g1.getCoordinates()[0].equals(g2.getCoordinates()[g2.getCoordinates().length-1]))
			return 2;
		if(g1.getCoordinates()[0].equals(g2.getCoordinates()[0]))
			return 3;
		if(g1.getCoordinates()[g1.getCoordinates().length-1].equals(g2.getCoordinates()[g2.getCoordinates().length-1]))
			return 4;
		return 0;	
	}
	
	/**
	 * Intenta conectar dos geometrias lineales
	 * @param g1
	 * @param g2
	 * @return La geometria resultante o null
	 */
	public static Geometry connectTwoLines(Geometry g1, Geometry g2){
		Geometry newGeom = null;
		switch(Cat2OsmUtils.areConnected(g1, g2)){
		case 1: newGeom = g1.union(g2); break;
		case 2: newGeom = g2.union(g1); break;
		case 3: newGeom = g1.reverse().union(g2); break;
		case 4: newGeom = g1.union(g2.reverse()); break;
		}
		return newGeom;
	}
	
	
	/** Comprueba si solo contiene caracteres numericos
	 * @param str String en el cual comprobar
	 * @return boolean de si es o no
	 */
	public boolean esNumero(String s)
	{
		if (s.isEmpty() || s == null)
			return false;

		for (int x = 0; x < s.length(); x++) {
			if (!Character.isDigit(s.charAt(x)))
				return false;
		}
		return true;
	}
	
	
	/** Elimina ceros a la izquierda en un String
	 * @param s String en el cual eliminar los ceros de la izquierda
	 * @return String sin los ceros de la izquierda
	 */
	public String eliminarCerosString(String s){
		String temp = s.trim();
		if (esNumero(temp) && !temp.isEmpty()){
			Integer i = Integer.parseInt(temp);
			if (i != 0)
				temp = i.toString();
			else 
				temp = "";
		}
		return temp;
	}

}
