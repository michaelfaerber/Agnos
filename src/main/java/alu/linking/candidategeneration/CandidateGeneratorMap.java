package alu.linking.candidategeneration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.yars.nx.Node;

import com.beust.jcommander.internal.Lists;

import alu.linking.mentiondetection.Mention;

public class CandidateGeneratorMap implements CandidateGenerator {
	private final Map<String, Set<String>> linking;

	public CandidateGeneratorMap(Map<String, Set<String>> linking) {
		this.linking = linking;
	}

	/**
	 * Generates all possible assignments for given mention
	 * 
	 * @param mention
	 * @return set of possible assignments
	 */
	@Override
	public List<PossibleAssignment> generate(Mention mention) {
		final Set<String> possibleEntities = this.linking.get(mention.getMention());
		if (possibleEntities == null) {
			System.out.println("Could not find any such mention(" + mention.getMention() + ") o.o");
			return null;
		}
		final List<PossibleAssignment> ret = Lists.newArrayList();
		for (String entity : possibleEntities) {
			ret.add(PossibleAssignment.createNew(entity, mention.getSource(), mention.getMention()));
		}
		return ret;
	}
}
