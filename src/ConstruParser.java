import java.util.HashMap;
import java.util.Map;


public class ConstruParser {

	public ConstruParser(){
		
	}
	
	/** Parsea el atributo constru entero, este se compone de distintos
	 * elementos separados por el caracter '+'
	 * @param constru Atributo constru
	 * @return Lista con los tags que genera
	 */
	public Map<String, String> parseConstru(String constru){
		Map<String, String> l = new HashMap<String, String>();
		constru = constru.trim();
		String[] construs = constru.split("\\+");
		int alturaMax = Integer.MIN_VALUE;
		int alturaMin = Integer.MAX_VALUE;
		
		for (String s: construs){

			Map<String, String> temp = construElemParser(s.toUpperCase());

			// Si es un numero, no sabemos si es el de altura superior o inferior
			// por eso lo almacenamos hasta el final.
			if (temp.get("NUM") != null) {
				int num = Integer.parseInt(temp.get("NUM"));
				alturaMax = (alturaMax > num ? alturaMax : num);
				alturaMin = (alturaMin < num ? alturaMin : num);
			}
			else {
				l.putAll(temp);
			}
		}

		// Comparamos si tenemos algun numero almacenado
		if (alturaMax != Integer.MIN_VALUE || alturaMin != Integer.MAX_VALUE){

			// Si los dos valores han quedado iguales, es que solo se
			// ha recogido un numero, se entiende si es mayor que 0, que alturaMin
			// es 0 y si menor que 0, entonces alturaMax sera 0
			if (alturaMax == alturaMin) {
				alturaMax = (alturaMax>0)? alturaMax : 0;
				alturaMin = (alturaMin<0)? alturaMin : 0;
			}

			if (alturaMax != 0){
				// Comprobamos si se quiere exportar en formato catastro3d los pisos positivos
				if(!Config.get("Catastro3d").equals("0")){
					l.put("building:levels", alturaMax+"");
					l.put("height", (alturaMax * 3)+"");
				}
				if (l.get("building") == null){
					l.put("building","yes");
				}
			}

			if(alturaMin != 0) {
				// Comprobamos si se quiere exportar en formato catastro3d los pisos negativos
				if(Config.get("Catastro3d").equals("-1")){
					l.put("building:min_level",alturaMin+"");
					l.put("min_height", (alturaMin * 3)+"");
				}
				if (l.get("building") == null){
					l.put("building","yes");
				}
			}
			
			// Comprobacion de edificios que no tienen altura, les ponemos 1
			/*if (l.get("building") != null && l.get("building:levels") == null && !Config.get("Catastro3d").equals("0")){
				l.put("building:levels", "1");
				l.put("height", "3");
			}*/
		}
		return l;
	}
	
	
	/** Parsea cada elemento que ha sido separado
	 * @param elem Elemto a parsear
	 * @return Lista con los tags que genera cada elemento
	 */
	private Map<String, String> construElemParser(String elem){

		Map<String, String> l = new HashMap<String, String>();
		
		if (elem.isEmpty()){
			return l;
		}
		
		switch(elem)
		{
		case "CO":
			l.put("building", "warehouse");
			return l;

		case "PI":
			l.put("leisure","swimming_pool");
			l.put("access","private");
			return l;

		case "TEN":
			l.put("leisure","pitch");
			l.put("sport","tennis");
			return l;

		case "ETQ":
			l.put("landuse","reservoir");
			return l;

		case "SILO":
			l.put("man_made","silo");
			l.put("building","silo");
			return l;

		case "SUELO":
		case "TERRENY":
		case "SOLAR":
			l.put("landuse","greenfield");
			return l;

		case "DEP":
			l.put("man_made","storage_tank");
			return l;

		case "ESC":
			//l.put("highway","steps");
			return l;

		case "TRF":
			l.put("power","substation");
			return l;

		case "JD":
			l.put("leisure","garden");
			return l;

		case "YJD":
			l.put("leisure","garden");
			return l;

		case "FUT":
			l.put("leisure","stadium");
			l.put("building","stadium");
			return l;

		case "ZD":
			l.put("leisure","sports_centre");
			return l;

		case "RUINA":
			l.put("ruins","yes");
			l.put("building","ruins");
			return l;

		case "CONS":
			l.put("landuse","construction");
			return l;

		case "PRESA":
			l.put("waterway","dam");
			return l;

		case "ZBE":
			l.put("landuse","reservoir");
			return l;

		case "GOLF":
			l.put("leisure","golf_course");
			return l;

		case "CAMPING":
			l.put("tourism","camp_site");
			return l;

		case "PTLAN":
			l.put("man_made","pier");
			return l;

		case "DARSENA":
			l.put("waterway","dock");
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
		l.putAll(numRomanoParser(elem));

		return l;
	}
	
	/** Parsea los numeros romanos del atributo constru
	 * @param elem numero romano a parsear
	 * @return equivalente en numero decimal
	 */
	public Map<String, String> numRomanoParser(String elem){

		Map<String, String> l = new HashMap<String, String>();
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

		l.put("NUM", sumaTotal+"");
		return l;
	}


}
