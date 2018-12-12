package alu.linking.disambiguation.hops.pathbuilding;

import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import alu.linking.disambiguation.hops.graph.Graph;
import alu.linking.disambiguation.hops.graph.GraphNode;
import alu.linking.structure.FineLoggable;

public class PathBuilder<T> implements Callable<List<String>>, FineLoggable {
	private Logger logger = Logger.getLogger(getClass());

	private static final String newLine = alu.linking.config.constants.Strings.NEWLINE.val;
	private PathCrawler<T> pc;
	public static final double max = Math.pow((double) (Graph.getInstance().getNodes().size()), 3.0);
	private final Map<T, GraphNode<T>> allNodes;
	/**
	 * Entrypoint to finding all paths from a pre-computed path taken from
	 * Graph.getInstance()
	 */

	private final T[] srcNodes;

	public PathBuilder(T[] srcNodes, PathCrawler<T> pc, Map<T, GraphNode<T>> allNodes) {
		this.pc = pc;
		this.srcNodes = srcNodes;
		this.allNodes = allNodes;
	}

	/**
	 * Gets nodes from Graph.getNodes()
	 */
	public void findPaths() {

		final boolean display = false;
		// Display all nodes
		if (display) {
			for (T node : srcNodes) {
				final GraphNode<T> gnode = allNodes.get(node);
				debug(System.out.format("%-60s %-60s %-60s \n", node, "- pre[" + gnode.getPredecessors(),
						"succ[" + gnode.getSuccessors() + "]").toString());
			}
		}
		// Pass through all nodes to determine paths
		PathCrawlerBasic.pathBuildStartTime = System.currentTimeMillis();
		try {
			for (T idnode : srcNodes) {
				// As long as no doubled element exists in the path, it's k
				final GraphNode<T> gnode = allNodes.get(idnode);

				if (gnode != null) {
					if (display) {
						debug("Searching for START(" + idnode + ")");
					}
					final LinkedList<T> l = new LinkedList<T>();
					l.add(idnode);
					pc.crawlPaths(l);
				}
			}
			long endTime = System.currentTimeMillis();
			debug("[COMPUTE PATHS] Execution time: " + (endTime - PathCrawlerBasic.pathBuildStartTime) + "ms");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			List<T> nodeList = Arrays.asList(srcNodes);
			debug("[CLOSING WRITERS] Closing outputs (First/Last Elements) [" + nodeList.get(0) + "-"
					+ nodeList.get(nodeList.size() - 1) + "]");
			pc.closeOutputs();
		}
	}

	public T[] getNodes() {
		return this.srcNodes;
	}

	public static void outputNodeIDs() {
		Graph.getInstance().outputNodeIDs();
	}

	public static void outputPredicateIDs() {
		Graph.getInstance().outputPredicateIDs();
	}

	@Override
	public List<String> call() throws Exception {
		findPaths();
		if (pc instanceof PathCrawlerBasic) {
			final Writer wrt = ((PathCrawlerBasic) pc).getOut_nodes();
			if (wrt instanceof StringListWriter) {
				return ((StringListWriter) wrt).getEntries();
			}
		}
		return null;
	}

}
