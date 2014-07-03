import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ShapeAttributes {

	private Map<String, String> attributes;

	public ShapeAttributes(){
			attributes = new HashMap<String, String>();
	}

	public void addAttribute(String k, String v){
		attributes.put(k, v);
	}
	
	public void addAttributeIfNotExistValue(String k, String v){
		if (attributes.get(k) == null)
		attributes.put(k, v);
	}
	
	public void addAttributeIfKeyValue(String existKey, String existValue, String k, String v){
		if (attributes.get(existKey)!= null && attributes.get(existKey).equals(existValue))
		attributes.put(k, v);
	}
	
	public void overwriteAttribute(String k, String existingV, String newV){
		if (attributes.get(k) != null && attributes.get(k).equals(existingV)){
			attributes.put(k, newV);
		}
	}
	
	/**
	 * Anade el atributo K y valor V si no existe la clave K en los atributos o si 
	 * esta existe y su valor es uno de los contenidos en equal
	 * @param k
	 * @param v
	 * @param equal
	 */
	public void addAttributeIfNotExistsValueOrEqualTo(String k, String v, List<String> equal){
		if (attributes.get(k) == null || (equal != null && equal.contains(attributes.get(k)) ))
		attributes.put(k, v);
	}
	
	public void addAll(Map<String,String> attributes){
		this.attributes.putAll(attributes);
	}
	
	public void removeAttribute(String k){
		attributes.remove(k);
	}
	
	public void removeAttribute(String k, String v){
		if (attributes.get(k) != null && attributes.get(k).equals(v))
			attributes.remove(k);
	}
	
	public int getSize(){
		return attributes.size();
	}
	
	public Set<String> getKeys(){
		return attributes.keySet();
	}

	public Collection<String> getValues(){
		return attributes.values();
	}

	public String getValue(String key){
		return attributes.get(key);
	}
	
	public Map<String, String> asHashMap(){
		return attributes;
	}
	
	public String keyToOSM(String key){
		return oneAttributeToOSM(key, attributes.get(key));
	}
	
	public String toOSM(){
		StringBuilder str = new StringBuilder();
		for (String key : attributes.keySet()){
			str.append(oneAttributeToOSM(key, attributes.get(key)));
		}
		return str.toString();
	}
	
	public String oneAttributeToOSM(String key, String value){
		return ("<tag k=\"" + key + "\" v=\"" + value + "\"/>\n");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attributes == null) ? 0 : attributes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		boolean equal = true;
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ShapeAttributes other = (ShapeAttributes) obj;
		if (attributes == null) {
			if (other.attributes != null || !equal)
				return false;
		} for(String key : attributes.keySet()){
			if (other.attributes.get(key) == null){
				return false;
			} else {
				equal = equal && attributes.get(key).equals(other.attributes.get(key));
			}
		} 
		for(String key : other.attributes.keySet()){
			if (attributes.get(key) == null || !equal){
				return false;
			} else {
				equal = equal && other.attributes.get(key).equals(attributes.get(key));
			}
		}
		return equal;
	}
	
}
