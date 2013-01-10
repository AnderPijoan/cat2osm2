import java.util.ArrayList;
import java.util.List;

public class Cat {

	int tipoRegistro;
	String refCatastral; // Referencia Catastral
	String subparce; // Codigo Subparcela
	int numOrdenConstru; // Numero de orden de construccion
	List<ShapeAttribute> attributes;
	String usoDestino; // Codigo de uso o destino para los registros 14 y 15
	Double area; // Area
	long fechaConstru = Cat2OsmUtils.getFechaArchivos(); // Fecha de construccion AAAAMMDD
	// Empieza en el valor maximo y se reduce a la menor fecha de construccion de los inmuebles
	// de la parcela

	
	/** Constructor
	 * @param r Tipo de Registro
	 */
	public Cat(int r){
		tipoRegistro = r;
	}
	
	
	public int getTipoRegistro(){
		return tipoRegistro;
	}
	
	
	public void setRefCatastral(String r){
		refCatastral = r;
	}
	
	
	public void setSubparce(String r){
		subparce = r;
	}
	
	
	public String getRefCatastral(){
		return refCatastral;
	}
	
	
	public int getNumOrdenConstru() {
		return numOrdenConstru;
	}


	public void setNumOrdenConstru(int numOrdenConstru) {
		this.numOrdenConstru = numOrdenConstru;
	}
	
	
	public String getSubparce(){
		return subparce;
	}

	
	public long getFechaConstru() {
		return fechaConstru;
	}


	public void setFechaConstru(long fechaConstru) {
		this.fechaConstru = fechaConstru;
	}


	/** Anade un atributo leido a la lista
	 * @param nombre Clave del atributo
	 * @param valor Valor del atributo
	 */
	public void addAttribute(String nombre, String valor){
		if(attributes == null)
			attributes = new ArrayList<ShapeAttribute>();
		
		if (valor != null && !valor.trim().isEmpty()){
			ShapeAttribute atr = new ShapeAttribute(nombre, valor.trim());
			if (!attributes.contains(atr))
				attributes.add(atr);
		}
	}
	
	
	public void addAttribute(List<ShapeAttribute> l){
		if (l != null)
			attributes.addAll(l);
	}
	
	
	public List<ShapeAttribute> getAttributes(){
		return attributes;
	}
	
	
	public String getUsoDestino() {
		return usoDestino;
	}
	
	
	public void setUsoDestino(String usoDestino) {
		this.usoDestino = usoDestino;
	}


	public Double getArea() {
		return area;
	}


	public void setArea(Double area) {
		this.area = area;
	}
}
