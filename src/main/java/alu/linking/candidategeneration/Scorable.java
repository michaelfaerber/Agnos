package alu.linking.candidategeneration;

/**
 * Interface allowing objects to be scored based on a computeScore() function
 * 
 * @author Kristian Noullet
 *
 */
public interface Scorable {
	/**
	 * Compute the score this scorable object should receive
	 * @return score
	 */
	public Number computeScore();
}
