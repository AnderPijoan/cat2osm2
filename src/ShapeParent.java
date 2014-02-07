import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.PolygonExtracter;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

public abstract class ShapeParent extends ShapePolygonal {

	// Es un ShapePolygonal pero que almacena en su interior otros
	// ShapesPolygonal que le pertenecen, completando la jerarquia
	// MASA > PARCELA > SUBPARCELA/CONSTRU
	
	// Constante para ampliar las geometrias en un buffer
	
	// Los edificios que contendra una parcela urbana
	// o
	// las subparcelas que contendra una masa rustica
	protected List<Shape> subshapes;
	
	public ShapeParent(SimpleFeature f, String tipo) {
		super(f, tipo);
	}
	
	
	public List<Shape> getSubshapes() {
		return subshapes;
	}


	public void addSubshape(ShapePolygonal subshape){
		if (subshapes == null)
			subshapes = new ArrayList<Shape>();
		
		ShapePolygonal s = subshape;
		
		// Comprobamos si NO se quiere exportar en formato catastro3d
		// En caso de que NO se quiera, los subshapes urbanos NO pueden sobresalir de su shape padre
		// Por ejemplo un edificio en Catastro podria sobresalir de su parcela ya que hay casos que
		// los balcones sobresalen.
		if(Config.get("Catastro3d").equals("0")){
			List<?> polys = PolygonExtracter.getPolygons(getGeometry().intersection(subshape.getGeometry()));
			s.setGeometry(subshape.getGeometry().getFactory().buildGeometry(polys));
		}
		
		// Si el subshape coincide perfectamente con su parcela o
		// son practicamente la misma
		// directamente solo anadimos los tags a su padre
		if (s.getGeometry().equals(this.geometry) || 
				s.getGeometry().equalsExact(this.geometry) ||
				// Si son la misma pero mal dibujada
				Math.abs(this.geometry.getArea() - s.getGeometry().intersection(this.geometry).getArea()) <= 0.00000000001){
			getAttributes().addAll(s.getAttributes().asHashMap());
		} else {
			subshapes.add(s);
		}
	}


	/** 
	 * Une todos los subshapes que se toquen y tengan los mismos tags en uno solo
	 * @param removeParentTags Eliminar los tags que coindican con los del shape padre. 
	 * En el caso de las parcelas rusticas como luego no se van a 
	 * imprimir, dejamos los tags en sus edificios.
	 */
	public void joinSubshapes(boolean removeParentTags) {
		if (subshapes == null)
			return;
		
		// En los subshapes eliminamos los tags que coincidan con los de su shape padre
		Iterator<Shape> it = subshapes.iterator();
		while(it.hasNext()){
			Shape subshape = it.next();
			
			if (removeParentTags && subshape.getAttributes() != null)
				for(String key : this.getAttributes().getKeys()){
					subshape.getAttributes().removeAttribute(key, this.getAttributes().getValue(key));
				}
			
			// Si se ha quedado sin tags o no tiene, es innecesaria
			if(subshape.getAttributes() == null || subshape.getAttributes().getSize() == 0){
				it.remove();
			}
		}
		
		// Comprobamos todos los subshapes con todos
		for(int x = 0; x < subshapes.size(); x++)
			for(int y = x; y < subshapes.size(); y++){
				
				Shape subshape1 = subshapes.get(x);
				Shape subshape2 = subshapes.get(y);
				
				if(	x != y &&
						subshape1.sameAttributes(subshape2.getAttributes()) &&
						(subshape1.getGeometry().touches(subshape2.getGeometry()) || 
							subshape1.getGeometry().intersects(subshape2.getGeometry()))
							){
			
					subshape1.setGeometry(
							subshape1.getGeometry().union(
									subshape2.getGeometry()));
					subshape1.getGeometry().normalize();
					
					// Comprobamos que la geometria de el ultimo subshape creado
					// no sea la de la parcela
					if (subshape1.getGeometry().equals(this.geometry) ||
							subshape1.getGeometry().equalsExact(this.geometry) ||
							// Si son la misma pero mal dibujada
							Math.abs(this.geometry.getArea() - subshape1.getGeometry().intersection(this.geometry).getArea()) <= 0.00000000001){
						getAttributes().addAll(subshape1.getAttributes().asHashMap());
						subshapes.remove(subshape1);
						x = 0;
					}
						
					// Actualizamos los indices para seguir buscando
					subshapes.remove(subshape2);
					y = 0;
				}
		}
	}
	
	
	public boolean toOSM(Cat2OsmUtils utils, double threshold, ShapeParent parent){
		
		if (!this.getGeometry().isEmpty()){
			
			//Simplificamos la geometria
			TopologyPreservingSimplifier tps = new TopologyPreservingSimplifier(this.getGeometry());
			tps.setDistanceTolerance(threshold);
			this.setGeometry(tps.getResultGeometry());

			// Los subshapes no pueden sobresalir de su shape padre
			// Por ejemplo un edificio no puede sobresalir de su parcela ya que hay casos que
			// los balcones de este si que salen.
			if(parent != null && parent.getGeometry() != null){
				List polys = PolygonExtracter.getPolygons(parent.getGeometry().intersection(this.getGeometry()));
				this.setGeometry(this.getGeometry().getFactory().buildGeometry(polys));
			}

			// SHAPE PARCELA, convertir a OSM todos sus subshapes 
			// pasandoles la parcela para que no sobresalgan
			if(this instanceof ShapeParcela && null != ((ShapeParcela) this).getEntrances()){
				
				for(ShapeElemtex entrance : ((ShapeParcela) this).getEntrances()){

					// Parseamos y creamos el nodeOsm con sus tags. Como este nodo se va a anadir a la geometria
					// de la parcela, al convertirla luego a OSM se reutilizara el id teniendo ya los tags cargados
					boolean converted = entrance.toOSM(utils, threshold, this);

					if (converted){
						
						// Anadimos el punto a la geometria de la parcela
						List polys = PolygonExtracter.getPolygons(this.getGeometry().union());
						Coordinate[] coorsArray = this.getGeometry().getFactory().buildGeometry(polys).getCoordinates();
						List<Coordinate> coors = new ArrayList<Coordinate>();
						for(Coordinate c : coorsArray) coors.add(c);

						int pos = -1; // Posicion donde se metera la entrada
						double minDist = Double.MAX_VALUE; // Distancia de la entrada a un par de coordenadas de la parcela

						// Comprobamos entre que dos coordenadas esta la nueva de la entrada
						for(int x = 0; x < coors.size()-1; x++){
							Coordinate[] c = {coors.get(x), coors.get(x+1)};

							LineString line = this.getGeometry().getFactory().createLineString(c);
							if(entrance.getGeometry().distance(line) < minDist){
								pos = x+1;
								minDist = entrance.getGeometry().distance(line);
							}
						}

						// Si hemos encontrado posicion para la entrada, anadimos esa coordenada a nuestra geometria
						// Luego al pasar ese punto a NodeOSM reutilizaremos el de la entrada ya creado
						if (pos > -1){
							coors.add(pos, entrance.getGeometry().getCoordinate());
							coorsArray = new Coordinate[coors.size()];
							for(int x = 0; x < coors.size(); x++)
								coorsArray[x] = coors.get(x);

							if(coorsArray[coorsArray.length-1].equals(coorsArray[0])){
								this.setGeometry(this.getGeometry().getFactory().createPolygon(
										this.getGeometry().getFactory().createLinearRing(coorsArray), null));
							}
						}
						
					}
				}
			}
			
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
				
			} else {
				
				System.out.println("["+new Timestamp(new Date().getTime())+"]\tGeometría en formato desconocido : " + this.getGeometry().getGeometryType().toString());
				return false;
			}
		}
		
		// Si este shape parent tiene subshapes
		// mandarlos que se conviertan a OSM
		if(null != ((ShapeParent) this).getSubshapes()){
			Iterator<Shape> it = ((ShapeParent) this).getSubshapes().iterator();
			while(it.hasNext()){
				Shape shape = it.next();
				if(shape instanceof ShapePolygonal) {
					boolean converted = ((ShapePolygonal) shape).toOSM(utils, threshold, this);
					if (!converted){
						System.out.println("["+new Timestamp(new Date().getTime())+"]\tSubshape que no se ha podido convertir");
					}
				} else if (shape instanceof ShapeParent) {
					boolean converted = ((ShapeParent) shape).toOSM(utils, threshold, this);
					if (!converted){
						System.out.println("["+new Timestamp(new Date().getTime())+"]\tSubshape que no se ha podido convertir");
					}
				}
			}
		}
		return true;
	}
	
	
	public abstract void createAttributesFromUsoDestino();
	
}
