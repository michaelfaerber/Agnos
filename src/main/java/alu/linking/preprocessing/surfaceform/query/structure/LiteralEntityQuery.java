package alu.linking.preprocessing.surfaceform.query.structure;

import java.io.BufferedWriter;
import java.io.File;
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
import alu.linking.config.kg.EnumModelType;
import alu.linking.preprocessing.surfaceform.query.QuerySolutionIterator;
import alu.linking.utils.FileUtils;

public abstract class LiteralEntityQuery {
	protected final EnumModelType KG;

	protected LiteralEntityQuery(EnumModelType KG) {
		this.KG = KG;
	}

	public void execQueries() throws IOException {
		final Dataset dataset = TDBFactory.createDataset(FilePaths.DATASET.getPath(KG));
		final Model model = dataset.getDefaultModel();

		try (final BufferedWriter bwAlternate = initAlternateChannelWriter()) {
			for (File f : new File(getQueryInputDir()).listFiles()) {
				if (f.isDirectory())
					continue;
				// Load query/-ies
				final String queryStr = FileUtils.getContents(f);
				List<BufferedWriter> writers = Lists.newArrayList();
				try (final BufferedWriter bwQuery = new BufferedWriter(
						new FileWriter(getQueryOutDir() + "/" + f.getName()))) {
					// Query the dataset and (1) output query outputs and (2) output alternate
					// channel data (e.g. linking)
					writers.add(bwQuery);
					if (bwAlternate != null) {
						writers.add(bwAlternate);
					}
					execSelectQuery(queryStr, model, writers);
				}
			}
		}
	}

	/**
	 * 
	 * @param result  Result to be output
	 * @param writers Writers[0]: General results output; Writer[1..n]: Specialised
	 *                alternate channel outputs
	 * @throws IOException
	 */
	protected void processResultLine(QuerySolution result, List<BufferedWriter> writers) throws IOException {
		final Iterator<String> itVars = new QuerySolutionIterator(result);
		while (itVars.hasNext()) {
			final String varName = itVars.next();
			final String value = result.get(varName).toString();
			// Output to query results
			outputMainChannel(varName, value, itVars.hasNext(), writers.get(0));
			// Output to query linking when appropriate
			outputAlternateChannels(varName, value, itVars.hasNext(), writers.subList(1, writers.size()));
		}
	}

	/**
	 * Executes passed query on specified model, writing returned triples out
	 * appropriately (as specified by parameters)
	 * 
	 * @param queryStr      query to be executed
	 * 
	 * @param model         model on which to execute passed query
	 * 
	 * @param outputLinking Writer outputting specific linking results in a
	 *                      main-memory-friendly way
	 * @param outputQuery   Writer outputting all query results to appropriate file
	 * @throws IOException
	 */
	protected void execSelectQuery(final String queryStr, Model model, final List<BufferedWriter> writers)
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

	protected abstract void outputMainChannel(String varName, String value, boolean hasNext, BufferedWriter writer)
			throws IOException;

	protected abstract void outputAlternateChannels(String varName, String value, boolean hasNext,
			List<BufferedWriter> writers) throws IOException;

	protected abstract BufferedWriter initAlternateChannelWriter() throws IOException;

	protected abstract String getQueryInputDir();

	protected abstract String getQueryOutDir();

}
