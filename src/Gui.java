import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;


public class Gui extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** Interfaz visual en caso de que no se encuentre el archivo de configuracion
	 */
	public Gui (){
		super("Cat2Osm, ayuda para crear el archivo de configuración.");
		this.setSize(1000, 800);
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		this.add(new JLabel("GUÍA DE USO EN: http://wiki.openstreetmap.org/wiki/Cat2Osm2"), BorderLayout.NORTH);

		JPanel options = new JPanel();
		options.setLayout(new BorderLayout());


		JPanel descriptions = new JPanel();
		descriptions.setLayout(new GridLayout(13,1));

		String[] descriptionsTexts = {"<html><b>NECESARIO</b><br/>Carpeta donde se exportarán los archivos temporales y el resultado.\n(Tiene que tener privilegios lectura/escritura).</html>",
				"<html><b>NECESARIO</b><br/>Nombre del archivo que exportará Cat2Osm como resultado</html>",
				"<html><b>NECESARIO</b><br/>Ruta a una carpeta que contenga los ARCHIVOS DE CATASTRO DE UNA POBLACIÓN. Sin descomprimir y con el nombre tal cual se descargan de catastro, es decir: XX_XXX_UA_XXXX-XX-XX_SHF.zip, XX_XXX_RA_XXXX-XX-XX_SHF.zip, XX_XXX_U_XXXX-XX-XX.CAT.gz y XX_XXX_R_XXXX-XX-XX.CAT.gz (Si falta alguno se procesará sin él)</html>", 
				"<html><b>NECESARIO</b><br/>Zona en la que se encuentra la población (peninsula para Península + Islas Canarias o baleares para las Islas Baleares)</html>",
				"<html><b>DEJAR ASÍ PARA FUNCIONAMIENTO POR DEFECTO</b><br/>Proyección en la que se encuentran los archivos shapefile.<ul>" +
						"<li><b>\"auto\"</b> para que busque automáticamente"+
						"<li><b>32628</b> para WGS84/ Zona UTM 29N"+
						"<li><b>23029</b> para ED_1950/ Zona UTM 29N</li>"+
						"<li><b>23030</b> para ED_1950/ Zona UTM 30N</li>"+
						"<li><b>23031</b> para ED_1950/ Zona UTM 31N</li>"+
						"<li><b>25829</b> para ETRS_1989/ Zona UTM 29N</li>"+
						"<li><b>25830</b> para ETRS_1989/ Zona UTM 30N</li>"+
						"<li><b>25831</b> para ETRS_1989/ Zona UTM 31N</li>"+
						"</ul>(Se puede comprobar abriendo con un editor de texto cualquiera de los archivos .PRJ)</html>",
						"<html><b>DEJAR ASÍ PARA FUNCIONAMIENTO POR DEFECTO</b><br/>Si se quiere delimitar una fecha de alta de los datos (Formato AAAAMMDD), sino 00000000.\nTomará los datos que se han dado de alta a partir de esta fecha.\nEjemeplo: 20060127 (27 de Enero del 2006)</html>",
						"<html><b>DEJAR ASÍ PARA FUNCIONAMIENTO POR DEFECTO</b><br/>Si se quiere delimitar una fecha de baja de los datos (Formato AAAAMMDD), sino 99999999.\nTomará los shapes que se han dado de alta hasta esta fecha y siguen sin darse de baja después.\nEjemeplo: 20060127 (27 de Enero del 2006)</html>", 
						"<html><b>DEJAR ASÍ PARA FUNCIONAMIENTO POR DEFECTO</b><br/>Si se quiere delimitar una fecha de construcción desde la cual coger los datos (Formato AAAAMMDD), sino 00000000.\nÚnicamente imprimirá las relations que cumplan estar entre las fechas de construcción.\nEjemeplo: 20060127 (27 de Enero del 2006)</html>",
						"<html><b>DEJAR ASÍ PARA FUNCIONAMIENTO POR DEFECTO</b><br/>Si se quiere delimitar una fecha de construcción hasta la cual coger los datos (Formato AAAAMMDD), sino 99999999.\nÚnicamente imprimirá las relations que cumplan estar entre las fechas de construcción.\nEjemeplo: 20060127 (27 de Enero del 2006)</html>", 
						"<html><b>DEJAR ASÍ PARA FUNCIONAMIENTO POR DEFECTO</b><br/>Tipo de Registro de catastro a usar (0 = todos).\nLos registros de catastro tienen la mayoría de la información necesaria para los shapefiles</html>",
						"<html><b>DEJAR ASÍ PARA FUNCIONAMIENTO POR DEFECTO</b><br/>Imprimir las geometrias separadas por alturas y con los tags building:levels y las subparcelas por separado sin ser unidas por tags. Esto crea archivos muy pesados NO RECOMENDADOS PARA SUBIR A OSM</html>",
						"<html><b>DEJAR ASÍ PARA FUNCIONAMIENTO POR DEFECTO</b><br/>Imprimir tanto en las vías como en las relaciones la lista de shapes que las componen o las utilizan.\nEs para casos de debugging si se quiere tener los detalles</html>",
		"<html><b>OPCIONAL</b><br/>Utilizar de forma adicional un archivo de reglas para ASIGNAR TAGS a los elementos de ELEMTEX (consultar wiki para el funcionamiento). Si no se selecciona ningún archivo ELEMTEX será exportado sin asignación de tags</html>"};


		// Crear los botones
		JPanel buttons = new JPanel();
		buttons.setLayout(new GridLayout(13,1));

		JButton resultPath, filesDirPath, rulesFile = null;

		final JFileChooser fcResult = new JFileChooser();
		fcResult.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		final JTextField resultFileName = new JTextField("Resultado");

		final JFileChooser filesDir = new JFileChooser();
		filesDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		final JComboBox<String> fcGsb = new JComboBox<String>();

		final JComboBox<String> proj = new JComboBox<String>();

		final JTextField fdesde = new JTextField("00000000");
		final JTextField fhasta= new JTextField("99999999");
		final JTextField fconstrudesde = new JTextField("00000000");
		final JTextField fconstruhasta= new JTextField("99999999");

		final JComboBox<String> tipoReg = new JComboBox<String>();
		final JComboBox<String> catastro3d = new JComboBox<String>();
		final JComboBox<String> shapesId = new JComboBox<String>();


		final JFileChooser fcRules = new JFileChooser();
		fcRules.setFileFilter(new ExtensionFileFilter("Archivos .rules", ".rules"));

		// Crear las descripciones
		for (int x = 0; x < descriptionsTexts.length; x++){

			HTMLEditorKit kit = new HTMLEditorKit();
			HTMLDocument doc = new HTMLDocument();
			JTextPane t = new JTextPane();
			t.setEditorKit(kit);
			t.setDocument(doc);
			t.setText(descriptionsTexts[x]);
			if (x < 4)
				t.setBackground(new Color(255,220,220));
			else 
				t.setBackground(new Color(220,220,220));
			descriptions.add(new JScrollPane(t));

			switch (x){

			case 0:{
				resultPath = new JButton("Seleccionar carpeta destino");
				resultPath.addActionListener(new ActionListener()
				{  public void actionPerformed(ActionEvent e)  
				{ fcResult.showOpenDialog(new JFrame()); }  
				});  
				buttons.add(resultPath);
				break;
			}
			case 1:{
				buttons.add(resultFileName);
				break;
			}
			case 2:{
				filesDirPath = new JButton("Seleccionar carpeta de archivos");
				filesDirPath.addActionListener(new ActionListener()  
				{  public void actionPerformed(ActionEvent e)  
				{ filesDir.showOpenDialog(new JFrame()); }  
				});  
				buttons.add(filesDirPath);
				break;
			}
			case 3:{		
				fcGsb.addItem("peninsula");
				fcGsb.addItem("baleares");
				buttons.add(fcGsb);
				break;
			}
			case 4:{
				proj.addItem("auto");
				proj.addItem("32628");
				proj.addItem("23029");
				proj.addItem("23030");
				proj.addItem("23031");
				proj.addItem("25829");
				proj.addItem("25830");
				proj.addItem("25831");
				proj.setBackground(new Color(255,255,255));
				buttons.add(proj);
				break;        		
			}
			case 5:{
				buttons.add(fdesde);
				break;
			}
			case 6:{
				new JTextField("99999999");
				buttons.add(fhasta);
				break;
			}
			case 7:{
				buttons.add(fconstrudesde);
				break;
			}
			case 8:{
				new JTextField("99999999");
				buttons.add(fconstruhasta);
				break;
			}
			case 9:{
				tipoReg.addItem("0");
				tipoReg.addItem("11");
				tipoReg.addItem("13");
				tipoReg.addItem("14");
				tipoReg.addItem("15");
				tipoReg.addItem("16");
				tipoReg.addItem("17");
				tipoReg.setBackground(new Color(255,255,255));
				buttons.add(tipoReg);
				break;
			}
			case 10:{
				catastro3d.addItem("NO");
				catastro3d.addItem("SI");
				catastro3d.setBackground(new Color(255,255,255));
				buttons.add(catastro3d);
				break;
			}
			case 11:{
				shapesId.addItem("NO");
				shapesId.addItem("SI");
				shapesId.setBackground(new Color(255,255,255));
				buttons.add(shapesId);
				break;
			}
			case 12:{
				rulesFile = new JButton("Seleccionar archivo de reglas .rules");
				rulesFile.addActionListener(new ActionListener()  
				{  public void actionPerformed(ActionEvent e)  
				{ fcRules.showOpenDialog(new JFrame()); }  
				});  
				buttons.add(rulesFile);
				break;
			}
			}
		}

		options.add(descriptions, BorderLayout.CENTER);
		options.add(buttons, BorderLayout.EAST);

		this.add(options,BorderLayout.CENTER);

		final JButton exe = new JButton("CREAR ARCHIVO DE CONFIGURACIÓN");

		// Boton de ejecutar Cat2Osm
		this.add(exe,BorderLayout.SOUTH);
		exe.addActionListener(new ActionListener()  
		{  public void actionPerformed(ActionEvent e)  
		{

			StringBuilder popupText = new StringBuilder();

			if (fcResult.getSelectedFile() == null){
				popupText.append("Especifique dónde crear el archivo resultado.\n\n");
			}

			if (resultFileName.getText().equals(null)){
				popupText.append("Especifique el nombre que tomará el archivo resultado de cat2osm.\n\n");
			}

			if (filesDir.getSelectedFile() == null){
				popupText.append("Especifique la carpeta que contiene los archivos de catastro.\n\n");	
			}

			if (fcGsb.getSelectedItem() == null){
				popupText.append("Seleccione el archivo de rejilla para la reproyección.\n\n");	
			}

			if (fdesde.getText().equals(null)){
				popupText.append("Especifique la fecha desde la cual tomar los datos (AAAAMMDD). Por defecto para tomar todos los datos indique 00000000\n\n");
			}

			if (fhasta.getText().equals(null)){
				popupText.append("Especifique la fecha hasta la cual tomar los datos (AAAAMMDD). Por defecto para tomar todos los datos indique 99999999\n\n");
			}

			if (fconstrudesde.getText().equals(null)){
				popupText.append("Especifique la fecha de construcción desde la cual tomar los datos (AAAAMMDD).\n\n");
			}

			if (fconstruhasta.getText().equals(null)){
				popupText.append("Especifique la fecha de construcción hasta la cual tomar los datos (AAAAMMDD). Por defecto para tomar todos los datos indique 99999999\n\n");
			}

			if (!popupText.toString().isEmpty()){
				JOptionPane.showMessageDialog(null,popupText,"Faltan datos",JOptionPane.ERROR_MESSAGE);
			}
			else {

				File dir = new File(""+fcResult.getSelectedFile());
				if (!dir.exists())
					dir.mkdirs();

				try {
					FileWriter fstream = new FileWriter(""+fcResult.getSelectedFile()+"/config");
					BufferedWriter out = new BufferedWriter(fstream);

					out.write("ResultPath="+fcResult.getSelectedFile().toString());out.newLine();
					out.write("ResultFileName="+resultFileName.getText());out.newLine();
					out.write("InputDirPath="+filesDir.getSelectedFile().toString());out.newLine();
					out.write("NadgridsPath="+"auto:"+fcGsb.getSelectedItem());out.newLine();
					out.write("Proyeccion="+proj.getSelectedItem().toString());out.newLine();
					out.write("FechaDesde="+fdesde.getText());out.newLine();
					out.write("FechaHasta="+fhasta.getText());out.newLine();
					out.write("FechaConstruDesde="+fconstrudesde.getText());out.newLine();
					out.write("FechaConstruHasta="+fconstruhasta.getText());out.newLine();
					out.write("TipoRegistro="+tipoReg.getSelectedItem().toString());out.newLine();
					out.write("Catastro3d="+catastro3d.getSelectedIndex()+"");out.newLine();
					out.write("PrintShapeIds="+shapesId.getSelectedIndex()+"");out.newLine();
					if (fcRules.getSelectedFile() != null)
						out.write("ElemtexRules="+fcRules.getSelectedFile().toString());out.newLine();

						out.close();
				}
				catch (Exception e1){ e1.printStackTrace(); }

				exe.setText("ARCHIVO CREADO EN LA CARPETA : "+ fcResult.getSelectedFile());
			}

		}  
		});  

		this.setVisible(true);
	}

}
