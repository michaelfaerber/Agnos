package de.dwslab.petar.jena.io;

import java.io.File;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.util.FileManager;

public class BulkLoader {

	public static void loadDBpeida(String storageDir, String inputDir) {

		System.out.println("WE start the code!!");
		Dataset dataset = TDBFactory.createDataset(storageDir);

		// assume we want the default model, or we could get a named model here
		Model tdb = dataset.getDefaultModel();

		// read the input file - only needs to be done once

		File folder = new File(inputDir);
		if (folder.listFiles().length > 0)
			System.out.println("We are at the correct file");
		for (final File fileEntry : folder.listFiles()) {

			System.out.println(fileEntry.getName());
			if (fileEntry.getName().contains(".bz2"))
				continue;
			System.out.println(fileEntry.getAbsolutePath());
			System.out.println(fileEntry.getPath());
			try {
				FileManager.get().readModel(tdb, fileEntry.getPath(),
						"N-TRIPLES");
			} catch (Exception e) {
				System.out.println("File didn't finish");
				e.printStackTrace();
			}
		}
		try {
			// run a query
			String q = "select (count(distinct ?s) as ?c) where {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t} ";
			Query query = QueryFactory.create(q);
			QueryExecution qexec = QueryExecutionFactory.create(query, tdb);
			ResultSet results = qexec.execSelect();
			ResultSetFormatter.out(System.out, results, query);
		} catch (Exception e) {
			System.out.println("The stupid query failed");
		}
		dataset.close();
	}

	public static void main(String[] args) {
		//loadDBpeida(args[0], args[1]);
		// loadDBpeida(
		// "dbpediaTDBrepo",
		// "C:\\Users\\petar\\Documents\\ProjectsFiles\\DeepLearning\\Datasets\\Original\\DBpediaTTsadL");
		Dataset dataset = TDBFactory.createDataset(args[0]);
		Model tdb = dataset.getDefaultModel();
		try {
			// run a query
			String q = "Select distinct ?t Where { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t . FILTER(STRSTARTS(STR(?s), \"http://www.wikidata.org/entity/Q\"))}";
			Query query = QueryFactory.create(q);
			QueryExecution qexec = QueryExecutionFactory.create(query, tdb);
			ResultSet results = qexec.execSelect();
			ResultSetFormatter.out(System.out, results, query);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
