package alu.linking.disambiguation.scorers;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import alu.linking.config.constants.Numbers;
import alu.linking.disambiguation.PostScorer;
import alu.linking.disambiguation.hops.graph.Graph;
import alu.linking.disambiguation.hops.graph.GraphNode;
import alu.linking.disambiguation.hops.pathbuilding.ConcurrentPathBuilderBatch;
import alu.linking.disambiguation.hops.pathbuilding.ConcurrentPathBuilderSingle;
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.PossibleAssignment;

/**
 * Context-based scorer making use of an in-memory graph and node connectivities
 * 
 * @author Kristian Noullet
 *
 */
public class VicinityScorer implements PostScorer<PossibleAssignment, Mention> {
	private Logger logger = Logger.getLogger(getClass());
	private double sigma_ratio = Numbers.VICINITY_SCORING_WEIGHT_SIGMA.val.doubleValue();
	private Collection<Mention> context;
	private final Graph<Integer> graph;
	private final Set<Integer> goalNodesSet = new HashSet<>();

	public VicinityScorer() {
		// Initialize graph for path building
		this.graph = Graph.getInstance();
		//this.graph = new DirectedHoppableSparseGraph<String, String>();
	}

	@Override
	public Number computeScore(final PossibleAssignment assignment) {
		double retScore = 0d;
		// We have an assignment and we check the neighborhood through the graph
		// Take the assignment's source as a source node
		final HashMap<Integer, GraphNode<Integer>> allNodesMap = graph.getNodes();
		final HashSet<Integer> fromNodes = new HashSet<>();
		final HashMap<Integer, GraphNode<Integer>> fromNodesMap = new HashMap<>();
		// Get the ID for the source of this mention as a source node for the graph
		// traversal
		// Not completely sure whether nodeURL should be the assignment's URL or the
		// mention's, but since we are doing disambiguation for entity linking here, it
		// seems more likely that it should be from the assignment
		final String nodeURL = assignment.getAssignment().toString();
		final Integer startNodeID = graph.getIDMapping().getKey(nodeURL);
		if (startNodeID == null) {
			logger.error("No node found for " + nodeURL);
			logger.debug("\t->Skipping path building for: Assignment(" + assignment + ") - " + nodeURL);
			return 0;
		}
		// Add mention's source as a 'starting'/'from' node
		if (sigma_ratio > 1) {
			logger.warn("Hop-Scoring decline weight(" + sigma_ratio + ") greater than 1 (negative score possible).");
		}
		final boolean STRING_ALTERNATIVE = false;
		if (STRING_ALTERNATIVE) {
			fromNodesMap.put(startNodeID, allNodesMap.get(startNodeID));
			boolean outputPaths = true;
			boolean outputEdges = false;
			boolean outputDirections = false;
			boolean outputToList = true;
			final List<String> paths = new ConcurrentPathBuilderBatch().concurrentBuild(allNodesMap, fromNodesMap,
					goalNodesSet, outputPaths, outputEdges, outputDirections, outputToList);
			return -1d;
		} else {
			fromNodes.add(startNodeID);
			final Deque<LinkedList<Integer>> paths = new ConcurrentPathBuilderSingle().concurrentBuild(allNodesMap,
					fromNodes, goalNodesSet);
			// logger.debug(nodeURL + "(" + startNodeID + ") - Found paths(" + paths.size()
			// + "):\n" + paths);
			return pathworth(paths);
		}
	}

//	private double pathworth(List<String> paths) {
//		double retScore = 0d;
//		for (String path : paths) {
//			final String[] pathNodes = path.trim().split(" ");
//			retScore += pathworth(pathNodes);
//		}
//		return retScore;
//	}

	private double pathworth(Collection<LinkedList<Integer>> paths) {
		// Initially a path is worth 100% of the usual worth
		double retScore = 0d;
		for (Collection path : paths) {
			double pathWorth = 1d;
			// ((1.0d) / ((double) (path.split(" ").length - 1)));
			for (double i = 0; i < path.size() - 2; ++i) {
				// Starts at index 1 due to the first node being the source node
				pathWorth -= (sigma_ratio * pathWorth);
			}
			retScore += pathWorth;
		}
		return retScore;
	}

	/**
	 * Updates context based on linked context
	 */
	@Override
	public void updateContext() {
		goalNodesSet.clear();
		// Update what nodes can be used as goal nodes
		for (Mention contextMention : context) {
			for (PossibleAssignment possibleAssignment : contextMention.getPossibleAssignments()) {
				final String nodeURL = possibleAssignment.getAssignment().toString();
				final Integer endNodeID = graph.getIDMapping().getKey(nodeURL);
				if (endNodeID != null) {
					goalNodesSet.add(endNodeID);
				}
			}
		}
	}

	@Override
	public void linkContext(Collection<Mention> context) {
		this.context = context;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		return hashCode() == obj.hashCode();
	}

	@Override
	public int hashCode() {
		return getClass().getName().hashCode();
	}

	@Override
	public Number getWeight() {
		return Numbers.VICINITY_WEIGHT.val;
	}

}
