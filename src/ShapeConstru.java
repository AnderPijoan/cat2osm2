import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.util.PolygonExtracter;


public class ShapeConstru extends ShapePolygonal {

	// Variable autoincremental que se concatena al shapeId
	private volatile static long URID = (long) 0;
	private volatile static long RUID = (long) 0;
	private String constru; // Campo Constru solo en Constru.shp


	public ShapeConstru(SimpleFeature f, String tipo) {
		
		super(f, tipo);

		shapeId = "CONSTRU" + tipo + (tipo.equals("UR") ? (URID = super.newShapeId(URID)) : (RUID = super.newShapeId(RUID)));
		
		// Para agrupar geometrias segun su codigo de masa
		codigoMasa = ((String) f.getAttribute("MASA")).replaceAll("[^\\p{L}\\p{N}]", "")+"-";

		// Constru.shp trae la geometria en formato MultiPolygon
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiPolygon")){

			// Poligono, trae el primer punto de cada poligono repetido al final.
			geometry = (MultiPolygon) f.getDefaultGeometry();
			
			// Eliminamos posibles poligonos multiples
			List<?> polys = PolygonExtracter.getPolygons(geometry.union());
			geometry = geometry.getFactory().buildGeometry(polys);
			geometry.normalize();
		}
		else
			System.out.println("["+new Timestamp(new Date().getTime())+"] Formato geometrico "
					+ f.getDefaultGeometry().getClass().getName() +" desconocido del shapefile CONSTRU");

		// Los demas atributos son metadatos y de ellos sacamos 
		referenciaCatastral = (String) f.getAttribute("REFCAT");

		constru = (String) f.getAttribute("CONSTRU");
		
		if (constru != null)
			addAttributesAsStringArray(construParser(constru));
	}


	public String getConstru() {
		return constru;
	}
	

	public boolean isValid(){
		
		if (Cat2OsmUtils.getOnlyConstru())
			return true;
		
		switch(constru){

		case "B":
			return false;

		case "T":
			return false;

		case "TZA":
			return false;

		case "POR":
			return false;

		case "SOP":
			return false;

		case "PJE":
			return false;

		case "MAR":
			return false;

		case "P":
			return false;

		case "CO":
			return true;

		case "EPT":
			return false;

		case "SS":
			return false;

		case "ALT":
			return false;

		case "PI":
			return true;

		case "TEN":
			return true;

		case "ETQ":
			return true;

		case "SILO":
			return true;

		case "SUELO":
		case "TERRENY":
		case "SOLAR":
			return true;

		case "PRG":
			return false;

		case "DEP":
			return true;

		case "ESC":
			return true;

		case "TRF":
			return true;

		case "JD":
			return true;

		case "YJD":
			return true;

		case "FUT":
			return true;

		case "VOL":
			return false;

		case "ZD":
			return true;

		case "RUINA":
			return true;

		case "CONS":
			return true;

		case "PRESA":
			return true;

		case "ZBE":
			return true;

		case "ZPAV":
			return false;

		case "GOLF":
			return true;

		case "CAMPING":
			return true;

		case "HORREO":
			return false;

		case "PTLAN":
			return true;

		case "DARSENA":
			return true;
			
		case "LV":
			return false;
			
		}
		return true;
	}


	/** Parsea el atributo constru entero, este se compone de distintos
	 * elementos separados por el caracter '+'
	 * @param constru Atributo constru
	 * @return Lista con los tags que genera
	 */
	public List<String[]> construParser(String constru){

		List<String[]> l = new ArrayList<String[]>();
		constru = constru.trim();
		String[] construs = constru.split("\\+");
		int alturaMax = Integer.MIN_VALUE;
		int alturaMin = Integer.MAX_VALUE;
		// Variable para saber si ya se ha escrito algun tag building=*
		// Ya que sino, podriamos tener building=warehouse + building=yes por ejemplo
		boolean building = false;
		
		
		for (String s: construs){

			List<String[]> temp = construElemParser(s.toUpperCase());

			// Si es un numero, no sabemos si es el de altura superior o inferior
			// por eso lo almacenamos hasta el final.
			if (!temp.isEmpty() && temp.get(0)[0].equals("NUM")) {
				String[] num = temp.get(0);
				alturaMax = (alturaMax>Integer.parseInt(num[1]))? alturaMax : Integer.parseInt(num[1]);
				alturaMin = (alturaMin<Integer.parseInt(num[1]))? alturaMin : Integer.parseInt(num[1]);
			}
			else{
				for (String[] tag : temp)
					if (tag[0].equals("building"))
						building = true;
				
				l.addAll(temp);
			}
		}

		// Comparamos si tenemos algun numero almacenado
		if (alturaMax != Integer.MIN_VALUE && alturaMin != Integer.MAX_VALUE){

			// Si los dos valores han quedado iguales, es que solo se
			// ha recogido un numero, se entiende si es mayor que 0, que alturaMin
			// es 0 y si menor que 0, entonces alturaMax sera 0
			if (alturaMax == alturaMin) {
				alturaMax = (alturaMax>0)? alturaMax : 0;
				alturaMin = (alturaMin<0)? alturaMin : 0;
			}
			String[] s = new String[2];

			if (alturaMax != 0){
				// Comprobamos si se quiere exportar en formato catastro3d los pisos positivos
				if(!Config.get("Catastro3d").equals("0")){
					s[0] = "building:levels"; s[1] = alturaMax+""; 
					l.add(s);
				}
				if (!building){
					s = new String[2];
					s[0] = "building"; s[1] ="yes";
					l.add(s);
				}
			}

			if(alturaMin != 0) {
				// Comprobamos si se quiere exportar en formato catastro3d los pisos negativos
				if(Config.get("Catastro3d").equals("-1")){
					s = new String[2];
					s[0] = "building:min_level"; s[1] = alturaMin+"";
					l.add(s);
				}
				if (!building){
					s = new String[2];
					s[0] = "building"; s[1] ="yes";
					l.add(s);
				}
			}
		}

		return l;
	}


	/** Parsea cada elemento que ha sido separado
	 * @param elem Elemto a parsear
	 * @return Lista con los tags que genera cada elemento
	 */
	private List<String[]> construElemParser(String elem){

		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];
		
		if (elem.isEmpty()){
			return l;
		}
		
		switch(elem)
		{
		case "CO":
			s[0] = "building"; s[1] = "warehouse";
			l.add(s);
			return l;

		case "PI":
			s[0] = "leisure"; s[1] = "swimming_pool";
			l.add(s);
			s = new String[2];
			s[0] = "access"; s[1] = "private";
			l.add(s);
			return l;

		case "TEN":
			s[0] = "leisure"; s[1] = "pitch";
			l.add(s);
			s = new String[2];
			s[0] = "sport"; s[1] = "tennis";
			l.add(s);
			return l;

		case "ETQ":
			s[0]="landuse"; s[1]="reservoir";
			l.add(s);
			return l;

		case "SILO":
			s[0] = "man_made"; s[1] = "silo";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "SUELO":
		case "TERRENY":
		case "SOLAR":
			s[0] = "landuse"; s[1] = "greenfield";
			l.add(s);
			return l;

		case "DEP":
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "ESC":
			s[0] = "highway"; s[1] ="steps";
			l.add(s);
			return l;

		case "TRF":
			s[0] = "power"; s[1] ="sub_station";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "JD":
			s[0] = "leisure"; s[1] = "garden";
			l.add(s);
			return l;

		case "YJD":
			s[0] = "leisure"; s[1] = "garden";
			l.add(s);
			return l;

		case "FUT":
			s[0] = "leisure"; s[1] = "stadium";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "ZD":
			s[0] = "leisure"; s[1] = "sports_centre";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "RUINA":
			s[0] = "ruins"; s[1] = "yes";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "CONS":
			s[0] = "landuse"; s[1] = "construction";
			l.add(s);
			return l;

		case "PRESA":
			s[0] = "waterway"; s[1] = "dam";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "ZBE":
			s[0]="landuse"; s[1]="reservoir";
			l.add(s);
			return l;

		case "GOLF":
			s[0] = "leisure"; s[1] = "golf_course";
			l.add(s);
			return l;

		case "CAMPING":
			s[0] = "tourism"; s[1] = "camp_site";
			l.add(s);
			return l;

		case "PTLAN":
			s[0] = "man_made"; s[1] = "pier";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "DARSENA":
			s[0] = "waterway"; s[1] = "dock";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;
		}
		
		// Si no ha coincidido exactamente con ninguno de los codigos anteriores
		// Descartamos los que no hay que representar (en este caso se hace con
		// CONTAINS ya que estos codigos se puede concatenar con alturas, lo que 
		// haria que el parser de numeros romanos de datos incorrectos)
		if (elem.toUpperCase().contains("HORREO")){
			return l;
		}
		else if (elem.toUpperCase().contains("ZPAV")){
			return l;
		}
		else if (elem.toUpperCase().contains("TZA")){
			return l;
		}
		else if (elem.toUpperCase().contains("SOP")){
			return l;
		}
		else if (elem.toUpperCase().contains("PJE")){
			return l;
		}
		else if (elem.toUpperCase().contains("POR")){
			return l;
		}
		else if (elem.toUpperCase().contains("MAR")){
			return l;
		}
		else if (elem.toUpperCase().contains("EPT")){
			return l;
		}
		else if (elem.toUpperCase().contains("ALT")){
			return l;
		}
		else if (elem.toUpperCase().contains("PRG")){
			return l;
		}
		else if (elem.toUpperCase().contains("VOL")){
			return l;
		}
		else if (elem.toUpperCase().contains("SS")){
			return l;
		}
		else if (elem.toUpperCase().contains("LV")){
			return l;
		}
		else if (elem.toUpperCase().contains("T")){
			return l;
		}
		else if (elem.toUpperCase().contains("B")){
			return l;
		}
		else if (elem.toUpperCase().contains("P")){
			return l;
		}

		// Si no, finalmente vamos al parser de numeros romanos
		l.addAll(numRomanoParser(elem));

		return l;

	}

	/** Parsea los numeros romanos del atributo constru
	 * @param elem numero romano a parsear
	 * @return equivalente en numero decimal
	 */
	public List<String[]> numRomanoParser(String elem){

		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];
		String numRomano = elem;
		int sumaTotal = 0;
		boolean negativo = numRomano.startsWith("-");

		for (int x = 0; x < numRomano.length()-2; x++){
			if (numRomano.substring(x, x+3).toUpperCase().equals("III")){
				sumaTotal += 3;
				numRomano = numRomano.replace("III", "");
			}
			else if (numRomano.substring(x, x+3).toUpperCase().equals("XXX")){
				sumaTotal += 30;
				numRomano = numRomano.replace("XXX", "");
			}
			else if (numRomano.substring(x, x+3).toUpperCase().equals("CCC")){
				sumaTotal += 300;
				numRomano = numRomano.replace("CCC", "");
			}
			else if (numRomano.substring(x, x+3).toUpperCase().equals("MMM")){
				sumaTotal += 3000;
				numRomano = numRomano.replace("MMM", "");
			}
		}

		for (int x = 0; x < numRomano.length()-1; x++){
			if (numRomano.substring(x, x+2).toUpperCase().equals("IV")){
				sumaTotal += 4;				
				numRomano = numRomano.replace("IV", "");
			}
			else if (elem.substring(x, x+2).toUpperCase().equals("IX")){
				sumaTotal += 9;				
				numRomano = numRomano.replace("IX", "");
			}
			else if (elem.substring(x, x+2).toUpperCase().equals("XL")){
				sumaTotal += 40;				
				numRomano = numRomano.replace("XL", "");
			}
			else if (numRomano.substring(x, x+2).toUpperCase().equals("XC")){
				sumaTotal += 90;				
				numRomano = numRomano.replace("XC", "");
			}
			else if (numRomano.substring(x, x+2).toUpperCase().equals("CD")){
				sumaTotal += 400;				
				numRomano = numRomano.replace("CD", "");
			}
			else if (numRomano.substring(x, x+2).toUpperCase().equals("CM")){
				sumaTotal += 900;				
				numRomano = numRomano.replace("CM", "");
			}
		}

		for (int x = 0; x < numRomano.length(); x++){
			if (numRomano.substring(x, x+1).toUpperCase().equals("I"))
				sumaTotal += 1;
			else if (numRomano.substring(x, x+1).toUpperCase().equals("V"))
				sumaTotal += 5;
			else if (numRomano.substring(x, x+1).toUpperCase().equals("X"))
				sumaTotal += 10;
			else if (numRomano.substring(x, x+1).toUpperCase().equals("L"))
				sumaTotal += 50;
			else if (numRomano.substring(x, x+1).toUpperCase().equals("C"))
				sumaTotal += 100;
			else if (numRomano.substring(x, x+1).toUpperCase().equals("D"))
				sumaTotal += 500;
			else if (numRomano.substring(x, x+1).toUpperCase().equals("M"))
				sumaTotal += 1000;
		}

		if (negativo)
			sumaTotal = (0 - sumaTotal);

		s[0] = "NUM"; s[1] = sumaTotal+"";
		l.add(s);
		return l;
	}
}


