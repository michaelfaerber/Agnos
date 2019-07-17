package alu.linking.disambiguation.hops.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Class handling blacklisting of specific edges within our in-memory graph
 * <br>
 * See {@link NodeBlacklisting} for the node equivalent.
 * 
 * @author Kristian Noullet
 *
 */
public class EdgeBlacklisting {
	private Graph<Integer> graph = null;
	private Logger logger = Logger.getLogger(getClass());

	private static class EdgeBlacklistingWrapper {
		static EdgeBlacklisting INSTANCE = new EdgeBlacklisting(Graph.getInstance());
	}

	/**
	 * By default created a blacklisting associated to the Graph singleton, but
	 * another can be specified with {@link #switchGraph(Graph)}
	 * 
	 * @return instance of NodeBlackListing
	 */
	public EdgeBlacklisting getInstance() {
		return EdgeBlacklistingWrapper.INSTANCE;
	}

	public EdgeBlacklisting(final Graph<Integer> graph) {
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
		int mappingSize = this.graph.getPredicateIDMapping().size();
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
		double allNodesSize = (double) nodes.size();
		for (Map.Entry<Integer, GraphNode<Integer>> e : nodes.entrySet()) {
			counter += e.getValue().removeEdges(IDs);
			if (idProgressCounter % Math.ceil(allNodesSize / 10.0) == 0) {// Updates for every 0.1%
				double perc_progress = ((double) ((int) ((((float) idProgressCounter) / allNodesSize) * 1000.0))
						/ 10.0);
				logger.debug("Progress " + idProgressCounter + "/" + allNodesSize + " : [" + perc_progress + "%]");
			}
			idProgressCounter++;
		}
		logger.info("Removed " + IDs.size() + " blacklisted edges from succ/pred from " + counter + " other nodes.");

		return ret;
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
	public Set<String> computeAndGetAllBlacklistedEdgesURL() {
		syncSets();
		return getBlacklistedURLs();
	}

	/**
	 * Synchronizes both sets to each other and returns the Integer-typed one
	 * <b>Requires</b>: ID mappings must be loaded into Graph
	 * 
	 * @return
	 */
	public Set<Integer> computeAndGetAllBlacklistedEdgesID() {
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
			final String URL = this.graph.getPredicateIDMapping().get(ID);
			if (URL != null) {
				blacklistURL.add(URL);
			}
		}

		for (String URL : blacklistURL) {
			final Integer ID = this.graph.getPredicateIDMapping().getKey(URL);
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
	 * @param ID Integer to be checked for blacklisting
	 * @return True if yes, false otherwise
	 */
	public boolean isBlacklisted(final Integer ID) {
		boolean ret = false;
		ret |= getBlacklistedIDs().contains(ID);
		if (ret) {
			return ret;
		}
		final String URL = this.graph.getPredicateIDMapping().get(ID);
		if (URL != null) {
			ret |= getBlacklistedURLs().contains(URL);
		}
		return ret;
	}

	/**
	 * Used to find out whether a String is blacklisted
	 * 
	 * @param URL String of a URL that might have been added to a black list
	 * @return Whether a given URL (or String-typed ID) is contained in one of the
	 *         black lists
	 */
	public boolean isBlacklisted(final String URL) {
		boolean ret = false;
		ret |= blacklistURL.contains(URL);
		Integer ID = this.graph.getPredicateIDMapping().getKey(URL);
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
}
