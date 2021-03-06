package alu.linking.disambiguation;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

import alu.linking.config.constants.EnumEmbeddingMode;
import alu.linking.config.kg.EnumModelType;
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.PossibleAssignment;

/**
 * Handles different scorers for pre- and post-scoring. <br>
 * Handles disambiguation as a whole.
 * 
 * @author Kwizzer
 *
 * @param
 */
public class AssignmentChooser {
	private static Logger logger = Logger.getLogger(AssignmentChooser.class);
	private AssignmentScorer scorer = null;

	public AssignmentChooser(final EnumModelType KG) throws ClassNotFoundException, IOException {
		this(KG, EnumEmbeddingMode.DEFAULT.val);
	}

	public AssignmentChooser(final EnumModelType KG, final EnumEmbeddingMode enumEmbeddingMode)
			throws ClassNotFoundException, IOException {
		this.scorer = new AssignmentScorer(KG, enumEmbeddingMode);
		// Graph.getInstance().readIn(FilePaths.FILE_HOPS_GRAPH_DUMP.getPath(KG),
		// FilePaths.FILE_HOPS_GRAPH_DUMP_PATH_IDS.getPath(KG),
		// FilePaths.FILE_HOPS_GRAPH_DUMP_EDGE_IDS.getPath(KG));
	}

	/**
	 * Calls the scorer appropriately and returns a list of the assignments
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	private Collection<PossibleAssignment> score(final Mention mention) throws InterruptedException {
		// logger.debug(mention.getMention() + " / " + mention.getSource());
		return scorer.score(mention);
	}

	/**
	 * Assigns the proper values to the mentions
	 * 
	 * @param mentions
	 * @throws InterruptedException
	 */
	public void choose(final List<Mention> mentions) throws InterruptedException {
		// Update context for post-scoring (does it for all linked post-scorers)
		scorer.updatePostContext(mentions);
		// Score the possible assignments for each detected mention

		// In order to avoid disambiguating multiple times for the same mention word, we
		// split our mentions up and then just copy results from the ones that were
		// computed
		final Map<String, List<Mention>> mentionMap = new HashMap<>();
		// Split up the mentions by their keys
		for (final Mention mention : mentions) {
			List<Mention> val;
			if ((val = mentionMap.get(mention.getMention())) == null) {
				val = Lists.newArrayList();
				mentionMap.put(mention.getMention(), val);
			}
			val.add(mention);
		}
		for (final Map.Entry<String, List<Mention>> e : mentionMap.entrySet()) {
			// Just score the first one within the lists
			final List<Mention> sameWordMentions = e.getValue();
			final Mention mention = sameWordMentions.get(0);
			score(mention);
			// Assign the top-scored possible assignment to the mention
			mention.assignBest();
			// Copy into the other mentions
			for (int i = 1; i < e.getValue().size(); ++i) {
				// Skip the first one as it's just time lost...
				final Mention sameWordMention = sameWordMentions.get(i);
				sameWordMention.copyResults(mention);
			}
		}
		/*
		 * for (Mention m : mentions) { m.assignBest(); }
		 */
	}

	public AssignmentScorer getAssignmentScorer() {
		return this.scorer;
	}
}
