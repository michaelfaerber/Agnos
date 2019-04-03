package alu.linking.disambiguation.pagerank;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Executable;
import alu.linking.utils.DecoderUtils;

public class PageRankLoader implements Executable {
	private final EnumModelType KG;
	public Map<String, Number> pagerankScores = null;
	public static Set<String> setPRNotFound = new HashSet<>();

	public PageRankLoader(final EnumModelType KG) {
		this.KG = KG;
	}

	@Override
	public void init() {
		// Nothing to do here
	}

	@Override
	public Map<String, Number> exec() throws IOException {
		return exec(null);
	}

	@Override
	public Map<String, Number> exec(Object... o) throws IOException {
		// Load PR from file
		this.pagerankScores = readIn(new File(FilePaths.FILE_PAGERANK.getPath(this.KG)));
		return this.pagerankScores;
	}

	@Override
	public boolean destroy() {
		this.pagerankScores.clear();
		this.pagerankScores = null;
		return true;
	}

	/**
	 * Returns the score if the pagerank was loaded
	 * 
	 * @param entity entity for which a score exists
	 * @return score associated to the entity
	 */
	public Number getScore(final String entity) {
		synchronized (this.pagerankScores) {
			if (this.pagerankScores == null || this.pagerankScores.size() == 0) {
				try {
					exec();
				} catch (IOException e) {
					getLogger().error(
							"IOException while loading PageRank scores from file (KG = " + this.KG.name() + ")", e);
				}
			}
		}
		final String entityURL = processKey(entity);
		Number ret = this.pagerankScores.get(entityURL);
		if (ret == null) {
			final String decoded = DecoderUtils.escapePercentage(entityURL);
			if (decoded != null && decoded.length() > 0) {
				ret = this.pagerankScores.get(decoded);
			}
		}
		return ret;
	}

	/**
	 * Takes all the possible assignments and checks for their PR scores and ranks
	 * them in a list (descendingly) and takes the topK defined
	 * 
	 * @param mention
	 * @param topK
	 * @return
	 */
	public List<AssignmentScore> getTopK(final Mention mention, final int topK) {
		return getTopK(mention.getPossibleAssignments(), topK);
	}

	public List<AssignmentScore> getTopK(final Mention mention, final int topK, final double minThreshold) {
		final List<AssignmentScore> topKList = getTopK(mention, topK);
		int cutOffIndex = -1;
		for (int i = 0; i < topKList.size(); ++i) {
			final AssignmentScore assScore = topKList.get(i);
			if (assScore.score.doubleValue() < minThreshold) {
				cutOffIndex = i;
				break;
			}
		}
		if (cutOffIndex == 0) {
			return null;
		}
		return topKList.subList(0, cutOffIndex);
	}

	/**
	 * Cuts off the passed (sorted) list if the score goes under the passed
	 * threshold<br>
	 * Returns NULL rather than an empty list if the first one already is below
	 * threshold
	 * 
	 * @param scores
	 * @param minThreshold
	 * @return
	 */
	public <T extends Comparable<T>> List<AssignmentScore> cutOff(final Collection<T> scores,
			final double minThreshold) {
		int cutOffIndex = -1;
		int counter = 0;
		List<AssignmentScore> assignmentScores = makeOrPopulateList(scores);
		Collections.sort(assignmentScores, Comparator.reverseOrder());
		for (AssignmentScore assScore : assignmentScores) {
			if (assScore.score.doubleValue() < minThreshold) {
				cutOffIndex = counter;
				break;
			}
			counter++;
		}
		if (cutOffIndex == -1) {
			// Means none of them was too small, so take them all!
			// cutOffIndex = scores.size();
			// getLogger().info("ALL scores are good 'enough':" + scores);
			return assignmentScores;
		}

		if (cutOffIndex == 0) {
			// getLogger().info("NULL - Too small Scores list:" + scores);
			return Lists.newArrayList();
		}
		// getLogger().info("LIMITED scores [0," + cutOffIndex + "]:" + scores);
		final List<AssignmentScore> retList = assignmentScores.subList(0, cutOffIndex);
		return retList;
	}

	public <T extends Comparable<T>> List<AssignmentScore> getTopK(final Collection<T> assignments, final int topK) {
		final List<AssignmentScore> assignmentScores = makeOrPopulateList(assignments);
		if (assignmentScores.size() == 0) {
			return assignmentScores;
		}
		Collections.sort(assignmentScores, Comparator.reverseOrder());
		return assignmentScores.subList(0, Math.min(assignmentScores.size(), topK));
	}

	public <T extends Comparable<T>> List<AssignmentScore> makeOrPopulateList(Collection<T> assignments) {
		final List<AssignmentScore> assignmentScores = Lists.newArrayList();
		for (T possAss : assignments) {
			if (possAss instanceof AssignmentScore) {
				assignmentScores.add((AssignmentScore) possAss);
			} else {
				Number foundScore = getScore(possAss.toString());
				if (foundScore == null) {
					//getLogger().error("[" + possAss.toString() + "] No PR score.");
					setPRNotFound.add(possAss.toString());
					foundScore = 0d;
				}

				final AssignmentScore assignmentScore = new AssignmentScore().assignment(possAss.toString())
						.score(foundScore);
				assignmentScores.add(assignmentScore);
			}
		}
		return assignmentScores;
	}

	/**
	 * Reads pagerank from a proper pagerank RDF file where the source is the node
	 * for which the object is the pagerank value of e.g. <a> <:PRValue> "50.23"
	 * 
	 * @param inFile
	 * @return
	 */
	public static Map<String, Number> readIn(final File inFile) {
		final Map<String, Number> map = new HashMap<String, Number>();
		try (BufferedReader brIn = Files.newBufferedReader(Paths.get(inFile.getPath()))) {
			final NxParser nxparser = new NxParser(brIn);
			while (nxparser.hasNext()) {
				final Node[] nodes = nxparser.next();
				try {
					final String key = processKey(nodes[0].toString());
					Number val;
					if ((val = map.get(key)) == null) {
						val = 0f;
					}
					map.put(key, val.floatValue() + Float.valueOf(nodes[2].toString()));
				} catch (ArrayIndexOutOfBoundsException aiooe) {
					getLog().error("Error appeared with: " + Arrays.toString(nodes));
					throw aiooe;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}

	private static String processKey(final String key) {
		return key.toLowerCase();
	}

	public static Logger getLog() {
		return org.apache.log4j.Logger.getLogger(PageRankLoader.class.getName());
	}
}
