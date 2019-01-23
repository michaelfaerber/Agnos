package alu.linking.config.kg;

public enum EnumModelType {
	DBPEDIA("./dbpedia/", EntityQuery.DEFAULT), //
	DBPEDIA_FULL("./dbpedia_full/", EntityQuery.DEFAULT),//
	FREEBASE("./freebase/", EntityQuery.DEFAULT), // 
	CRUNCHBASE("./crunchbase/", EntityQuery.DEFAULT), // 
	CRUNCHBASE2("./crunchbase2/", EntityQuery.DEFAULT),//
	MINI_MAG("./mini_mag/", EntityQuery.MAG), //
	MAG("./mag/", EntityQuery.MAG), //
	DBLP("./dblp/", EntityQuery.DBLP), //
	DEFAULT("./", EntityQuery.DEFAULT) //
	;
	public final String root;
	public final EntityQuery query;

	EnumModelType(final String folderName, final EntityQuery query) {
		this.root = folderName;
		this.query = query;
	}

}
