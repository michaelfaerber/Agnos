package de.dwslab.petar.walks;

public class EntityDefinitions {
	public static String MAGEntityDefinition = "SELECT DISTINCT ?s WHERE { ?s <http://xmlns.com/foaf/0.1/name> ?o }";
	public static String originalEntityDefinition = "Select ?s Where { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Thing>}";
}
