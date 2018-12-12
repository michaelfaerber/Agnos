package de.dwslab.petar.jena.io;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;

public class QueryManager {

	public static void runQuery(String repoDir, String queryString) {
		Dataset dataset = TDBFactory.createDataset(repoDir);

		Model model = dataset.getDefaultModel();

		Query query = QueryFactory.create(queryString);

		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();

		ResultSetFormatter.out(System.out, results, query);
		// iterate all municipal districts
		while (results.hasNext()) {
			QuerySolution result = results.next();

		}

		// Important - free up resources used running the query
		qe.close();
	}

	public static void main(String[] args) {
		runQuery(args[0], args[1]);
	}
}
