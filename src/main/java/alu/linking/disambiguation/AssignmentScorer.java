package alu.linking.disambiguation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import alu.linking.candidategeneration.PossibleAssignment;
import alu.linking.config.constants.Numbers;
import alu.linking.config.kg.EnumModelType;
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Loggable;

/**
 * Scores assignments for ranking and topK extraction
 * 
 * @author Kwizzer
 *
 */
public class AssignmentScorer implements Loggable {

	private static Logger logger = Logger.getLogger(AssignmentScorer.class);
	private final HashSet<Mention> context = new HashSet<>();
	public final ScoreCombiner<PossibleAssignment> combiner;
	public final Collection<Scorer<PossibleAssignment>> scorers;
	public final Collection<PostScorer<PossibleAssignment, Mention>> postScorers;

	public AssignmentScorer(final ScoreCombiner<PossibleAssignment> combiner,
			Collection<Scorer<PossibleAssignment>> scorers,
			Collection<PostScorer<PossibleAssignment, Mention>> postScorers)
			throws FileNotFoundException, ClassNotFoundException, IOException {
		this.combiner = combiner;
		this.scorers = scorers;
		this.postScorers = postScorers;
		
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
		for (PossibleAssignment assgnmt : possAssignments) {
			// Multi thread here
			final Future<Integer> future = executor.submit(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					// assgnmt.computeScore();
					computeScore(assgnmt);
					return doneCounter.incrementAndGet();
				}
			});
		}
		executor.shutdown();
		long sleepCounter = 0l;
		final long sleepStep = 20l;
		do {
			// No need for await termination as this is pretty much it already...
			Thread.sleep(sleepStep);
			sleepCounter += sleepStep;
			if ((sleepCounter > 5_000) && ((sleepCounter % 5000) <= sleepStep)) {
				getLogger().debug(
						"Score Computation - In progress [" + doneCounter.get() + " / " + assSize + "] documents.");
			}
		} while (!executor.isTerminated());
		final boolean terminated = executor.awaitTermination(10L, TimeUnit.DAYS);
		if (!terminated) {
			throw new RuntimeException("Could not compute score in time.");
		}
		return mention.getPossibleAssignments();
	}

	/**
	 * Compute score for PossibleAssignment
	 * 
	 * @param assignment
	 */
	public void computeScore(PossibleAssignment assignment) {
		Number currScore = null;
		// Goes through all the scorers that have been defined and combines them in the
		// wanted manner
		// Pre-scoring step
		for (@SuppressWarnings("rawtypes")
		Scorer<PossibleAssignment> scorer : scorers) {
			currScore = combiner.combine(currScore, scorer, assignment);
		}
		// Post-scoring step
		for (@SuppressWarnings("rawtypes")
		PostScorer<PossibleAssignment, Mention> scorer : postScorers) {
			currScore = combiner.combine(currScore, scorer, assignment);
		}
		assignment.score(currScore);
		// return currScore;
	}

	/**
	 * Updates the context for post-scorers (required for proper functioning)
	 * 
	 * @param mentions
	 */
	public void updatePostContext(Collection<Mention> mentions) {
		this.context.clear();
		this.context.addAll(mentions);
		for (PostScorer postScorer : postScorers) {
			postScorer.updateContext(this.context);
		}
	}

}
