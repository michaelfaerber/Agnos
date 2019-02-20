package alu.linking.config.kg;

import alu.linking.config.constants.EnumConnection;

public enum EnumModelType {
	DBPEDIA("./dbpedia/", EntityQuery.DEFAULT), //
	DBPEDIA_FULL("./dbpedia_full/", EntityQuery.DEFAULT, EnumConnection.SHETLAND_VIRTUOSO, true, "http://dbpedia.org"), //
	FREEBASE("./freebase/", EntityQuery.DEFAULT), //
	CRUNCHBASE("./crunchbase/", EntityQuery.CRUNCHBASE2), //
	CRUNCHBASE2("./crunchbase2/", EntityQuery.CRUNCHBASE2), //
	MINI_MAG("./mini_mag/", EntityQuery.MAG), //
	MAG("./mag/", EntityQuery.MAG), //
	DBLP("./dblp/", EntityQuery.DBLP), //
	DEFAULT("./", EntityQuery.DEFAULT) //
	;
	public final String root;
	public final EntityQuery query;

	public final EnumConnection virtuosoConn;
	private final boolean useVirtuoso;
	public final String virtuosoGraphname;

	EnumModelType(final String folderName, final EntityQuery query) {
		this(folderName, query, null);
	}

	EnumModelType(final String folderName, final EntityQuery query, EnumConnection virtuosoConn) {
		this(folderName, query, virtuosoConn, virtuosoConn != null, null);
	}

	EnumModelType(final String folderName, final EntityQuery query, EnumConnection virtuosoConn,
			final boolean useVirtuoso, final String graphName) {
		this.root = folderName;
		this.query = query;
		this.virtuosoConn = virtuosoConn;
		this.useVirtuoso = useVirtuoso;
		this.virtuosoGraphname = graphName;
	}

	/**
	 * Whether to use Virtuoso for this KG
	 * 
	 * @return
	 */
	public boolean useVirtuoso() {
		return this.useVirtuoso && this.virtuosoConn != null;
	}

	@Override
	public String toString() {
		return name();
	}

}
