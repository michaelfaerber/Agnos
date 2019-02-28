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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Executable;

public class PageRankLoader implements Executable {
	private final EnumModelType KG;
	public Map<String, Number> pagerankScores = null;

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
		return this.pagerankScores.get(entity);
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

	public <T> List<AssignmentScore> getTopK(final Collection<T> assignments, final int topK,
			final double minThreshold) {
		final List<AssignmentScore> topKList = getTopK(assignments, topK);
		int cutOffIndex = -1;
		for (int i = 0; i < topKList.size(); ++i) {
			final AssignmentScore assScore = topKList.get(i);
			if (assScore.score.doubleValue() < minThreshold) {
				cutOffIndex = i;
				break;
			}
		}
		if (cutOffIndex == -1) {
			//Means none of them was too small
			cutOffIndex = topKList.size();
		}
		
		if (cutOffIndex == 0) {
			return null;
		}
		return topKList.subList(0, cutOffIndex);
	}

	public <T> List<AssignmentScore> getTopK(final Collection<T> assignments, final int topK) {
		final List<AssignmentScore> assignmentScores = Lists.newArrayList();
		for (T possAss : assignments) {
			assignmentScores
					.add(new AssignmentScore().assignment(possAss.toString()).score(getScore(possAss.toString())));
		}
		Collections.sort(assignmentScores, Comparator.reverseOrder());
		return assignmentScores.subList(0, Math.min(assignmentScores.size(), topK));
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
					map.put(nodes[0].toString(), Float.valueOf(nodes[2].toString()));
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

	public static Logger getLog() {
		return org.apache.log4j.Logger.getLogger(PageRankLoader.class.getName());
	}
}
