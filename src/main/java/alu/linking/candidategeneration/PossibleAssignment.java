package alu.linking.candidategeneration;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.yars.nx.Resource;

import alu.linking.disambiguation.PostScorer;
import alu.linking.disambiguation.ScoreCombiner;
import alu.linking.disambiguation.Scorer;
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Loggable;

public class PossibleAssignment implements Scorable, Comparable<PossibleAssignment>, Loggable {
	private static Logger logger = Logger.getLogger(PossibleAssignment.class);
	private Number score = Float.valueOf(0f);
	private final String assignment;
	private final String mentionSource, mentionToken;
	private boolean computedScore = false;
	private boolean warned = false;
	@SuppressWarnings("rawtypes")
	private static Set<Scorer<PossibleAssignment>> scorers = new HashSet<>();
	@SuppressWarnings("rawtypes")
	private static Set<PostScorer<PossibleAssignment, Mention>> postScorers = new HashSet<>();
	@SuppressWarnings("rawtypes")
	private static ScoreCombiner<PossibleAssignment> combiner = null;

	public static Set<Scorer<PossibleAssignment>> getScorers() {
		return scorers;
	}

	public static Set<PostScorer<PossibleAssignment, Mention>> getPostScorers() {
		return postScorers;
	}

	@SuppressWarnings("rawtypes")
	public static PossibleAssignment createNew(final String assignment, final String mentionSource,
			final String mentionToken) {
		return new PossibleAssignment(new Resource(assignment, false).toN3(), mentionSource, mentionToken);
	}

	public PossibleAssignment(final String assignment, final String mentionSource, final String mentionToken) {
		this.assignment = assignment;
		this.mentionSource = mentionSource;
		this.mentionToken = mentionToken;
	}

	/**
	 * Adds a scorer for disambiguation
	 * 
	 * @param scorer
	 */
	public static void addScorer(@SuppressWarnings("rawtypes") final Scorer<PossibleAssignment> scorer) {
		scorers.add(scorer);
	}

	public static void addPostScorer(
			@SuppressWarnings("rawtypes") final PostScorer<PossibleAssignment, Mention> scorer) {
		postScorers.add(scorer);
	}

	public static void setScoreCombiner(
			@SuppressWarnings("rawtypes") final ScoreCombiner<PossibleAssignment> combiner) {
		PossibleAssignment.combiner = combiner;
	}

	@Override
	public int compareTo(final PossibleAssignment o) {
		return Double.compare(this.score.doubleValue(), o.score.doubleValue());
	}

	public String getAssignment() {
		return assignment;
	}

	@Override
	public Number computeScore() {
		Number currScore = null;
		// Goes through all the scorers that have been defined and combines them in the
		// wanted manner
		// Pre-scoring step
		for (@SuppressWarnings("rawtypes")
		Scorer<PossibleAssignment> scorer : scorers) {
			currScore = combiner.combine(currScore, scorer, this);
		}
		// Post-scoring step
		for (@SuppressWarnings("rawtypes")
		PostScorer<PossibleAssignment, Mention> scorer : postScorers) {
			currScore = combiner.combine(currScore, scorer, this);
		}
		this.score = currScore;
		computedScore = true;
		return currScore;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PossibleAssignment) {
			@SuppressWarnings("rawtypes")
			final PossibleAssignment ass = ((PossibleAssignment) obj);
			return this.score.equals(ass.score) && this.assignment.equals(ass.assignment);
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return this.assignment.hashCode();
	}

	@Override
	public String toString() {
		return this.assignment.toString();
	}

	public Number getScore() {
		if (!computedScore && !warned) {
			// Warns only once per possible assignment
			logger.warn("Score has not yet been computed.");
			warned = true;
		}
		return this.score;
	}

	public String getMentionSource() {
		return mentionSource;
	}

	public String getMentionToken() {
		return mentionToken;
	}
}
