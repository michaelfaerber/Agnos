package alu.linking.disambiguation.hops.pathbuilding;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.jena.ext.com.google.common.collect.Lists;

import alu.linking.config.constants.Numbers;
import alu.linking.disambiguation.hops.graph.GraphNode;
import alu.linking.utils.Stopwatch;

/**
 * Concurrently builds paths between nodes based on Graph singleton
 */
public class ConcurrentPathBuilderSingle extends ConcurrentPathBuilderWrapper {

	public Deque<LinkedList<Integer>> concurrentBuild(final Map<Integer, GraphNode<Integer>> possible_nodes_map,
			final Set<Integer> all_source_nodes, final Set<Integer> goalNodes) {
		try {
			// #########################################
			// Task preparation / execution
			// #########################################
			// final ThreadPoolExecutor executor = (ThreadPoolExecutor)
			// Executors.newCachedThreadPool();
			final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
					.newFixedThreadPool(Numbers.HOPS_THREAD_AMT.val.intValue());
			Stopwatch.start(this.getClass().getName());
			final ConcurrentLinkedDeque<LinkedList<Integer>> foundPaths = new ConcurrentLinkedDeque<LinkedList<Integer>>();
			// Create tasks for the threads to execute on loop
			final PathCrawlerDeepDivingMultiThreaded<Integer> crawler = new PathCrawlerDeepDivingMultiThreaded<Integer>(
					executor, foundPaths, possible_nodes_map, all_source_nodes, goalNodes);
			crawler.crawlPaths();
			debug("Found total of paths: " + foundPaths.size());
			debug("Finished executing all threads. (Duration: " + (Stopwatch.endDiff(this.getClass().getName()))
					+ "ms)");

			List<String> retList = Lists.newArrayList();
			Iterator<LinkedList<Integer>> itPaths = foundPaths.iterator();
			// while (itPaths.hasNext()) {
			// retList.add(itPaths.next().toString());
			// itPaths.remove();
			// }
			// return retList;
			return foundPaths;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<String> concurrentBuild(Map<Integer, GraphNode<Integer>> possible_nodes_map,
			Map<Integer, GraphNode<Integer>> all_source_nodes_map, Set<Integer> goalNodes, boolean outputPaths,
			boolean outputEdges, boolean outputDirections, boolean outputToList) {
		throw new RuntimeException("Not implemented");
	}
}
