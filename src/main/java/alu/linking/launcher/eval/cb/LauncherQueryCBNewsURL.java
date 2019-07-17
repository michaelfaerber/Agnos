package alu.linking.launcher.eval.cb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.launcher.debug.LauncherTestQuery;
import alu.linking.utils.RDFUtils;
import alu.linking.utils.Stopwatch;

public class LauncherQueryCBNewsURL {

	public static void main(String[] args) throws IOException {
		final EnumModelType KG = EnumModelType.CRUNCHBASE2//
		;
		System.out.println("Running query for: " + KG.name());
		Stopwatch.start(LauncherQueryCBNewsURL.class.getName());
		final Dataset dataset = TDBFactory.createDataset(FilePaths.DATASET.getPath(KG));
		System.out.println("Finished loading!");
		Stopwatch.endOutputStart(LauncherTestQuery.class.getName());

		final Model model = dataset.getDefaultModel();
		System.out.println("Executing query...");
		Stopwatch.endOutputStart(LauncherQueryCBNewsURL.class.getName());
		final BufferedWriter bwOut = new BufferedWriter(
				new FileWriter(new File(FilePaths.FILE_CB_NEWS_URL.getPath(KG))));
		getCrunchbaseNews(model, bwOut);
		System.out.println("Finished!");
	}

	private static void getCrunchbaseNews(Model model, BufferedWriter bwOut) throws IOException {
		System.out.println("############################################");
		System.out.println("# News                                  #");
		System.out.println("############################################");
		final String queryStr = "SELECT ?s ?url WHERE {\n"
				+ "?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://ontologycentral.com/2010/05/cb/vocab#News> .\n"
				+ "?s <http://ontologycentral.com/2010/05/cb/vocab#url> ?url ." //
				+ "}\n";
		execQuery(model, queryStr, bwOut, "s", "url");
		System.out.println("---------------------------------------------");
	}

	private static void execQuery(Model model, String queryStr, final BufferedWriter bwOut, String... params)
			throws IOException {
		System.out.println("Executing");
		System.out.println(queryStr);
		final ResultSet results = RDFUtils.execQuery(model, queryStr);
		outputQuery(bwOut, results, params);
	}

	private static void outputQuery(final BufferedWriter bwOut, ResultSet results, String... params)
			throws IOException {
		final String sep = Strings.NEWS_URL_SEP.val;
		if (params == null || params.length == 0) {
			while (results.hasNext()) {
				final QuerySolution qs = results.next();
				Iterator<String> it = new de.dwslab.petar.walks.QuerySolutionIterator(qs);
				while (it.hasNext()) {
					final String varName = it.next();
					bwOut.append(qs.get(varName) + sep);
				}
				bwOut.newLine();
			}
		} else {
			while (results.hasNext()) {
				final QuerySolution qs = results.next();
				for (String varName : params) {
					bwOut.append(qs.get(varName) + sep);
				}
				bwOut.newLine();
			}
		}
	}

}
