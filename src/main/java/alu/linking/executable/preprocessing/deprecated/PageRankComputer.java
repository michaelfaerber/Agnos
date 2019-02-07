package alu.linking.executable.preprocessing.deprecated;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.structure.Executable;
import eu.wdaqua.pagerank.PageRankRDF;

public class PageRankComputer implements Executable {

	final EnumModelType KG;

	public PageRankComputer(final EnumModelType KG) {
		this.KG = KG;
	}

	@Override
	public void init() {

	}

	@Override
	public boolean reset() {
		return false;
	}

	private void pagerank() throws IOException {
		// String dump = ".\\resources\\data\\extended_graph\\literal_address_city.txt";
		// String in = FilePaths.TEST_FILE_PAGERANKRDF_EXAMPLE3_IN.path;
		// String out = FilePaths.TEST_FILE_PAGERANKRDF_EXAMPLE3_OUT.path;
		final String in = FilePaths.FILE_EXTENDED_GRAPH.getPath(KG);
		final String out = FilePaths.FILE_PAGERANK.getPath(KG);
		System.out.println(in);

		PageRankRDF pageRankRDF = new PageRankRDF(in, 0.50, 1.0, 50, true);
		pageRankRDF.compute();
		try (PrintWriter wrt = new PrintWriter(new BufferedWriter(new FileWriter(new File(out))))) {
			pageRankRDF.printPageRankScoresRDF(wrt);
		}
		// List<PageRankScore> scores = pageRankRDF.getPageRankScores();
		// for (PageRankScore score : scores) {
		// System.out.println(score.node + " - " + score.pageRank);
		// }
	}

	@Override
	public <T> T exec(Object... o) {
		try {
			pagerank();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean destroy() {
		return false;
	}

	@Override
	public String getExecMethod() {
		return "pagerank";
	}

}
