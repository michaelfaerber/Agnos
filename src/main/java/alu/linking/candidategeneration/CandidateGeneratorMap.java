package alu.linking.candidategeneration;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.internal.Lists;

import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Loggable;

public class CandidateGeneratorMap implements CandidateGenerator<String>, Loggable {
	private final Map<String, Collection<String>> linking;

	public CandidateGeneratorMap(Map<String, Collection<String>> linking) {
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
		final Collection<String> possibleEntities = this.linking.get(mention.getMention());
		if (possibleEntities == null) {
			getLogger().error("Could not find any such mention(" + mention.getMention() + ") o.o");
			return null;
		}
		final List<PossibleAssignment> ret = Lists.newArrayList();
		for (String entity : possibleEntities) {
			ret.add(PossibleAssignment.createNew(entity, mention.getMention()));
		}
		return ret;
	}
}
