package alu.linking.disambiguation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

import alu.linking.candidategeneration.PossibleAssignment;
import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.pagerank.PageRankLoader;
import alu.linking.disambiguation.scorers.GraphWalkEmbeddingScorer;
import alu.linking.disambiguation.scorers.PageRankScorer;
import alu.linking.disambiguation.scorers.embedhelp.ClusterItemPicker;
import alu.linking.disambiguation.scorers.embedhelp.EntitySimilarityService;
import alu.linking.disambiguation.scorers.hillclimbing.HillClimbingPicker;
import alu.linking.mentiondetection.Mention;

/**
 * Handles different scorers for pre- and post-scoring. <br>
 * Handles disambiguation as a whole.
 * 
 * @author Kwizzer
 *
 * @param
 */
public class AssignmentChooser {
	public enum CombineOperation {
		OCCURRENCE(ClusterItemPicker::occurrenceOperation), //
		MAX_SIM(ClusterItemPicker::maxedOperation), //
		SIM_ADD(ClusterItemPicker::similarityOperation), //
		SIM_SQUARE_ADD(ClusterItemPicker::similaritySquaredOperation),//

		;
		final BiFunction<Double, Double, Double> combineOperation;

		CombineOperation(BiFunction<Double, Double, Double> combineOperation) {
			this.combineOperation = combineOperation;
		}
	}

	private final EntitySimilarityService similarityService;
	private final EnumModelType KG;

	private static Logger logger = Logger.getLogger(AssignmentChooser.class);
	// How to load pagerank
	final PageRankLoader pagerankLoader;

	public AssignmentChooser(final EnumModelType KG) throws ClassNotFoundException, IOException {
		this.KG = KG;
		// Do all the heavy pre-loading
		final Map<String, List<Number>> entityEmbeddingsMap = GraphWalkEmbeddingScorer.humanload(
				FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_HUMAN.getPath(KG),
				FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS.getPath(KG));
		this.similarityService = new EntitySimilarityService(entityEmbeddingsMap);

		// Graph.getInstance().readIn(FilePaths.FILE_HOPS_GRAPH_DUMP.getPath(KG),
		// FilePaths.FILE_HOPS_GRAPH_DUMP_PATH_IDS.getPath(KG),
		// FilePaths.FILE_HOPS_GRAPH_DUMP_EDGE_IDS.getPath(KG));

		// Load it once
		pagerankLoader = new PageRankLoader(KG);
		// Loads the pagerank from file
		pagerankLoader.exec();
	}

	/**
	 * Assigns the proper values to the mentions. Instantiates a new
	 * AssignmentScorer for each time this method is called. As such, there should
	 * be no overlapping of contexts due to repeated AssignmentChooser instance
	 * reuse.
	 * 
	 * @param mentions
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws FileNotFoundException
	 */
	public void choose(final List<Mention> mentions)
			throws InterruptedException, FileNotFoundException, ClassNotFoundException, IOException {
		final ScoreCombiner<PossibleAssignment> combiner = new ScoreCombiner<PossibleAssignment>();
		final Collection<Scorer<PossibleAssignment>> scorers = Lists.newArrayList();
		final Collection<PostScorer<PossibleAssignment, Mention>> postScorers = Lists.newArrayList();
		// #############
		// Pre-scoring
		scorers.add(new PageRankScorer(KG, pagerankLoader));
		// #############

		
		// #############
		// Post-scoring
		// PossibleAssignment.addPostScorer(new VicinityScorer());
		final CombineOperation combineOperation = CombineOperation.OCCURRENCE;

		// postScorer.add(new GraphWalkEmbeddingScorer(new
		// ContinuousHillClimbingPicker(combineOperation.combineOperation,
		// similarityService, pagerankLoader)));
		postScorers.add(new GraphWalkEmbeddingScorer(
				new HillClimbingPicker(combineOperation.combineOperation, similarityService, pagerankLoader)));

		// postScorer.add(new GraphWalkEmbeddingScorer(new
		// PairwisePicker(combineOperation.combineOperation, similarityService,
		// pagerankLoader)));

		// postScorer.add(new GraphWalkEmbeddingScorer(new
		// SubPageRankPicker(similarityService, 0.5d)));
		// postScorer.add(new SSPEmbeddingScorer(KG));
		// #############

		final AssignmentScorer assignmentScorer = new AssignmentScorer(combiner, scorers, postScorers);

		// Update context for post-scoring (does it for all linked post-scorers)
		assignmentScorer.updatePostContext(mentions);

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
			assignmentScorer.score(mention);
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

	public EntitySimilarityService getSimilarityService() {
		return this.similarityService;
	}

}
