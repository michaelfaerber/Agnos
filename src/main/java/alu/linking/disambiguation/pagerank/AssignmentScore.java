package alu.linking.disambiguation.pagerank;

import alu.linking.executable.preprocessing.loader.PageRankLoader;

/**
 * A score assignment - used majorly by {@link PageRankLoader}. Each
 * AssignmentScore has a score and an assignment it is related to and can be
 * compared with a precision of 10^-4.<br>
 * Mainly used for comparing PR scores in order to apply Top K type logic on it
 * 
 * @author Kristian Noullet
 *
 */
public class AssignmentScore implements Comparable<AssignmentScore> {
	public Number score;
	public String assignment;

	@Override
	public int compareTo(AssignmentScore o) {
		return (int) (Math.round((10_000 * (this.score.doubleValue() - o.score.doubleValue()))));
	}

	public AssignmentScore assignment(String assignment) {
		this.assignment = assignment;
		return this;
	}

	public AssignmentScore score(final Number score) {
		this.score = score;
		return this;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(this.assignment);
		sb.append(",");
		sb.append(this.score);
		sb.append("]");
		return sb.toString();
	}

}
