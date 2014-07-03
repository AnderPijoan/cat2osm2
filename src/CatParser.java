import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;


public class CatParser {

	public static Cat2OsmUtils utils;

	public CatParser (Cat2OsmUtils utils){
		CatParser.utils = utils;
	}
	
	/** Lee linea a linea el archivo cat, coge los shapes q coincidan 
	 * con esa referencia catastral y les anade los tags de los registros .cat
	 * @param cat Archivo cat del que lee linea a linea
	 * @param List<Shape> Lista de los elementos shp parseados ya en memoria
	 * @throws IOException
	 */
	public void parseFile(String tipo, File cat, HashMap <String, List<Shape>> shapesTotales) throws IOException{

		BufferedReader bufRdr = createCatReader(cat);
		String line = null; // Para cada linea leida del archivo .cat

		int tipoRegistrosBuscar = Integer.parseInt(Config.get("TipoRegistro"));

		// Lectura del archivo .cat
		while((line = bufRdr.readLine()) != null)
		{
			// Parsear la linea leida
			Cat c = catLineParser(line);
			String key = "";

			if (tipo.equals("UR") && c.getRefCatastral() != null) // El codigo de masa URBANA son los primeros 5 caracteres
				key = c.getRefCatastral().substring(0, 5).replaceAll("[^\\p{L}\\p{N}]", "") + "-";
			if (tipo.equals("RU") && c.getRefCatastral() != null) // El codigo de masa RUSTICA son los caracteres 6, 7 y 8
				key = c.getRefCatastral().substring(6, 9).replaceAll("[^\\p{L}\\p{N}]", "") + "-";

			if (shapesTotales.get(key) != null && (c.getTipoRegistro() == tipoRegistrosBuscar || tipoRegistrosBuscar == 0)){

				// Obtenemos los shape que coinciden con la referencia catastral de la linea leida
				List <Shape> matches = buscarRefCat(shapesTotales.get(key), c.getRefCatastral());

				if (matches != null)
					switch (c.getTipoRegistro()){

					// El registro 11 solo se tiene que asignar a parcelas
					case 11:
						matches = buscarParce(matches);
						break;

						// El registro 13 es para bienes inmuebles e incluye la fecha de construccion. Como no
						// hay forma de asociarlo con cada bien inmueble, se la asociamos a toda su referencia
						// catastral, cogiendo la menor de todas, es decir la mas antigua.

						// El registro 14 es para bienes inmuebles pero como no hay forma de relacionarlo
						// se ha hecho que la parcela acumule todos los destinos y usos y al final elija el que mayor
						// area tiene. Ese dato solo se almacena en el shape, luego habra que llamar al metodo
						// que calcule los destinos para pasarlos a las relaciones.
					case 14: 
						matches = buscarParce(matches);
						for (Shape match : matches){
							((ShapeParcela) match).addDestino(c.getUsoDestino(), c.getArea());
						}
						break;

						// El registro 15 es para bienes inmuebles pero como no hay forma de relacionarlo
						// se ha hecho que la parcela acumule todos los destinos y usos y al final elija el que mayor
						// area tiene. Ese dato solo se almacena en el shape, luego habra que llamar al metodo
						// que calcule los destinos para pasarlos a las relaciones.
					case 15:
						matches = buscarParce(matches);
						for (Shape match : matches){
							((ShapeParcela) match).addUso(c.getUsoDestino(), c.getArea());
						}
						break;

						// Para los tipos de registro de subparcelas, buscamos la subparcela concreta para
						// anadirle los atributos
					case 17:
						//matches = buscarSubparce(matches, c.getSubparce());
						break;

					}

				// Insertamos los atributos leidos, la fecha de construccion y le indicamos que parsee
				// los usos y destinos para que cree los tags relevantes
				if (matches != null)
					for (Shape shape : matches)
						if (shape != (null) && c.getAttributes() != null){
							shape.getAttributes().addAll(c.getAttributes().asHashMap());
							shape.setFechaConstru(c.getFechaConstru());
						}
			}
		}
		bufRdr.close();
	}
	
	
	/** Lee linea a linea el archivo cat, coge los registros 14 que son los que tienen uso
	 * de inmuebles y con el punto X e Y del centroide de la parcela que coincide con su referencia
	 * catastral crea nodos con los usos
	 * de los bienes inmuebles
	 */
	public void parseUsosFile(String tipo, File cat, HashMap <String, List<Shape>> shapesTotales) throws IOException{

				BufferedReader bufRdr = createCatReader(cat);
				String line = null; // Para cada linea leida del archivo .cat
		
				// Lectura del archivo .cat
				while((line = bufRdr.readLine()) != null)
				{
					Cat c = catLineParser(line);
					String key = "";
		
					if (tipo.equals("UR") && c.getRefCatastral() != null) // El codigo de masa son los primeros 5 caracteres
						key = c.getRefCatastral().substring(0, 5).replaceAll("[^\\p{L}\\p{N}]", "") + "-";
					if (tipo.equals("RU") && c.getRefCatastral() != null) // El codigo de masa son los caracteres 6, 7 y 8
						key = c.getRefCatastral().substring(6, 9).replaceAll("[^\\p{L}\\p{N}]", "") + "-";
		
					// Si es registro 14
					if (!"".equals(key) && null != shapesTotales.get(key) && utils.esNumero(line.substring(0,2)) && line.substring(0,2).equals("14")){
		
						// Cogemos las geometrias con esa referencia catastral.
						List<Shape> matches = buscarRefCat(shapesTotales.get(key), c.getRefCatastral());
		
						// Puede que no haya shapes para esa refCatastral
						if (matches != null)
							for (Shape shape : matches)
								if (shape != null && shape instanceof ShapeParcela && shape.getGeometry() != null && !shape.getGeometry().isEmpty()){
		
									ShapeAttributes tags = new ShapeAttributes();
									
									/*
									// Metemos los tags de uso de inmuebles con el numero de inmueble por delante
									for(String[] s : ShapeParcela.destinoParser(c.getUsoDestino())){
										tags.addAttribute(c.getRefCatastral()+":"+c.getNumOrdenConstru()+":"+s[0], s[1]);
									}

									// Anadimos el piso de esa unidad constructiva
									tags.addAttribute(c.getRefCatastral()+":"+c.getNumOrdenConstru()+":addr:floor", line.substring(64,67).trim());
									
									// Anadimos el area de esa unidad constructiva
									tags.addAttribute(c.getRefCatastral()+":"+c.getNumOrdenConstru()+":area", c.getArea()+"");
									
									shape.addAttributes(tags.getAttributesAsHashMap());
									*/
									System.out.println("ARREGLAR!!");
								}
					}
				}
				bufRdr.close();
	}
	
	
	private BufferedReader createCatReader(File archivoCat) throws IOException {
		InputStream inputStream = new FileInputStream(archivoCat);
		if (archivoCat.getName().toLowerCase().endsWith(".gz")){
			inputStream = new GZIPInputStream(inputStream);
		}
		return new BufferedReader(new InputStreamReader(inputStream, "ISO-8859-15"));
	}
	
	
	/** De la lista de shapes con la misma referencia catastral devuelve la mas actual
	 * del periodo indicado.
	 * @param shapes Lista de shapes
	 * @returns El shape que hay para el periodo indicado porque hay shapes que 
	 * tienen distintas versiones a lo largo de los anos
	 */
	public Shape buscarShapesParaFecha(List<Shape> shapes){

		// Lo habitual es que venga ordenado de mas reciente a mas antiguo
		Shape s = shapes.get(shapes.size()-1);
		long fA = 00000101;
		long fechaHasta = Long.parseLong(Config.get("FechaHasta"));

		for (int x = shapes.size()-1 ; x >= 0 ; x--){

			if (shapes.get(x).getFechaAlta() >= fA && shapes.get(x).getFechaAlta() < fechaHasta && shapes.get(x).getFechaBaja() >= fechaHasta){
				s = shapes.get(x);
				fA = s.getFechaAlta();
			}
		}
		return s;
	}
	
	
	/** Busca en la lista de shapes los que coincidan con la ref catastral
	 * @param codigo Codigo de masa
	 * @param ref referencia catastral a buscar
	 * @returns List<Shape> lista de shapes que coinciden                    
	 */
	private List<Shape> buscarRefCat(List<Shape> shapes, String ref){

		List<Shape> shapeList = null;

		for(Shape shape : shapes) 
			if (shape != null && shape.getRefCat() != null && shape.getRefCat().equals(ref)){
				if (shapeList == null)
					shapeList = new ArrayList<Shape>();
				shapeList.add(shape);
			}

		return shapeList;
	}


	/** Busca en la lista de shapes los que coincidan con el codigo de subparce
	 * @param shapes lista de shapes que han coincidido con la refCat para buscar en ella las subparcelas
	 * @param subparce Codigo de subparcela para obtener la que corresponda
	 * @returns List<Shape> lista de shapes que coinciden                    
	 */
	private List<Shape> buscarSubparce(List<Shape> shapes, String subparce){
		List<Shape> shapeList = new ArrayList<Shape>();

		for(Shape shape : shapes) 
			if (shape != null && shape instanceof ShapeSubparce)
				if (((ShapeSubparce) shape).getSubparce().equals(subparce))
					shapeList.add(shape);

		return shapeList;
	}


	/** Busca en la lista de shapes los que sean parcelas
	 * @param shapes lista de shapes
	 * @return List<Shape> lista de shapes que coinciden  
	 */
	private List<Shape> buscarParce(List<Shape> shapes){
		List<Shape> shapeList = new ArrayList<Shape>();

		for(Shape shape : shapes) 
			if (shape != null && shape instanceof ShapeParcela)
				shapeList.add((ShapeParcela) shape);

		return shapeList;
	}

	
	/** Parsea la linea del archivo .cat y devuelve un elemento Cat
	 * @param line Linea del archivo .cat
	 * @returns Cat Elemento Cat con todos los campos leidos en la linea
	 * @throws IOException 
	 * @see http://www.catastro.meh.es/pdf/formatos_intercambio/catastro_fin_cat_2006.pdf
	 */
	private Cat catLineParser(String line) throws IOException{

		Cat c = null;

		if (utils.esNumero(line.substring(0,2)))
			c = new Cat(Integer.parseInt(line.substring(0,2)));
		else
			c = new Cat(0);

		switch(c.getTipoRegistro()){ // Formato de los tipos distintos de registro .CAT
		case 01: {

			/*System.out.println("\nTIPO DE REGISTRO 01: REGISTRO DE CABECERA\n");
			System.out.println("TIPO DE ENTIDAD GENERADORA                  :"+line.substring(2,3));
			System.out.println("CODIGO DE LA ENTIDAD GENERADORA             :"+line.substring(3,12));
			System.out.println("NOMBRE DE LA ENTIDAD GENERADORA             :"+line.substring(12,39));
			System.out.println("FECHA DE GENERACION (AAAAMMDD)              :"+line.substring(39,47)); 
			System.out.println("HORA DE GENERACION (HHMMSS)                 :"+line.substring(47,53));
			System.out.println("TIPO DE FICHERO                             :"+line.substring(53,57));
			System.out.println("DESCRIPCION DEL CONTENIDO DEL FICHERO       :"+line.substring(57,96));
			System.out.println("NOMBRE DE FICHERO                           :"+line.substring(96,117));
			System.out.println("CODIGO DE LA ENTIDAD DESTINATARIA           :"+line.substring(117,120));
			System.out.println("FECHA DE INICIO DEL PERIODO (AAAAMMDD)      :"+line.substring(120,128));
			System.out.println("FECHA DE FIN DEL PERIODO (AAAAMMDD)         :"+line.substring(128,136));
			 */
			break;}
		case 11: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2)); 
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(25,28)));
			//c.addAttribute("BLANCO EXCEPTO INMUEBLES ESPECIALES",line.substring(28,30));
			c.addAttribute("catastro:special",eliminarComillas(line.substring(28,30)));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44)); 
			//c.addAttribute("CODIGO DE PROVINCIA",line.substring(50,52));
			//c.addAttribute("catastro:ref:province",eliminarCerosString(line.substring(50,52)));
			//c.addAttribute("NOMBRE DE PROVINCIA",line.substring(52,77));
			//c.addAttribute("is_in:province",line.substring(52,77));
			//c.addAttribute("CODIGO DE MUNICIPIO",line.substring(77,80));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(77,80)));
			//c.addAttribute("CODIGO DE MUNICIPIO (INE). EXCLUIDO ULTIMO DIGITO DE CONTROL:",line.substring(80,83));
			//c.addAttribute("ine:ref:municipality",eliminarCerosString(line.substring(80,83)));
			//c.addAttribute("NOMBRE DE MUNICIPIO",line.substring(83,123));
			//c.addAttribute("is_in:municipality",eliminarComillas(line.substring(83,123)));
			//c.addAttribute("NOMBRE DE LA ENTIDAD MENOR EN CASO DE EXISTIR",line.substring(123,153));
			//c.addAttribute("CODIGO DE VIA PUBLICA",line.substring(153,158));
			//c.addAttribute("catastro:ref:way",eliminarCerosString(line.substring(153,158)));
			//c.addAttribute("TIPO DE VIA O SIGLA PUBLICA",line.substring(158,163));
			//c.addAttribute("NOMBRE DE VIA PUBLICA",line.substring(163,188));
			c.addAttribute("addr:street",nombreTipoViaParser(line.substring(158,163).trim())+" "+formatearNombreCalle(eliminarComillas(line.substring(163,188).trim())));
			//c.addAttribute("PRIMER NUMERO DE POLICIA",line.substring(188,192));
			//c.addAttribute("PRIMERA LETRA (CARACTER DE DUPLICADO)",line.substring(192,193));
			c.addAttribute("addr:housenumber",utils.eliminarCerosString(line.substring(188,192))+line.substring(192,193).trim());
			//c.addAttribute("SEGUNDO NUMERO DE POLICIA",line.substring(193,197));
			//c.addAttribute("SEGUNDA LETRA (CARACTER DE DUPLICADO)",line.substring(197,198));
			//c.addAttribute("KILOMETRO (3enteros y 2decimales)",line.substring(198,203));
			//c.addAttribute("BLOQUE",line.substring(203,207));
			//c.addAttribute("TEXTO DE DIRECCION NO ESTRUCTURADA",line.substring(215,240));
			c.addAttribute("name",eliminarComillas(line.substring(215,240)));
			//c.addAttribute("CODIGO POSTAL",line.substring(240,245));
//			if (!line.substring(240,245).equals("00000"))
//				c.addAttribute("addr:postcode", line.substring(240,245));
//			if (!line.substring(240,245).isEmpty() && !line.substring(240,245).equals("00000"))
//				c.addAttribute("addr:country","ES");
			//c.addAttribute("DISTRITO MUNICIPAL",line.substring(245,247));
			//c.addAttribute("CODIGO DEL MUNICIPIO ORIGEN EN CASO DE AGREGACION",line.substring(247,250));
			//c.addAttribute("CODIGO DE LA ZONA DE CONCENTRACION",line.substring(250,252));
			//c.addAttribute("CODIGO DE POLIGONO",line.substring(252,255));
			//c.addAttribute("catastro:ref:polygon",eliminarCerosString(line.substring(252,255)));
			//c.addAttribute("CODIGO DE PARCELA",line.substring(255,260));
			//c.addAttribute("CODIGO DE PARAJE",line.substring(260,265));
			//c.addAttribute("NOMBRE DEL PARAJE",line.substring(265,295));
			//c.addAttribute("SUPERFICIE CATASTRAL (metros cuadrados)",line.substring(295,305));
			//c.addAttribute("catastro:surface",eliminarCerosString(line.substring(295,305)));
			//c.addAttribute("SUPERFICIE CONSTRUIDA TOTAL",line.substring(305,312));
			//if (!eliminarCerosString(line.substring(295,305)).equals(eliminarCerosString(line.substring(305,312))))
			//c.addAttribute("catastro:surface:built",eliminarCerosString(line.substring(305,312)));
			//c.addAttribute("SUPERFICIE CONSTRUIDA SOBRE RASANTE",line.substring(312,319));
			//if (!eliminarCerosString(line.substring(295,305)).equals(eliminarCerosString(line.substring(312,319))))
			//c.addAttribute("catastro:surface:overground",eliminarCerosString(line.substring(312,319)));
			//c.addAttribute("SUPERFICIE CUBIERTA",line.substring(319,333));
			//c.addAttribute("COORDENADA X (CON 2 DECIMALES Y SIN SEPARADOR)",line.substring(333,342));
			//c.addAttribute("COORDENADA Y (CON 2 DECIMALES Y SIN SEPARADOR)",line.substring(342,352));
			//c.addAttribute("REFERENCIA CATASTRAL BICE DE LA FINCA",line.substring(581,601));
			//c.addAttribute("catastro:ref:bice",eliminarCerosString(line.substring(581,601)));
			//c.addAttribute("DENOMINACION DEL BICE DE LA FINCA",line.substring(601,666));
			//c.addAttribute("HUSO GEOGRAFICO SRS",line.substring(666,676));

			return c;}
		case 13: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DE MUNICIPIO",line.substring(25,28));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(25,28)));
			//c.addAttribute("CLASE DE LA UNIDAD CONSTRUCTIVA",line.substring(28,30));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("CODIGO DE LA UNIDAD CONSTRUCTIVA",line.substring(44,48));
			if (utils.esNumero(line.substring(44,48)) && Integer.parseInt(line.substring(44,48)) != 0)
				c.setNumOrdenConstru(Integer.parseInt(line.substring(44,48)));
			else
				c.setSubparce(line.substring(44,48));
			//c.addAttribute("CODIGO DE PROVINCIA",line.substring(50,52));
			//c.addAttribute("catastro:ref:province",eliminarCerosString(line.substring(50,52)));
			//c.addAttribute("NOMBRE PROVINCIA",line.substring(52,77));
			//c.addAttribute("is_in_province",line.substring(52,77));
			//c.addAttribute("CODIGO DEL MUNICIPIO DGC",line.substring(77,80));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(77,80)));
			//c.addAttribute("CODIGO DE MUNICIPIO (INE) EXCLUIDO EL ULTIMO DIGITO DE CONTROL",line.substring(80,83));
			//c.addAttribute("ine:ref:municipality",eliminarCerosString(line.substring(80,83)));
			//c.addAttribute("NOMBRE DEL MUNICIPIO",line.substring(83,123));
			//c.addAttribute("is_in:municipality",eliminarComillas(line.substring(83,123)));
			//c.addAttribute("NOMBRE DE LA ENTIDAD MENOR EN CASO DE EXISTIR",line.substring(123,153));
			//c.addAttribute("CODIGO DE VIA PUBLICA DGC",line.substring(153,158));
			//c.addAttribute("catastro:ref:way",eliminarCerosString(line.substring(153,158)));
			//c.addAttribute("TIPO DE VIA O SIBLA PUBLICA",line.substring(158,163));
			//c.addAttribute("NOMBRE DE VIA PUBLICA",line.substring(163,188));
			//c.addAttribute("addr:street",nombreTipoViaParser(line.substring(158,163).trim())+" "+formatearNombreCalle(eliminarComillas(line.substring(163,188).trim())));
			//c.addAttribute("PRIMER NUMERO DE POLICIA",line.substring(188,192));
			//c.addAttribute("addr:housenumber",eliminarCerosString(line.substring(188,192)));
			//c.addAttribute("PRIMERA LETRA (CARACTER DE DUPLICADO)",line.substring(192,193));
			//c.addAttribute("SEGUNDO NUMERO DE POLICIA",line.substring(193,197));
			//c.addAttribute("SEGUNDA LETRA (CARACTER DE DUPLICADO)",line.substring(197,198));
			//c.addAttribute("KILOMETRO (3enteros y 2decimales)",line.substring(198,203));
			//c.addAttribute("TEXTO DE DIRECCION NO ESTRUCTURADA",line.substring(215,240));
			//c.addAttribute("name",eliminarComillas(line.substring(215,240).trim()));
			//c.addAttribute("ANO DE CONSTRUCCION (AAAA)",line.substring(295,299));
			c.setFechaConstru(Long.parseLong(line.substring(295,299)+"0101"));
			//c.addAttribute("INDICADOR DE EXACTITUD DEL ANO DE CONTRUCCION",line.substring(299,300));
			//c.addAttribute("SUPERFICIE DE SUELO OCUPADA POR LA UNIDAD CONSTRUCTIVA",line.substring(300,307));
			//c.addAttribute("catastro:surface",eliminarCerosString(line.substring(300,307)));
			//c.addAttribute("LONGITUD DE FACHADA",line.substring(307,312));
			//c.addAttribute("CODIGO DE UNIDAD CONSTRUCTIVA MATRIZ",line.substring(409,413));

			return c; }

		case 14: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2)); 
			//c.addAttribute("CODIGO DE DELEGACION DEL MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(25,28)));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("NUMERO DE ORDEN DEL ELEMENTO DE CONSTRUCCION",line.substring(44,48));
			if (utils.esNumero(line.substring(44,48)) && Integer.parseInt(line.substring(44,48)) != 0)
				c.setNumOrdenConstru(Integer.parseInt(line.substring(44,48)));
			else
				c.setSubparce(line.substring(44,48));
			//c.addAttribute("NUMERO DE ORDEN DEL BIEN INMUEBLE FISCAL",line.substring(50,54));
			//c.addAttribute("CODIGO DE LA UNIDAD CONSTRUCTIVA A LA QUE ESTA ASOCIADO EL LOCAL",line.substring(54,58));
			//c.addAttribute("BLOQUE",line.substring(58,62));
			//c.addAttribute("ESCALERA",line.substring(62,64));
			//c.addAttribute("PLANTA",line.substring(64,67));
			//c.addAttribute("PUERTA",line.substring(67,70));
			//c.addAttribute("CODIGO DE DESTINO SEGUN CODIFICACION DGC",line.substring(70,73));
			c.setUsoDestino(line.substring(70,73).trim());
			//c.addAttribute("INDICADOR DEL TIPO DE REFORMA O REHABILITACION",line.substring(73,74));
			//c.addAttribute("ANO DE REFORMA EN CASO DE EXISTIR",line.substring(74,78));
			//c.addAttribute("ANO DE ANTIGUEDAD EFECTIVA EN CATASTRO",line.substring(78,82)); 
			//c.addAttribute("INDICADOR DE LOCAL INTERIOR (S/N)",line.substring(82,83));
			//c.addAttribute("SUPERFICIE TOTAL DEL LOCAL A EFECTOS DE CATASTRO",line.substring(83,90));
			if (utils.esNumero(line.substring(83,90).trim()))
				c.setArea(Double.parseDouble(line.substring(83,90).trim()));
			else
				c.setArea((double) 10);
			//c.addAttribute("SUPERFICIA DE PORCHES Y TERRAZAS DEL LOCAL",line.substring(90,97));
			//c.addAttribute("SUPERFICIE IMPUTABLE AL LOCAL SITUADA EN OTRAS PLANTAS",line.substring(97,104));
			//c.addAttribute("TIPOLOGIA CONSTRUCTIVA SEGUN NORMAS TECNICAS DE VALORACION",line.substring(104,109));
			//c.addAttribute("CODIGO DE MODALIDAD DE REPARTO",line.substring(111,114));

			return c;}

		case 15: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(25,28)));
			//c.addAttribute("CLASE DE BIEN INMUEBLE (UR, RU, BI)",line.substring(28,30));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("NUMERO SECUENCIAL DEL BIEN INMUEBLE DENTRO DE LA PARCELA",line.substring(44,48));
			if (utils.esNumero(line.substring(44,48)) && Integer.parseInt(line.substring(44,48)) != 0)
				c.setNumOrdenConstru(Integer.parseInt(line.substring(44,48)));
			else
				c.setSubparce(line.substring(44,48));
			//c.addAttribute("PRIMER CARACTER DE CONTROL",line.substring(48,49));
			//c.addAttribute("SEGUNDO CARACTER DE CONTROL",line.substring(49,50));
			//c.addAttribute("NUMERO FIJO DEL BIEN INMUEBLE",line.substring(50,58));
			//c.addAttribute("CAMPO PARA LA IDENTIFICACION DEL BIEN INMUEBLE ASIGNADO POR EL AYTO",line.substring(58,73));
			//c.addAttribute("NUMERO DE FINCA REGISTRAL",line.substring(73,92));
			//c.addAttribute("CODIGO DE PROVINCIA",line.substring(92,94));
			//c.addAttribute("catastro:ref:province",eliminarCerosString(line.substring(92,94)));
			//c.addAttribute("NOMBRE DE PROVINCIA",line.substring(94,119));
			//c.addAttribute("is_in:province",eliminarComillas(line.substring(94,119)));
			//c.addAttribute("CODIGO DE MUNICIPIO",line.substring(119,122));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(119,122)));
			//c.addAttribute("CODIGO DE MUNICIPIO (INE) EXCLUIDO EL ULTIMO DIGITO DE CONTROL",line.substring(122,125));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(122,125)));
			//c.addAttribute("NOMBRE DE MUNICIPIO",line.substring(125,165));
			//c.addAttribute("is_in:municipality",eliminarComillas(line.substring(125,165)));
			//c.addAttribute("NOMBRE DE LA ENTIDAD MENOR EN CASO DE EXISTIR",line.substring(165,195));
			//c.addAttribute("CODIGO DE VIA PUBLICA",line.substring(195,200));
			//c.addAttribute("catastro:ref:way",eliminarCerosString(line.substring(195,200)));
			//c.addAttribute("TIPO DE VIA O SIGLA PUBLICA",line.substring(200,205));
			//c.addAttribute("NOMBRE DE VIA PUBLICA",line.substring(205,230));
			c.addAttribute("addr:street",nombreTipoViaParser(line.substring(200,205).trim())+" "+formatearNombreCalle(eliminarComillas(line.substring(205,230).trim())));
			//c.addAttribute("PRIMER NUMERO DE POLICIA",line.substring(230,234));
			//c.addAttribute("PRIMERA LETRA (CARACTER DE DUPLICADO)",line.substring(234,235));
			c.addAttribute("addr:housenumber",utils.eliminarCerosString(line.substring(230,234))+line.substring(234,235).trim());
			//c.addAttribute("SEGUNDO NUMERO DE POLICIA",line.substring(235,239));
			//c.addAttribute("SEGUNDA LETRA (CARACTER DE DUPLICADO)",line.substring(239,240));
			//c.addAttribute("KILOMETRO (3enteros y 2decimales)",line.substring(240,245));
			//c.addAttribute("BLOQUE",line.substring(245,249));
			//c.addAttribute("ESCALERA",line.substring(249,251));
			//c.addAttribute("PLANTA",line.substring(251,254));
			//c.addAttribute("PUERTA",line.substring(254,257));
			//c.addAttribute("TEXTO DE DIRECCION NO ESTRUCTURADA",line.substring(257,282));
			c.addAttribute("name",eliminarComillas(line.substring(257,282).trim()));
			//c.addAttribute("CODIGO POSTAL",line.substring(282,287));
			if (!line.substring(282,287).equals("00000"))
				c.addAttribute("addr:postcode",line.substring(282,287));
//			if (!line.substring(282,287).isEmpty() && !line.substring(282,287).equals("00000"))
//				c.addAttribute("addr:country" ,"ES");
			//c.addAttribute("DISTRITO MUNICIPAL",line.substring(287,289));
			//c.addAttribute("CODIGO DEL MUNICIPIO DE ORIGEN EN CASO DE AGREGACION",line.substring(289,292));
			//c.addAttribute("CODIGO DE LA ZONA DE CONCENTRACION",line.substring(292,294));
			//c.addAttribute("CODIGO DE POLIGONO",line.substring(294,297));
			//c.addAttribute("CODIGO DE PARCELA",line.substring(297,302));
			//c.addAttribute("CODIGO DE PARAJE",line.substring(302,307));
			//c.addAttribute("NOMBRE DEL PARAJE",line.substring(307,337));
			//c.addAttribute("NUMERO DE ORDEN DEL INMUEBLE EN LA ESCRITURA DE DIVISION HORIZONTAL",line.substring(367,371));
			//c.addAttribute("ANO DE ANTIGUEDAD DEL BIEN INMUEBLE",line.substring(371,375)); 
			//c.addAttribute("CLAVE DE GRUPO DE LOS BIENES INMUEBLES DE CARAC ESPECIALES",line.substring(427,428));
			c.setUsoDestino(line.substring(427,428).trim());
			//c.addAttribute("SUPERFICIE DEL ELEMENTO O ELEMENTOS CONSTRUCTIVOS ASOCIADOS AL INMUEBLE",line.substring(441,451));
			if (utils.esNumero(line.substring(441,451).trim()))
				c.setArea(Double.parseDouble(line.substring(441,451).trim()));
			else
				c.setArea((double) 10);
			//c.addAttribute("SUPERFICIE ASOCIADA AL INMUEBLE",line.substring(451,461));
			//c.addAttribute("COEFICIENTE DE PROPIEDAD (3ent y 6deci)",line.substring(461,470));


			return c;}

		case 16: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("NUMERO DE ORDEN DEL ELEMENTO CUYO VALOR SE REPARTE",line.substring(44,48));
			if (utils.esNumero(line.substring(44,48)) && Integer.parseInt(line.substring(44,48)) != 0)
				c.setNumOrdenConstru(Integer.parseInt(line.substring(44,48)));
			else
				c.setSubparce(line.substring(44,48));
			//c.addAttribute("CALIFICACION CATASTRAL DE LA SUBPARCELA",line.substring(48,50));
			//c.addAttribute("BLOQUE REPETITIVO HASTA 15 VECES",line.substring(50,999));


			return c;}

		case 17: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("NATURALEZA DEL SUELO OCUPADO POR EL CULTIVO (UR, RU)",line.substring(28,30));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("CODIGO DE LA SUBPARCELA",line.substring(44,48));
			c.setSubparce(line.substring(44,48));
			//c.addAttribute("NUMERO DE ORDEN DEL BIEN INMUEBLE FISCAL",line.substring(50,54));
			//c.addAttribute("TIPO DE SUBPARCELA (T, A, D)",line.substring(54,55));
			//c.addAttribute("SUPERFICIE DE LA SUBPARCELA (m cuadrad)",line.substring(55,65));
			//c.addAttribute("catastro:surface",eliminarCerosString(line.substring(55,65)));
			//if (esNumero(line.substring(55,65).trim()))
			//c.setArea(Double.parseDouble(line.substring(55,65).trim()));
			//else
			//c.setArea((double) 10);
			//c.addAttribute("CALIFICACION CATASTRAL/CLASE DE CULTIVO",line.substring(65,67));
			//c.addAttribute("DENOMINACION DE LA CLASE DE CULTIVO",line.substring(67,107));
			//c.addAttribute("INTENSIDAD PRODUCTIVA",line.substring(107,109));
			//c.addAttribute("CODIGO DE MODALIDAD DE REPARTO",line.substring(126,129));


			return c;}
		case 90: {

			/*System.out.println("\nTIPO DE REGISTRO 90: REGISTRO DE COLA\n"); 
			System.out.println("Numero total de registros tipo 11           :"+line.substring(9,16));
			System.out.println("Numero total de registros tipo 13           :"+line.substring(23,30));
			System.out.println("Numero total de registros tipo 14           :"+line.substring(30,37));
			System.out.println("Numero total de registros tipo 15           :"+line.substring(37,44));
			System.out.println("Numero total de registros tipo 16           :"+line.substring(44,51));
			System.out.println("Numero total de registros tipo 17           :"+line.substring(51,58));
			 */
			break;}
		}
		return c;
	}
	

	/** Eliminar las comillas '"' de los textos, sino al leerlo JOSM devuelve error
	 * pensando que ha terminado un valor antes de tiempo.
	 * @param s String al que quitar las comillas
	 * @return String sin las comillas
	 */
	private static String eliminarComillas(String s){
		String ret = new String();
		for (int x = 0; x < s.length(); x++)
			if (s.charAt(x) != '"') ret += s.charAt(x);
		return ret;
	}
	
	private static String nombreTipoViaParser(String codigo){

		switch(codigo){
		case "CL":return "Calle";
		case "AL":return "Aldea/Alameda";
		case "AR":return "Area/Arrabal";
		case "AU":return "Autopista";
		case "AV":return "Avenida";
		case "AY":return "Arroyo";
		case "BJ":return "Bajada";
		case "BO":return "Barrio";
		case "BR":return "Barranco";
		case "CA":return "Cañada";
		case "CG":return "Colegio/Cigarral";
		case "CH":return "Chalet";
		case "CI":return "Cinturon";
		case "CJ":return "Calleja/Callejón";
		case "CM":return "Camino";
		case "CN":return "Colonia";
		case "CO":return "Concejo/Colegio";
		case "CP":return "Campa/Campo";
		case "CR":return "Carretera/Carrera";
		case "CS":return "Caserío";
		case "CT":return "Cuesta/Costanilla";
		case "CU":return "Conjunto";
		case "DE":return "Detrás";
		case "DP":return "Diputación";
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
		case "GV":return "Gran Vía";
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
		case "PL":return "Polígono";
		case "PM":return "Paramo";
		case "PQ":return "Parroquia/Parque";
		case "PR":return "Prolongación/Continuación";
		case "PS":return "Paseo";
		case "PT":return "Puente";
		case "PZ":return "Plaza";
		case "QT":return "Quinta";
		case "RB":return "Rambla";
		case "RC":return "Rincón/Rincona";
		case "RD":return "Ronda";
		case "RM":return "Ramal";
		case "RP":return "Rampa";
		case "RR":return "Riera";
		case "RU":return "Rua";
		case "SA":return "Salida";
		case "SD":return "Senda";
		case "SL":return "Solar";
		case "SN":return "Salón";
		case "SU":return "Subida";
		case "TN":return "Terrenos";
		case "TO":return "Torrente";
		case "TR":return "Travesía";
		case "UR":return "Urbanización";
		case "VR":return "Vereda";
		case "CY":return "Caleya";
		}
		return codigo;
	}
	
	
	/** Pasa todo el nombre de la calle a minusculas y luego va poniendo en mayusculas las primeras
	 * letras de todas las palabras a menos que sean DE|DEL|EL|LA|LOS|LAS
	 * @param s El nombre de la calle
	 * @return String con el nombre de la calle pasando los articulos a minusculas.
	 */
	public static String formatearNombreCalle(String c){

		String[] l = c.toLowerCase().split(" ");
		String ret = "";

		for (String s : l){
			if (!s.isEmpty() && !s.equals("de") && !s.equals("del") && !s.equals("la") && !s.equals("las") && !s.equals("el") && !s.equals("los")){
				char mayus = Character.toUpperCase(s.charAt(0));
				ret += mayus + s.substring(1, s.length())+" ";
			}
			else
				ret += s+" ";
		}
		return ret.trim();
	}
}
