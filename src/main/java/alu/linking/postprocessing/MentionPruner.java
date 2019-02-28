package alu.linking.postprocessing;

import java.util.Collection;
import java.util.List;

import alu.linking.mentiondetection.Mention;

public interface MentionPruner {
	public List<Mention> prune(final Collection<Mention> mentions);
}
