

public class ShapeAttribute {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		ShapeAttribute other = (ShapeAttribute) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	private String key;
	private String value;

	public ShapeAttribute(String a, String o){
		key = a;
		value = o;
	}

	public String getKey(){
		return key;
	}

	public String getValue(){
		return value;
	}

	public String[] toStringArray(){
		return new String[]{key,value};
	}
	
	public String toString(){
		return ("<tag k=\"" + key + "\" v=\"" + value + "\"/>\n");
	}
	
}
