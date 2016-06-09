
public class ParcelaParser {

	public ParcelaParser(){
		
	}

	/**
	 * Comprueba si hay destinos o usos, coge el de mayor
	 * area y actualiza sus propiedades y las de los subshapes en consecuencia.
	 * 
	 * Hay tags que vienen mas detallados en los shapefiles por eso puede que
	 * no se sobreescriban o si.
	 */
	public void parseParcela(ShapeParcela parcela){
		
		String usodestino = parcela.getUsoDestinoMasArea();
		
		switch (usodestino){
		case "A":
		case "B":
			break;
		case "AAL":
		case "BAL":
			parcela.getAttributes().addAttribute("landuse", "residential");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes", "warehouse");
			break;
		case "AAP":
		case "BAP":
			parcela.getAttributes().addAttribute("landuse", "garages");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes", "garage");
			break;
		case"ACR":
		case"BCR":
			break;
		case "ACT":
		case "BCT":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "power", "substation");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes", "substation");
			break;
		case "AES":
		case "BES":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "public_transport", "station");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes", "station");
			break;
		case "AIG":
		case "BIG":
			parcela.getAttributes().addAttribute("landuse","farmyard");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes", "livestock");
			break;
		case "C":
		case "D":
			parcela.getAttributes().addAttributeIfNotExistValue("landuse","retail");
			break;
		case "CAT":
		case "DAT":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "shop", "car");
			break;
		case "CBZ":
		case "DBZ":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "shop","electronics");
			break;
		case "CCE":
		case "DCE":
			parcela.getAttributes().addAttribute("landuse","retail");
			break;
		case "CCL":
		case "DCL":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "shop","shoes");
			break;
		case "CCR":
		case "DCR":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "shop","butcher");
			break;
		case "CDM":
		case "DDM":
			parcela.getAttributes().addAttribute("landuse","retail");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "shop","yes");
			break;
		case "CDR":
		case "DDR":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "shop","chemist");
			break;
		case "CFN":
		case "DFN":;
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","bank");
			break;
		case "CFR":
		case "DFR":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","pharmacy");
			break;
		case "CFT":
		case "DFT":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "craft","plumber");
			break;
		case "CGL":
		case "DGL":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","marketplace");
			break;
		case "CIM":
		case "DIM":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "shop","copyshop");
			break;
		case "CJY":
		case "DJY":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "shop","jewelry");
			break;
		case "CLB":
		case "DLB":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "shop","books");
			break;
		case "CMB":
		case "DMB":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "shop","furniture");
			break;
		case "CPA":
		case "DPA":
			parcela.getAttributes().addAttribute("landuse","retail");
			break;
		case "CPR":
		case "DPR":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "shop","chemist");
			break;
		case "CRL":
		case "DRL":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "craft","watchmaker");
			break;
		case "CSP":
		case "DSP":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "shop","clothes");
			break;
		case "CTJ":
		case "DTJ":
			parcela.getAttributes().addAttribute("landuse","retail");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "shop","supermarket");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes", "supermarket");
			break;
		case "E":
		case "F":
			parcela.getAttributes().addAttributeIfNotExistValue("amenity","school");
			addAttributeInConstruIfKeyValue(parcela, "building","yes","amenity","school");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","school");
			break;
		case "EBL":
		case "FBL":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","library");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes", "library");
			break;
		case "EBS":
		case "FBS":
			parcela.getAttributes().addAttribute("amenity","school");
			parcela.getAttributes().addAttribute("isced:level","1;2");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity", "school");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "isced:level","1;2");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes", "school");
			break;
		case "ECL":
		case "FCL":
			parcela.getAttributes().addAttribute("amenity","community_centre");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity", "community_centre");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes", "community_centre");
			break;
		case "EIN":
		case "FIN":
			parcela.getAttributes().addAttribute("amenity","school");
			parcela.getAttributes().addAttribute("isced:level","3;4");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity", "school");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "isced:level","3;4");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","school");
			break;
		case "EMS":
		case "FMS":
			parcela.getAttributes().addAttribute("tourism","museum");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "tourism", "museum");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","museum");
			break;
		case "EPR":
		case "FPR":
			parcela.getAttributes().addAttribute("amenity","school");
			parcela.getAttributes().addAttribute("isced:level","4");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity", "school");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "isced:level","4");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","school");
			break;
		case "EUN":
		case "FUN":
			parcela.getAttributes().addAttribute("amenity","university");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity", "university");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","university");
			break;
		case "G":
		case "H":
			parcela.getAttributes().addAttribute("tourism","hotel");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "tourism","hotel");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","hotel");
			break;
		case "GC1":
		case "HC1":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","cafe");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "forks","1");
			break;
		case "GC2":
		case "HC2":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","cafe");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "forks","2");
			break;
		case "GC3":
		case "HC3":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","cafe");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "forks","3");
			break;
		case "GC4":
		case "HC4":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","cafe");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "forks","4");
			break;
		case "GC5":
		case "HC5":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","cafe");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "forks","5");
			break;
		case "GH1":
		case "HH1":
			parcela.getAttributes().addAttribute("tourism","hotel");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "tourism","hotel");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "stars","1");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","hotel");
			break;
		case "GH2":
		case "HH2":
			parcela.getAttributes().addAttribute("tourism","hotel");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "tourism","hotel");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "stars","2");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","hotel");
			break;
		case "GH3":
		case "HH3":
			parcela.getAttributes().addAttribute("tourism","hotel");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "tourism","hotel");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "stars","3");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","hotel");
			break;
		case "GH4":
		case "HH4":
			parcela.getAttributes().addAttribute("tourism","hotel");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "tourism","hotel");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "stars","4");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","hotel");
			break;
		case "GH5":
		case "HH5":
			parcela.getAttributes().addAttribute("tourism","hotel");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "tourism","hotel");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "stars","5");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","hotel");
			break;
		case "GP1":
		case "HP1":
			parcela.getAttributes().addAttribute("tourism","apartments");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "tourism","apartments");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "category","1");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","apartments");
			break;
		case "GP2":
		case "HP2":
			parcela.getAttributes().addAttribute("tourism","apartments");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "tourism","apartments");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "category","2");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","apartments");
			break;
		case "GP3":
		case "HP3":
			parcela.getAttributes().addAttribute("tourism","apartments");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "tourism","apartments");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "category","3");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","apartments");
			break;
		case "GPL":
		case "HPL":
			parcela.getAttributes().addAttribute("tourism","apartments");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "tourism","apartments");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","apartments");
			break;
		case "GR1":
		case "HR1":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","restaurant");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "forks","1");
			break;
		case "GR2":
		case "HR2":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","restaurant");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "forks","2");
			break;
		case "GR3":
		case "HR3":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","restaurant");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "forks","3");
			break;
		case "GR4":
		case "HR4":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","restaurant");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "forks","4");
			break;
		case "GR5":
		case "HR5":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","restaurant");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "forks","5");
			break;
		case "GS1":
		case "HS1":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "tourism","hotel");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "stars","1");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","hotel");
			break;
		case "GS2":
		case "HS2":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "tourism","hotel");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "stars","2");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","hotel");
			break;
		case "GS3":
		case "HS3":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "tourism","hotel");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "stars","3");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","hotel");
			break;
		case "GT1":
		case "HT1":
		case "GT2":
		case "HT2":
		case "GT3":
		case "HT3":
		case "GTL":
		case "HTL":
			// Como no sabemos a que se puede referir esto, mejor ponemos un fixme
			parcela.getAttributes().addAttribute("fixme","Documentar codificaci��n de destino de los bienes inmuebles en catastro c��digo="+ usodestino +" en http://wiki.openstreetmap.org/wiki/Traduccion_metadatos_catastro_a_map_features#Codificaci.C3.B3n_de_los_destinos_de_los_bienes_inmuebles");
			break;
		case "I":
		case "J":
			parcela.getAttributes().addAttributeIfNotExistValue("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IAG":
		case "JAG":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","farming");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IAL":
		case "JAL":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","food");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IAM":
		case "JAM":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","storage_tank");
			parcela.getAttributes().addAttribute("content","OMW");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IAR":
		case "JAR":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","agricultural");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IAS":
		case "JAS":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("craft","sawmill");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IBB":
		case "JBB":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","drinks");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IBD":
		case "JBD":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","winery");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IBR":
		case "JBR":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","ceramic");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "ICH":
		case "JCH":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","mushrooms");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "ICN":
		case "JCN":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","building");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "ICT":
		case "JCT":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","quarry");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IEL":
		case "JEL":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","electric");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IGR":
		case "JGR":
			parcela.getAttributes().addAttribute("landuse","farmyard");
			break;
		case "IIM":
		case "JIM":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","chemistry");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IIN":
		case "JIN":
			parcela.getAttributes().addAttribute("landuse","greenhouse_horticulture");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","greenhouse");
			break;
		case "IMD":
		case "JMD":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","wood");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IMN":
		case "JMN":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","manufacturing");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IMT":
		case "JMT":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","metal");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IMU":
		case "JMU":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","machinery");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IPL":
		case "JPL":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","plastics");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IPP":
		case "JPP":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","paper");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IPS":
		case "JPS":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","fishing");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IPT":
		case "JPT":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","petroleum");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "ITB":
		case "JTB":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","tobacco");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "ITX":
		case "JTX":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","clothing");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "IVD":
		case "JVD":
			parcela.getAttributes().addAttribute("landuse","industrial");
			parcela.getAttributes().addAttribute("man_made","works");
			parcela.getAttributes().addAttribute("works","glass");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","industrial");
			break;
		case "K":
		case "L":
			parcela.getAttributes().addAttributeIfNotExistValue("leisure","sports_centre");
			break;
		case "KDP":
		case "LDP":
			parcela.getAttributes().addAttribute("leisure","pitch");
			parcela.getAttributes().addAttribute("fixme","Codigo="+ usodestino +", afinar sport=X si es posible.");
			break;
		case "KES":
		case "LES":
			parcela.getAttributes().addAttribute("leisure","stadium");
			parcela.getAttributes().addAttribute("fixme","Codigo="+ usodestino +", afinar sport=X si es posible.");
			break;
		case "KPL":
		case "LPL":
			parcela.getAttributes().addAttribute("leisure","sports_centre");
			parcela.getAttributes().addAttribute("fixme","Codigo="+ usodestino +", afinar sport=X si es posible.");
			break;
		case "KPS":
		case "LPS":
			parcela.getAttributes().addAttribute("leisure","swimming_pool");
			parcela.getAttributes().addAttribute("sport","swimming");
			break;
		case "M":
		case "N":
			parcela.getAttributes().addAttributeIfNotExistValue("landuse","greenfield");
			break;
		case "O":
		case "X":
			parcela.getAttributes().addAttributeIfNotExistValue("landuse","commercial");
			break;
		case "O02":
		case "X02":
			parcela.getAttributes().addAttribute("landuse","commercial");
			parcela.getAttributes().addAttribute("fixme","Codigo="+ usodestino +", Profesional superior. Afinar office=X si es posible.");
			break;
		case "O03":
		case "X03":
			parcela.getAttributes().addAttribute("landuse","commercial");
			parcela.getAttributes().addAttribute("fixme","Codigo="+ usodestino +", Profesional medio. Afinar office=X si es posible.");
			break;
		case "O06":
		case "X06":
			parcela.getAttributes().addAttribute("landuse","commercial");
			parcela.getAttributes().addAttribute("fixme","Codigo="+ usodestino +", M��dicos, abogados... Afinar office=X si es posible.");
			break;
		case "O07":
		case "X07":
			parcela.getAttributes().addAttribute("landuse","health");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "health_facility:type","office");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "health_person:type","nurse");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","health");
			break;
		case "O11":
		case "X11":
			parcela.getAttributes().addAttribute("landuse","commercial");
			parcela.getAttributes().addAttribute("fixme","Codigo="+ usodestino +", Profesores Mercant. Afinar office=X si es posible.");
			break;
		case "O13":
		case "X13":
			parcela.getAttributes().addAttribute("landuse","commercial");
			parcela.getAttributes().addAttribute("fixme","Codigo="+ usodestino +", Profesores Universitarios. Afinar office=X si es posible.");
			break;
		case "O15":
		case "X15":
			parcela.getAttributes().addAttribute("landuse","commercial");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "office","writer");
			break;
		case "O16":
		case "X16":
			parcela.getAttributes().addAttribute("landuse","commercial");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "craft","painter");
			break;
		case "O17":
		case "X17":
			parcela.getAttributes().addAttribute("landuse","commercial");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "office","musician");
			break;
		case "O43":
		case "X43":
			parcela.getAttributes().addAttribute("landuse","commercial");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "office","salesman");
			break;
		case "O44":
		case "X44":
			parcela.getAttributes().addAttribute("landuse","commercial");
			parcela.getAttributes().addAttribute("fixme","Codigo="+ usodestino +", agentes. Afinar office=X si es posible.");
			break;
		case "O75":
		case "X75":
			parcela.getAttributes().addAttribute("landuse","commercial");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "craft","weaver");
			break;
		case "O79":
		case "X79":
			parcela.getAttributes().addAttribute("landuse","commercial");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "craft","tailor");
			break;
		case "O81":
		case "X81":
			parcela.getAttributes().addAttribute("landuse","commercial");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "craft","carpenter");
			break;
		case "O88":
		case "X88":
			parcela.getAttributes().addAttribute("landuse","commercial");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "craft","jeweller");
			break;
		case "O99":
		case "X99":
			parcela.getAttributes().addAttribute("landuse","commercial");
			parcela.getAttributes().addAttribute("fixme","Codigo="+ usodestino +", otras actividades. Afinar office=X si es posible.");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes", "commercial");
			break;
		case "P":
		case "Q":
			parcela.getAttributes().addAttribute("amenity","public_building:part");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","public_building:part");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","public");
			break;
		case "PAA":
		case "QAA":
			parcela.getAttributes().addAttribute("amenity","townhall");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","townhall");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","public");
			break;
		case "PAD":
		case "QAD":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","courthouse");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "operator","autonomous_community");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","public");
			break;
		case "PAE":
		case "QAE":
			parcela.getAttributes().addAttribute("amenity","townhall");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","townhall");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","public");
			break;
		case "PCB":
		case "QCB":
			parcela.getAttributes().addAttribute("office","administrative");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "office","administrative");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","public");
			break;
		case "PDL":
		case "QDL":
		case "PGB":
		case "QGB":
			parcela.getAttributes().addAttribute("office","government");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "office","government");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","public");
			break;
		case "PJA":
		case "QJA":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","courthouse");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "operator","county");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","public");
			break;
		case "PJO":
		case "QJO":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","courthouse");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "operator","province");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","public");
			break;
		case "R":
		case "S":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","place_of_worship");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","church");
			break;
		case "RBS":
		case "SBS":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","place_of_worship");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "religion","christian");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "denomination","roman_catholic");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","basilica");
			break;
		case "RCP":
		case "SCP":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","place_of_worship");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "religion","christian");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "denomination","roman_catholic");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","chapel");
			break;
		case "RCT":
		case "SCT":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","place_of_worship");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "religion","christian");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "denomination","roman_catholic");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","cathedral");
			break;
		case "RER":
		case "SER":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","place_of_worship");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "religion","christian");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "denomination","roman_catholic");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","hermitage");
			break;
		case "RPR":
		case "SPR":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","place_of_worship");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "religion","christian");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "denomination","roman_catholic");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","parish_church");
			break;
		case "RSN":
		case "SSN":
			parcela.getAttributes().addAttribute("landuse","health");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","hospital");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","hospital");
			break;
		case "T":
		case "U":
			break;
		case "TAD":
		case "UAD":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","auditorium");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","auditorium");
			break;
		case "TCM":
		case "UCM":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","cinema");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","cinema");
			break;
		case "TCN":
		case "UCN":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","cinema");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","cinema");
			break;
		case "TSL":
		case "USL":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","hall");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","hall");
			break;
		case "TTT":
		case "UTT":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","theatre");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","theatre");
			break;
		case "V":
		case "W":
			parcela.getAttributes().addAttributeIfNotExistValue("landuse","residential");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","residential");
			break;
		case "Y":
		case "Z":
			break;
		case "YAM":
		case "ZAM":
		case "YCL":
		case "ZCL":
			parcela.getAttributes().addAttribute("landuse","health");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","clinic");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "medical_system:western","yes");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","clinic");
			break;
		case "YBE":
		case "ZBE":
			parcela.getAttributes().addAttribute("landuse","pond");
			break;
		case "YCA":
		case "ZCA":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","casino");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","casino");
			break;
		case "YCB":
		case "ZCB":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","club");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","club");
			break;
		case "YCE":
		case "ZCE":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","casino");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","casino");
			break;
		case "YCT":
		case "ZCT":
			parcela.getAttributes().addAttribute("landuse","quarry");
			break;
		case "YDE":
		case "ZDE":
			parcela.getAttributes().addAttribute("man_made","wastewater_plant");
			break;
		case "YDG":
			parcela.getAttributes().addAttribute("man_made","storage_tank");
			parcela.getAttributes().addAttribute("content","gas");
			break;
		case "ZDG":
			parcela.getAttributes().addAttribute("landuse","farmyard");
			parcela.getAttributes().addAttribute("man_made","storage_tank");
			parcela.getAttributes().addAttribute("content","gas");
			break;
		case "YDL":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "man_made","storage_tank");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "content","liquid");
			break;
		case "ZDL":
			parcela.getAttributes().addAttribute("landuse","farmyard");
			parcela.getAttributes().addAttribute("man_made","storage_tank");
			parcela.getAttributes().addAttribute("content","liquid");
			break;
		case "YDS":
		case "ZDS":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","pharmacy");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "dispensing","yes");
			break;
		case "YGR":
		case "ZGR":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","kindergarten");
			break;
		case "YGV":
		case "ZGV":
			parcela.getAttributes().addAttribute("landuse","surface_mining");
			parcela.getAttributes().addAttribute("mining_resource","gravel");
			break;
		case "YHG":
		case "ZHG":
			// Como no sabemos a que se puede referir esto, mejor ponemos un fixme
			parcela.getAttributes().addAttribute("fixme","Documentar codificaci��n de destino de los bienes inmuebles en catastro c��digo="+ usodestino +" en http://wiki.openstreetmap.org/wiki/Traduccion_metadatos_catastro_a_map_features#Codificaci.C3.B3n_de_los_destinos_de_los_bienes_inmuebles");
			break;
		case "YHS":
		case "ZHS":
		case "YSN":
		case "ZSN":
			parcela.getAttributes().addAttribute("landuse","health");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "amenity","hospital");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "medical_system:western","yes");
			overwriteBuildingAttributesToConstruAndParts(parcela, "yes","hospital");
			break;
		case "YMA":
		case "ZMA":
			parcela.getAttributes().addAttribute("landuse","surface_mining");
			parcela.getAttributes().addAttribute("fixme","Codigo="+ usodestino +", afinar mining_resource=X si es posible.");
			break;
		case "YME":
		case "ZME":
			parcela.getAttributes().addAttribute("man_made","pier");
			break;
		case "YPC":
		case "ZPC":
			parcela.getAttributes().addAttribute("landuse","aquaculture");
			break;
		case "YRS":
		case "ZRS":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "social_facility","group_home");
			break;
		case "YSA":
		case "ZSA":
		case "YSO":
		case "ZSO":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "office","labour_union");
			break;
		case "YSC":
		case "ZSC":
			parcela.getAttributes().addAttribute("landuse","health");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "health_facility:type", "first_aid");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "medical_system:western", "yes");
			break;
		case "YSL":
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "man_made", "storage_tank");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "content", "solid");
			break;
		case "ZSL":
			parcela.getAttributes().addAttribute("landuse","farmyard");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "man_made","storage_tank");
			addAttributeInConstruIfKeyValue(parcela, "building", "yes", "content","solid");
			break;
		case "YVR":
		case "ZVR":
			parcela.getAttributes().addAttribute("landuse","landfill");
			break;
		default:
			if (!usodestino.isEmpty()){
				parcela.getAttributes().addAttribute("fixme","Documentar nuevo codificaci��n de destino de los bienes inmuebles en catastro c��digo="+ usodestino +" en http://wiki.openstreetmap.org/wiki/Traduccion_metadatos_catastro_a_map_features#Codificaci.C3.B3n_de_los_destinos_de_los_bienes_inmuebles");
				}
		}
	}


	/**
	 * Anade el atributo a los constru solo si existe la clave especificada
	 * @param ifKey Clave que tiene que existir
	 * @param k Clave
	 * @param v Valor
	 */
	public void addAttributeInConstruIfKeyValue(ShapeParcela parcela, String existKey, String existValue, String k, String v){
		if(parcela.subshapes != null){
			if (parcela.subshapes.isEmpty()){
				// Anadir el tag a este parent, ya que no tiene subshapes. Eso significa
				// que los subshapes coincidian justo con la geometria del parent
				parcela.getAttributes().addAttributeIfKeyValue(existKey, existValue, k, v);
				
			} else {
				for(Shape sub : parcela.subshapes){
					if (sub instanceof ShapeConstruExterior){
						sub.getAttributes().addAttributeIfKeyValue(existKey, existValue, k, v);
						for (Shape subsub : ((ShapeConstruExterior) sub).getSubshapes()){
							subsub.getAttributes().addAttributeIfKeyValue(existKey, existValue, k, v);
						}
					}
				}
			}
		}
	}


	/**
	 * Sobreescribe el valor de building en los construExterior y building:part en los
	 * construPart
	 */
	public void overwriteBuildingAttributesToConstruAndParts(ShapeParcela parcela, String existingV, String newV){
		if(parcela.subshapes != null){
			if (parcela.subshapes.isEmpty()){
				// Anadir el tag a este parent, ya que no tiene subshapes. Eso significa
				// que los subshapes coincidian justo con la geometria del parent
				parcela.getAttributes().overwriteAttribute("building", existingV, newV);
			} else {
				for(Shape sub : parcela.subshapes){
					if (sub instanceof ShapeConstruPart){
						sub.getAttributes().overwriteAttribute("building", existingV, newV);
					}
					else if (sub instanceof ShapeConstruExterior){
						sub.getAttributes().overwriteAttribute("building", existingV, newV);
						for (Shape subsub : ((ShapeConstruExterior) sub).getSubshapes()){
							subsub.getAttributes().overwriteAttribute("building", existingV, newV);
						}
					}
				}
			}
		}
	}

}
