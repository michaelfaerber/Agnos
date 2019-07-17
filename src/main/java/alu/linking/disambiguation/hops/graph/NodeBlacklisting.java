package alu.linking.disambiguation.hops.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Class handling blacklisting of specific node within our in-memory graph
 * <br>
 * See {@link EdgeBlacklisting} for the edge equivalent.
 * @author Kristian Noullet
 *
 */
public class NodeBlacklisting {
	private Graph<Integer> graph = null;
	private Logger logger = Logger.getLogger(getClass());

	private static class NodeBlacklistingWrapper {
		static NodeBlacklisting INSTANCE = new NodeBlacklisting(Graph.getInstance());
	}

	/**
	 * By default created a blacklisting associated to the Graph singleton, but
	 * another can be specified with {@link #switchGraph(Graph)}
	 * 
	 * @return instance of NodeBlackListing
	 */
	public NodeBlacklisting getInstance() {
		return NodeBlacklistingWrapper.INSTANCE;
	}

	public NodeBlacklisting(final Graph<Integer> graph) {
		this.graph = graph;
	}

	private final Set<String> blacklistURL = new HashSet<String>();
	private final Set<Integer> blacklistID = new HashSet<Integer>();

	/**
	 * Changes what graph this blacklisting should be applied on
	 * 
	 * @param graph
	 */
	public void switchGraph(final Graph<Integer> graph) {
		this.graph = graph;
	}

	/**
	 * Checks whether all that is required has been loaded into the graph.<br>
	 * We need a graph's nodes and ID mappings to be loaded for this to work, hence
	 * we check for them.
	 * 
	 * @return true : valid, false : invalid
	 */
	public boolean checkValidity() {
		int graphSize = this.graph.getNodes().size();
		int mappingSize = this.graph.getIDMapping().size();
		boolean ret = true;
		if (graphSize == 0) {
			ret = false;
			System.err.println("Nodes need to be loaded.");
		}
		if (mappingSize == 0) {
			ret = false;
			System.err.println("Mappings need to be loaded.");
		}
		return ret;
	}

	/**
	 * Enforces blacklisting on Graph singleton<br>
	 * If given parameter is 'not a String', but an Integer number in String format,
	 * it tries to recover it through conversion.<br>
	 * <b>Requires</b>: Nodes and ID mappings to be loaded into Graph<br>
	 * 
	 * @param URL
	 * @throws RuntimeException
	 *             When the graph is not yet properly loaded
	 */
	private void enforceOnGraph(final String URL) {
		ifUncheckedThrowException();
		final Integer id = this.graph.getIDMapping().getKey(URL);
		if (id == null) {
			// -> there is no such URL, try to recover it though
			Integer idFromString = null;
			try {
				idFromString = new Integer(URL);
			} catch (NumberFormatException e) {
				// Could not convert URL to integer
				idFromString = null;
			}
			if (idFromString != null) {
				final String graphNodeURL = this.graph.getIDMapping().get(idFromString);
				if (this.graph.getNodes().containsKey(idFromString) && graphNodeURL != null) {
					// -> Yay, we managed to recover! We found a node that was
					// passed as a String even though it's an Integer in String
					// form...
					// So what do we do now? We go and remove it from the Graph,
					// every tiny last trace of it!
					removeFromGraph(idFromString);
				}
			} else {
				// Nothing to recover... / Couldn't recover
			}
		} else {
			// It exists and we got the ID -> remove it
			if (this.graph.getNodes().containsKey(id)) {
				removeFromGraph(id);
			}
		}
	}

	/**
	 * Enforces blacklisting on graph singleton with passed node ID
	 * 
	 * @param ID
	 *            Identifier of node that should be blacklisted from the graph
	 */
	private void enforceOnGraph(final Integer ID) {
		ifUncheckedThrowException();
		if (this.graph.getNodes().containsKey(ID)) {
			removeFromGraph(ID);
		}
	}

	/**
	 * Checks whether everything required is initialised and calls
	 * {@link #removeFromGraph(Set)}
	 * 
	 * @param IDs
	 */
	private void enforceOnGraph(final Set<Integer> IDs) {
		ifUncheckedThrowException();
		removeFromGraph(IDs);
	}

	/**
	 * Alternative to enforcing blacklisting on a graph while assuming that it works
	 * properly.
	 * 
	 * @param IDs
	 * @return
	 */
	private boolean removeDependencies(final Set<Integer> IDs) {
		/*
		 * Assumes that the graph is coherent. Assumes that for each node, all of its
		 * occurrences are in the 'prev' and 'next' of its own 'prev' and 'next'-listed
		 * nodes
		 */
		if (IDs == null || IDs.size() == 0) {
			return false;
		}
		Set<Integer> visited = new HashSet<Integer>();
		HashMap<Integer, GraphNode<Integer>> nodes = this.graph.getNodes();
		boolean ret = true;
		int counter = 0;
		int idProgressCounter = 0;
		for (Integer ID : IDs) {
			logger.debug("Removing Node(" + ID + "): " + nodes.get(ID).getLabel());
			GraphNode<Integer> gNode = nodes.get(ID);
			for (Integer pred : gNode.getPredecessors()) {
				if (!visited.contains(pred) && !IDs.contains(pred)) {
					ret &= nodes.get(pred).removeNodeFromSuccessorsAndOrPredecessors(IDs);
					visited.add(pred);
					counter++;
				}
			}
			for (Integer next : gNode.getSuccessors()) {
				if (!visited.contains(next) && !IDs.contains(next)) {
					ret &= nodes.get(next).removeNodeFromSuccessorsAndOrPredecessors(IDs);
					visited.add(next);
					counter++;
				}
			}
			if (idProgressCounter % Math.ceil((double) (IDs.size() / 1000)) == 0) {// Updates for every 0.1%
				double perc_progress = ((double) ((int) ((((float) idProgressCounter) / ((float) IDs.size())) * 1000.0))
						/ 10.0);
				logger.debug("Progress " + idProgressCounter + "/" + IDs.size() + " : [" + perc_progress + "%]");
			}
			idProgressCounter++;
		}
		for (Integer ID : IDs) {
			ret &= (nodes.remove(ID) != null);
		}
		logger.info("Removed " + IDs.size() + " blacklisted nodes from succ/pred from " + counter + " other nodes.");

		return ret;
	}

	private boolean removeFromGraph(final Set<Integer> IDs) {
		boolean success = true;
		final HashMap<Integer, GraphNode<Integer>> nodes = this.graph.getNodes();
		// Go through all nodes and remove any potential links to specified node
		Set<Integer> deleted = new HashSet<Integer>();
		for (Map.Entry<Integer, GraphNode<Integer>> e : nodes.entrySet()) {
			if (!IDs.contains(e.getKey())) {
				// Only delete successors/predecessors if the inspected node is
				// not one that will be removed, as such nodes will be simply
				// removed afterwards
				GraphNode<Integer> gNode = e.getValue();
				success &= gNode.removeNodeFromSuccessorsAndOrPredecessors(IDs);
			}
			deleted.add(e.getKey());
			if (deleted.size() % (nodes.size() / 1000) == 0) {// Updates for
																// every
																// 0.1%
				double perc_progress = ((double) ((int) ((((float) deleted.size()) / ((float) nodes.size())) * 1000.0))
						/ 10.0);
				logger.debug("Progress " + deleted.size() + "/" + nodes.size() + ":[" + perc_progress + "%]");
			}
		}
		for (Integer ID : deleted) {
			success &= (this.graph.getNodes().remove(ID) != null);
		}

		return success;
	}

	private boolean removeFromGraph(final Integer ID) {
		final HashMap<Integer, GraphNode<Integer>> nodes = this.graph.getNodes();
		// Go through all nodes and remove any potential links to specified node
		for (Map.Entry<Integer, GraphNode<Integer>> e : nodes.entrySet()) {
			GraphNode<Integer> gNode = e.getValue();
			gNode.removeFromSuccessorsAndOrPredecessors(ID);
		}
		boolean success = this.graph.getNodes().remove(ID, this.graph.getNodes().get(ID));
		return success;
	}

	/**
	 * Enforces previously blacklisted items on the given graph. <b>Requires</b>:
	 * Graph to be loaded into memory along with node ID mappings
	 */
	public void enforce() {
		syncSets();
		/*
		 * for (Integer ID : blacklistID) { enforceOnGraph(ID); }
		 */
		// enforceOnGraph(blacklistID);
		removeDependencies(blacklistID);
	}

	/**
	 * Blacklists a given URL.<br>
	 * Note: Is not enforced until {@link #enforce()} is called.<br>
	 * See {@link #blacklist(Integer)} for more information.
	 * 
	 * @param URL
	 */
	public void blacklist(final String URL) {
		blacklistURL.add(URL);
	}

	/**
	 * Adds a given node's ID to the list of blacklisted nodes.<br>
	 * Uses 2 Sets to keep track of what is to be blacklisted.<br>
	 * One set is for Integer values (IDs), the other for String values (URIs).<br>
	 * Rather than using a single set, both are used in order to avoid needing to
	 * load the ID mappings prior to adding the blacklisted items (but which is
	 * required prior to enforcing it on the loaded graph)
	 * 
	 * @param ID
	 */
	public void blacklist(final Integer ID) {
		blacklistID.add(ID);
	}

	public Set<Integer> getBlacklistedIDs() {
		return blacklistID;
	}

	public Set<String> getBlacklistedURLs() {
		return blacklistURL;
	}

	/**
	 * Synchronizes both sets to each other and returns the String-typed one
	 * <b>Requires</b>: ID mappings must be loaded into Graph
	 * 
	 * @return
	 */
	public Set<String> computeAndGetAllBlacklistedNodesURL() {
		syncSets();
		return getBlacklistedURLs();
	}

	/**
	 * Synchronizes both sets to each other and returns the Integer-typed one
	 * <b>Requires</b>: ID mappings must be loaded into Graph
	 * 
	 * @return
	 */
	public Set<Integer> computeAndGetAllBlacklistedNodesID() {
		syncSets();
		return getBlacklistedIDs();
	}

	/**
	 * Synchronizes both sets - does NOT mean that they both need to be of the same
	 * size as the user might have added IDs to the String-valued set<br>
	 * <b>Requires</b>: ID mappings must be loaded into Graph
	 */
	private void syncSets() {
		ifUncheckedThrowException();
		for (Integer ID : blacklistID) {
			final String URL = this.graph.getIDMapping().get(ID);
			if (URL != null) {
				blacklistURL.add(URL);
			}
		}

		for (String URL : blacklistURL) {
			final Integer ID = this.graph.getIDMapping().getKey(URL);
			if (ID != null) {
				blacklistID.add(ID);
			}
		}

	}

	private void ifUncheckedThrowException() {
		if (!checkValidity()) {
			throw new RuntimeException("Graph not properly loaded yet");
		}
	}

	/**
	 * Checks whether passed ID is blacklisted
	 * 
	 * @param ID
	 *            Integer to be checked for blacklisting
	 * @return True if yes, false otherwise
	 */
	public boolean isBlacklisted(final Integer ID) {
		boolean ret = false;
		ret |= getBlacklistedIDs().contains(ID);
		if (ret) {
			return ret;
		}
		final String URL = this.graph.getIDMapping().get(ID);
		if (URL != null) {
			ret |= getBlacklistedURLs().contains(URL);
		}
		return ret;
	}

	/**
	 * Used to find out whether a String is blacklisted
	 * 
	 * @param URL
	 *            String of a URL that might have been added to a black list
	 * @return Whether a given URL (or String-typed ID) is contained in one of the
	 *         black lists
	 */
	public boolean isBlacklisted(final String URL) {
		boolean ret = false;
		ret |= blacklistURL.contains(URL);
		Integer ID = this.graph.getIDMapping().getKey(URL);
		if (ret) {
			return ret;
		}
		if (ID != null) {
			ret |= blacklistID.contains(ID);
		}
		if (ret) {
			return ret;
		}
		ID = null;
		try {
			ID = new Integer(URL);
		} catch (Exception e) {
			ID = null;
		}
		if (ID != null) {
			ret |= blacklistID.contains(ID);
		}
		return ret;
	}

	/**
	 * In a generic fashion removes nodes from graph which are connected to more
	 * than the passed threshold's ratio of total nodes
	 * 
	 * @param threshold
	 *            anything above this threshold is blacklisted
	 * @return
	 */
	public int blacklistConnectionsOver(final double threshold) {
		final Map<Integer, GraphNode<Integer>> allNodes = this.graph.getNodes();
		int blacklisted = 0;
		for (Map.Entry<Integer, GraphNode<Integer>> e : allNodes.entrySet()) {
			final GraphNode<Integer> node = e.getValue();
			if (((double) (node.getPredecessors().size() + node.getSuccessors().size()))
					/ ((double) allNodes.size()) >= threshold) {
				blacklist(e.getKey());
				blacklisted++;
			}
		}
		return blacklisted;
	}

}
