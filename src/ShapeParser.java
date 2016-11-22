import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Geometry;

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
			//ShapefileDataStore ds = new ShapefileDataStore(new URL(file.getAbsolutePath()));
			//FeatureSource fs = (FeatureSource)ds.getFeatureSource("xuhui_polyline");
			//FeatureCollection fr = fs.getFeatures();
			//FeatureIterator f = fr.features();
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
					Shape shape = new ShapeConstruPart(reader.next(), tipo);

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

		} catch (IOException e) { 
			//e.printStackTrace();
		}
	}


	/** Reproyecta el archivo de shapes de su proyeccion
	 * EPSG a WGS84 que es la que utiliza OpenStreetMap. Tambien convierte las 
	 * coordenadas UTM en Lat/Lon
	 * @param f Archivo a reproyectar
	 * @return File Archivo reproyectado
	 */
	public synchronized File reproyectarWGS84(File f, String tipo){

		try {
					
			FileDataStore store = FileDataStoreFinder.getDataStore( f );
			SimpleFeatureSource featureSource = store.getFeatureSource();
			SimpleFeatureType schema = featureSource.getSchema();
			CoordinateReferenceSystem dataCRS = schema.getCoordinateReferenceSystem();

			CoordinateReferenceSystem wgs84CRS = DefaultGeographicCRS.WGS84; 
			boolean lenient = true; // allow for some error due to different datums
			MathTransform transform = CRS.findMathTransform(dataCRS, wgs84CRS, lenient);

			SimpleFeatureCollection featureCollection = featureSource.getFeatures();

			DataStoreFactorySpi factory = new ShapefileDataStoreFactory();
			Map<String, Serializable> create = new HashMap<String, Serializable>();

			File out = new File(Config.get("ResultPath") + File.separatorChar +  Config.get("ResultFileName") + File.separator + tipo + f.getName());
			
			create.put("url", out.toURI().toURL());
			create.put("create spatial index", Boolean.TRUE);
			DataStore dataStore = factory.createNewDataStore(create);
			SimpleFeatureType featureType = SimpleFeatureTypeBuilder.retype(schema, wgs84CRS);
			dataStore.createSchema(featureType);

			Transaction transaction = null;
			FeatureWriter<SimpleFeatureType, SimpleFeature> writer = null;
			SimpleFeatureIterator iterator = null;

			try {
				
				transaction = new DefaultTransaction("Reproject");
				writer = dataStore.getFeatureWriterAppend( (tipo + featureType.getTypeName()), transaction);
				iterator = featureCollection.features();
			
				while (iterator.hasNext()) {
					// copy the contents of each feature and transform the geometry
					SimpleFeature feature = iterator.next();
					SimpleFeature copy = writer.next();
					copy.setAttributes(feature.getAttributes());

					Geometry geometry = (Geometry) feature.getDefaultGeometry();
					Geometry geometry2 = JTS.transform(geometry, transform);

					copy.setDefaultGeometry(geometry2);
					writer.write();
				}
				transaction.commit();
				
			} catch (Exception problem) {
				
				//problem.printStackTrace();
				transaction.rollback();
				System.out.println("["+new Timestamp(new Date().getTime())+"]\tNo se han podido reproyectar los shapefiles "+tipo+f.getName()+".");
				
			} finally {
				
				if( writer != null){ writer.close(); }
				if( iterator != null){ iterator.close(); }
				if( transaction != null){ transaction.close(); }
				System.out.println("["+new Timestamp(new Date().getTime())+"]\tCerrando transacción "+tipo+f.getName()+".");
				
			}

		} catch (Exception er){ 
			System.out.println("["+new Timestamp(new Date().getTime())+"]\tNo se ha podido proyectar los shapefiles "+tipo+f.getName()+". Algunos archivos de catastro pueden estar vacios dependiendo del municipio."); 
			//er.printStackTrace(); 
		}

		return new File(Config.get("ResultPath") + File.separatorChar + Config.get("ResultFileName") + File.separatorChar + tipo+f.getName());
	}


	/** Borra los shapefiles temporales creados. Hay que borrar si se quiere
	 * reproyectar nuevos y como urbano y rustico tienen los mismos nombres
	 * de shapefiles, cada vez que usamos uno, lo borramos.
	 * @param filename
	 */
	public void borrarShpFiles(String filename){

		String path = Config.get("ResultPath") + File.separatorChar + Config.get("ResultFileName");

		System.out.println("["+new Timestamp(new Date().getTime())+"]\tTerminado de leer los archivos "+filename+".");

		boolean borrado = true;

		// Borrar archivo con el mismo nombre si existe, porque sino concatenaria el nuevo
		borrado &= new File(path + File.separatorChar + filename.substring(0, filename.length()-4) +".SHP").delete();
		borrado &= new File(path + File.separatorChar + filename.substring(0, filename.length()-4) +".SHX").delete();
		borrado &= new File(path + File.separatorChar + filename.substring(0, filename.length()-4) +".PRJ").delete();
		borrado &= new File(path + File.separatorChar + filename.substring(0, filename.length()-4) +".DBF").delete();
		borrado &= new File(path + File.separatorChar + filename.substring(0, filename.length()-4) +".FIX").delete();
		borrado &= new File(path + File.separatorChar + filename.substring(0, filename.length()-4) +".QIX").delete();

		if (!borrado)
			System.out.println("["+new Timestamp(new Date().getTime())+"]\tNo se pudo borrar alguno de los archivos temporales de "+filename+"." +
					" Estos estaran en la carpeta "+ path +".");
	}

}
