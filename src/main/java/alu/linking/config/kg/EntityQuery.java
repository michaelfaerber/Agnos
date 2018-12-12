package alu.linking.config.kg;

public enum EntityQuery {
//	DBPEDIA(""), //
//	FREEBASE(""), //
//	CRUNCHBASE(""), //
	MAG("SELECT DISTINCT ?s WHERE { ?s <http://xmlns.com/foaf/0.1/name> ?o }"), //
	DEFAULT("Select ?s Where { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Thing>}") //
	;
	public final String query;

	EntityQuery(final String entityQuery) {
		this.query = entityQuery;
	}
}
