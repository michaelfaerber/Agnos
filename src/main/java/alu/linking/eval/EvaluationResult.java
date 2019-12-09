package alu.linking.eval;

import java.util.Set;

/**
 * Container object for annotation tasks, used for the appropriate evaluation of
 * the framework
 * 
 * @author Kristian Noullet
 *
 */
public class EvaluationResult {
	public String inputText;
	public double precision, recall, f1;
	public String inputFile;
	public int tp, fp, tn, fn;
	public static int globalTP = 0, globalFP = 0, globalTN = 0, globalFN = 0;
	public Set<TestMarking> goldStandard, foundMarkings;
}
