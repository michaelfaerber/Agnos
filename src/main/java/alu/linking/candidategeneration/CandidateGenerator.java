package alu.linking.candidategeneration;

import java.util.List;

import alu.linking.mentiondetection.Mention;

/**
 * Simple interface for candidate generators with one method -
 * 'generate(Mention), taking an input mention and returning a list of
 * candidates for it
 * 
 * @author Kristian Noullet
 *
 */
public interface CandidateGenerator {
	/**
	 * Takes a mention and returns a list of possible assignments (=candidates) for
	 * it <br>
	 * Note: Enforced list rather than collection due to the different assumptions
	 * that may be made in regards to the annotation procedure, whether the used
	 * object is a set or a list
	 * 
	 * @param mention Mention for which to find candidates
	 * @return List of candidates
	 */
	public List<PossibleAssignment> generate(Mention mention);
}
