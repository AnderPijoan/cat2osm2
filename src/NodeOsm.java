import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;


public class NodeOsm {

	private Coordinate coor;
	private List<Shape> shapes; // Lista de shapes a los que pertenece DIRECTAMENTE. 
	// Es decir que solo tendra shapes si son shapes puntuales como Elemtex o Elempun. 
	// Si es un nodo de un way de un shape entonces estara vacio
	// Se usa para que luego vaya a buscar los tags a sus shapes

	public NodeOsm(Coordinate c){
		coor = new Coordinate();
		// Coordenadas en Lat/Lon. Ogr2Ogr hace el cambio de 
		// UTM a Lat/Lon ya que en el shapefile vienen en UTM
		this.coor.x = c.x; 
		this.coor.y = c.y;
		this.coor.z = c.z;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((coor == null) ? 0 : coor.hashCode());
		return result;
	}

	@Override
	public synchronized boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		NodeOsm other = (NodeOsm) obj;
		if (coor == null) {
			if (other.coor != null)
				return false;
		} else if (!coor.equals(other.coor))
			return false;
		return true;
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


	public Coordinate getCoor(){
		return coor;
	}


	public void setCoor(Coordinate c){
		this.coor = c;
	}


	/** Imprime en el formato Osm el nodo con la informacion
	 * @param id Id del nodo
	 * @param huso Huso geografico para la conversion UTM a Lat/Long
	 * @return Devuelve en un String el nodo listo para imprimir
	 * @throws UnsupportedEncodingException 
	 */
	public String printNode(Long id){
		String s = "";

		s = ("<node id=\""+ id +"\" timestamp=\""+new Timestamp(new Date().getTime())+"\" version=\"6\" lat=\""+this.coor.y+"\" lon=\""+this.coor.x+"\">\n");

		// Imprimir los tags yendo a sus shapes a recogerlos
		if(shapes != null){
			int pos = 1;
			for(Shape shape : shapes){
				s += shape.printAttributes();
				
				if( Config.get("PrintShapeIds").equals("1"))
					s += "<tag k=\"CAT2OSMSHAPEID-" + pos++ +"\" v=\"" + shape.getShapeId() + "\"/>\n";
			}
		}
		
		// Imprimir info de Cat2Osm
		s += "<tag k=\"source\" v=\"catastro\"/>\n";
		s += "<tag k=\"source:date\" v=\""+new StringBuffer(Cat2OsmUtils.getFechaArchivos()+"").insert(4, "-").toString().substring(0, 7)+"\"/>\n";

		s += ("</node>\n");
	
		return s;
	}

}
