package alu.linking.disambiguation.scorers.embedhelp;

import java.util.Collection;
import java.util.List;

import alu.linking.mentiondetection.Mention;

public class HillClimbingPicker<S> implements ClusterItemPicker<S> {

	@Override
	public void linkContext(Collection<Mention<S>> context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateContext() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<S> combine() {
		//Choose an initial combination and improve on it at every step until it can no longer be improved
		//Try making it with a chain-like minimal distance logic
		//"Get smallest for one of current chains"
		return null;
	}

}
