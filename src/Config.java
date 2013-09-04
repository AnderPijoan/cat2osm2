import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;


public class Config {

	/** Properties con la configuracion. */
	private static Properties configuration = new Properties();


	/**
	 * Carga la configuración.
	 * @param file directorio donde encontrar los archivos
	 */
	public static void loadConfig(File dir) {
		searchAndBuildFilesPath(dir);
		normalizeVariableContent("UrbanoSHPPath");
		normalizeVariableContent("RusticoSHPPath");
		try {
			CatExtractor.extract(Config.get("UrbanoSHPPath"));
			CatExtractor.extract(Config.get("RusticoSHPPath"));
		}
		catch (IOException ioe){
			ioe.printStackTrace();
		}

		String directorio = Config.get("UrbanoSHPPath");
		if (StringUtils.isBlank(directorio)){
			directorio = Config.get("RusticoSHPPath");
		}

		Config.set("ResultPath", dir.getPath());

		if (configuration.get("ResultFileName") == null)
			Config.set("ResultFileName", "Resultado");

		if (configuration.get("FechaDesde") == null)
			Config.set("FechaDesde", "00000101");

		if (configuration.get("FechaHasta") == null)
			Config.set("FechaHasta", "99999999");

		if (configuration.get("FechaConstruDesde") == null)
			Config.set("FechaConstruDesde", "00000101");

		if (configuration.get("FechaConstruHasta") == null)
			Config.set("FechaConstruHasta", "99999999");

		if (configuration.get("TipoRegistro") == null)
			Config.set("TipoRegistro", "0");

		if (configuration.get("Catastro3d") == null)
			Config.set("Catastro3d", "1");
		
		/* Esto une las parcelas rusticas, no hay forma de tocarlo si no es desde codigo */
		if (configuration.get("SplitRU") == null)
			Config.set("SplitRU", "0");

		if (configuration.get("PrintShapeIds") == null)
			Config.set("PrintShapeIds", "0");
	}


	/**
	 * Si se indica un directorio como parametro, busca en el
	 * los archivos cat.gz y SHF.zip necesarios, y completa la configuración.
	 * Ojo, el directorio solo debe contener los archivos de un municipio/localidad
	 * 
	 */
	private static void searchAndBuildFilesPath(File dir){

		if (!dir.isDirectory()){
			System.out.println("["+new Timestamp(new Date().getTime())+"] El directorio indicado donde buscar los archivos no existe.");
			System.exit(-1);
		}

		// Ver si se encuentran los 4 archivos de catastro
		File[] entradas = dir.listFiles();

		for (File fichero : entradas){
			if (fichero.isFile()){
				String nombre = fichero.getName().toUpperCase();
				if (nombre.matches("^\\d+_\\d+_RA_\\d+-\\d+-\\d+_SHF\\.ZIP$")){
					configuration.setProperty("RusticoSHPPath", fichero.getAbsolutePath());
				}
				else if (nombre.matches("^\\d+_\\d+_UA_\\d+-\\d+-\\d+_SHF\\.ZIP$")){
					configuration.setProperty("UrbanoSHPPath", fichero.getAbsolutePath());
				}
				else if (nombre.matches("^\\d+_\\d+_U_\\d+-\\d+-\\d+\\.CAT\\.GZ$")){
					configuration.setProperty("UrbanoCATFile", fichero.getAbsolutePath());
				}
				else if (nombre.matches("^\\d+_\\d+_R_\\d+\\-\\d+\\-\\d+\\.CAT\\.GZ$")){
					configuration.setProperty("RusticoCATFile", fichero.getAbsolutePath());
				}
			}
		}

		if (configuration.getProperty("UrbanoSHPPath") == null){
			System.out.println("["+new Timestamp(new Date().getTime())+"] No se encontró el archivo de geometrías urbanas (XX_XXX_UA_AAAA-MM-DD_SHF.zip).");
			System.exit(-1);
		}
		if (configuration.getProperty("RusticoSHPPath") == null){
			System.out.println("["+new Timestamp(new Date().getTime())+"] No se encontró el archivo de geometrías rústicas (XX_XXX_RA_AAAA-MM-DD_SHF.zip).");
			System.exit(-1);
		}
		if (configuration.getProperty("UrbanoCATFile") == null){
			System.out.println("["+new Timestamp(new Date().getTime())+"] No se encontró el archivo de registros urbanos (XX_XXX_U_XXXX-XX-XX.CAT.gz).");
			System.exit(-1);
		}
		if (configuration.getProperty("RusticoCATFile") == null){
			System.out.println("["+new Timestamp(new Date().getTime())+"] No se encontró el archivo de registros rústicos (XX_XXX_R_XXXX-XX-XX.CAT.gz ).");
			System.exit(-1);
		}

	}


	/**
	 * Elimina la extensión zip del nombre de archivo de shp de catastro.
	 * Para usar en el resto del programa el directorio en lugar del zip
	 * @param varName
	 */
	private static void normalizeVariableContent(String varName) {
		String value = Config.get(varName);
		if (value != null && value.toLowerCase().endsWith(".zip")){
			Config.set(varName, value.replaceFirst("\\.[Zz][Ii][Pp]$", ""));
		}
	}


	/** Obtiene la opcion de configuracion. Si no existe devuelve "".
	 *  @param option Opcion de configuracion a buscar.
	 *  @param required Indica si la opción es obligatoria (true) u opcional (false).
	 *  @return Valor que tiene en el hashMap o "". */
	public static String get(String option, boolean required){

		if (configuration.get(option) == null && required){
			System.out.println("["+new Timestamp(new Date().getTime())+"] " +
					"El programa no ha conseguido inicializar la variable "+option+". ");
		}
		return configuration.getProperty(option, "");
	}

	public static String get(String option) {
		return get(option, true);
	}


	/** Modifica la opcion del hashMap de configuracion.
	 * @param option Opcion a modificar.
	 * @param value  Nuevo valor a poner. */
	public static void set(String option, String value){ 
		configuration.put(option,value);
	}


	/** Anade la lista al hashMap de configuracion.
	 * @param l Lista de pares de strings*/
	public static void set(List<String[]> l){

		for (String[] s : l)
			configuration.put(s[0], s[1]);
	}

}
