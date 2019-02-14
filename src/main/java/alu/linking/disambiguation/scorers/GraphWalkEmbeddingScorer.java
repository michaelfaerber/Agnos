package alu.linking.disambiguation.scorers;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import alu.linking.candidategeneration.PossibleAssignment;
import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.PostScorer;
import alu.linking.disambiguation.scorers.embedhelp.ClusterItemPicker;
import alu.linking.disambiguation.scorers.embedhelp.EntitySimilarityService;
import alu.linking.disambiguation.scorers.embedhelp.HillClimbingPicker;
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Loggable;
import alu.linking.utils.EmbeddingsUtils;
import alu.linking.utils.IDMappingLoader;
import alu.linking.utils.Stopwatch;

public class GraphWalkEmbeddingScorer<N> implements PostScorer<PossibleAssignment<N>, Mention<N>>, Loggable {
	private final Map<String, List<Number>> entityEmbeddingsMap;
	private boolean hasChanged = true;
	private final Set<N> bestCombination = new HashSet<>();
	private final String changedLock = "hasChangedLock";
	private final ClusterItemPicker<N> clusterHelper;

	public GraphWalkEmbeddingScorer(final Map<String, List<Number>> entityEmbeddingsMap,
			final EntitySimilarityService similarityService) {
		this.entityEmbeddingsMap = entityEmbeddingsMap;
		if (similarityService != null) {
			this.clusterHelper = new HillClimbingPicker(this.entityEmbeddingsMap, similarityService);
		} else {
			this.clusterHelper = new HillClimbingPicker(this.entityEmbeddingsMap);
		}

	}

	public GraphWalkEmbeddingScorer(final EnumModelType KG)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		// Whether to load it from a raw object-dump or a line-separated entity
		// embeddings output
		final boolean RAW_LOAD = false;
		if (RAW_LOAD) {
			this.entityEmbeddingsMap = rawload(
					FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS_RAWMAP.getPath(KG));
		} else {
			this.entityEmbeddingsMap = humanload(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_HUMAN.getPath(KG),
					FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS.getPath(KG));
		}

		// clusterHelper = new GreedyOptimalPicker<N>(this.entityEmbeddingsMap);
		// clusterHelper = new SubPageRankPicker<N>(this.entityEmbeddingsMap);
		this.clusterHelper = new HillClimbingPicker(this.entityEmbeddingsMap);
	}

	/**
	 * Load entity embeddings from a human readable file and translate the entity
	 * mappings directly to the fully-qualified IRIs
	 * 
	 * @return the fully-qualified entity embeddings
	 * @throws IOException
	 */
	public static Map<String, List<Number>> humanload(final String mappingInPath, final String embeddingInPath)
			throws IOException {
		IDMappingLoader<String> entityMapping = new IDMappingLoader<String>().loadHumanFile(new File(mappingInPath));
		final File embedFile = new File(embeddingInPath);
		log().info("Loading embeddings from: " + embedFile.getAbsolutePath());
		Stopwatch.start(GraphWalkEmbeddingScorer.class.getName());
		final Map<String, List<Number>> entityEmbeddingsMap = EmbeddingsUtils.readEmbeddings(embedFile, entityMapping,
				true);
		log().info("Finished(" + Stopwatch.endOutput(GraphWalkEmbeddingScorer.class.getName())
				+ " ms.) loading embeddings from: " + embedFile.getAbsolutePath());
		entityMapping = null;
		return entityEmbeddingsMap;
	}

	private static Logger log() {
		return Logger.getLogger(GraphWalkEmbeddingScorer.class.getName());
	}

	/**
	 * Loads embeddings from a raw file
	 * 
	 * @param inPathRaw
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private Map<String, List<Number>> rawload(final String inPathRaw)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		Map<String, List<Number>> helperMap = null;
		try {
			try (final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(inPathRaw)))) {
				helperMap = (Map<String, List<Number>>) ois.readObject();
			}
		} catch (EOFException eof) {
			getLogger().error("Exception!", eof);
		}
		return helperMap;
		// finally {
		// getLogger().info("HelperMap: " + helperMap);
		// this.entityEmbeddingsMap = helperMap;
		// }
	}

	@Override
	public Number computeScore(PossibleAssignment<N> assignment) {
		synchronized (changedLock) {
			if (hasChanged) {
				recomputeOptimum();
			}
		}

		if (bestCombination.contains(assignment.toString())) {
			// This assignment is in the final wanted combination!
			return 1;
		} else {
			// This assignment is NOT in the final wanted combination.
			return 0;
		}
	}

	/**
	 * Recomputes the best combination of entities to minimize distance
	 */
	private synchronized void recomputeOptimum() {
		try {
			bestCombination.clear();
			bestCombination.addAll(clusterHelper.combine());
			getLogger().info("Recomputeoptimum combo: ");
			getLogger().info(bestCombination);
		} catch (Exception exc) {
//			getLogger().error(exc);
//			getLogger().error(exc.getMessage());
//			getLogger().error(exc.getCause());
//			getLogger().error(exc.getLocalizedMessage());
			getLogger().error(exc.getMessage(), exc);
		}
		hasChanged = false;
	}

	@Override
	public Number getWeight() {
		return 20f;
	}

	@Override
	public void linkContext(Collection<Mention<N>> context) {
		clusterHelper.linkContext(context);
	}

	@Override
	public void updateContext() {
		synchronized (changedLock) {
			hasChanged = true;
			clusterHelper.updateContext();
		}
	}

}
