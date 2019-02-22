package alu.linking.executable.preprocessing.deprecated;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.beust.jcommander.internal.Lists;

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

	private void pagerank(final Collection<String> inPaths) throws IOException {
		// String dump = ".\\resources\\data\\extended_graph\\literal_address_city.txt";
		// String in = FilePaths.TEST_FILE_PAGERANKRDF_EXAMPLE3_IN.path;
		// String out = FilePaths.TEST_FILE_PAGERANKRDF_EXAMPLE3_OUT.path;
		// final String in = FilePaths.FILE_EXTENDED_GRAPH.getPath(KG);
		final String out = FilePaths.FILE_PAGERANK.getPath(KG);

		final List<String> inFiles = Lists.newArrayList();
		for (String inPath : inPaths) {
			final File inFile = new File(inPath);
			if (inFile.isFile()) {
				inFiles.add(inFile.getAbsolutePath());
			} else if (inFile.isDirectory()) {
				for (File f : inFile.listFiles()) {
					if (f.isFile()) {
						inFiles.add(f.getAbsolutePath());
					}
				}
			}
		}
		final PageRankRDF pageRankRDF = new PageRankRDF(inFiles, 0.85, 1.0, 50, true);
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
			if (o instanceof String[]) {
				pagerank(Arrays.asList((String[]) o));
			}
			final List<String> files = Lists.newArrayList();
			for (Object obj : o) {
				if (obj instanceof String) {
					files.add((String) obj);
				} else if (obj instanceof String[]) {
					files.addAll(Arrays.asList((String[]) obj));
				} else if (obj instanceof Collection) {
					for (Object subObj : ((Collection) obj)) {
						if (subObj instanceof String) {
							files.add(((String) subObj));
						}
					}
				}
			}
			pagerank(files);
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
