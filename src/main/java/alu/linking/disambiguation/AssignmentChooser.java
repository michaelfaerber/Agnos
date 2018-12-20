package alu.linking.disambiguation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

import alu.linking.candidategeneration.PossibleAssignment;
import alu.linking.config.kg.EnumModelType;
import alu.linking.mentiondetection.Mention;

/**
 * Handles different scorers for pre- and post-scoring. <br>
 * Handles disambiguation as a whole.
 * 
 * @author Kwizzer
 *
 * @param <N>
 */
public class AssignmentChooser<N> {
	private static Logger logger = Logger.getLogger(AssignmentChooser.class);
	private AssignmentScorer<N> scorer = null;

	public AssignmentChooser(final EnumModelType KG, final File pageRankFile)
			throws ClassNotFoundException, IOException {
		this.scorer = new AssignmentScorer<N>(KG, pageRankFile);
		// Graph.getInstance().readIn(FilePaths.FILE_HOPS_GRAPH_DUMP.getPath(KG),
		// FilePaths.FILE_HOPS_GRAPH_DUMP_PATH_IDS.getPath(KG),
		// FilePaths.FILE_HOPS_GRAPH_DUMP_EDGE_IDS.getPath(KG));
	}
	public AssignmentChooser(final EnumModelType KG)
			throws ClassNotFoundException, IOException {
		this.scorer = new AssignmentScorer<N>(KG);
	}

	/**
	 * Calls the scorer appropriately and returns a list of the assignments
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	private Collection<PossibleAssignment<N>> score(final Mention<N> mention) throws InterruptedException {
		// logger.debug(mention.getMention() + " / " + mention.getSource());
		return scorer.score(mention);
	}

	/**
	 * Assigns the proper values to the mentions
	 * 
	 * @param mentions
	 * @throws InterruptedException
	 */
	public void choose(final List<Mention<N>> mentions) throws InterruptedException {
		// Update context for post-scoring (does it for all linked post-scorers)
		scorer.updatePostContext(mentions);
		// Score the possible assignments for each detected mention

		// In order to avoid disambiguating multiple times for the same mention word, we
		// split our mentions up and then just copy results from the ones that were
		// computed
		final Map<String, List<Mention<N>>> mentionMap = new HashMap<>();
		// Split up the mentions by their keys
		for (final Mention<N> mention : mentions) {
			List<Mention<N>> val;
			if ((val = mentionMap.get(mention.getMention())) == null) {
				val = Lists.newArrayList();
				mentionMap.put(mention.getMention(), val);
			}
			val.add(mention);
		}
		for (final Map.Entry<String, List<Mention<N>>> e : mentionMap.entrySet()) {
			// Just score the first one within the lists
			final List<Mention<N>> sameWordMentions = e.getValue();
			final Mention<N> mention = sameWordMentions.get(0);
			score(mention);
			// Assign the top-scored possible assignment to the mention
			mention.assignBest();
			// Copy into the other mentions
			for (int i = 1; i < e.getValue().size(); ++i) {
				// Skip the first one as it's just time lost...
				final Mention<N> sameWordMention = sameWordMentions.get(i);
				sameWordMention.copyResults(mention);
			}
		}
		/*
		 * for (Mention<N> m : mentions) { m.assignBest(); }
		 */
	}

}
