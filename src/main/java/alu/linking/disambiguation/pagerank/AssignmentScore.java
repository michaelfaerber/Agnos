package alu.linking.disambiguation.pagerank;

public class AssignmentScore implements Comparable<AssignmentScore> {
	public Number score;
	public String assignment;

	@Override
	public int compareTo(AssignmentScore o) {
		return (int) (10_000 * (this.score.doubleValue() - o.score.doubleValue()));
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
