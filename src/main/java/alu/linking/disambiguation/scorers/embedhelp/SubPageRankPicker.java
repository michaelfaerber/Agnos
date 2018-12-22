package alu.linking.disambiguation.scorers.embedhelp;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import alu.linking.mentiondetection.Mention;

public class SubPageRankPicker<S> implements ClusterItemPicker<S> {

	private Collection<Mention<S>> context;
	
	@Override
	public void linkContext(Collection<Mention<S>> context) {
		this.context = context;
	}

	@Override
	public void updateContext() {
		//Nothing?
	}

	@Override
	public List<S> combine() {
		final List<S> ret = Lists.newArrayList();
		
		return null;
	}

}
