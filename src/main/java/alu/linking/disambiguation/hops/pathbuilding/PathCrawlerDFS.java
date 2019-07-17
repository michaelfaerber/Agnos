package alu.linking.disambiguation.hops.pathbuilding;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.jena.ext.com.google.common.collect.Lists;

import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.hops.graph.Graph;
import alu.linking.disambiguation.hops.graph.GraphNode;

/**
 * Naive DFS implementation for path crawling
 * @author Kristian Noullet
 *
 * @param <T> node type
 */
public class PathCrawlerDFS<T> extends PathCrawlerBasic<T> {

	final boolean useFile;
	final boolean saveInRAM;
	final HashMap<Object, LinkedHashSet<LinkedList<Object>>> allPaths;
	final LinkedList<LinkedList<T>> pending_paths;

	PathCrawlerDFS(final EnumModelType KG, final Map<T, GraphNode<T>> allNodes) throws IOException {
		this(KG, allNodes, false, true);

	}

	public PathCrawlerDFS(final EnumModelType KG, final Map<T, GraphNode<T>> allNodes, final boolean useFile, final boolean saveInRAM)
			throws IOException {
		super(KG, allNodes);
		this.useFile = useFile;
		this.saveInRAM = saveInRAM;
		pending_paths = new LinkedList<LinkedList<T>>();
		allPaths = new HashMap<Object, LinkedHashSet<LinkedList<Object>>>();

	}

	/**
	 * DFS pretty much<br>
	 * Problem: unfortunatley this way takes waaaaay too much RAM
	 * 
	 * @param v
	 * @param useFile
	 * @param allPaths
	 * @param saveInRAM
	 * @return
	 */
	@Override
	public LinkedHashSet<LinkedList<T>> crawlPaths(final List<T> v) {
		final LinkedList<T> addV = new LinkedList<T>();
		final LinkedHashSet<LinkedList<T>> ret = new LinkedHashSet<LinkedList<T>>();
		final LinkedList<T> queue_list = new LinkedList<T>();

		addV.add(v.get(0));
		pending_paths.add(addV);
		while (!pending_paths.isEmpty()) {
			LinkedList<T> currPath = pending_paths.poll();// get first
			if (saveInRAM) {
				storePath(ret, currPath);
			}
			if (useFile) {
				storeAmazonPath(currPath);
			}
			T vertex = (T) currPath.getLast();// currPath.toArray()[currPath.size()
												// - 1];// get last
			expandAndFilter(currPath, vertex);
		}
		return ret;
	}

	private void expandAndFilter(LinkedList<T> path, T v) {

		long extendStart = System.currentTimeMillis();
		for (T s : filter(expandVertex(v), path)) {
			pending_paths.addLast(extend(path, s));// add last
		}
		long extendTime = System.currentTimeMillis() - extendStart;
		if (extendTime > timeOutputThreshold * 10) {
			System.out.println("POST FILTER - Pending paths: " + pending_paths.size());
		}
	}

	@Override
	public List<T> filter(final List<T> next, final Collection<T> path) {
		for (T o : path) {
			if (next.contains(o)) {
				next.remove(o);
			}
		}
		return next;
	}

	/**
	 * Expands a path and appends all possible continuing paths from vertex to
	 * pending_paths (requires currPath to add ot)
	 * 
	 * @param vertex
	 *            Node to be expanded
	 * @param expandingPath
	 *            Path that is currently ahead
	 * @param pending_paths
	 *            List of paths that still need to be expanded/checked out
	 * @param useFile
	 *            Whether a file should be used for storage
	 * @param allPaths
	 *            The list of all paths that paths will be added to
	 */
	@Override
	public List<T> expandVertex(final Object v) {
		final GraphNode n = Graph.getInstance().getNodes().get(v);
		List<T> pred = n.getPredecessors();
		List<T> next = n.getSuccessors();
		List<T> allNext = Lists.newArrayList();
		for (T s : pred) {
			allNext.add(s);
		}
		for (T s : next) {
			allNext.add(s);
		}
		return allNext;

	}

	@Override
	/**
	 * Unused unless a recursive way of finding paths
	 */
	public LinkedHashSet<LinkedList<T>> recursiveCrawlPaths(LinkedList<T> path) {
		// TODO Auto-generated method stub
		return null;
	}
}
