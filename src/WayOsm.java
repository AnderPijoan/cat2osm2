import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class WayOsm {

	private List<Long> nodos; // Nodos que componen ese way
	private List<Shape> shapes; // Lista de shapes a los que pertenece DIRECTAMENTE. 
	// Es decir que solo tendra shapes si son shapes lineales como Elemlin o Ejes. 
	// Si es un way de una relation de un shape entonces estara vacio
	// Se usa para que luego vaya a buscar los tags a sus shapes


	public WayOsm(List<Long> l){
		if (l == null)
			this.nodos = new ArrayList<Long>();
		else
			this.nodos = l;
	}

	/** Anade un nodo al final de la lista, comprueba que no este repetido salvo si es igual que el primero
	 * @param l Nodo a anadir
	 */
	public void addNode(Long l){
		if (!nodos.contains(l))
			nodos.add(l);
		else if (l == nodos.get(0)){
			nodos.add(l);
		}

		// Borramos si existiese algun nulo
		nodos.remove(null);
	}


	/** Anade un nodo desplazando los existentes hacia la derecha
	 * @param pos
	 * @param l
	 */
	public void addNode(int pos, Long l){
		if (!nodos.contains(l))
			nodos.add(pos, l);

		// Borramos si existiese algun nulo
		nodos.remove(null);
	}


	/** Anade nodos a la lista de nodos del way comprobando que no puedan estar repetidos salvo el ultimo
	 * que puede ser igual al primero
	 * @param l Lista de nodos a anadir
	 */
	public void addNodes(List<Long> l){
		for (int x = 0; x < l.size()-1; x++)
			if (!nodos.contains(l.get(x)))
				nodos.add(l.get(x));

		if (!nodos.contains(l.get(l.size()-1)) || l.get(l.size()-1) == nodos.get(0))
			nodos.add(l.get(l.size()-1));
	}


	/** Anade una lista de nodos en esa posicion desplazando a la derecha. 
	 *  No comprueba que puedan estar repetidos
	 * @param l Lista de nodos
	 * @param pos Posicion a anadir
	 */
	public void addNodes(List<Long> l, int pos){
		nodos.addAll(pos, l);
	}


	public List<Long> getNodes() {
		return nodos;
	}


	public void reverseNodes(){
		Collections.reverse(nodos);
	}


	public List<Shape> getShapes() {
		return shapes;
	}


	public synchronized void setShapes(List<Shape> shapes) {
		shapes.remove(null);
		this.shapes = shapes;
	}

	public synchronized void addShapes(List<Shape> shapes) {

		shapes.remove(null);

		if (this.shapes == null)
			this.shapes = new ArrayList<Shape>();

		this.shapes.addAll(shapes);
	}


	public synchronized void addShape(Shape shape) {

		if(shape == null)
			return;

		if (this.shapes == null)
			this.shapes = new ArrayList<Shape>();

		if (!this.shapes.contains(shape))
			this.shapes.add(shape);
	}


	/** Metodo para comparar si dos WayOsm pertenecen a los mismos shapes, de esta forma se
	 * iran simplificando los ways.
	 * @param s Lista de shapes del otro WayOsm a comparar
	 * @return boolean de si pertencen a los mismos o no.
	 */
	public synchronized boolean sameShapes(List<Shape> shape){

		if (this.shapes == null || shape == null)
			return false;

		if (this.shapes.size() != shape.size())
			return false; 

		List<String> l1 = new ArrayList<String>();
		List<String> l2 = new ArrayList<String>();
		for (Shape s : this.shapes)
			l1.add(s.getShapeId());
		Collections.sort(l1);
		for (Shape s : shapes)
			l2.add(s.getShapeId());
		Collections.sort(l2);

		return l1.equals(l2);
	}


	public List<Long> sortNodes(){
		List<Long> result = new ArrayList<Long>();
		for (Long l : nodos)
			result.add(l);
		Collections.sort(result);
		return result;
	}


	/** Sobreescribir el hashcode, para que compare los nodos aunque estan en otro orden
	 * para que dos ways con los mismos nodos pero en distinta direccion se detecten como iguales.
	 * ATENCION: ESTO PUEDE DAR PROBLEMAS EN EL FUTURO SI ALGUIEN INTENTA COMPARAR WAYS PARA OTRO USO
	 * DOS WAYS IGUALES PERO EN DISTINTO SENTIDO ESTE LO DA COMO QUE SON EL MISMO, YA QUE A EFECTOS DE
	 * SIMPLIFICACIÓN, TIENEN QUE SER IGUALES.
	 */
	@Override
	public synchronized int hashCode() {
		final int prime = 31 + nodos.size();
		long result = 17;
		for (long l : sortNodes())
			result = result * prime +  (int) (l^(l>>>32));

		return (int)result;
	}


	/** Sobreescribir el equals, para que compare los nodos aunque estan en otro orden
	 * para que dos ways con los mismos nodos pero en distinta direccion se detecten como iguales.
	 * ATENCION: ESTO PUEDE DAR PROBLEMAS EN EL FUTURO SI ALGUIEN INTENTA COMPARAR WAYS PARA OTRO USO
	 * DOS WAYS IGUALES PERO EN DISTINTO SENTIDO ESTE LO DA COMO QUE SON EL MISMO, YA QUE A EFECTOS DE
	 * SIMPLIFICACIÓN, TIENEN QUE SER IGUALES.
	 */
	@Override
	public synchronized boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;

		WayOsm other = (WayOsm) obj;

		if (nodos == null) {
			if (other.nodos != null)
				return false;
		} else if (this.sortNodes().size() == other.sortNodes().size()){
			boolean equal = true;
			for(int x = 0; equal && x < this.sortNodes().size(); x++)
				equal = this.sortNodes().get(x).equals(other.sortNodes().get(x));
			return equal;
		}
		else 
			return false; 

		return true;
	}


	/** Comprueba si este way esta conectado en alguno de sus nodos a el
	 * way dado 
	 * @param way Way al que comprobar si esta conectado
	 * @return boolean de si lo esta o no
	 */
	public boolean connectedTo(WayOsm way){

		boolean encontrado = false;

		for (int x = 0; !encontrado && x < this.nodos.size(); x++)
			encontrado = way.getNodes().contains(this.nodos.get(x));

		return encontrado;
	}


	/** Imprime en el formato Osm el way con la informacion
	 * @param id Id del way
	 * @return Devuelve en un String el way listo para imprimir
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	public String printWay(Long id, String key, BufferedWriter outNodes, Cat2OsmUtils utils, List<Shape> shapesRelation) throws IOException{
		String s = "";
		boolean hasData = false; // variable para comprobar si tiene datos relevantes

		// Si un way no tiene mas de dos nodos, es incorrecto
		if (nodos.size()<2){
			System.out.println("Way id="+ id +" con menos de dos nodos. No se imprimira.");
			return "";
		}

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		s = ("<way id=\""+ id +"\" timestamp=\""+df.format(new Date())+"\" version=\"6\">\n");

		// Imprimir los tags yendo a sus shapes a recogerlos
		if(shapes != null){
			int pos = 1;
			for(Shape shape : shapes){
				s += shape.printAttributes();
				hasData = hasData || shape.hasRelevantAttributesForPrinting();

				if( Config.get("PrintShapeIds").equals("1"))
					s += "<tag k=\"CAT2OSMSHAPEID-" + pos++ + "\" v=\"" + shape.getShapeId() + "\"/>\n";
			}
		}

		// Imprimir tags si viene de una relation que se ha convertido en way
		if(shapesRelation != null){
			int pos = 1;
			for(Shape shape : shapesRelation){
				s += shape.printAttributes();
				hasData = hasData || shape.hasRelevantAttributesForPrinting();

				if( Config.get("PrintShapeIds").equals("1"))
					s += "<tag k=\"CAT2OSMSHAPEID-" + pos++ + "\" v=\"" + shape.getShapeId() + "\"/>\n";
			}
		}
		
		// Si no tiene tags relevantes y no ha venido de una relation que lo necesita, no se imprime
		if (!hasData && (null != shapes || null != shapesRelation)) return "";
		
		// Imprimir info de Cat2Osm
				s += "<tag k=\"source\" v=\"catastro\"/>\n";
				s += "<tag k=\"source:date\" v=\""+new StringBuffer(Cat2OsmUtils.getFechaArchivos()+"").insert(4, "-").toString().substring(0, 7)+"\"/>\n";

		// Imprimir el way y las referencias
		// Una vez imprimido borrarlo de la lista
		for (Long nodeId : nodos){

			NodeOsm node = (NodeOsm) utils.getKeyFromValue( (Map<String, Map <Object, Long>>) ((Object)utils.getTotalNodes()), key, nodeId);

			if (node != null){
				String writeString = node.printNode(nodeId);
				if (!writeString.isEmpty()){
					outNodes.write(writeString);
					outNodes.newLine();
					s += ("<nd ref=\""+ nodeId +"\"/>\n");
				}
			}
		}
		
		s += ("</way>\n");
		return s;
	}

}
