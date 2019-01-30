package alu.linking.launcher.debug;

import java.util.Iterator;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.preprocessing.surfaceform.query.QuerySolutionIterator;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

public class LauncherTestQuery {

	public static void main(String[] args) {
		final EnumModelType KG = EnumModelType.DBPEDIA_FULL;
		System.out.println("Testing query for: " + KG.name());
		final Dataset dataset = TDBFactory.createDataset(FilePaths.DATASET.getPath(KG));
		final Model model = dataset.getDefaultModel();
//		final String queryStr = "select distinct ?s (CONCAT(CONCAT(?fname, \" \"), ?lname) AS ?o) where {\r\n"
//				+ "?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://ontologycentral.com/2010/05/cb/vocab#Person> .\r\n"
//				+ "?s <http://ontologycentral.com/2010/05/cb/vocab#last_name> ?lname .\r\n"
//				+ "?s <http://ontologycentral.com/2010/05/cb/vocab#first_name> ?fname .\r\n" + "}";
		System.out.println("Executing query...");
//		getABC(model);
		// getPredicates(model);
		// getTypes(model);
		// getRandom(model);
		testVirtuoso();
		// getDBLPAuthors(model);

		// getPredicatesAndTypes(model);
		System.out.println("Finished!");
		// "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 100"
	}

	private static void testVirtuoso() {
		final boolean LOCAL = true;
		if (LOCAL) {
			final String user = "";
			final String password = "";
			final String url = "jdbc:virtuoso://localhost:1112";
			// "http://shetland.informatik.uni-freiburg.de:8890/sparql";//
			// virtuoso.jdbc4.VirtuosoException:
			// Wrong port number
			final String graphName = "http://dbpedia.org";
			final String queryStr = "select ?s ?p ?o where { ?s ?p ?o } LIMIT 100";
			// "select distinct ?bPred where { \n" + "?aSubj ?bPred ?cObj . \n" + "} LIMIT
			// 100";
			final VirtGraph virtGraph = new VirtGraph(graphName, url, user, password);
			execQuery(virtGraph, queryStr);
		} else {
//			final String queryStr = "select distinct ?bPred where { \n" + "?aSubj ?bPred ?cObj . \n" + "} LIMIT 100";
//
//			try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", queryStr)) {
//				// Set the DBpedia specific timeout.
//				((QueryEngineHTTP) qexec).addParam("timeout", "10000");
//
//				// Execute.
//				ResultSet rs = qexec.execSelect();
//				while (rs.hasNext()) {
//					System.out.println(rs.next().toString());
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//				System.err.println("============================================");
//				System.err.println(queryStr);
//				System.err.println("============================================");
//			}
		}
	}

	private static void execQuery(VirtGraph virtGraph, String queryStr) {
		final VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(queryStr, virtGraph);
		final ResultSet results = vqe.execSelect();
		displayQuery(results);
	}

	private static void getRandom(Model model) {
		System.out.println("############################################");
		System.out.println("# Random                                   #");
		System.out.println("############################################");
		final String queryStr = "select distinct ?a ?b ?c where { \n" + " ?a ?b ?c . \n" + " }" + " ORDER BY RAND()"
				+ " LIMIT 100";
		execQuery(model, queryStr);
		System.out.println("---------------------------------------------");
	}

	private static void getDBLPAuthors(Model model) {
		System.out.println("############################################");
		System.out.println("# Authors                                  #");
		System.out.println("############################################");
		final String queryStr = "select distinct ?author ?b where { \n"
				+ "?author <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Agent> . \n"
				// + "?author <http://www.w3.org/2000/01/rdf-schema#label> ?b . \n"
				+ "?author <http://xmlns.com/foaf/0.1/name> ?b . \n"

				+ "}" + " LIMIT 100";
		execQuery(model, queryStr);
		System.out.println("---------------------------------------------");
	}

	private static void getTypes(Model model) {
		System.out.println("############################################");
		System.out.println("# Types                                    #");
		System.out.println("############################################");
		final String queryStr = "select distinct ?aType1 where { \n"
				+ "?aSubj <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?aType1 . \n" + "}";
		execQuery(model, queryStr);
		System.out.println("---------------------------------------------");
	}

	private static void getPredicates(Model model) {
		System.out.println("############################################");
		System.out.println("# Predicates                               #");
		System.out.println("############################################");
		final String queryStr = "select distinct ?bPred where { \n" + "?aSubj ?bPred ?cObj . \n" + "}";
		execQuery(model, queryStr);
		System.out.println("---------------------------------------------");
	}

	private static void getABC(Model model) {
		final String queryStr = "select distinct ?a ?b ?c where { ?a ?b ?c } LIMIT 100";
		execQuery(model, queryStr);
	}

	private static void getPredicatesAndTypes(final Model model) {
		System.out.println("############################################");
		System.out.println("# Predicates linking different types       #");
		System.out.println("############################################");
		final String queryStr = "select distinct ?aType1 ?bPred ?cType2 where { \n"
				+ "?aSubj <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?aType1 . \n"
				+ "?cObj <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?cType2 . \n" + "?aSubj ?bPred ?cObj . \n"
				+ "}";
		execQuery(model, queryStr);
		System.out.println("---------------------------------------------");
	}

	private static void execQuery(Model model, String queryStr) {
		final Query query = QueryFactory.create(queryStr);
		// Execute the query and obtain results
		final QueryExecution qe = QueryExecutionFactory.create(query, model);
		final ResultSet results = qe.execSelect();
		displayQuery(results);
	}

	private static void displayQuery(ResultSet results) {
		while (results.hasNext()) {
			final QuerySolution qs = results.next();
			Iterator<String> it = new QuerySolutionIterator(qs);
			while (it.hasNext()) {
				System.out.print(qs.get(it.next()) + " ");
			}
			System.out.println();
		}
	}
}
