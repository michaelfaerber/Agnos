package eu.wdaqua.pagerank;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.jena.graph.Triple;
import org.apache.jena.riot.lang.PipedRDFIterator;

import com.beust.jcommander.internal.Lists;

public class PageRankRDF implements PageRank {

	private double dampingFactor = 0.85D;
	private double startValue = 0.1D;
	private int numberOfIterations = 40;
	private Collection<String> dumps;
	private HashMap<String, Double> pageRankScores = new HashMap();
	private boolean literals;

	public PageRankRDF(String dump) {
		this.dumps = Lists.newArrayList(dump);
	}

	public PageRankRDF(String dump, double dampingFactor, double startValue, int numberOfIterations, boolean literals) {
		this(Lists.newArrayList(dump), dampingFactor, startValue, numberOfIterations, literals);
	}

	public PageRankRDF(String dump, double dampingFactor, double startValue, int numberOfIterations) {
		this(Lists.newArrayList(dump), dampingFactor, startValue, numberOfIterations, false);
	}

	public PageRankRDF(String[] dumps, double dampingFactor, double startValue, int numberOfIterations,
			boolean literals) {
		this(Arrays.asList(dumps), dampingFactor, startValue, numberOfIterations, literals);
	}

	public PageRankRDF(Collection<String> dumps, double dampingFactor, double startValue, int numberOfIterations,
			boolean literals) {
		this.dumps = dumps;
		this.dampingFactor = dampingFactor;
		this.startValue = startValue;
		this.numberOfIterations = numberOfIterations;
		this.literals = literals;
	}

	public void compute() {

		// Compute the number of outgoing edges
		final HashMap<String, Integer> numberOutgoing = new HashMap<>();
		final HashMap<String, ArrayList<String>> incomingPerPage = new HashMap<String, ArrayList<String>>();
		long time = System.currentTimeMillis();
		final long initTime = time;
		for (String dump : this.dumps) {
			PipedRDFIterator<Triple> iter = Parser.parse(dump);
			while (iter.hasNext()) {
				Triple t = iter.next();
				if (literals || t.getObject().isURI()) {
					ArrayList<String> incoming = incomingPerPage.get(t.getObject().toString());
					if (incoming == null) {
						incoming = new ArrayList<>();
						incomingPerPage.put(t.getObject().toString(), incoming);
					}
					ArrayList<String> incoming2 = incomingPerPage.get(t.getSubject().toString());
					if (incoming2 == null) {
						incomingPerPage.put(t.getSubject().toString(), new ArrayList<>());
					}
					incoming.add(t.getSubject().toString());
					Integer numberOut = numberOutgoing.get(t.getSubject().toString());
					if (numberOut == null) {
						numberOutgoing.put(t.getSubject().toString(), Integer.valueOf(1));
					} else {
						numberOutgoing.put(t.getSubject().toString(), Integer.valueOf(numberOut.intValue() + 1));
					}
				}
			}
			iter.close();
			System.err.println("Reading input(" + dump + ") took " + (System.currentTimeMillis() - time) / 1000L + "s");
			time = System.currentTimeMillis();
		}

		System.err.println("Computing PageRank: " + numberOfIterations + " iterations, damping factor " + dampingFactor
				+ ", start value " + startValue + ", considering literals " + literals);

		Set<String> keyset = incomingPerPage.keySet();
		System.err.println("Iteration ...");
		for (int j = 1; j <= numberOfIterations; j++) {
			System.err.print(j + " ");
			for (String string : keyset) {
				ArrayList<String> incomingLinks = incomingPerPage.get(string);

				double pageRank = 1.0D - dampingFactor;
				for (String inLink : incomingLinks) {
					Double pageRankIn = (Double) pageRankScores.get(inLink);
					if (pageRankIn == null) {
						pageRankIn = Double.valueOf(startValue);
					}
					int numberOut = ((Integer) numberOutgoing.get(inLink)).intValue();
					pageRank += dampingFactor * (pageRankIn.doubleValue() / numberOut);
				}
				pageRankScores.put(string, Double.valueOf(pageRank));
			}
		}
		System.err.println();

		System.err.println("Computing PageRank took " + (System.currentTimeMillis() - time) / 1000L + "s");
		System.err.println("Total execution time: " + (System.currentTimeMillis() - initTime) / 1000L + "s");
	}

	public List<PageRankScore> getPageRankScores() {
		List<PageRankScore> scores = new ArrayList<PageRankScore>();
		Set<String> keysetNew = pageRankScores.keySet();
		for (String string : keysetNew) {
			PageRankScore s = new PageRankScore();
			s.node = string;
			s.pageRank = pageRankScores.get(string);
			scores.add(s);
		}
		return scores;
	}

	public void printPageRankScoresTSV(PrintWriter writer) {
		Set<String> keysetNew = pageRankScores.keySet();
		for (String string : keysetNew) {
			writer.println(string + "\t" + String.format("%.10f", pageRankScores.get(string)));
		}
	}

	public void printPageRankScoresRDF(PrintWriter writer) {
		Set<String> keysetNew = pageRankScores.keySet();
		for (String string : keysetNew) {
			writer.println("<" + string + "> <http://purl.org/voc/vrank#pagerank> \""
					+ String.format("%.10f", pageRankScores.get(string))
					+ "\"^^<http://www.w3.org/2001/XMLSchema#float> .");
		}
	}
}
