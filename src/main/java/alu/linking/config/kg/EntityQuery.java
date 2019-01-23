package alu.linking.config.kg;

public enum EntityQuery {
//	DBPEDIA(""), //
//	FREEBASE(""), //
//	CRUNCHBASE(""), //
	//Note that the entity variable HAS to be ?s for RDF2Vec
	MAG("SELECT DISTINCT ?s WHERE { ?s <http://xmlns.com/foaf/0.1/name> ?o }"), //
	DBLP("select distinct ?s where { \n"
			+ "?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Agent> . \n" 
			+ "?s <http://www.w3.org/2000/01/rdf-schema#label> ?b . \n"
			//+ "?author <http://xmlns.com/foaf/0.1/name> ?b . \n"
			
			+ "}"), //
	DEFAULT("Select ?s Where { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Thing>}") //
	;
	public final String query;

	EntityQuery(final String entityQuery) {
		this.query = entityQuery;
	}
}
