package alu.linking.executable.debug;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.ext.com.google.common.collect.Lists;
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
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;

public class LiteralPredicateAggregator {

	public static void main(String[] args) {
		try {
			EnumModelType KG = EnumModelType.DEFAULT;
			final String outPath = "./used_predicates_" + KG.name() + ".txt";
			new LiteralPredicateAggregator().execQuery(KG, outPath);
			System.out.println("Outputting results to: " + outPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void execQuery(EnumModelType KG, final String outPath) throws IOException {
		final Dataset dataset = TDBFactory.createDataset(FilePaths.DATASET.getPath(KG));
		final Model model = dataset.getDefaultModel();

		// Load query/-ies
		final String queryStr = "select distinct ?p where {?s ?p ?o . FILTER(isLiteral(?o)) }";
		List<BufferedWriter> writers = Lists.newArrayList();
		try (final BufferedWriter bwQuery = new BufferedWriter(new FileWriter(outPath))) {
			// Query the dataset and (1) output query outputs and (2) output alternate
			// channel data (e.g. linking)
			writers.add(bwQuery);
			execSelectQuery(queryStr, model, writers);
		}
	}

	public void execSelectQuery(final String queryStr, Model model, final List<BufferedWriter> writers)
			throws IOException {
		final Query query = QueryFactory.create(queryStr);
		// Execute the query and obtain results
		final QueryExecution qe = QueryExecutionFactory.create(query, model);
		final ResultSet results = qe.execSelect();

		// Iterate through returned triples
		while (results.hasNext()) {
			final QuerySolution result = results.next();
			processResultLine(result, writers);
		}
		qe.close();
	}

	protected void processResultLine(QuerySolution result, List<BufferedWriter> writers) throws IOException {
		final Iterator<String> itVars = result.varNames();
		while (itVars.hasNext()) {
			final String varName = itVars.next();
			final String value = result.get(varName).toString();
			// Output to query results
			outputMainChannel(varName, value, itVars.hasNext(), writers.get(0));
		}
	}

	protected void outputMainChannel(String varName, String value, boolean hasNext, BufferedWriter writer)
			throws IOException {
		writer.write(value + (hasNext ? "\t" : Strings.NEWLINE.val));
	}

}
