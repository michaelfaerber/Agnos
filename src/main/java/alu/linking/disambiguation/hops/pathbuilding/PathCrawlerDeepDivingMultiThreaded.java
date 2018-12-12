package alu.linking.disambiguation.hops.pathbuilding;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.jena.ext.com.google.common.collect.Lists;

import alu.linking.disambiguation.hops.graph.GraphNode;
import alu.linking.structure.Loggable;

public class PathCrawlerDeepDivingMultiThreaded<T> implements PathCrawler<T>, Loggable {
	private final int MAX_LENGTH = alu.linking.config.constants.Numbers.HOPS_PATH_LENGTH.val.intValue();
	private final HashMap<T, Set<T>> alreadyExpanded = new HashMap<T, Set<T>>();
	protected ConcurrentLinkedDeque<LinkedList<T>> foundPaths;
	protected final Set<T> goalNodes;
	protected final Set<T> sourceNodes;
	private final Map<T, GraphNode<T>> allNodes;
	private final ThreadPoolExecutor executor;

	public PathCrawlerDeepDivingMultiThreaded(ThreadPoolExecutor executor,
			ConcurrentLinkedDeque<LinkedList<T>> foundPaths, Map<T, GraphNode<T>> possible_nodes_map,
			Set<T> sourceNodes, Set<T> goalNodes) {
		this.executor = executor;
		this.goalNodes = goalNodes;
		this.sourceNodes = sourceNodes;
		this.allNodes = possible_nodes_map;
		this.foundPaths = foundPaths;
	}

	@Override
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
	@Override
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
	@Override
	public LinkedHashSet<LinkedList<T>> recursiveCrawlPaths(LinkedList<T> path) {
		// Step 1: Take the vertex and expand from it.
		// Step 2: Take the first possibility it shows (unless already used for this
		// vertex)
		// Step 3: For the next vertex, expand it and take the first possibility (that
		// is not in current path)
		if (path.size() < MAX_LENGTH) {
			this.executor.execute(new Runnable() {
				@Override
				public void run() {
					final List<T> next = filter(expandVertex(path.get(path.size() - 1)), path);
					for (T succ : next) {
						final LinkedList<T> newPath = new LinkedList<T>(path);
						newPath.add(succ);
						storePath(newPath);
						recursiveCrawlPaths(newPath);
					}
				}
			});
		}
		return null;
	}

	/**
	 * Entry point
	 * 
	 * @return all found paths
	 * @throws InterruptedException
	 */
	public ConcurrentLinkedDeque<LinkedList<T>> crawlPaths() throws InterruptedException {
		long startCrawl = System.currentTimeMillis();
		for (T node : sourceNodes) {
			LinkedList<T> initPath = new LinkedList<T>();
			initPath.add(node);
			crawlPaths(initPath);
		}
		int prevSize = -1;
		do {
			// No need for await termination as this is pretty much it already...
			if (!executor.isShutdown() && prevSize == foundPaths.size()) {
				executor.shutdown();
			}
			prevSize = foundPaths.size();
			Thread.sleep(10);
		} while (!executor.isTerminated());

		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		// warn("DONE! Duration: " + (System.currentTimeMillis() - startCrawl));
		return foundPaths;
	}

	@Override
	public LinkedHashSet<LinkedList<T>> crawlPaths(List<T> v) {
		// Passed argument is a linked list
		return recursiveCrawlPaths((LinkedList<T>) v);
	}

	@Override
	public void storePath(LinkedList<T> l) {
		foundPaths.add(l);
	}

	@Override
	public void storePath(LinkedHashSet<LinkedList<T>> ret, LinkedList<T> l) {
	}

	@Override
	public void closeOutputs() {
	}
}
