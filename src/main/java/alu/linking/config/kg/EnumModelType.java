package alu.linking.config.kg;

public enum EnumModelType {
	DBPEDIA("./dbpedia/", EntityQuery.DEFAULT), //
	FREEBASE("./freebase/", EntityQuery.DEFAULT), // 
	CRUNCHBASE("./crunchbase/", EntityQuery.DEFAULT), // 
	MAG("./mag/", EntityQuery.MAG), //
	DEFAULT("./", EntityQuery.DEFAULT) //
	;
	public final String root;
	public final EntityQuery query;

	EnumModelType(final String folderName, final EntityQuery query) {
		this.root = folderName;
		this.query = query;
	}

}
