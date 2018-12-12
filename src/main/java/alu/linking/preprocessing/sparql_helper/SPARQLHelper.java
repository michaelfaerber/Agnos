package alu.linking.preprocessing.sparql_helper;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import alu.linking.config.constants.RDFConstants;

public class SPARQLHelper {
	
	// List containing all (s,p,o) triples from the triple store
	private List<QuerySolution> RDFList;
	
	// Map holding all the (s,p,o) triples from the triple store
	// Key: each entity's subject
	// Value: set of predicate-object pairs for a given subject
	private HashMap<String, List<String[]>> RDFMap;

	public SPARQLHelper() {
		RDFList = new ArrayList<>();
		RDFMap = new HashMap<>();
	}

	public SPARQLHelper(String query) {
		this();
		RDFList = buildRDFList(query);
		RDFMap = buildRDFMap();
	}
	
	/**
	 * 
	 * @param query
	 * 		SPARQL query to extract all the
	 * 		(s,p,o) triples from the triple store
	 * @return
	 * 		List storing all the (s,p,o) triples
	 * 		from the triple store
	 */
    public List<QuerySolution> buildRDFList(String query){
    	QueryExecution qe = QueryExecutionFactory.sparqlService(
                "http://localhost:3030/ds/query", query);
        ResultSet results = qe.execSelect();
        List<QuerySolution> list = ResultSetFormatter.toList(results);
        qe.close();
        
        return list;
    }
	
	public HashMap<String, List<String[]>> buildRDFMap(){
    	// Number of (s,p,o) triples for each entity
    	// Currently we have 3 (rdf:type - rdf:label - bmw:entityType)
    	int number = getNumberOfTriples(RDFList);
//    	System.out.println("Number of triples: " + number);
    	
    	// List that holds the (p,o) pairs of each entity
        List<String[]> poPairs = new ArrayList<>();
        String subject = "";
        // Iterating through all the entities:
        // Each entity has its subject,
        // and a number of (p,o) pairs
        for(int i = 0; i < RDFList.size(); i += number) {
        	poPairs = new ArrayList<>();
        	boolean isBlankNode = RDFList.get(i).get("subject").isAnon();
        	subject = RDFList.get(i).get("subject").toString();
        	// Iterating through all the (p,o) pairs of this entity
        	for(int j = 0; j < number; j++) {
        		String[] poPair = new String[2];
        		poPair[0] = RDFList.get(i + j).get("predicate").toString();
        		poPair[1] = RDFList.get(i + j).get("object").toString();
        		poPairs.add(poPair);
        	}
        	RDFMap.put(subject, poPairs);
        }
        return RDFMap;
	}
	
	public List<QuerySolution> getRDFList(){
		return RDFList;
	}
	
	public HashMap<String, List<String[]>> getRDFMap() {
		return RDFMap;
	}
	
	public void setRDFList(List<QuerySolution> list) {
		this.RDFList = list;
	}
	
	public void setRDFMap(HashMap<String, List<String[]>> map) {
		this.RDFMap = map;
	}
		
	/*
	 * Helper methods to extract predicates of "types.ttl"
	 * i.e. when STEMMING is TRUE
	 */
	public String getType(String subject) {
		List<String[]> list = RDFMap.get(subject);
        int typeIndex = predicateIndex(RDFConstants.SPARQL_PRED_TYPE.val, list);
		
        return list.get(typeIndex)[1];
	}
	
	public String getLabel(String subject) {
		List<String[]> list = RDFMap.get(subject);
        int labelIndex = predicateIndex(RDFConstants.SPARQL_PRED_LABEL.val, list);
        return list.get(labelIndex)[1];
	}
	
	public String getEntityType(String subject) {
		List<String[]> list = RDFMap.get(subject);
        int entityTypeIndex = predicateIndex(RDFConstants.SPARQL_PRED_ENTITY_TYPE.val, list);
		
        return list.get(entityTypeIndex)[1];
	}
	
	/*
	 * Helper methods to extract predicates of "rdf.ttl"
	 * i.e. when STEMMING is FALSE
	 */
	public String getAnchorOf(String subject) {
		List<String[]> list = RDFMap.get(subject);
        int typeIndex = predicateIndex(RDFConstants.SPARQL_PRED_ANCHOR_OF.val, list);

        return list.get(typeIndex)[1];
	}

	public String getLinkedTo(String subject) {
		List<String[]> list = RDFMap.get(subject);
        int typeIndex = predicateIndex(RDFConstants.SPARQL_PRED_LINKED_TO.val, list);

        return list.get(typeIndex)[1];

	}
	
	
	
	
	
	
	// Setup the fuseki server
	// by uploading the appropriate RDF file
	public void setup(String fileName) {
		// Create a model that reads from the input RDF file
		// and upload it to the Fuseki server
    	try {
			Model m = ModelFactory.createDefaultModel();
			FileInputStream in = new FileInputStream(fileName);
			m.read(in, null, "TURTLE");
			DatasetAccessor accessor = DatasetAccessorFactory.createHTTP("http://localhost:3030/ds/data");
	        accessor.putModel(m);        
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
    // Returns the number of (s,p,o) triples in each entity
    public int getNumberOfTriples(List<QuerySolution> list) {
        for(int i = 0; i < list.size(); i++) {
        	if(!list.get(i).get("subject").toString().equals(list.get(i+1).get("subject").toString()))
        		return i+1;
        }
    	return list.size();
    }

    // Finds the index of the String[] array which contains the given predicate
    // within the given list
    public static int predicateIndex(String predicate, List<String[]> list) {
    	for(int i = 0; i < list.size(); i++) {
    		String[] arr = list.get(i);
    		if(arr[0].equals(predicate)) {
    			return i;
    		}
    	}
    	return -1;
    }
    
}
