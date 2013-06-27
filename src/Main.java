import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;


public class Main {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {

		for(int x = 0; x < args.length; x++){

			if (args[x].toLowerCase().equals("-v")){
				System.out.println("Cat2Osm2 versión "+Cat2Osm.VERSION+".");
				System.exit(0);
			}

			if (new File(args[x]).isDirectory()){
				Config.loadConfig(new File(args[x]));
			}

			if (args[x].toLowerCase().equals("-rslt")){
				Config.set("ResultFileName", args[x+1]);
			}
			if (args[x].toLowerCase().equals("-3d")){
				Config.set("Catastro3d", args[x+1]);
			}
			if (args[x].toLowerCase().equals("-dbg")){
				Config.set("PrintShapeIds", args[x+1]);
			}
			if (args[x].toLowerCase().equals("-reg")){
				Config.set("TipoRegistro", args[x+1]);
			}

			if (args[x].toLowerCase().equals("-constru")){
				Config.set("ExportType", "CONSTRU");
			}
			else if (args[x].toLowerCase().equals("-ejes")){
				Config.set("ExportType", "EJES");
			}
			else if (args[x].toLowerCase().equals("-elemlin")){
				Config.set("ExportType", "ELEMLIN");
			}
			else if (args[x].toLowerCase().equals("-elempun")){
				Config.set("ExportType", "ELEMPUN");
			}
			else if (args[x].toLowerCase().equals("-elemtex")){
				Config.set("ExportType", "ELEMTEX");
			}
			else if (args[x].toLowerCase().equals("-masa")){
				Config.set("ExportType", "MASA");
			}
			else if (args[x].toLowerCase().equals("-parcela")){
				Config.set("ExportType", "PARCELA");
			}
			else if (args[x].toLowerCase().equals("-subparce")){
				Config.set("ExportType", "SUBPARCE");
			}
			else if (args[x].toLowerCase().equals("-usos")){
				Config.set("ExportType", "USOS");
			}
			else {
				Config.set("ExportType", "COMPLETA");
			}
		}

		if (!Config.get("ResultPath").isEmpty()){

			switch (Config.get("ExportType")) {

			case "CONSTRU":
			case "EJES":
			case "ELEMLIN":
			case "ELEMPUN":
			case "ELEMTEX":
			case "MASA":
			case "PARCELA":
			case "SUBPARCE":
				System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm en el directorio indicado " + Config.get("ResultPath") + " para exportar únicamente "+ Config.get("ExportType") + ".");
				ejecutarCat2Osm(Config.get("ExportType").toUpperCase());
				break;

			case "USOS":
				System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm en el directorio indicado " + Config.get("ResultPath")  + " para exportar únicamente el archivo de destinos a corregir.");
				crearUsos();
				break;

			case "COMPLETA":
				System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm en el directorio indicado " + Config.get("ResultPath")  + ".");
				ejecutarCat2Osm("*");
				break;
			
			default:
				System.out.println("No se ha definido un tipo de exportación");
				System.exit(-1);
			}

		} else {

			System.out.println("Cat2Osm versión "+Cat2Osm.VERSION+".\n");
			System.out.println("Forma de uso:");
			System.out.println("");
			System.out.println("Es necesrio indicar el directorio donde se encuentren los 4 archivos de catastro TAL CUAL se descargan de la web y para una única población.");
			System.out.println("");
			System.out.println("   java -jar [-XmxMemoria] cat2osm2.jar [Opciones] / [Directorio]\n");
			System.out.println("");
			System.out.println("Ejemplo:");
			System.out.println("");
			System.out.println("   java -jar -Xmx10240M cat2osm2.jar /home/yo/carpetaArchivos -rslt MiPueblo -3d 1 -reg 0 -constru -dbg 1 \n");
			System.out.println("");
			System.out.println("Parámetros opcionales");
			System.out.println("");
			System.out.println("-v            Muestra la version de Cat2Osm2");
			System.out.println("-rslt         Nombre del resultado (si no se indica, será 'Resultado')");
			System.out.println("-3d           Exportar las alturas de los edificios (1=Pisos sobre tierra, 0=No, -1=Pisos sobre y bajo tierra), por defecto es 0");
			System.out.println("-reg          Utilizar un único tipo de registro de catastro (11,14,15 o 17), por defecto es 0=todos");
			System.out.println("-dbg          Añadir a las geometrías el ID que tienen internamente en Cat2Osm2 para debuggin (1=Si, 0=No), por defecto es 0");
			System.out.println("-constru      Generar un archivo SOLO con las geometrías CONSTRU");
			System.out.println("-ejes         Generar un archivo SOLO con las geometrías EJES");
			System.out.println("-elemlin      Generar un archivo SOLO con las geometrías ELEMLIN");
			System.out.println("-elempun      Generar un archivo SOLO con las geometrías ELEMPUN");
			System.out.println("-elemtex      Generar un archivo SOLO con las geometrías ELEMTEX y mostrando todos los textos de Parajes y Comarcas, Información urbana y rústica y Vegetación y Accidentes demográficos");
			System.out.println("-masa         Generar un archivo SOLO con las geometrías MASA");
			System.out.println("-parcela      Generar un archivo SOLO con las geometrías PARCELA");
			System.out.println("-subparce     Generar un archivo SOLO con las geometrías SUBPARCE");
			System.out.println("-usos         Generar un archivo SOLO con los usos de inmuebles que no se pueden asignar directamente a una construcción");
			System.out.println("");
			System.out.println("Para mas informacion acceder a:");
			System.out.println("");
			System.out.println("http://wiki.openstreetmap.org/wiki/Cat2Osm2");

		}
	}


	/** Ejecutar cat2osm, dependiendo si se indica para un archivo concreto o para todos cambian
	 * un poco las operaciones que hace
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void ejecutarCat2Osm(String archivo) throws IOException, InterruptedException{

		// Clases
		Cat2OsmUtils utils = new Cat2OsmUtils();
		Cat2Osm catastro = new Cat2Osm(utils);


		// Cuando queremos ver todo el archivo Elemtex, tendremos que mostrar no solo las entradas sino todo
		if (archivo.equals("ELEMTEX"))
			Cat2Osm.utils.setOnlyEntrances(false);

		// Cuando queremos ver todo el archivo Constru, tendremos que mostrar todas las geometrias ya que en la ejecucion
		// normal no se usan todas
		if (archivo.equals("CONSTRU"))
			Cat2Osm.utils.setOnlyConstru(true);

		// Nos aseguramos de que existe la carpeta result
		File dir = new File(Config.get("ResultPath"));
		if (!dir.exists()) 
		{
			System.out.println("["+new Timestamp(new Date().getTime())+"] Creando el directorio general de resultados ("+Config.get("ResultPath")+").");
			try                { dir.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}

		// Nos aseguramos de que existe la carpeta result/nombreresult
		File dir2 = new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName"));
		if (!dir2.exists()) 
		{
			System.out.println("["+new Timestamp(new Date().getTime())+"] Creando el directorio donde almacenar este resultado concreto ("+Config.get("ResultPath")+ "/" + Config.get("ResultFileName")+").");
			try                { dir2.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}


		// Archivo global de resultado
		// Borrar archivo con el mismo nombre si existe, porque sino concatenaria el nuevo
		new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + Config.get("ResultFileName") + ".osm").delete();
		new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + Config.get("ResultFileName") + ".osm.gz").delete();

		// Archivo al que se le concatenan todos los archivos de nodos, ways y relations
		String fstreamOsm = Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + Config.get("ResultFileName") + ".osm.gz";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outOsmGlobal = new BufferedWriter( new OutputStreamWriter (new GZIPOutputStream(new FileOutputStream(fstreamOsm)), "UTF-8"));

		// Cabecera del archivo
		outOsmGlobal.write("<?xml version='1.0' encoding='UTF-8'?>");outOsmGlobal.newLine();
		outOsmGlobal.write("<osm version=\"0.6\" upload=\"false\" generator=\"cat2osm-"+Cat2Osm.VERSION+"\">");outOsmGlobal.newLine();


		Pattern p = Pattern.compile("\\d{4}-\\d{1,2}");
		Matcher m = p.matcher(Config.get("UrbanoCATFile"));

		if (m.find()) {
			Cat2OsmUtils.setFechaArchivos(Long.parseLong(m.group().substring(0, 4)+"0101"));
		}
		else{
			System.out.println("["+new Timestamp(new Date().getTime())+"] El archivo Cat Urbano debe tener el formato de nombre que viene por defecto en Catastro (XX_XX_U_aaaa-mm-dd.CAT) para leer de él la fecha de creación.");
			System.exit(-1);
		}

		// Si va a leer ELEMTEX comprueba si existe fichero de reglas
		if (archivo.equals("ELEMTEX")) {
			if (!Config.get("ElemtexRules", false).equals("") && new File(Config.get("ElemtexRules", false)).exists()) {
				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo de reglas " + Config.get("ElemtexRules", false));
				new Rules(Config.get("ElemtexRules", false));
			}
		}


		// Listas
		// Lista de shapes, agrupados por codigo de masa a la que pertenecen
		// Si es un tipo de shapes que no tienen codigo de masa se meteran en una cuyo
		// codigo sea el nombre del archivo shapefile
		HashMap <String, List<Shape>> shapes = new HashMap <String, List<Shape>>();
		List<ShapeParser> parsers = new ArrayList<ShapeParser>();

		// Recorrer los directorios Urbanos, en este se no cogen las MASAS
		File dirU = new File (Config.get("UrbanoSHPPath"));

		// Si archivo es * cogemos todos los shapefiles necesarios para obtener el resultado
		// Si se indica un shapefile concreto cogemos solo ese
		if( dirU.exists() && dirU.isDirectory()){
			File[] filesU = dirU.listFiles();
			for(int i=0; i < filesU.length; i++)
				if ( (archivo.equals("*") && (
						filesU[i].getName().toUpperCase().equals("CONSTRU") ||
						filesU[i].getName().toUpperCase().equals("EJES") 	||
						filesU[i].getName().toUpperCase().equals("ELEMLIN") ||
						filesU[i].getName().toUpperCase().equals("ELEMPUN") ||
						filesU[i].getName().toUpperCase().equals("ELEMTEX") ||
						filesU[i].getName().toUpperCase().equals("PARCELA") ||
						filesU[i].getName().toUpperCase().equals("SUBPARCE")
						))
						|| filesU[i].getName().toUpperCase().equals(archivo)
						)
					try{

						System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesU[i].getName() +" Urbano.");
						parsers.add(new ShapeParser("UR", new File(filesU[i] + "/" + filesU[i].getName() + ".SHP"), utils, shapes));

					}
			catch(Exception e)
			{
				System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer alguno de los shapefiles urbanos. " + e.getMessage());}
		}

		else
			System.out.println("["+new Timestamp(new Date().getTime())+"]    El directorio de shapefiles urbanos "+Config.get("UrbanoSHPPath")+" no existe.");

		// Recorrer los directorios Rusticos, en este se cogen las MASAS
		File dirR = new File (Config.get("RusticoSHPPath"));

		if( dirR.exists() && dirR.isDirectory()){
			File[] filesR = dirR.listFiles();
			for(int i=0; i < filesR.length; i++)
				if ( (archivo.equals("*") && (
						filesR[i].getName().toUpperCase().equals("CONSTRU") ||
						filesR[i].getName().toUpperCase().equals("EJES") 	||
						filesR[i].getName().toUpperCase().equals("ELEMLIN") ||
						filesR[i].getName().toUpperCase().equals("ELEMPUN") ||
						filesR[i].getName().toUpperCase().equals("ELEMTEX") ||
						filesR[i].getName().toUpperCase().equals("MASA")    ||
						filesR[i].getName().toUpperCase().equals("PARCELA") ||
						filesR[i].getName().toUpperCase().equals("SUBPARCE") 
						))
						|| filesR[i].getName().toUpperCase().equals(archivo)
						)
					try{
						System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesR[i].getName() +" Rustico.");
						parsers.add(new ShapeParser("RU", new File(filesR[i] + "/" + filesR[i].getName() + ".SHP"), utils, shapes));
					}
			catch(Exception e)
			{
				System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer alguno de los archivos shapefiles rústicos. " + e.getMessage());}
		}
		else
			System.out.println("["+new Timestamp(new Date().getTime())+"]    El directorio de shapefiles rústicos "+Config.get("RusticoSHPPath")+" no existe.");

		for (ShapeParser sp : parsers)
			sp.join();

		// Leemos archivo .cat
		// No todos los shapefiles tienen referencia catastral por lo que algunos
		// no hay forma de relacionarlos con los registros de catastro.
		if (archivo.equals("*") || archivo.equals("CONSTRU") || archivo.equals("PARCELA") || archivo.equals("SUBPARCE")){
			try {
				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat urbano.");
				catastro.catParser("UR", new File(Config.get("UrbanoCATFile")), shapes);
			}catch(Exception e)
			{System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer archivo Cat urbano. " + e.getCause());}

			try {
				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat rústico.");
				catastro.catParser("RU", new File(Config.get("RusticoCATFile")), shapes);
			}catch(Exception e)
			{System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer archivo Cat rústico. " + e.getCause());}	
		}


		// Mover las entradas de las casas a sus respectivas parcelas
		if (archivo.equals("*")){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Moviendo puntos de entrada a sus parcelas mas cercanas.");
			HashMap <String, List<Shape>> shapesTemp = catastro.calcularEntradas(shapes);
			if (shapesTemp != null)
				shapes = shapesTemp;
		}

		int pos = 0;
		for (String key : shapes.keySet()){


			String folder = key.startsWith("ELEM")? "elementos" : ( key.startsWith("EJES")? "ejes" : "masas" );

			System.out.println("["+new Timestamp(new Date().getTime())+"] Exportando " + Config.get("ResultFileName") + "-" + key + " [" + ++pos +"/" + shapes.keySet().size() + "]");

			// Por si acaso si hubiera archivos de un fallo en ejecucion anterior
			if (new File(Config.get("ResultPath") + "/" + folder + "/" + Config.get("ResultFileName") + key +"tempRelations.osm").exists()
					&& new File(Config.get("ResultPath") + "/" + folder + "/" + Config.get("ResultFileName") + key +"tempWays.osm").exists()
					&& new File(Config.get("ResultPath") + "/" + folder + "/" + Config.get("ResultFileName") + key +"tempNodes.osm").exists()){

				System.out.println("["+new Timestamp(new Date().getTime())+"] Se han encontrado 3 archivos temporales de una posible ejecución interrumpida, se procederá a juntarlos en un archivo resultado.");
				catastro.juntarFilesTemporales(key, folder, Config.get("ResultFileName"), outOsmGlobal);
				System.out.println("["+new Timestamp(new Date().getTime())+"] ¡¡Terminada la exportación de " + Config.get("ResultFileName") + "!!");

			}
			else if (shapes.get(key) != null){

				try {

					// Montar la jerarquia de parcelas.
					if (archivo.equals("*") && !key.startsWith("EJES") && !key.startsWith("ELEM")){
						System.out.println("["+new Timestamp(new Date().getTime())+"]    Creando jerarquia de parcelas/subparcelas/construcciones.");
						catastro.createHyerarchy(key, shapes.get(key));
					}

					// Si son ELEMLIN o EJES, juntar todos los ways que compartan un node
					// aunque sean de distintos shapes
					// y
					// Simplificar nodos intermedios en lineas rectas
					if (key.startsWith("EJES") || key.startsWith("ELEMLIN") ){
						System.out.println("["+new Timestamp(new Date().getTime())+"]    Encadenando shapes lineales.");
						catastro.joinLinearElements(key, shapes.get(key));

						System.out.println("["+new Timestamp(new Date().getTime())+"]    Simplificando geometrías.");
						catastro.simplifyGeometries(shapes.get(key), 0.000002);
					}
					//				else {
					//					System.out.println("["+new Timestamp(new Date().getTime())+"]    Simplificando geometrías.");
					//					catastro.simplifyGeometries(shapes.get(key), 0.0000005);
					//				}

					// Operacion de simplificacion de relaciones sin tags relevantes
					if (archivo.equals("*")){
						System.out.println("["+new Timestamp(new Date().getTime())+"]    Simplificando Shapes sin tags relevantes.");
						catastro.simplificarShapesSinTags(key, shapes.get(key));
					}

					// Desmontar las geometrias en elementos de OSM
					catastro.convertShapes2OSM(shapes.get(key), 0.000002);

				} catch (Exception e) {
					System.out.print("["+new Timestamp(new Date().getTime())+"]    La exportación de " + Config.get("ResultFileName") + "-" + key + " [" + ++pos +"/" + shapes.keySet().size() + "] falló.\r");
					e.printStackTrace();
				} finally {

					// Escribir los datos en los archivos temporales
					System.out.print("["+new Timestamp(new Date().getTime())+"]    Escribiendo archivos temporales.\r");
					catastro.printResults(key, folder, shapes.get(key));

					System.out.print("["+new Timestamp(new Date().getTime())+"]    Escribiendo el archivo resultado.\r");
					catastro.juntarFilesTemporales(key, folder, Config.get("ResultFileName") + "-" + key, outOsmGlobal);
					System.out.println("["+new Timestamp(new Date().getTime())+"]    Terminado " + Config.get("ResultFileName") + "-" + key + " [" + pos +"/" + shapes.keySet().size() + "]\r");
				}
			}
		}

		// Terminamos el archivo global de resultado
		outOsmGlobal.write("</osm>");outOsmGlobal.newLine();
		outOsmGlobal.close();

		System.out.println("["+new Timestamp(new Date().getTime())+"] ¡¡Terminada la exportación!!");

	}


	/** Metodo para utilizar solamente los archivos de parcelas para crear sobre ellas nodos con todos sus usos y destinos
	 * leidos de los registros del .CAT 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void crearUsos() throws IOException, InterruptedException{

		// Clases
		Cat2OsmUtils utils = new Cat2OsmUtils();
		Cat2Osm catastro = new Cat2Osm(utils);

		Cat2Osm.utils.setOnlyUsos(true);

		// Nos aseguramos de que existe la carpeta result
		File dir = new File(Config.get("ResultPath"));
		if (!dir.exists()) 
		{
			System.out.println("["+new Timestamp(new Date().getTime())+"] Creando el directorio general de resultados ("+Config.get("ResultPath")+").");
			try                { dir.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}

		// Nos aseguramos de que existe la carpeta result/nombreresult
		File dir2 = new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName"));
		if (!dir2.exists()) 
		{
			System.out.println("["+new Timestamp(new Date().getTime())+"] Creando el directorio donde almacenar este resultado concreto ("+Config.get("ResultPath")+ "/" + Config.get("ResultFileName")+").");
			try                { dir2.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}


		// Archivo global de resultado
		// Borrar archivo con el mismo nombre si existe, porque sino concatenaria el nuevo
		new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + Config.get("ResultFileName") + ".osm").delete();
		new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + Config.get("ResultFileName") + ".osm.gz").delete();

		// Archivo al que se le concatenan todos los archivos de nodos, ways y relations
		String fstreamOsm = Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + Config.get("ResultFileName") + ".osm.gz";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outOsmGlobal = new BufferedWriter( new OutputStreamWriter (new GZIPOutputStream(new FileOutputStream(fstreamOsm)), "UTF-8"));

		// Cabecera del archivo
		outOsmGlobal.write("<?xml version='1.0' encoding='UTF-8'?>");outOsmGlobal.newLine();
		outOsmGlobal.write("<osm version=\"0.6\" upload=\"false\" generator=\"cat2osm-"+Cat2Osm.VERSION+"\">");outOsmGlobal.newLine();


		Pattern p = Pattern.compile("\\d{4}-\\d{1,2}");
		Matcher m = p.matcher(Config.get("UrbanoCATFile"));

		if (m.find()) {
			Cat2OsmUtils.setFechaArchivos(Long.parseLong(m.group().substring(0, 4)+"0101"));
		}
		else{
			System.out.println("["+new Timestamp(new Date().getTime())+"] El archivo Cat Urbano debe tener el formato de nombre que viene por defecto en Catastro (XX_XX_U_aaaa-mm-dd.CAT) para leer de él la fecha de creación.");
			System.exit(-1);
		}


		// Listas
		// Lista de shapes, agrupados por codigo de masa a la que pertenecen
		// Si es un tipo de shapes que no tienen codigo de masa se meteran en una cuyo
		// codigo sea el nombre del archivo shapefile
		HashMap <String, List<Shape>> shapes = new HashMap <String, List<Shape>>();
		List<ShapeParser> parsers = new ArrayList<ShapeParser>();

		// Recorrer los directorios Urbanos, en este se no cogen las MASAS
		File dirU = new File (Config.get("UrbanoSHPPath"));

		// Si archivo es * cogemos todos los shapefiles necesarios para obtener el resultado
		// Si se indica un shapefile concreto cogemos solo ese
		if( dirU.exists() && dirU.isDirectory()){
			File[] filesU = dirU.listFiles();
			for(int i=0; i < filesU.length; i++)
				if ( filesU[i].getName().toUpperCase().equals("PARCELA"))
					try{
						System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesU[i].getName() +" Urbano.");
						parsers.add(new ShapeParser("UR", new File(filesU[i] + "/" + filesU[i].getName() + ".SHP"), utils, shapes));
					}
			catch(Exception e)
			{
				System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer alguno de los shapefiles urbanos. " + e.getMessage());}
		}

		else
			System.out.println("["+new Timestamp(new Date().getTime())+"]    El directorio de shapefiles urbanos "+Config.get("UrbanoSHPPath")+" no existe.");

		// Recorrer los directorios Rusticos, en este se cogen las MASAS
		File dirR = new File (Config.get("RusticoSHPPath"));

		if( dirR.exists() && dirR.isDirectory()){
			File[] filesR = dirR.listFiles();
			for(int i=0; i < filesR.length; i++)
				if ( filesR[i].getName().toUpperCase().equals("PARCELA"))
					try{
						System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesR[i].getName() +" Rustico.");
						parsers.add(new ShapeParser("RU", new File(filesR[i] + "/" + filesR[i].getName() + ".SHP"), utils, shapes));
					}
			catch(Exception e)
			{
				System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer alguno de los archivos shapefiles rústicos. " + e.getMessage());}
		}
		else
			System.out.println("["+new Timestamp(new Date().getTime())+"]    El directorio de shapefiles rústicos "+Config.get("RusticoSHPPath")+" no existe.");

		for (ShapeParser sp : parsers)
			sp.join();

		// Leemos archivo .cat
		// No todos los shapefiles tienen referencia catastral por lo que algunos
		// no hay forma de relacionarlos con los registros de catastro.
		try {
			System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat urbano.");
			catastro.catUsosParser("UR", new File(Config.get("UrbanoCATFile")), shapes);
		}catch(Exception e)
		{System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer archivo Cat urbano. " + e.getCause());}

		try {
			System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat rústico.");
			catastro.catUsosParser("RU", new File(Config.get("RusticoCATFile")), shapes);
		}catch(Exception e)
		{System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer archivo Cat rústico. " + e.getCause());}	


		int pos = 0;
		for (String key : shapes.keySet()){

			String folder = key.startsWith("ELEM")? "elementos" : ( key.startsWith("EJES")? "ejes" : "masas" );

			System.out.println("["+new Timestamp(new Date().getTime())+"] Exportando " + Config.get("ResultFileName") + "-" + key + " [" + pos++ +"/" + shapes.keySet().size() + "]");

			// Por si acaso si hubiera archivos de un fallo en ejecucion anterior
			if (new File(Config.get("ResultPath") + "/" + folder + "/" + Config.get("ResultFileName") + key +"tempRelations.osm").exists()
					&& new File(Config.get("ResultPath") + "/" + folder + "/" + Config.get("ResultFileName") + key +"tempWays.osm").exists()
					&& new File(Config.get("ResultPath") + "/" + folder + "/" + Config.get("ResultFileName") + key +"tempNodes.osm").exists()){

				System.out.println("["+new Timestamp(new Date().getTime())+"] Se han encontrado 3 archivos temporales de una posible ejecución interrumpida, se procederá a juntarlos en un archivo resultado.");
				catastro.juntarFilesTemporales(key, folder, Config.get("ResultFileName"), outOsmGlobal);
				System.out.println("["+new Timestamp(new Date().getTime())+"] ¡¡Terminada la exportación de " + Config.get("ResultFileName") + "!!");

			}
			else if (shapes.get(key) != null){

				// Desmontar las geometrias en elementos de OSM
				catastro.convertShapes2OSM(shapes.get(key), 0.000002);

				// Escribir los datos en los archivos temporales
				System.out.print("["+new Timestamp(new Date().getTime())+"]    Escribiendo archivos temporales.\r");
				catastro.printResults(key, folder, shapes.get(key));

				System.out.print("["+new Timestamp(new Date().getTime())+"]    Escribiendo el archivo resultado.\r");
				catastro.juntarFilesTemporales(key, folder, Config.get("ResultFileName") + "-" + key, outOsmGlobal);
				System.out.println("["+new Timestamp(new Date().getTime())+"]    Terminado " + Config.get("ResultFileName") + "-" + key + " [" + pos +"/" + shapes.keySet().size() + "]\r");

			}
		}

		// Terminamos el archivo global de resultado
		outOsmGlobal.write("</osm>");outOsmGlobal.newLine();
		outOsmGlobal.close();

		System.out.println("["+new Timestamp(new Date().getTime())+"] ¡¡Terminada la exportación de " + Config.get("ResultFileName") + "!!");



	}
}
