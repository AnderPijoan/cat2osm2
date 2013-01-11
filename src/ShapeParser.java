import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.geotools.data.FeatureReader;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class ShapeParser extends Thread{

	String tipo; // UR/RU
	File file;
	Cat2OsmUtils utils;
	HashMap <String, List<Shape>> shapeList;
	
	static int num = 0;

	public ShapeParser (String t, File f, Cat2OsmUtils u, HashMap<String, List<Shape>> s){
		super (f.getName());
		this.tipo = t;
		this.file = reproyectarWGS84(f, t);
		this.utils = u;
		shapeList = s;
		
		start();
	}

	public void run () {

		try {
			FileDataStore store = FileDataStoreFinder.getDataStore(file);
			//ShapefileDataStore store = new ShapefileDataStore(file.toURI().toURL());
			//ShapefileDataStore store = new ShapefileDataStore(file.toURI().toURL(),true,Charset.forName("ISO-8859-15"));
			FeatureReader<SimpleFeatureType, SimpleFeature> reader = 
					((FileDataStore) store).getFeatureReader();

			long fechaDesde = Long.parseLong(Config.get("FechaDesde"));
			long fechaHasta = Long.parseLong(Config.get("FechaHasta"));

			// Creamos el shape dependiendo de su tipo
			if (file.getName().toUpperCase().equals(tipo+"MASA.SHP"))

				// Shapes del archivo MASA.SHP
				while (reader.hasNext()) {
					Shape shape = new ShapeMasa(reader.next(), tipo);
					
					// Si cumple estar entre las fechas
					if (shape != null && shape.checkCatastroDate(fechaDesde, fechaHasta) && shape.isValid()){
						// Anadimos el shape creado a la lista
						if (shapeList.get(shape.getCodigoMasa()) == null)
							shapeList.put(shape.getCodigoMasa(), new ArrayList<Shape>());
						shapeList.get(shape.getCodigoMasa()).add(shape);
					}
				}
			else if (file.getName().toUpperCase().equals(tipo+"PARCELA.SHP")){

				// Shapes del archivo PARCELA.SHP
				while (reader.hasNext()) {
					
					Shape shape = new ShapeParcela(reader.next(), tipo);

					// Si cumple estar entre las fechas
					if (shape != null && shape.checkCatastroDate(fechaDesde, fechaHasta) && shape.isValid()){
						// Anadimos el shape creado a la lista
						if (shapeList.get(shape.getCodigoMasa()) == null)
							shapeList.put(shape.getCodigoMasa(), new ArrayList<Shape>());
						shapeList.get(shape.getCodigoMasa()).add(shape);
					}
				}
			}
			else if (file.getName().toUpperCase().equals(tipo+"SUBPARCE.SHP"))

				// Shapes del archivo SUBPARCE.SHP
				while (reader.hasNext()) {
					Shape shape = new ShapeSubparce(reader.next(), tipo);

					// Si cumple estar entre las fechas 
					if (shape != null && shape.checkCatastroDate(fechaDesde, fechaHasta) && shape.isValid()){
						// Anadimos el shape creado a la lista
						if (shapeList.get(shape.getCodigoMasa()) == null)
							shapeList.put(shape.getCodigoMasa(), new ArrayList<Shape>());
						shapeList.get(shape.getCodigoMasa()).add(shape);
					}
				}
			else if (file.getName().toUpperCase().equals(tipo+"CONSTRU.SHP"))

				// Shapes del archivo CONSTRU.SHP
				while (reader.hasNext()) {
					Shape shape = new ShapeConstru(reader.next(), tipo);
					
					// Si cumple estar entre las fechas
					if (shape != null && shape.checkCatastroDate(fechaDesde, fechaHasta) && shape.isValid()){
						// Anadimos el shape creado a la lista
						if (shapeList.get(shape.getCodigoMasa()) == null)
							shapeList.put(shape.getCodigoMasa(), new ArrayList<Shape>());
						shapeList.get(shape.getCodigoMasa()).add(shape);
					}
				}
			else if (file.getName().toUpperCase().equals(tipo+"ELEMTEX.SHP"))

				// Shapes del archivo ELEMTEX.SHP
				while (reader.hasNext()) {
					Shape shape = new ShapeElemtex(reader.next(), tipo);

					// Si cumple estar entre las fechas
					// Si cumple tener un ttggss valido (no interesa mostrar todos)
					if (shape != null && shape.checkCatastroDate(fechaDesde, fechaHasta) && shape.isValid()){
						// Anadimos el shape creado a la lista
						if (shapeList.get(shape.getCodigoMasa()) == null)
							shapeList.put(shape.getCodigoMasa(), new ArrayList<Shape>());
						shapeList.get(shape.getCodigoMasa()).add(shape);
					}
				}
			else if (file.getName().toUpperCase().equals(tipo+"ELEMPUN.SHP"))

				// Shapes del archivo ELEMPUN.SHP
				while (reader.hasNext()) {
					Shape shape = new ShapeElempun(reader.next(), tipo);

					// Si cumple estar entre las fechas
					// Si cumple tener un ttggss valido (no interesa mostrar todos)
					if (shape != null && shape.checkCatastroDate(fechaDesde, fechaHasta) && shape.isValid()){
						// Anadimos el shape creado a la lista
						if (shapeList.get(shape.getCodigoMasa()) == null)
							shapeList.put(shape.getCodigoMasa(), new ArrayList<Shape>());
						shapeList.get(shape.getCodigoMasa()).add(shape);
					}
				}
			else if (file.getName().toUpperCase().equals(tipo+"ELEMLIN.SHP"))

				// Shapes del archivo ELEMLIN.SHP
				while (reader.hasNext()) {
					Shape shape = new ShapeElemlin(reader.next(), tipo);

					// Si cumple estar entre las fechas
					// Si cumple tener un ttggss valido (no interesa mostrar todos)
					if (shape != null && shape.checkCatastroDate(fechaDesde, fechaHasta) && shape.isValid()){
						// Anadimos el shape creado a la lista
						if (shapeList.get(shape.getCodigoMasa()) == null)
							shapeList.put(shape.getCodigoMasa(), new ArrayList<Shape>());
						shapeList.get(shape.getCodigoMasa()).add(shape);
					}
				}
			else if (file.getName().toUpperCase().equals(tipo+"EJES.SHP"))

				// Shapes del archivo EJES.SHP
				while (reader.hasNext()) {
					Shape shape = new ShapeEjes(reader.next(), tipo);

					// Si cumple estar entre las fechas
					if (shape != null && shape.checkCatastroDate(fechaDesde, fechaHasta) && shape.isValid()){
						// Anadimos el shape creado a la lista
						if (shapeList.get(shape.getCodigoMasa()) == null)
							shapeList.put(shape.getCodigoMasa(), new ArrayList<Shape>());
						shapeList.get(shape.getCodigoMasa()).add(shape);
					}
				}

			reader.close();
			store.dispose();

			borrarShpFiles(file.getName().toUpperCase());

		} catch (IOException e) {e.printStackTrace();}
	}


	/** Utilizando ogr2ogr reproyecta el archivo de shapes de su proyeccion
	 * EPSG a WGS84 que es la que utiliza OpenStreetMap. Tambien convierte las 
	 * coordenadas UTM en Lat/Lon
	 * @param f Archivo a reproyectar
	 * @return File Archivo reproyectado
	 */
	public synchronized File reproyectarWGS84(File f, String tipo){

		try
		{			
			String os = System.getProperty("os.name").toLowerCase();
			BufferedReader bf;
			String line;
			int pro = Integer.parseInt(Config.get("Proyeccion"));

			// Windows
			if (os.indexOf("win") >= 0){
				// Archivo temporal para escribir el script
				FileWriter fstreamScript = new FileWriter(tipo + f.getName() + "script.bat");
				BufferedWriter outScript = new BufferedWriter(fstreamScript);

				outScript.write("@echo off \r\n"+
						"SET FWTOOLS_DIR="     +Config.get("FWToolsPath")+"\r\n" +
						"PATH="                +Config.get("FWToolsPath")+"\\bin;" + Config.get("FWToolsPath") + "\\python;%PATH%\r\n"+
						"SET PYTHONPATH="      +Config.get("FWToolsPath")+"\\pymod\r\n"+
						"SET PROJ_LIB="        +Config.get("FWToolsPath")+"\\proj_lib\r\n"+
						"SET GEOTIFF_CSV="     +Config.get("FWToolsPath")+"\\data\r\n"+
						"SET GDAL_DATA="       +Config.get("FWToolsPath")+"\\data\r\n"+
						"SET GDAL_DRIVER_PATH="+Config.get("FWToolsPath")+"\\gdal_plugins\r\n");

				if (pro == 32628)                       // Canarias
					outScript.write("ogr2ogr.exe -t_srs EPSG:4326 " +                          // proyeccion
							f.getPath().substring(0, f.getPath().length()-4) + " " +   // archivo origen
							Config.get("ResultPath") + "\\" +  Config.get("ResultFileName") + "\\" + tipo + f.getName());        // archivo fin
				else if (23029 <= pro && pro <= 23031)	// ED50				
					outScript.write("ogr2ogr.exe -s_srs \"+init=epsg:" + pro + " +nadgrids=.\\" + Config.get("NadgridsPath") + " +wktext\" -t_srs EPSG:4326 " + 
							f.getPath().substring(0, f.getPath().length()-4) + " " +
							Config.get("ResultPath") + "\\" +  Config.get("ResultFileName") + "\\" + tipo + f.getName());
				else if (25829 <= pro && pro <= 25831)  // ETRS89
					outScript.write("ogr2ogr.exe -s_srs \"+init=epsg:" + pro + " +wktext\" -t_srs EPSG:4326 " +
							f.getPath().substring(0, f.getPath().length()-4) + " " +
							Config.get("ResultPath") + "\\" +  Config.get("ResultFileName") + "\\" + tipo + f.getName());
				outScript.close();

				Runtime run = Runtime.getRuntime();
				Process pr  = run.exec(tipo + f.getName() + "script.bat");
				pr.waitFor();
				bf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
				while ((line = bf.readLine()) != null)
					System.out.println(line);
			}
			
			// Mac y Linux
			else {
				String proyeccion = "";
				if      (23029 <= pro && pro <= 23031)  // ED50 
					proyeccion = "-s_srs \"+init=epsg:" + pro + " +nadgrids=" + Config.get("NadgridsPath") + " +wktext\""; 
				else if (25829 <= pro && pro <= 25831)  // ETRS89
					proyeccion = "-s_srs \"+init=epsg:" + pro + " +wktext\"";

				String command = "ogr2ogr " + proyeccion + " -t_srs EPSG:4326 " + 
						"\"" + Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + tipo + f.getName() + "\" " +  // archivo fin
						"\"" + f.toPath() + "\"";                                                 // archivo origen

				FileWriter fstreamScript = new FileWriter(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/script" + tipo+f.getName() + ".sh");
				BufferedWriter outScript = new BufferedWriter(fstreamScript);

				outScript.write("#!/bin/bash\n");
				outScript.write(command);
				outScript.close();
				Process pr = Runtime.getRuntime().exec( new String[] { "chmod", "+x", Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/script" + tipo+f.getName() + ".sh" } );
				pr.waitFor();
				System.out.println("["+new Timestamp(new Date().getTime())+"] Ejecutando proyeccion de los shapefiles " +
						tipo+f.getName() +": " + command);

				Runtime run = Runtime.getRuntime();
				pr  = run.exec( new String[] { Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/script" + tipo+f.getName() + ".sh" } );
				pr.waitFor();
				bf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
				while ((line = bf.readLine()) != null)
					System.out.println(line);
			}

			//line = "scripts/ogr2ogr.bat " + f.getPath().substring(0, f.getPath().length()-4) +" "+ Config.get("ResultPath")+"/"+tipo+f.getName() +" "+ f.getPath();
		} catch (Exception er){ System.out.println("["+new Timestamp(new Date().getTime())+"] No se ha podido proyectar los shapefiles "+tipo+f.getName()+"."); er.printStackTrace(); }

		return new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + tipo+f.getName());
	}


	/** Borra los shapefiles temporales creados. Hay que borrar si se quiere
	 * reproyectar nuevos y como urbano y rustico tienen los mismos nombres
	 * de shapefiles, cada vez que usamos uno, lo borramos.
	 * @param filename
	 */
	public void borrarShpFiles(String filename){

		String path = Config.get("ResultPath") + "/" + Config.get("ResultFileName");

		System.out.println("["+new Timestamp(new Date().getTime())+"] Terminado de leer los archivos "+filename+".");

		boolean borrado = true;

		// Borrar archivo con el mismo nombre si existe, porque sino concatenaria el nuevo
		borrado = borrado && new File(path +"/"+ filename.substring(0, filename.length()-4) +".shp").delete();
		borrado = borrado && new File(path +"/"+ filename.substring(0, filename.length()-4) +".dbf").delete();
		borrado = borrado && new File(path +"/"+ filename.substring(0, filename.length()-4) +".prj").delete();
		borrado = borrado && new File(path +"/"+ filename.substring(0, filename.length()-4) +".shx").delete();
		borrado = borrado && new File(path +"/script"+ filename +".sh").delete();

		if (!borrado)
			System.out.println("["+new Timestamp(new Date().getTime())+"] No se pudo borrar alguno de los archivos temporales de "+filename+"." +
					" Estos estaran en la carpeta "+ path +".");

	}

}
