package alu.linking.disambiguation.hops.pathbuilding.embeddings;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.jena.ext.com.google.common.collect.Lists;

import alu.linking.disambiguation.hops.graph.GraphNode;
import alu.linking.structure.Loggable;

public class BFSMultiThreaded<T> implements Loggable {
	private final int MAX_LENGTH = alu.linking.config.constants.Numbers.HOPS_PATH_LENGTH.val.intValue();
	private final HashMap<T, Set<T>> alreadyExpanded = new HashMap<T, Set<T>>();
	protected List<ConcurrentSkipListSet<T>> foundNodes;
	protected final Set<T> goalNodes;
	protected final Set<T> sourceNodes;
	private final Map<T, GraphNode<T>> allNodes;
	private final ThreadPoolExecutor executor;

	public BFSMultiThreaded(ThreadPoolExecutor executor, List<ConcurrentSkipListSet<T>> foundNodes,
			Map<T, GraphNode<T>> possible_nodes_map, Set<T> sourceNodes, Set<T> goalNodes) {
		this.executor = executor;
		this.goalNodes = goalNodes;
		this.sourceNodes = sourceNodes;
		this.allNodes = possible_nodes_map;
		this.foundNodes = foundNodes;
		//One per level
		for (int i = 0; i < MAX_LENGTH; ++i) {
			foundNodes.add(new ConcurrentSkipListSet<T>());
		}
	}

	public List<T> expandVertex(final T v) {
		final GraphNode<T> n = allNodes.get(v);
		if (n == null) {
			throw new RuntimeException("Could not find GraphNode for [" + v + "]");
		}
		List<T> pred;
		List<T> next;
		pred = n.getPredecessors();
		next = n.getSuccessors();
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
	public List<T> filter(final List<T> next, final Collection<T> path) {
		// No more searching needed
		if (path.size() >= MAX_LENGTH) {
			return Lists.newArrayList();
		}
		// Remove already visited nodes from 'next'
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
	public void recursiveCrawlPaths(List<T> path, final int depth) {
		// Step 1: Take the vertex and expand from it.
		// Step 2: Take the first possibility it shows (unless already used for this
		// vertex)
		// Step 3: For the next vertex, expand it and take the first possibility (that
		// is not in current path)
		final TreeSet<T> todo = new TreeSet<T>();
		if (depth < MAX_LENGTH) {
			final List<T> next = filter(expandVertex(path.get(path.size() - 1)), path);
			for (T succ : next) {
				this.executor.execute(new Runnable() {
					@Override
					public void run() {
						final LinkedList<T> newPath = new LinkedList<T>(path);
						newPath.add(succ);
						storePath(newPath, depth+1);
						recursiveCrawlPaths(newPath, depth+1);
					}
				});
			}

		}
	}

	/**
	 * Entry point
	 * 
	 * @return all found paths
	 * @throws InterruptedException
	 */
	public List<ConcurrentSkipListSet<T>> crawlPaths() throws InterruptedException {
		for (T node : sourceNodes) {
			LinkedList<T> initPath = new LinkedList<T>();
			initPath.add(node);
			crawlPaths(initPath);
		}
		int prevSize = -1;
		do {
			// No need for await termination as this is pretty much it already...
			if (!executor.isShutdown() && prevSize == foundNodes.size()) {
				executor.shutdown();
			}
			prevSize = foundNodes.size();
			Thread.sleep(10);
		} while (!executor.isTerminated());

		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		// warn("DONE! Duration: " + (System.currentTimeMillis() - startCrawl));
		return foundNodes;
	}

	public void crawlPaths(List<T> v) {
		// Passed argument is a linked list
		recursiveCrawlPaths((LinkedList<T>) v, 0);
	}

	public void storePath(LinkedList<T> l, int depth) {
		foundNodes.get(depth).add(l.getLast());
	}
}
