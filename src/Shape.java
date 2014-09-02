import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

public abstract class Shape {
	
	// Geometria en formato vectorial
	protected Geometry geometry = null;
	
	// Atributos y tags que se anadiran despues
	protected ShapeAttributes attributes;
	
	// Tipo del shape rustico o urbano RU/UR
	protected String tipo = null;
	
	// Id del shape SU TIPO + un numero autoincremental
	protected String shapeId = null;
	
	protected String codigoMasa = null; // Codigo de masa a la que pertenece
	// Esto se usa para la paralelizacion ya que luego solo se simplificaran geometrias que
	// pertenezcan a las mismas masas. Si alguna geometria no tiene codigo de masa, se le
	// asignara el nombre de tipo de archivo
	
	// Fechas de alta y baja en catastro
	protected long fechaAlta; // Fecha de Alta en catastro Formato AAAAMMDD
	protected long fechaBaja; // Fecha de Baja en catastro Formato AAAAMMDD
	
	// Fecha de construccion, iniciada con la fecha de creacion de los archivos de catastro
	protected long fechaConstru = Cat2OsmUtils.getFechaArchivos(); // Formato AAAAMMDD
	

	/**Constructor
	 * @param f Linea del archivo shp
	 */
	public Shape(SimpleFeature f, String tipo){
		
		this.tipo = tipo;
		
		// Algunos conversores de DATUM cambian el formato de double a int en el .shp
		// FECHAALATA y FECHABAJA siempre existen
		if (f.getAttribute("FECHAALTA") instanceof Double){
			double fa = (Double) f.getAttribute("FECHAALTA");
			fechaAlta = (long) fa;
		}
		else if (f.getAttribute("FECHAALTA") instanceof Long){
			fechaAlta = (long) f.getAttribute("FECHAALTA");
		}
		else if (f.getAttribute("FECHAALTA") instanceof Integer){
			int fa = (Integer) f.getAttribute("FECHAALTA");
			fechaAlta = (long) fa;
		}
		else System.out.println("["+new Timestamp(new Date().getTime())+"]\tNo se reconoce el tipo del atributo FECHAALTA "
				+ f.getAttribute("FECHAALTA").getClass().getName());	

		if (f.getAttribute("FECHABAJA") instanceof Integer){
			int fb = (Integer) f.getAttribute("FECHABAJA");
			fechaBaja = (long) fb;
		}
		else  if (f.getAttribute("FECHABAJA") instanceof Double){
			double fb = (Double) f.getAttribute("FECHABAJA");
			fechaBaja = (long) fb;
		}
		else if (f.getAttribute("FECHABAJA") instanceof Long){
			fechaBaja = (long) f.getAttribute("FECHABAJA");
		}
		else System.out.println("["+new Timestamp(new Date().getTime())+"]\tNo se reconoce el tipo del atributo FECHABAJA"
				+ f.getAttribute("FECHABAJA").getClass().getName());
		}

	
	/** Comprueba la fechaAlta y fechaBaja del shape para ver si se ha creado entre AnyoDesde y AnyoHasta
	 * Deben seguir dados de alta despues de fechaHasta para que los devuelva. Es decir, shapes que se hayan
	 * creado y dado de baja en ese intervalo no las devolvera.
	 * @param fechaDesde fecha a partir de la cual se cogeran los shapes
	 * @param fechaHasta fecha hasta la cual se cogeran
	 * @return boolean Devuelve si se ha creado entre fechaAlta y fechaBaja o no
	 */
	public boolean checkCatastroDate(long fechaDesde, long fechaHasta){
		return (fechaAlta >= fechaDesde && fechaAlta < fechaHasta && fechaBaja >= fechaHasta);
	}
	
	
	/** Comprueba la fecha de construccion del shape para ver si se ha construido entre AnyoDesde y AnyoHasta
	 * @param fechaDesde fecha a partir de la cual se cogeran los shapes
	 * @param fechaHasta fecha hasta la cual se cogeran
	 * @return boolean Devuelve si se ha creado entre fechaAlta y fechaBaja o no
	 */
	public boolean checkBuildingDate(long fechaDesde, long fechaHasta){
		return (fechaConstru >= fechaDesde && fechaConstru <= fechaHasta);
	}


	public long getFechaAlta(){
		return fechaAlta;
	}


	public long getFechaBaja(){
		return fechaBaja;
	}


	public String getTipo(){
		return tipo;
	}
	

	public Geometry getGeometry(){
		return geometry;
	}
	

	public void setGeometry(Geometry geometry){
		this.geometry = geometry;
	}
	
	
	public synchronized long newShapeId(long Id){
		Id++;
		return Id;
	}
	

	public ShapeAttributes getAttributes(){
		if(this.attributes == null)
			this.attributes = new ShapeAttributes();
		return attributes;
	}
	
	
	public long getFechaConstru() {
		return fechaConstru;
	}

	
	public void setFechaConstru(long fechaConstru) {
		if (this.fechaConstru > fechaConstru)
			this.fechaConstru = fechaConstru;
	}
	
	
	public String getShapeId(){
		return shapeId;
	}
	
	
	public String getCodigoMasa(){
		return codigoMasa;
	}
	
	
	public void setCodigoMasa(String cod){
		codigoMasa = cod;
	}
	
	
	public String printAttributes(){
		if(this.attributes != null){
			return this.attributes.toOSM();
		}
		else 
			return "";
	}
	
	
	public boolean sameAttributes(ShapeAttributes attributes){
		return this.attributes.equals(attributes);
	}
	
	/////////////////////////////////////////////////////////////////////////
	
	// Metodos abstractos que implementaran sus hijos
	
	public abstract void setNodes(List<List<Long>> nodesId);
	
	public abstract void addNode(int pos, long nodeId);
	
	public abstract List<Long> getNodesIds(int pos);
	
	public abstract void setWays(List<Long> waysId);

	public abstract void addWay(int pos, long wayId);
	
	public abstract List<Long> getWays();
	
	public abstract void deleteWay(long wayId);

	public abstract void setRelation(long relationId);
	
	public abstract Long getRelationId();

	public abstract boolean hasRelevantAttributesInternally();
	
	public abstract boolean hasRelevantAttributesForPrinting();

	public abstract String getRefCat();

	//public abstract String getTtggss();

	public abstract boolean isValid();
	
	public abstract boolean toOSM(Cat2OsmUtils utils, ShapeParent parent);

	//////////////////////////////////////////////////////////////////////////
	
	
	/** Traduce el tipo de via
	 * @param codigo Codigo de via
	 * @return Nombre del tipo de via
	 */
	public static String nombreTipoViaParser(String codigo){

		switch (codigo){
		case "CL":return "Calle";
		case "AG":return "Agregado";
		case "AL":return "Aldea/Alameda";
		case "AR":return "Area/Arrabal";
		case "AU":return "Autopista";
		case "AV":return "Avenida";
		case "AY":return "Arroyo";
		case "BJ":return "Bajada";
		case "BO":return "Barrio";
		case "BR":return "Barranco";
		case "CA":return "Ca��ada";
		case "CG":return "Colegio/Cigarral";
		case "CH":return "Chalet";
		case "CI":return "Cinturon";
		case "CJ":return "Calleja/Callej��n";
		case "CM":return "Camino";
		case "CN":return "Colonia";
		case "CO":return "Concejo/Colegio";
		case "CP":return "Campa/Campo";
		case "CR":return "Carretera/Carrera";
		case "CS":return "Caser��o";
		case "CT":return "Cuesta/Costanilla";
		case "CU":return "Conjunto";
		case "DE":return "Detr��s";
		case "DP":return "Diputaci��n";
		case "DS":return "Diseminados";
		case "ED":return "Edificios";
		case "EM":return "Extramuros";
		case "EN":return "Entrada, Ensanche";
		case "ER":return "Extrarradio";
		case "ES":return "Escalinata";
		case "EX":return "Explanada";
		case "FC":return "Ferrocarril";
		case "FN":return "Finca";
		case "GL":return "Glorieta";
		case "GR":return "Grupo";
		case "GV":return "Gran V��a";
		case "HT":return "Huerta/Huerto";
		case "JR":return "Jardines";
		case "LD":return "Lado/Ladera";
		case "LG":return "Lugar";
		case "MC":return "Mercado";
		case "ML":return "Muelle";
		case "MN":return "Municipio";
		case "MS":return "Masias";
		case "MT":return "Monte";
		case "MZ":return "Manzana";
		case "PB":return "Poblado";
		case "PD":return "Partida";
		case "PJ":return "Pasaje/Pasadizo";
		case "PL":return "Pol��gono";
		case "PM":return "Paramo";
		case "PQ":return "Parroquia/Parque";
		case "PR":return "Prolongaci��n/Continuaci��n";
		case "PS":return "Paseo";
		case "PT":return "Puente";
		case "PZ":return "Plaza";
		case "QT":return "Quinta";
		case "RB":return "Rambla";
		case "RC":return "Rinc��n/Rincona";
		case "RD":return "Ronda";
		case "RM":return "Ramal";
		case "RP":return "Rampa";
		case "RR":return "Riera";
		case "RU":return "Rua";
		case "SA":return "Salida";
		case "SD":return "Senda";
		case "SL":return "Solar";
		case "SN":return "Sal��n";
		case "SU":return "Subida";
		case "TN":return "Terrenos";
		case "TO":return "Torrente";
		case "TR":return "Traves��a";
		case "UR":return "Urbanizaci��n";
		case "VR":return "Vereda";
		case "CY":return "Caleya";
		}

		return codigo;
		
//		switch(codigo){
//		case "CL":return "Carrer";
//		case "AG":return "Agregat";
//		case "AL":return "Llogaret";
//		case "AR":return "Raval";
//		case "AU":return "Autopista";
//		case "AV":return "Avinguda";
//		case "AY":return "Rierol";
//		case "BJ":return "Baixada";
//		case "BO":return "Barri";
//		case "BR":return "Barranc";
//		case "CA":return "-";
//		case "CG":return "-";
//		case "CH":return "Xalet";
//		case "CI":return "Cintur��";
//		case "CJ":return "Carrer��";
//		case "CM":return "Cam��";
//		case "CN":return "Col��nia";
//		case "CO":return "-";
//		case "CP":return "Camp";
//		case "CR":return "Carretera";
//		case "CS":return "Mas ??";
//		case "CT":return "Pujada";
//		case "CU":return "Conjunt";
//		case "DE":return "-";
//		case "DP":return "Diputaci��";
//		case "DS":return "Disseminats";
//		case "ED":return "Edificis";
//		case "EM":return "Extramurs";
//		case "EN":return "Eixample ??";
//		case "ER":return "Extraradi";
//		case "ES":return "Escalinata";
//		case "EX":return "Pla";
//		case "FC":return "Ferrocarril";
//		case "FN":return "Finca";
//		case "GL":return "-";
//		case "GR":return "Grup";
//		case "GV":return "Gran V��a";
//		case "HT":return "Hort";
//		case "JR":return "Jardins";
//		case "LD":return "Vessant ??";
//		case "LG":return "Lloc ??";
//		case "MC":return "Mercat";
//		case "ML":return "Moll";
//		case "MN":return "Municipi";
//		case "MS":return "Masies";
//		case "MT":return "Muntanya ??";
//		case "MZ":return "Illa ??";
//		case "PB":return "Poblat ??";
//		case "PD":return "-";
//		case "PJ":return "Passatge";
//		case "PL":return "Pol��gon";
//		case "PM":return "-";
//		case "PQ":return "-";
//		case "PR":return "-";
//		case "PS":return "Passeig";
//		case "PT":return "Pont";
//		case "PZ":return "Pla��a";
//		case "QT":return "-";
//		case "RB":return "Rambla";
//		case "RC":return "-";
//		case "RD":return "Ronda";
//		case "RM":return "-";
//		case "RP":return "Rampa";
//		case "RR":return "Riera";
//		case "RU":return "Rua";
//		case "SA":return "Sortida";
//		case "SD":return "Sender";
//		case "SL":return "Solar";
//		case "SN":return "-";
//		case "SU":return "Pujada";
//		case "TN":return "Terrenys";
//		case "TO":return "Torrent";
//		case "TR":return "Travessera";
//		case "UR":return "Urbanitzaci��";
//		case "VR":return "-";
//		case "CY":return "-";}
		
	}


	/** Traduce el ttggss de Elemlin y Elempun. Elemtex tiene en su clase su propio parser
	 * ya que necesita mas datos suyos propios.
	 * @param ttggss Atributo ttggss
	 * @param rotulo Texto que se pasa SOLO en los elementos textuales
	 * @return Lista de los tags que genera
	 */
	public Map<String,String> ttggssParser(String ttggss, String rotulo){
		Map<String,String> l = new HashMap<String,String>();

		//setTtggss(ttggss);
		
		switch (ttggss){

		// Divisiones administrativas
		case "010401":
			l.put("admin_level","2");
			l.put("boundary","administrative");
			l.put("border_type","nation");
			return l;
		case "010301":
			l.put("admin_level","4");
			l.put("boundary","administrative");
			return l;
		case "010201":
			l.put("admin_level","6");
			l.put("boundary","administrative");
			return l;
		case "010101":
			l.put("admin_level","8");
			l.put("boundary","administrative");
			return l;
		case "010102":
			l.put("admin_level","10");
			l.put("boundary","administrative");
			return l;
		case "018507":
			l.put("historic","boundary_stone");
			return l;
		case "018506":
			l.put("historic","boundary_stone");
			return l;

			// Relieve
		case "028110":
			l.put("man_made","survey_point");
			return l;
		case "028112":
			l.put("man_made","survey_point");
			return l;

			// Hidrografia
		case "030102":
			l.put("waterway","river");
			return l;
		case "030202": 
			l.put("waterway","stream");
			return l;
		case "030302": 
			l.put("waterway","drain");
			return l;
		case "032301": 
			l.put("natural","coastline");
			return l;
		case "033301": 
			l.put("natural","water");
			l.put("fixme","Especificar tipo de agua (natural=water / leisure=swimming_pool / man_made=water_well / amenity=fountain / ...), eliminar landuse=reservoir y/o comprobar que no este duplicado o contenido en otra geometria de agua.");
			return l;
		case "037101": 
			l.put("man_made","water_well");
			return l;
		case "038101": 
			l.put("man_made","water_well");
			return l;
		case "038102": 
			l.put("amenity","fountain");
			return l;
		case "037102": 
			l.put("natural","water");
			l.put("fixme","Especificar tipo de agua (natural=water / leisure=swimming_pool / man_made=water_well / amenity=fountain / ...), eliminar landuse=reservoir y/o comprobar que no este duplicado o contenido en otra geometria de agua.");
			return l;
		case "037107": 
			l.put("waterway","dam");
			return l;

			// Vias de comunicacion
		case "060102": 
			l.put("ttggss",ttggss);
			return l;
		case "060104": 
			l.put("highway","motorway");
			return l;
		case "060202": 
			l.put("ttggss",ttggss);
			return l;
		case "060204": 
			l.put("highway","primary");
			return l;
		case "060402": 
			l.put("ttggss",ttggss);
			return l;
		case "060404": 
			l.put("highway","track");
			return l;
		case "060109": 
			l.put("railway","funicular");
			return l;
		case "061104": 
			l.put("railway","rail");
			return l;
		case "067121": 
			l.put("bridge","yes");
			return l;
		case "068401": 
			l.put("highway","milestone");
			return l;

			// Red geodesica y topografica
		case "108100": 
			l.put("man_made","survey_point");
			return l;
		case "108101": 
			l.put("man_made","survey_point");
			return l;
		case "108104": 
			l.put("man_made","survey_point");
			return l;

			// Delimitaciones catastrales urbanisticas y estadisticas
		case "111101": 
			l.put("admin_level","10");
			l.put("boundary","administrative");
			return l;
		case "111000": 
			l.put("admin_level","12");
			return l;
		case "111200": 
			l.put("admin_level","14");
			return l;
		case "111300": 
			l.put("admin_level","10");
			return l;
		case "115101": 
			l.put("ttggss",ttggss);
			return l;
		case "115000": 
			l.put("ttggss",ttggss);
			return l;
		case "115200": 
			l.put("ttggss",ttggss);
			return l;
		case "115300": 
			l.put("ttggss",ttggss);
			return l;

			// Rustica (Compatibilidad 2006 hacia atras)
		case "120100": 
			l.put("ttggss",ttggss);
			return l;
		case "120200": 
			l.put("ttggss",ttggss);
			return l;
		case "120500": 
			l.put("ttggss",ttggss);
			return l;
		case "120180": 
			l.put("ttggss",ttggss);
			return l;
		case "120280": 
			l.put("ttggss",ttggss);
			return l;
		case "120580": 
			l.put("ttggss",ttggss);
			return l;
		case "125101": 
			l.put("ttggss",ttggss);
			return l;
		case "125201": 
			l.put("ttggss",ttggss);
			return l;
		case "125501": 
			l.put("ttggss",ttggss);
			return l;
		case "125510": 
			l.put("ttggss",ttggss);
			return l;

			// Rustica y Urbana
		case "130100": 
			l.put("ttggss",ttggss);
			return l;
		case "130200": 
			l.put("ttggss",ttggss);
			return l;
		case "130500": 
			l.put("ttggss",ttggss);
			return l;
		case "135101": 
			l.put("ttggss",ttggss);
			return l;
		case "135201": 
			l.put("ttggss",ttggss);
			return l;
		case "135501": 
			l.put("ttggss",ttggss);
			return l;
		case "135510": 
			l.put("ttggss",ttggss);
			return l;

			// Urbana (Compatibilidad 2006 hacia atras)
		case "140100": 
			l.put("ttggss",ttggss);
			return l;
		case "140190": 
			l.put("ttggss",ttggss);
			return l;
		case "140200": 
			l.put("ttggss",ttggss);
			return l;
		case "140290": 
			l.put("ttggss",ttggss);
			return l;
		case "140500": 
			l.put("ttggss",ttggss);
			return l;
		case "140590": 
			l.put("ttggss",ttggss);
			return l;
		case "145101": 
			l.put("ttggss",ttggss);
			return l;
		case "145201": 
			l.put("ttggss",ttggss);
			return l;
		case "145501": 
			l.put("ttggss",ttggss);
			return l;
		case "145510": 
			l.put("ttggss",ttggss);
			return l;

			// Infraestructura/Mobiliario
		case "160101": 
			l.put("kerb","yes");
			return l;
		case "160131": 
			l.put("ttggss",ttggss);
			return l;
		case "160132": 
			l.put("ttggss",ttggss);
			return l;
		case "160201": 
			l.put("power","line");
			return l;
		case "160202": 
			l.put("telephone","line");
			return l;
		case "160300": 
			l.put("ttggss",ttggss);
			return l;
		case "161101": 
			l.put("highway","road");
			return l;
		case "167103": 
			l.put("historic","monument");
			return l;
		case "167104": 
			l.put("highway","steps");
			return l;
		case "167106": 
			l.put("highway","footway");
			l.put("tunnel","yes");
			return l;
		case "167111": 
			l.put("power","sub_station");
			return l;
		case "167167": 
			l.put("ttggss",ttggss);
			return l;
		case "167201": 
			l.put("barrier","hedge");
			return l;
		case "168100": 
			l.put("ttggss",ttggss);
			return l;
		case "168103": 
			l.put("historic","monument");
			return l;
		case "168113": 
			l.put("power","pole");
			return l;
		case "168116": 
			l.put("highway","street_lamp");
			return l;
		case "168153": 
			l.put("natural","tree");
			return l;
		case "168168": 
			l.put("amenity","parking_entrance");
			return l;
			
		// Elementos textuales
		case "189203":
			l.put("place","locality");
			l.put("name",rotulo);
			return l;
		case "189300":
		case "189700":
			l.put("name",rotulo);
			return l;
		case "189401":
			l.put("entrance","yes");
			l.put("add:housenumber", rotulo);
			return l;
			
		default: if (!ttggss.isEmpty()){
			l.put("fixme", "Documentar ttggss="+ttggss+" si es preciso en http://wiki.openstreetmap.org/w/index.php?title=Traduccion_metadatos_catastro_a_map_features");
			//setTtggss("0");
			}
		}
		return l;
	}


	/** Elimina los puntos '.' y espacios en un String
	 * @param s String en el cual eliminar los puntos
	 * @return String sin los puntos
	 */
	public static String eliminarPuntosString(String s){
		if (!s.isEmpty()){
			s = s.replace('.', ' ');
			s = s.replace(" ", "");
		}
		return s.trim();
	}


	/** Eliminar las comillas '"' de los textos, sino al leerlo JOSM devuelve error
	 * pensando que ha terminado un valor antes de tiempo.
	 * @param s String al que quitar las comillas
	 * @return String sin las comillas
	 */
	public static String eliminarComillas(String s){
		String ret = "";
		for (int x = 0; x < s.length(); x++)
			if (s.charAt(x) != '"') ret += s.charAt(x);
		return ret;
	}

}
