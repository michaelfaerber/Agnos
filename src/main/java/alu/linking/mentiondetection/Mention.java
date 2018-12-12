package alu.linking.mentiondetection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import alu.linking.candidategeneration.PossibleAssignment;
import alu.linking.structure.Loggable;

public class Mention<N> implements Loggable {
	private final String mention;
	private final String source;
	private PossibleAssignment<N> assignment = null;
	private final int offset;
	private double detectionConfidence = -1;
	private Collection<PossibleAssignment<N>> possibleAssignments = null;
	private final String originalMention;

	Mention(final String word, final String source, final PossibleAssignment<N> assignment, final int offset,
			final double detectionConfidence, final String originalMention) {
		this.mention = word;
		this.source = source;
		this.assignment = assignment;
		this.offset = offset;
		this.detectionConfidence = detectionConfidence;
		this.originalMention = originalMention;
	}

	Mention(final String word, final String source, final PossibleAssignment<N> assignment, final int offset) {
		// -1 being as 'not set'
		this(word, source, assignment, offset, -1, word);
	}

	@Override
	public String toString() {
		return getMention();
	}

	/**
	 * Assigns this mention to a specific URL
	 * 
	 * @param assignment
	 */
	public void assignTo(final PossibleAssignment<N> assignment) {
		this.assignment = assignment;
	}

	public void assignBest() {
		final List<PossibleAssignment> listAssignments;
		if (possibleAssignments instanceof List) {
			listAssignments = (List) possibleAssignments;
		} else {
			listAssignments = new ArrayList<>(possibleAssignments);
		}
		Collections.sort(listAssignments, Comparator.reverseOrder());
		if (listAssignments.size() > 0) {
			assignTo(listAssignments.get(0));
		}
	}

	public PossibleAssignment<N> getAssignment() {
		return this.assignment;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Mention && obj != null) {
			@SuppressWarnings("rawtypes")
			final Mention m = ((Mention) obj);
			boolean ret = true;
			ret &= (m.assignment == null && this.assignment == null) ? true
					: ((m.assignment == null) ? false : this.assignment.equals(m.assignment));
			ret &= (m.getMention() == null && this.getMention() == null) ? true
					: ((m.getMention() == null || getMention() == null) ? false
							: this.getMention().equals(m.getMention()));
			ret &= (m.getSource() == null && this.getSource() == null) ? true
					: ((m.getSource() == null || getSource() == null) ? false : this.getSource().equals(m.getSource()));
			ret &= (m.getOriginalMention() == null && this.getOriginalMention() == null) ? true
					: ((m.getOriginalMention() == null || getOriginalMention() == null) ? false
							: this.getOriginalMention().equals(m.getOriginalMention()));
			ret &= (m.getDetectionConfidence() == this.getDetectionConfidence());

			return ret;
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return ((this.assignment == null) ? 1337 : this.assignment.hashCode())
				+ ((this.getMention() == null) ? 1241 : (this.getMention().hashCode()))
				+ ((this.getSource() == null) ? 7832 : this.getSource().hashCode());
	}

	public String getMention() {
		return mention;
	}

	public String getSource() {
		return source;
	}

	public int getOffset() {
		return offset;
	}

	public void updatePossibleAssignments(Collection<PossibleAssignment<N>> possibleAssignments) {
		this.possibleAssignments = possibleAssignments;
	}

	public Collection<PossibleAssignment<N>> getPossibleAssignments() {
		return this.possibleAssignments;
	}

	public double getDetectionConfidence() {
		return detectionConfidence;
	}

	public String getOriginalMention() {
		return this.originalMention;
	}

	/**
	 * Copies disambiguation results from given mention to this one <b>NOTE</b>:
	 * This only crushes the current mention's possible assignments by the passed
	 * one's. If disambiguation evolves to make differences even for mentions linked
	 * to the same detected word, then this should be changed or scores will not
	 * reflect the ideas properly.
	 * 
	 * @param fromMention
	 *            mention from which to copy
	 */
	public void copyResults(Mention<N> fromMention) {
		// Make sure it's the same mention word
		if (fromMention.getMention() == null || getMention() == null
				|| !getMention().equals(fromMention.getMention())) {
			getLogger().error(
					"Consistency error. Both mentions should have the same mention token in order to copy disambiguation results.");
		}
		updatePossibleAssignments(fromMention.getPossibleAssignments());
		assignTo(fromMention.getAssignment());
	}
}
