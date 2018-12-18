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

public class LauncherTestQuery {

	public static void main(String[] args) {
		final EnumModelType KG = EnumModelType.CRUNCHBASE;
		final Dataset dataset = TDBFactory.createDataset(FilePaths.DATASET.getPath(KG));
		final Model model = dataset.getDefaultModel();
		final String queryStr = "select distinct ?s (CONCAT(CONCAT(?fname, \" \"), ?lname) AS ?o) where {\r\n" + 
				"?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://ontologycentral.com/2010/05/cb/vocab#Person> .\r\n" + 
				"?s <http://ontologycentral.com/2010/05/cb/vocab#last_name> ?lname .\r\n" + 
				"?s <http://ontologycentral.com/2010/05/cb/vocab#first_name> ?fname .\r\n" + 
				"}";
		// "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 100"
		final Query query = QueryFactory.create(queryStr);
		// Execute the query and obtain results
		final QueryExecution qe = QueryExecutionFactory.create(query, model);
		final ResultSet results = qe.execSelect();
		while (results.hasNext()) {
			final QuerySolution qs = results.next();
			Iterator<String> it = new QuerySolutionIterator(qs);
			while (it.hasNext()) {
				System.out.println(qs.get(it.next()));
			}
		}
	}

}
