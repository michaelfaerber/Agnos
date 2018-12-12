package alu.linking.structure;

import org.semanticweb.yars.nx.Node;

public interface MentionPossibilityProcessor {
	public void mentionPossibility(final String s, final String p, final String o);
	public void mentionPossibility(final Node s, final Node p, final Node o);
}
