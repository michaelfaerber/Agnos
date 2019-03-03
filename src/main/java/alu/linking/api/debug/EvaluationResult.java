package alu.linking.api.debug;

import java.util.List;

public class EvaluationResult {
	public String inputText;
	public double precision, recall, f1;
	public String inputFile;
	public List<TestMarking> goldStandard, foundMarkings;
}
