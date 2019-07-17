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
import alu.linking.disambiguation.PostScorer;
import alu.linking.disambiguation.scorers.embedhelp.ClusterItemPicker;
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Loggable;
import alu.linking.utils.EmbeddingsUtils;
import alu.linking.utils.IDMappingLoader;
import alu.linking.utils.Stopwatch;

/**
 * A wrapper class for embeddings-based scorers by instantiating it with a
 * cluster item picker which does most of the heavy lifting
 * 
 * @author Kristian Noullet
 *
 */
public class GraphWalkEmbeddingScorer implements PostScorer<PossibleAssignment, Mention>, Loggable {
	private boolean hasChanged = true;
	private final Set<String> bestCombination = new HashSet<>();
	private final String changedLock = "hasChangedLock";
	private final ClusterItemPicker clusterHelper;

	public GraphWalkEmbeddingScorer(final ClusterItemPicker entityPicker) {
		this.clusterHelper = entityPicker;
		this.clusterHelper.printExperimentSetup();
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
	public Number computeScore(PossibleAssignment assignment) {
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
			// getLogger().info("Recomputeoptimum combo: ");
			// getLogger().info(bestCombination);
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
		return this.clusterHelper.getPickerWeight();
		// return 20f;
	}

	@Override
	public void linkContext(Collection<Mention> context) {
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
