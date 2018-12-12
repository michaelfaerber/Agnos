package alu.linking.candidategeneration;

import java.util.List;

import alu.linking.mentiondetection.Mention;

public interface CandidateGenerator<N> {
	public List<PossibleAssignment<N>> generate(Mention<N> mention);
}
