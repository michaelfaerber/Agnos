package alu.linking.disambiguation.hops.pathbuilding;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import alu.linking.config.constants.Numbers;
import alu.linking.disambiguation.hops.graph.Graph;
import alu.linking.disambiguation.hops.graph.GraphNode;

public class PathCrawlerDeepDiving<T> extends PathCrawlerBasic<T> {
	private Logger logger = Logger.getLogger(getClass());

	private final HashMap<T, Set<T>> alreadyExpanded = new HashMap<T, Set<T>>();
	protected final Set<T> goalNodes;

	public PathCrawlerDeepDiving(final Map<T, GraphNode<T>> allNodes, final String OUT_PATHS, final String OUT_EDGES)
			throws IOException {
		this(allNodes, new FileWriter(OUT_PATHS), new FileWriter(OUT_EDGES));
	}

	/**
	 * 
	 * @param OUT_PATHS
	 *            Where to output paths (aka. bunch of nodes)
	 * @param OUT_EDGES
	 *            Where to output edges
	 * @param OUT_DIRS
	 *            Where to output directions of edges
	 * @param goalNodes
	 *            possible ending nodes (for full-on path computation, i.e. without
	 *            concatenation)
	 * @throws IOException
	 *             When problems with writers appear
	 */
	public PathCrawlerDeepDiving(final Map<T, GraphNode<T>> allNodes, final String OUT_PATHS, final String OUT_EDGES,
			final String OUT_DIRS, final Set<T> goalNodes) throws IOException {
		this(allNodes, OUT_PATHS == null ? null : new FileWriter(OUT_PATHS),
				OUT_EDGES == null ? null : new FileWriter(OUT_EDGES),
				OUT_DIRS == null ? null : new FileWriter(OUT_DIRS), goalNodes);
	}

	public PathCrawlerDeepDiving(final Map<T, GraphNode<T>> allNodes, final Writer OUT_PATHS, final Writer OUT_EDGES,
			final Writer OUT_DIRS, final Set<T> goalNodes) throws IOException {
		this(allNodes, OUT_PATHS, OUT_EDGES, OUT_DIRS, goalNodes, Numbers.HOPS_PATH_LENGTH.val.intValue());
	}

	public PathCrawlerDeepDiving(final Map<T, GraphNode<T>> allNodes, final Writer OUT_PATHS, final Writer OUT_EDGES,
			final Writer OUT_DIRS, final Set<T> goalNodes, int pathLenThreshold) throws IOException {
		super(allNodes, OUT_PATHS, OUT_EDGES, OUT_DIRS, pathLenThreshold);
		this.goalNodes = goalNodes;
	}

	public PathCrawlerDeepDiving(final Map<T, GraphNode<T>> allNodes, final Writer OUT_WRITER_NODES,
			final Writer OUT_WRITER_EDGES) throws IOException {
		this(allNodes, OUT_WRITER_NODES, OUT_WRITER_EDGES, null, Numbers.HOPS_PATH_LENGTH.val.intValue());
	}

	public PathCrawlerDeepDiving(final Map<T, GraphNode<T>> allNodes, final Writer OUT_WRITER_NODES,
			final Writer OUT_WRITER_EDGES, final Writer OUT_DIRS, final int pathLengthThreshold) throws IOException {
		this(allNodes, OUT_WRITER_NODES, OUT_WRITER_EDGES, OUT_DIRS, null, pathLengthThreshold);
	}

	@Override
	public List<T> expandVertex(final T v) {
		int nodeExpansionCounter = 0;
		long expandStart = System.currentTimeMillis();
		GraphNode<T> n = (GraphNode<T>) Graph.getInstance().getNodes().get(v);
		if (n == null) {
			logger.error("GraphSize:" + Graph.getInstance().getNodes().size());
			throw new RuntimeException("Could not find GraphNode for [" + v + "]");
		}
		List<T> pred;
		List<T> next;
		if (n.getPredecessors() != null) {
			pred = n.getPredecessors();
		} else {
			pred = null;
		}
		if (n.getSuccessors() != null) {
			next = n.getSuccessors();
		} else {
			next = null;
		}
		List<T> allNext = Lists.newArrayList();
		if (pred != null) {
			// Adds predecessor nodes
			for (T s : pred) {
				allNext.add(s);
			}
		}
		pred = null;
		if (next != null) {
			// Adds successor nodes
			for (T s : next) {
				allNext.add(s);
			}
		}
		next = null;
		// Finished adding
		long expandTime = System.currentTimeMillis() - expandStart;
		if (expandTime > timeOutputThreshold * 10) {
			logger.warn("Expand Deep Diving Vertex -" + expandTime);
		}
		return allNext;
	}

	/**
	 * O(|alrExpSet|)
	 * 
	 * @param next
	 *            List of all possible expansions for a given node
	 * @param alrExpSet
	 *            Set of elements already expanded
	 * @return
	 */
	@Override
	public List<T> filter(final List<T> next, final Collection<T> path) {
		if (path.size() >= alu.linking.config.constants.Numbers.HOPS_PATH_LENGTH.val.intValue()) {
			return Lists.newArrayList();
		}
		for (Object pathItem : path) {
			next.remove(pathItem);
		}
		final Set<T> alrdyExp;
		if (alreadyExpanded == null
				|| (alrdyExp = alreadyExpanded.get(((List<T>) path).get(path.size() - 1))) == null) {
			return next;
		}
		for (Object setItem : alrdyExp) {
			next.remove(setItem);
		}
		return next;
	}

	/**
	 * Deep-diving approach<br>
	 * In hopes of reducing RAM usage while keeping it working k
	 * 
	 * @param v
	 * @param useFile
	 * @param allPaths
	 * @param saveInRAM
	 * @return
	 */
	@Override
	public LinkedHashSet<LinkedList<T>> recursiveCrawlPaths(LinkedList<T> path) {
		// Step 1: Take the vertex and expand from it.
		// Step 2: Take the first possibility it shows (unless already used for this
		// vertex)
		// Step 3: For the next vertex, expand it and take the first possibility (that
		// is not in current path)
		final List<T> next = filter(expandVertex(path.get(path.size() - 1)), path);
		for (T succ : next) {
			// Stores found paths to file
			final LinkedList<T> newPath = extend(path, succ);
			storeFullAttributePath(newPath);
			recursiveCrawlPaths(newPath);
		}
		// Even with recursion: need to check if all of a node's successors have
		// been expanded
		return null;
	}

	/**
	 * Stores any path starting with a node from passed nodes and ending in one of
	 * the defined nodes through the constructor with GOAL_NODES
	 * 
	 * @param newPath
	 * @param end
	 */
	public void storeFullAttributePath(LinkedList<T> newPath) {
		if (goalNodes != null) {
			final T last = newPath.getLast();
			try {
				if (goalNodes.contains(last)) {
					storeAnyPath(newPath);
					// storeAnyPathWEdges(newPath);
				}
			} catch (NullPointerException npe) {
				npe.printStackTrace();
				final GraphNode<T> nodeFirst = this.allGraphNodes.get(newPath.getFirst()),
						nodeLast = this.allGraphNodes.get(last);
				logger.error("NPE - nodeFirst(" + nodeFirst + "), nodeLast(" + nodeLast + ") - "
						+ Arrays.toString(newPath.toArray()));
			}
		} else {
			// Goal nodes were not defined, so just store it when it doesn't matter
			// storeNonAmazonPath(newPath);
			storeAnyPath(newPath);
		}

	}

	@Override
	public LinkedHashSet<LinkedList<T>> crawlPaths(List<T> v) {
		// Passed argument is a linked list
		return recursiveCrawlPaths((LinkedList<T>) v);
	}

	@Override
	public void storePath(LinkedList<T> l) {
		storeAnyPath(l);
	}
}
