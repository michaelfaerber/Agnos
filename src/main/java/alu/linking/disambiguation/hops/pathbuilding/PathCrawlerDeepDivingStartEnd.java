package alu.linking.disambiguation.hops.pathbuilding;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import alu.linking.disambiguation.hops.graph.Graph;
import alu.linking.disambiguation.hops.graph.GraphNode;

public class PathCrawlerDeepDivingStartEnd<T> extends PathCrawlerDeepDiving<T> {
	private Logger logger = Logger.getLogger(getClass());

	public PathCrawlerDeepDivingStartEnd(final Map<T, GraphNode<T>> allGraphNodes, final T end, final Writer nodeWriter,
			final Writer edgeWriter) throws IOException {
		this(allGraphNodes, end, nodeWriter, edgeWriter,
				alu.linking.config.constants.Numbers.HOPS_PATH_LENGTH.val.intValue());
	}

	public PathCrawlerDeepDivingStartEnd(final Map<T, GraphNode<T>> allGraphNodes, final T end, final Writer nodeWriter,
			final Writer edgeWriter, final int pathLengthThreshold) throws IOException {
		super(allGraphNodes, nodeWriter, edgeWriter, null, pathLengthThreshold);
		this.end = end;
	}

	private T end;
	private static int counter = 0;

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
	public LinkedHashSet<LinkedList<T>> recursiveCrawlPaths(final LinkedList<T> path) {
		/*
		 * Step 1: Take the vertex and expand from it. STep 2: Take the first
		 * possibility it shows (unless already used for this vertex) Step 3: For the
		 * next vertex, expand it and take the first possibility (that is not in current
		 * path)
		 */

		final List<T> next;
		if (!path.contains(end) && path.size() < pathLengthThreshold) {
			next = filter(expandVertex(path.getLast()), path);
		} else {
			next = Lists.newArrayList();
		}
		for (T succ : next) {
			// Stores found paths to file
			final LinkedList<T> newPath = extend(path, succ);
			storePath(newPath);
			recursiveCrawlPaths(newPath);
		}
		// Even with recursion: need to check if all of a node's successors have
		// been expanded
		return null;
	}

	@Override
	public List<T> expandVertex(final Object v) {
		int nodeExpansionCounter = 0;
		long expandStart = System.currentTimeMillis();
		List<T> allNext = Lists.newArrayList();
		GraphNode n = Graph.getInstance().getNodes().get(v);
		List<T> pred = n.getPredecessors();
		List<T> next = n.getSuccessors();
		// Adds predecessor nodes
		for (T s : pred) {
			allNext.add(s);
		}
		pred = null;
		// Adds successor nodes
		for (T s : next) {
			allNext.add(s);
		}
		next = null;
		// Finished adding
		long expandTime = System.currentTimeMillis() - expandStart;
		if (expandTime > timeOutputThreshold * 10) {
			logger.warn("Expand Deep Diving Vertex -" + expandTime);
		}
		return allNext;
	}

	@Override
	public void storePath(LinkedList<T> l) {
		storeAnyPath(l, end);
	}

	protected void storeAnyPath(LinkedList<T> l, T endNode) {
		if (l.getLast().equals(endNode)) {
			storeAnyPath(l);
		}
	}

	@Override
	public LinkedHashSet<LinkedList<T>> crawlPaths(List<T> v) {
		// It's a linked list that's passed
		// Does it for all possible endings
		if (this.end == null) {
			for (Map.Entry<T, GraphNode<T>> end : this.allGraphNodes.entrySet()) {
				// As long as no doubled element exists in the path,
				// it's k
				if (!v.equals(end.getKey()) && end.getValue() != null) {
					// allPaths.put(e.getKey(), findPaths(e.getKey(),
					// useFile,
					// allPaths, saveInRAM));
					this.end = (T) end.getKey();
					recursiveCrawlPaths((LinkedList<T>) v);
					// pc.crawlPaths(initPath, new HashMap<Integer,
					// Set<Integer>>());
					// computedFor.add(e.getKey());
					counter++;
				}
			}
			return null;
		} else {
			recursiveCrawlPaths((LinkedList) v);
			return null;
		}
	}
}
