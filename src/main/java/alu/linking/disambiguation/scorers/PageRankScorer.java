package alu.linking.disambiguation.scorers;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;

import alu.linking.candidategeneration.PossibleAssignment;
import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Numbers;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.Scorer;
import alu.linking.disambiguation.pagerank.PageRankLoader;
import alu.linking.utils.Stopwatch;

public class PageRankScorer<K> implements Scorer<PossibleAssignment<K>> {
	private static Logger logger = Logger.getLogger(PageRankScorer.class);
	private static Map<String, Number> pageRankMap = null;
	private final EnumModelType KG;
	private int warnCounter = 0;

	public PageRankScorer(final EnumModelType KG) {
		this(KG, false);
	}

	public PageRankScorer(final EnumModelType KG, final boolean forceReload) {
		this(KG, forceReload, new File(FilePaths.FILE_PAGERANK.getPath(KG)));
	}

	public PageRankScorer(final EnumModelType KG, final File pageRankFile) {
		this(KG, false, pageRankFile);
	}

	/**
	 * Loads PageRank only once unless forced through the forceReload param
	 * 
	 * @param forceReload
	 */
	public PageRankScorer(final EnumModelType KG, final boolean forceReload, final File pageRankFile) {
		this.KG = KG;
		if (forceReload || pageRankMap == null) {
			// Only load pagerank once (or it will takes ages for the same result)
			Stopwatch.start("pagerankloading");
			pageRankMap = loadPageRank(pageRankFile);
			Stopwatch.endOutput("pagerankloading");
		}
	}

	@Override
	public Number computeScore(PossibleAssignment param) {
		final Object assignment = param.getAssignment();
		if (assignment != null) {
			final Number retNumber = pageRankMap.get(assignment.toString());
			if (retNumber == null) {
				warnCounter++;
				if (warnCounter % 10_000 == 0) {
					logger.warn(
							warnCounter + " - No page rank value found for: Assignment(" + assignment.toString() + ")");
				}
			}
			return retNumber == null ? 0f : retNumber;
		} else {
			logger.error("Assignment is NULL");
		}
		return 0f;
	}

	/**
	 * Loads the pagerank map from the default location
	 * 
	 * @return
	 */
	public Map<String, Number> loadPageRank(final File inputFile) {
		return new PageRankLoader().readIn(inputFile);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		return hashCode() == obj.hashCode();
	}

	@Override
	public int hashCode() {
		return getClass().getName().hashCode();
	}

	@Override
	public Number getWeight() {
		return Numbers.PAGERANK_WEIGHT.val;
	}

}
