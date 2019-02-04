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

import alu.linking.candidategeneration.PossibleAssignment;
import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.PostScorer;
import alu.linking.disambiguation.scorers.embedhelp.ClusterItemPicker;
import alu.linking.disambiguation.scorers.embedhelp.SubPageRankPicker;
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Loggable;
import alu.linking.utils.EmbeddingsUtils;
import alu.linking.utils.IDMappingLoader;

public class GraphWalkEmbeddingScorer<N> implements PostScorer<PossibleAssignment<N>, Mention<N>>, Loggable {
	private final EnumModelType KG;
	private final Map<String, List<Number>> entityEmbeddingsMap;
	private boolean hasChanged = true;
	private Collection<Mention<N>> context;
	private final Set<N> bestCombination = new HashSet<>();
	private final String changedLock = "hasChangedLock";
	private final ClusterItemPicker<N> clusterHelper;

	public GraphWalkEmbeddingScorer(final EnumModelType KG)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		this.KG = KG;
		// Whether to load it from a raw object-dump or a line-separated entity
		// embeddings output
		final boolean RAW_LOAD = false;
		if (RAW_LOAD) {
			Map<String, List<Number>> helperMap = null;
			try {
				try (final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
						new File(FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS_RAWMAP.getPath(KG))))) {
					helperMap = (Map<String, List<Number>>) ois.readObject();
				}
			} catch (EOFException eof) {
				System.out.println("Exception!");
			} finally {
				System.out.println("HelperMap: " + helperMap);
				this.entityEmbeddingsMap = helperMap;
			}
		} else {
			IDMappingLoader<String> entityMapping = new IDMappingLoader<String>()
					.loadHumanFile(new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_HUMAN.getPath(KG)));
			this.entityEmbeddingsMap = EmbeddingsUtils.readEmbeddings(
					new File(FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS.getPath(KG)), entityMapping);
			entityMapping = null;
		}

		// clusterHelper = new GreedyOptimalPicker<N>(this.entityEmbeddingsMap);
		clusterHelper = new SubPageRankPicker<N>(this.entityEmbeddingsMap);
	}

	@Override
	public Number computeScore(PossibleAssignment<N> assignment) {
		synchronized (changedLock) {
			if (hasChanged) {
				recomputeOptimum();
			}
		}
		if (bestCombination.contains(assignment.toString())) {
			// This assignment is similar to the final wanted combination!
			return 1;
		} else {
			// This assignment is NOT similar to the final wanted combination.
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
		this.context = context;
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
