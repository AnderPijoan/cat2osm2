import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class RelationOsm {

	private List <Long> ids; // Ids de los ways members
	private List <String> types; // Tipos (way) de los members
	private List <String> roles; // Roles de los members
	private String refCatastral; // Referencia catastral para manejar las relaciones de relaciones
	private List<Shape> shapes; // Shapes a los que pertenece

	public RelationOsm(List <Long> ids, List<String> types, List<String> roles){
		this.ids = ids;
		this.types = types;
		this.roles = roles;
	}


	public void addMember(Long id , String type, String role){
		if (!ids.contains(id)){
			ids.add(id);
			types.add(type);
			roles.add(role);}

		// Borramos si existiese algun nulo
		ids.remove(null);
		types.remove(null);
		roles.remove(null);
	}


	/** Inserta un nuevo member y desplaza los existentes a la derecha
	 * @param pos
	 * @param id
	 * @param type
	 * @param role
	 */
	public void addMember(int pos, Long id , String type, String role){
		if (!ids.contains(id)){
			ids.add(pos,id);
			types.add(pos,type);
			roles.add(pos,role);}

		// Borramos si existiese algun nulo
		ids.remove(null);
		types.remove(null);
		roles.remove(null);
	}


	public synchronized void removeMember(Long id){
		if (ids.contains(id)){
			int pos = ids.indexOf(id);
			ids.remove(pos);
			types.remove(pos);
			roles.remove(pos);
		}
	}


	public List<Long> getIds() {
		return ids;
	}


	public List<String> getTypes() {
		return types;
	}


	public List<String> getRoles() {
		return roles;
	}


	public String getRefCat(){
		return refCatastral;
	}

	public void setRefCat(String refCat){
		this.refCatastral = refCat;
	}


	public List<Long> sortIds(){
		List<Long> result = new ArrayList<Long>();
		for (Long l : ids)
			result.add(l);
		Collections.sort(result);
		return result;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sortIds() == null) ? 0 : sortIds().hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RelationOsm other = (RelationOsm) obj;
		if (ids == null) {
			if (other.ids != null)
				return false;
		} else if (!sortIds().equals(other.sortIds()))
			return false;
		if (roles == null) {
			if (other.roles != null)
				return false;
		} else if (!roles.equals(other.roles))
			return false;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		return true;
	}


	/** Imprime en el formato Osm la relation con la informacion. En caso de que
	 * la relacion solo tenga un way, la devuelve como way ya que sino es
	 * redundante.
	 * @param id ID de la relacion
	 * @param key Clave en el hashmap de codigos de masa en el que esta esa relacion
	 * @param outNodes fichero de escritura de nodos
	 * @param outWays fichero de escritura de ways
	 * @param outRelations fichero de escritura de relations
	 * @param utils Clase utils para utilidades
	 * @param shapesRelation Shapes que puede heredar de otra relation padre
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings({ "unchecked" })
	public String printRelation(Long id, String key, BufferedWriter outNodes, BufferedWriter outWays, BufferedWriter outRelations, Cat2OsmUtils utils, List<Shape> shapesRelation) throws IOException{
		String s = "";
		boolean hasData = false; // variable para comprobar si tiene datos relevantes

		// Si una relation tiene menos de dos ways, deberia quedarse como way
		// ya que sino es redundante.
		// Puede que sea un inner de un shape
		if (ids.size() == 1){

			if(types.get(0).equals("way")){

				// Recogemos el unico way que tiene esta relacion
				WayOsm way = ((WayOsm) utils.getKeyFromValue((Map<String, Map<Object, Long>>) ((Object)utils.getTotalWays()), key, ids.get(0)));

				if (way == null){
					return "";
				}
				return way.printWay(ids.get(0), key, outNodes, utils, this.shapes);
			} else if(types.get(0).equals("relation")){

				// Recogemos la unica relation que tiene esta relacion
				RelationOsm relation = ((RelationOsm) utils.getKeyFromValue((Map<String, Map<Object, Long>>) ((Object)utils.getTotalRelations()), key, ids.get(0)));

				if (relation == null){
					return "";
				}
				return relation.printRelation(ids.get(0), key, outNodes, outWays, outRelations, utils, this.shapes);

			}
		}

		// En caso de que tenga varios ways, si que se imprime como una relacion de ways.
		else {

			s = ("<relation id=\""+ id +"\" timestamp=\""+new Timestamp(new Date().getTime())+"\" visible=\"true\"  version=\"6\">\n");

			for (int x = 0; x < ids.size(); x++){

				if(types.get(x).equals("relation")){

					//RelationOsm relation = ((RelationOsm) utils.getKeyFromValue((Map<String, Map<Object, Long>>) ((Object)utils.getTotalRelations()), key, ids.get(x)));
				
				}
				else if(types.get(x).equals("way")){

					WayOsm way = ((WayOsm) utils.getKeyFromValue((Map<String, Map<Object, Long>>) ((Object)utils.getTotalWays()), key, ids.get(x)));
					if (way != null){
						String writeString = way.printWay(ids.get(x), key, outNodes, utils, shapesRelation);
						if (!writeString.isEmpty()){
							outWays.write(writeString);
							outWays.newLine();
							s += ("<member type=\""+ "way" +"\" ref=\""+ ids.get(x)+"\" role=\""+ roles.get(x) +"\" />\n");
						}
					}
				}
				else if (types.get(x).equals("node")){
					NodeOsm node = ((NodeOsm) utils.getKeyFromValue((Map<String, Map<Object, Long>>) ((Object)utils.getTotalNodes()), key, ids.get(x)));
					if (node != null){
						String writeString = node.printNode(ids.get(x));
						if (!writeString.isEmpty()){
							outNodes.write(writeString);
							outNodes.newLine();
							s += ("<member type=\""+ "node" +"\" ref=\""+ ids.get(x)+"\" role=\""+ roles.get(x) +"\" />\n");
						}
					}
				}
			}

			// Imprimimos los tags de sus shapes y en caso de que haya heredado de otros
			int pos = 1;
			for (Shape shape : shapes){
				for (ShapeAttribute atr : shape.getAttributes()){
					s += "<tag k=\""+atr.getKey()+"\" v=\""+atr.getValue()+"\"/>\n";
					hasData = hasData || shape.hasRelevantAttributesForPrinting();
				}
				if (Config.get("PrintShapeIds").equals("1"))
					s += "<tag k=\"CAT2OSMSHAPEID-" + pos++ + "\" v=\""+shape.getShapeId()+"\"/>\n";
			}

			if (shapesRelation != null)
				for (Shape shape : shapesRelation){
					for (ShapeAttribute atr : shape.getAttributes()){
						s += "<tag k=\""+atr.getKey()+"\" v=\""+atr.getValue()+"\"/>\n";
						hasData = hasData || shape.hasRelevantAttributesForPrinting();
					}
					if (Config.get("PrintShapeIds").equals("1"))
						s += "<tag k=\"CAT2OSMSHAPEID-" + pos++ + "\" v=\""+shape.getShapeId()+"\"/>\n";
				}

			// Si no tiene tags relevantes para imprimir
			if (!hasData) return "";
			
			s += "<tag k=\"type\" v=\"multipolygon\"/>\n";
			
			// Imprimir info de Cat2Osm
			s += "<tag k=\"source\" v=\"catastro\"/>\n";
			s += "<tag k=\"source:date\" v=\""+new StringBuffer(Cat2OsmUtils.getFechaArchivos()+"").insert(4, "-").toString().substring(0, 7)+"\"/>\n";

			s += ("</relation>\n");
		}

		return s;
	}


	public List<Shape> getShapes() {
		return shapes;
	}


	public synchronized void setShapes(List<Shape> shapes) {
		this.shapes = shapes;
	}

	public synchronized void addShapes(List<Shape> shapes) {
		if (this.shapes == null)
			this.shapes = new ArrayList<Shape>();

		this.shapes.addAll(shapes);
	}


	public synchronized void addShape(Shape shape) {
		if (this.shapes == null)
			this.shapes = new ArrayList<Shape>();

		if (!this.shapes.contains(shape))
			this.shapes.add(shape);
	}
}
