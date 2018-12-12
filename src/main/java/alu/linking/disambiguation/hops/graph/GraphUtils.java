package alu.linking.disambiguation.hops.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.ext.com.google.common.collect.Lists;

public class GraphUtils {
	public static List<Set<Integer>> detectSubgraphs(Graph g) {
		/*
		 * All right, so how do we detect a subgraph? Go through nodes and continue
		 * expanding by nodes... until it can't be done any more!
		 */
		final HashMap<Integer, GraphNode<Integer>> nodes = g.getNodes();
		System.out.println("Total nodes: " + nodes.size());
		List<Set<Integer>> subGraphs = Lists.newArrayList();
		int graphCounter = 0;
		for (Map.Entry<Integer, GraphNode<Integer>> e : nodes.entrySet()) {
			if (!graphExists(e.getKey(), subGraphs)) {
				HashSet<Integer> subGraph = new HashSet<Integer>();
				subGraphs.add(subGraph);
				final Integer subGraphIndex = subGraphs.size() - 1;
				subGraph.add(e.getKey());
				System.out.println("Graph#" + graphCounter + ": startnode: " + e.getKey());
				crawlGraph(nodes, subGraphs, subGraphIndex, e.getValue());
				System.out.println("Graph#" + graphCounter++ + " - computed (Size: " + subGraph.size() + ")");
			}
		}
		return subGraphs;
	}

	/**
	 * Checks if a subgraph exists with this particular node
	 * 
	 * @param node
	 *            Node to be checked for in subGraphs
	 * @param subGraphs
	 *            List containing all subgraphs
	 * @return TRUE if any subgraph contains node, false otherwise
	 */
	private static boolean graphExists(final Integer node, final List<Set<Integer>> subGraphs) {
		for (Set<Integer> subGraph : subGraphs) {
			if (subGraph.contains(node)) {
				return true;
			}
		}
		return false;
	}

	private static <T> void crawlGraph(HashMap<T, GraphNode<T>> nodes, List<Set<T>> subGraphs,
			final Integer subGraphIndex, final GraphNode<T> root) {
		// Expand root... continue with successors/predecessors
		expand(nodes, root, subGraphs.get(subGraphIndex));
	}

	private static <T> void expand(HashMap<T, GraphNode<T>> nodes, GraphNode<T> node, Set<T> subGraph) {
		final List<T> neighbours = new ArrayList<T>();
		neighbours.addAll(node.getSuccessors());
		neighbours.addAll(node.getPredecessors());
		// for (Integer n : neighbours) {
		for (int i = neighbours.size() - 1; i > 0; i--) {
			T n = neighbours.get(i);
			neighbours.remove(i);
			if (!subGraph.contains(n)) {
				subGraph.add(n);
				expand(nodes, nodes.get(n), subGraph);
			}
		}
	}

	public static <T> List<Set<T>> detectGraphz(Graph<T> g) {
		/*
		 * All right, so how do we detect a subgraph? Go through nodes and continue
		 * expanding by nodes... until it can't be done any more!
		 */
		final HashMap<T, GraphNode<T>> nodes = g.getNodes();
		final HashSet<T> toExpand = new HashSet<T>(nodes.keySet());
		final List<Set<T>> subGraphs = Lists.newArrayList();
		System.out.println("Expanding: " + nodes.size());
		int counter = 0;
		float perc_div = 10000.0f;
		while (toExpand.size() != 0) {
			final T node = toExpand.iterator().next();
			if (node != null) {
				toExpand.remove(node);
				Set<T> tmpSet = getSubgraph(node, subGraphs);
				// tmpSet == null -> we don't have the node in anything yet,
				// so... give it a new set!
				if (tmpSet == null) {
					tmpSet = new HashSet<T>();
					subGraphs.add(tmpSet);
				}
				long expTimerStart = System.currentTimeMillis();
				expandz(nodes, nodes.get(node), tmpSet);
				long expTimerEnd = System.currentTimeMillis();
				if (expTimerEnd - expTimerStart > 10) {
					System.out.println("exp: " + (expTimerEnd - expTimerStart) + "ms - " + nodes.get(node).getLabel()
							+ "(" + node + ")");
				}

				if (((float) counter) % (nodes.size() / perc_div) <= 0.01) {
					System.out.println("Processed " + counter + "; "
							+ ((((float) counter) / (((float) nodes.size()) / perc_div)) / 100.0) + "%: "
							+ subGraphs.size());
				}
				counter++;
			} else {
				break;
			}
		}
		return subGraphs;
	}

	private static <T> Set<T> getSubgraph(final T ID, final List<Set<T>> subgraphs) {
		Set<T> ret = null;
		for (Set<T> subgraph : subgraphs) {
			if (subgraph.contains(ID)) {
				ret = subgraph;
				break;
			}
		}
		return ret;
	}

	private static <T> void expandz(final HashMap<T, GraphNode<T>> nodes, final GraphNode<T> node,
			final Set<T> subGraph) {

		final List<T> neighbours = new ArrayList<T>();
		neighbours.addAll(node.getSuccessors());
		neighbours.addAll(node.getPredecessors());
		// for (Integer n : neighbours) {
		for (int i = neighbours.size() - 1; i > 0; i--) {
			T n = neighbours.get(i);
			neighbours.remove(i);
			if (!subGraph.contains(n)) {
				subGraph.add(n);
			}
		}
	}

	/**
	 * The add 'n merge way
	 * 
	 * @param g
	 * @return
	 */
	public static List<Set<Integer>> detectGraphsMerge(Graph g) {
		List<Set<Integer>> subgraphs = Lists.newArrayList();
		HashMap<Integer, GraphNode> nodes = g.getNodes();
		for (GraphNode node : nodes.values()) {
			Set<Integer> tmpSet = new HashSet<Integer>();
			simpleExpand(node, tmpSet);
			subgraphs.add(tmpSet);
		}
		System.out.println("Finished simply adding, time for merging");
		int mergeCounter = 0;
		boolean[] merged = new boolean[subgraphs.size()];
		for (int i = 0; i < merged.length; ++i) {
			merged[i] = false;
		}
		final float perc_div = 100_000f;

		for (int i = subgraphs.size() - 1; i > 0 && mergeCounter < subgraphs.size(); i--) {
			int srcIndex = i;
			System.out.println("srcIndex: " + srcIndex);
			merged[srcIndex] = true;
			for (int j = subgraphs.size() - 1; j > 0 && mergeCounter < subgraphs.size(); j--) {
				long start = System.currentTimeMillis();
				int targetIndex = j;
				if (!merged[targetIndex]) {// current target has not been
											// merged, so try it, else ignore
					final Set<Integer> currGraph = subgraphs.get(srcIndex);
					final Set<Integer> nextGraph = subgraphs.get(targetIndex);
					Set<Integer> mergedGraph = conditionalMergeCommonGraphs(currGraph, nextGraph);
					if (mergedGraph != null) {
						// Means it managed to merge them -> mark them
						// and add result

						// System.out.println("Merged("+srcIndex+","+targetIndex+"):
						// "+mergedGraph.size());
						percentageProgressOutput(mergeCounter, nodes.size(), perc_div, 4);
						merged[targetIndex] = true;
						mergeCounter++;

					} else {
						// Could not merge -> go to next
					}
				}
				long end = System.currentTimeMillis();
				if (end - start > 10) {
					System.out
							.println((end - start) + "ms: Merge - src(" + srcIndex + "), target(" + targetIndex + ")");
				}
			}

		}
		System.out.println("Removing empty sets...");
		int beforeSize = subgraphs.size();
		for (int i = subgraphs.size() - 1; i > 0; i--) {
			if (subgraphs.get(i).size() == 0) {
				subgraphs.remove(i);
			}
		}
		int afterSize = subgraphs.size();
		System.out.println("Removed " + (beforeSize - afterSize) + " empty graphs.");
		return subgraphs;
	}

	/**
	 * 
	 * @param currGraph
	 * @param nextGraph
	 * @return NULL if merge did NOT take place, merged graph otherwise
	 */
	private static Set<Integer> conditionalMergeCommonGraphs(Set<Integer> currGraph, Set<Integer> nextGraph) {
		// Merge 2 graphs if they contain a similar item
		for (Integer item : currGraph) {
			if (nextGraph.contains(item)) {
				// Which ones are to be merged?
				return mergeSetsAndClear(currGraph, nextGraph);
			}
		}
		return null;
	}

	private static int countMerged(boolean[] merged) {
		int ret = 0;
		for (boolean b : merged) {
			if (b)
				ret++;
		}
		return ret;
	}

	/**
	 * Adds all items from s1 to s2 and then cleans all items from s1
	 * 
	 * @param s1
	 * @param s2
	 */
	private static Set<Integer> mergeSetsAndClear(Set<Integer> s1, Set<Integer> s2) {
		s1.addAll(s2);
		s2.clear();
		return s1;
	}

	private static <T> void simpleExpand(GraphNode<T> node, Set<T> ret) {
		ret.addAll(node.getPredecessors());
		ret.addAll(node.getSuccessors());
	}

	public static float percentageProgressOutput(final float part, final float total, final float step,
			final int precision) {
		if (part % step == 0) {
			float val = ((float) (part)) / ((float) (total));
			float divPrecision = (float) (Math.pow(10, precision));
			float multPerc = divPrecision * 100.0f;
			float tmpVal = (val * multPerc) / step;
			float intVal = (int) tmpVal;
			float perc_val = ((float) intVal) / divPrecision;
			System.out.println(val + "->" + tmpVal + "->" + intVal + "->" + perc_val);
			// float perc_val = ((float)((int) ((val / perc_div) * 10_000.0f))) / 100.0f;
			System.out.println("[" + perc_val + "] Merged " + part + "/" + total + " graphs.");
			return perc_val;
		}
		return -1f;
	}

	public static <T> List<Set<T>> detectSubgraphsArrayIndex(final Graph<T> g) {
		HashMap<T, GraphNode<T>> nodes = g.getNodes();
		List<Set<T>> allgraphs = new ArrayList<Set<T>>(nodes.size());
		for (int i = 0; i < nodes.size(); ++i) {
			allgraphs.add(new HashSet<T>());
		}
		for (GraphNode<T> node : nodes.values()) {

			if (allgraphs.indexOf(node.getID()) != -1 && allgraphs.get(allgraphs.indexOf(node.getID())).size() != 0) {
				GraphNode<T> parent = node;
				expandAndSet(allgraphs, allgraphs.get(allgraphs.indexOf(node.getID())), node);
			}
		}
		return null;
	}

	private static <T> void expandAndSet(List<Set<T>> allgraphs, Set<T> parentSet, GraphNode<T> node) {
		// expand node
		final List<T> neighbours = new ArrayList<T>();
		neighbours.addAll(node.getSuccessors());
		neighbours.addAll(node.getPredecessors());
		HashSet<T> tobeSet = new HashSet<T>();
		HashSet<T> alreadySet = new HashSet<T>();
		// Populates set and not-set sets
		for (T child : neighbours) {
			if (allgraphs.indexOf(child) != -1 && allgraphs.get(allgraphs.indexOf(child)).size() != 0) {
				// Means it is set already to something
				// So what we want to do is check if parent and child are set to AT LEAST ONE
				// SAME VALUE
				// As that would establish the connection
				alreadySet.add(child);
			} else {
				// Isn't set -> add it to 'tobeSet' Set, so we'll set it once we find the value
				// to set it to
				tobeSet.add(child);
			}
		}
		Set<T> valuesToSetTo = new HashSet<T>();
		for (T child : alreadySet) {
			valuesToSetTo.addAll(allgraphs.get(allgraphs.indexOf(child)));
		}
		// Just take any of them and set it to that
		if (valuesToSetTo.size() == 0) {
			System.err.println("FATAL - valuesSetTo.size == 0");
		}
		T setTo = valuesToSetTo.iterator().next();
		for (T child : neighbours) {
			allgraphs.get(allgraphs.indexOf(child)).add(setTo);
		}

	}
}
