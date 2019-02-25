package alu.linking.disambiguation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import alu.linking.candidategeneration.PossibleAssignment;
import alu.linking.candidategeneration.Scorable;
import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Numbers;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.scorers.GraphWalkEmbeddingScorer;
import alu.linking.disambiguation.scorers.PageRankScorer;
import alu.linking.disambiguation.scorers.embedhelp.EntitySimilarityService;
import alu.linking.disambiguation.scorers.embedhelp.HillClimbingPicker;
import alu.linking.disambiguation.scorers.embedhelp.SubPageRankPicker;
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Loggable;

/**
 * Scores assignments for ranking and topK extraction
 * 
 * @author Kwizzer
 *
 */
public class AssignmentScorer<N> implements Loggable {
	private static Logger logger = Logger.getLogger(AssignmentScorer.class);
	private final HashSet<Mention> context = new HashSet<>();

	public AssignmentScorer(final EnumModelType KG) throws FileNotFoundException, ClassNotFoundException, IOException {
		// Determines how everything is scored!
		PossibleAssignment.setScoreCombiner(new ScoreCombiner<PossibleAssignment>());

		// Pre-scoring
		PossibleAssignment.addScorer(new PageRankScorer(KG));

		// Post-scoring
		// PossibleAssignment.addPostScorer(new VicinityScorer());
		final Map<String, List<Number>> entityEmbeddingsMap = GraphWalkEmbeddingScorer.humanload(
				FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_HUMAN.getPath(KG),
				FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS.getPath(KG));
		final EntitySimilarityService similarityService = new EntitySimilarityService(entityEmbeddingsMap);
		PossibleAssignment.addPostScorer(new GraphWalkEmbeddingScorer(new HillClimbingPicker(similarityService)));
		PossibleAssignment.addPostScorer(new GraphWalkEmbeddingScorer(new SubPageRankPicker(similarityService, 0.8d)));
		// PossibleAssignment.addPostScorer(new SSPEmbeddingScorer(KG));

		for (PostScorer postScorer : PossibleAssignment.getPostScorers()) {
			// Links a context object which will be updated when necessary through
			// updateContext(Collection<Mention<N>>)
			postScorer.linkContext(context);
		}
	}

	/**
	 * Scores all the assignments (possible sources for a particular mention /
	 * surface form) in order to detect which source / entity a particular mention
	 * should be attributed to<br>
	 * Applies pre-scoring & post-scoring
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public Collection<PossibleAssignment> score(final Mention mention) throws InterruptedException {
		// Now score all of the assignments based on their own characteristics
		// and on the contextual ones
		Collection<PossibleAssignment> possAssignments = mention.getPossibleAssignments();
		final int assSize = possAssignments.size();
		final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
				.newFixedThreadPool(Numbers.SCORER_THREAD_AMT.val.intValue());
		AtomicInteger doneCounter = new AtomicInteger(0);
		for (Scorable assgnmt : possAssignments) {
			// Multi thread here
			final Future<Integer> future = executor.submit(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					assgnmt.computeScore();
					return doneCounter.incrementAndGet();
				}
			});
		}
		executor.shutdown();
		long sleepCounter = 0l;
		do {
			// No need for await termination as this is pretty much it already...
			Thread.sleep(100);
			sleepCounter += 100l;
			if ((sleepCounter > 5_000) && ((sleepCounter % 5000) <= 100)) {
				getLogger().debug(
						"Score Computation - In progress [" + doneCounter.get() + " / " + assSize + "] documents.");
			}
		} while (!executor.isTerminated());
		final boolean terminated = executor.awaitTermination(10L, TimeUnit.MINUTES);
		if (!terminated) {
			throw new RuntimeException("Could not compute score in time.");
		}
		return mention.getPossibleAssignments();
	}

	/**
	 * Updates the context for post-scorers (required for proper functioning)
	 * 
	 * @param mentions
	 */
	public void updatePostContext(Collection<Mention> mentions) {
		this.context.clear();
		this.context.addAll(mentions);
		for (PostScorer postScorer : PossibleAssignment.getPostScorers()) {
			postScorer.updateContext();
		}
	}
}
