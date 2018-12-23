package alu.linking.disambiguation.scorers.embedhelp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.jena.ext.com.google.common.collect.Lists;

import alu.linking.utils.EmbeddingsUtils;

public class ScorerGraph {
	final Map<String, List<Number>> embeddings;
	final Map<String, ScorerGraphNode> nodes = new HashMap<String, ScorerGraphNode>();
	public Double defaultValue = 1d;
	// Whether neighbours should be unique, aka. node A and B are just connected to
	// each other once (even if attempted to be added multiple times)
	private boolean uniqueNeighbours = true;
	// How many iterations
	private int iter = 5;
	// Likelihood of following a link
	private Number damping = 0.85d;

	protected ScorerGraph(Map<String, List<Number>> embeddings) {
		this.embeddings = embeddings;
	}

	public void addNode(final String nodeName) {
		addNode(nodeName, -1);
	}

	public void addNode(final String nodeName, final int groupID) {
		nodes.put(nodeName, new ScorerGraphNode(nodeName, groupID));
	}

	public void addSuccessor(final ScorerGraphNode nodeSource, final ScorerGraphNode nodeDest) {
		nodeSource.addSuccessor(nodeDest);
	}

	public void addPredecessor(final ScorerGraphNode nodeSource, final ScorerGraphNode nodeDest) {
		nodeSource.addPredecessor(nodeDest);
	}

	public void addSuccessor(final String nodeName, final String successorName) {
		addSuccessor(nodes.get(nodeName), nodes.get(successorName));
	}

	public void addPredecessor(final String nodeName, final String predecessorName) {
		addPredecessor(nodes.get(nodeName), nodes.get(predecessorName));
	}

	class ScorerGraphNode {
		private Number value = defaultValue;
		private String nodeName;
		private List<ScorerGraphNode> predecessors = Lists.newArrayList();
		private List<ScorerGraphNode> successors = Lists.newArrayList();
		private List<Number> predecessorWeights = Lists.newArrayList();
		private List<Number> successorWeights = Lists.newArrayList();
		private int groupID;

		private ScorerGraphNode(final String nodeName) {
			this(nodeName, -1);
		}

		private ScorerGraphNode(final String nodeName, final int groupID) {
			this.nodeName = nodeName;
			this.groupID = groupID;
		}

		private void addSuccessor(final ScorerGraphNode successor) {
			addNext(successor, successors, successorWeights);
		}

		private void addPredecessor(final ScorerGraphNode predecessor) {
			addNext(predecessor, predecessors, predecessorWeights);
		}

		private void addNext(ScorerGraphNode next, List<ScorerGraphNode> nexts, List<Number> nextWeights) {
			if (next == null) {
				throw new RuntimeException(
						"NULL next node passed (successor/predecessor node likely does not exist in the nodes map)");
			}
			if (uniqueNeighbours && nexts.indexOf(next) != -1) {
				return;
			}

			final Number weight = EmbeddingsUtils.cosineSimilarity(embeddings.get(this.nodeName),
					embeddings.get(next.nodeName));
			if (weight == null || weight.doubleValue() < 0 || weight.doubleValue() > 1) {
				throw new RuntimeException("Invalid weight");
			}

			// Adds weight and successor
			nextWeights.add(weight);
			nexts.add(next);
		}
	}

	public ScorerGraph startValue(Number num) {
		this.defaultValue = num.doubleValue();
		return this;
	}

	public ScorerGraph iterations(final int iterations) {
		this.iter = iterations;
		return this;
	}

	public ScorerGraph dampingFactor(final Number damping) {
		this.damping = damping;
		return this;
	}

	public ScorerGraph uniqueNeighbours(final boolean unique) {
		this.uniqueNeighbours = unique;
		return this;
	}

	/**
	 * Computes pagerank on constructed graph
	 */
	public void pagerank() {
		final double d = damping.doubleValue();
		final double N = (double) nodes.size();
		final Map<ScorerGraphNode, Number> iterMap = new HashMap<>(nodes.size());
		for (int i = 0; i < this.iter; ++i) {
			for (Map.Entry<String, ScorerGraphNode> node : nodes.entrySet()) {
				double predecessorPRSum = 0d;
				final ScorerGraphNode currNodeVal = node.getValue();
				final List<ScorerGraphNode> prevNodes = node.getValue().predecessors;
				for (int prevIndex = 0; prevIndex < prevNodes.size(); ++prevIndex) {
					final ScorerGraphNode prevNode = prevNodes.get(prevIndex);
					predecessorPRSum += currNodeVal.predecessorWeights.get(prevIndex).doubleValue()
							* (prevNode.value.doubleValue() / ((double) (prevNode.successors.size())));
				}
				final Number PR = (double) ((1.0d - d) / N + d * predecessorPRSum);
				iterMap.put(node.getValue(), PR);
			}

			// Update the values within the graph
			for (Map.Entry<ScorerGraphNode, Number> e : iterMap.entrySet()) {
				e.getKey().value = e.getValue();
			}

			// Clean items within iterMap
			iterMap.clear();
		}
	}

	/**
	 * Return the sorted map once pagerank has finished computing
	 * 
	 * @return
	 */
	public Map<String, Double> sortByScore(final boolean reverse) {
		final Map<String, Double> helperMap = new HashMap<String, Double>();
		for (Map.Entry<String, ScorerGraphNode> e : nodes.entrySet()) {
			helperMap.put(e.getKey(), e.getValue().value.doubleValue());
		}
		return sortByValue(helperMap, reverse);
	}

	/**
	 * Returns the best value for each cluster
	 * 
	 * @return map containing one immutable pair per cluster (cluster ID determined
	 *         by order of passed cluster during populating iteration)
	 */
	public Map<Integer, ImmutablePair<String, Double>> topByGroup() {
		Map<Integer, ImmutablePair<String, Double>> retMap = new HashMap<>();
		// Sorted by ascending order, meaning the last ones are the biggest ones
		Map<String, Double> scoreSorted = sortByScore(false);
		for (Map.Entry<String, Double> e : scoreSorted.entrySet()) {
			retMap.put(nodes.get(e.getKey()).groupID, new ImmutablePair<String, Double>(e.getKey(), e.getValue()));
		}
		return retMap;
	}

	/**
	 * Sort map by value
	 * 
	 * @param map
	 * @return
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, final boolean reverse) {
		List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
		if (reverse) {
			list.sort(Entry.comparingByValue(Comparator.reverseOrder()));
		} else {
			list.sort(Entry.comparingByValue(Comparator.naturalOrder()));
		}
		Map<K, V> result = new LinkedHashMap<>();
		for (Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	/**
	 * Populates and interconnects nodes as required
	 * 
	 * @param clusters
	 * @return
	 */
	public ScorerGraph populate(final Map<String, List<String>> clusters) {
		int groupCounter = 0;
		for (Map.Entry<String, List<String>> e : clusters.entrySet()) {
			for (String IRI : e.getValue()) {
				addNode(IRI, groupCounter);
			}
			groupCounter++;
		}
		connectDifferentGroups();
		return this;
	}

	/**
	 * Nodes from one cluster are connected to nodes of all other clusters
	 * @return
	 */
	public ScorerGraph connectDifferentGroups() {
		for (Map.Entry<String, ScorerGraphNode> e1 : nodes.entrySet()) {
			for (Map.Entry<String, ScorerGraphNode> e2 : nodes.entrySet()) {
				if (e1.getValue().groupID != e2.getValue().groupID) {
					// Different groups, so connect them to each other
					// Left to right
					ScorerGraph.this.addSuccessor(e1.getValue(), e2.getValue());
					ScorerGraph.this.addPredecessor(e2.getValue(), e1.getValue());
					// Right to left
					ScorerGraph.this.addSuccessor(e2.getValue(), e1.getValue());
					ScorerGraph.this.addPredecessor(e1.getValue(), e2.getValue());
				}
			}
		}
		return this;
	}

}
