package alu.linking.disambiguation.hops.pathbuilding;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.jena.ext.com.google.common.collect.Lists;

import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.hops.graph.Graph;
import alu.linking.disambiguation.hops.graph.GraphNode;
import alu.linking.structure.FineLoggable;

public abstract class ConcurrentPathBuilderWrapper implements FineLoggable {
	public List<String> fromFile(final EnumModelType KG, final BufferedReader readerInAllTraversalNodes,
			final BufferedWriter wrtNotFoundTraversalNodes, final boolean outputPaths, final boolean outputEdges,
			final boolean outputDirections, final boolean outputToList) throws IOException {
		// All nodes in the graph
		final HashMap<Integer, GraphNode<Integer>> possible_nodes_map = Graph.getInstance().getNodes();
		// Mapping for node IDs to String resources
		final DualHashBidiMap<Integer, String> mapping = Graph.getInstance().getIDMapping();
		// Sources nodes that will be used
		final TreeMap<Integer, GraphNode<Integer>> all_source_nodes_map = new TreeMap<>();
		// Destination nodes (if not in this set, a path is not considered valid)
		final HashSet<Integer> goalNodes = new HashSet<>();
		// Read in all the node labels we want to compute path computation for
		String br_all_attributes_line = null;
		// Nodes that were not found
		final List<String> notFoundAttributes = Lists.newArrayList();

		// Populate from a reader what should be used as sources
		while ((br_all_attributes_line = readerInAllTraversalNodes.readLine()) != null) {
			final Integer ID = mapping.getKey(br_all_attributes_line);
			if (ID == null) {
				Integer val = null;
				try {
					val = Integer.valueOf(br_all_attributes_line);
				} catch (NumberFormatException nfe) {
					val = null;
				}
				if (val == null) {
					// Even trying to read it as an integer failed, so we really can't do anything
					// more
					notFoundAttributes.add(br_all_attributes_line);
				} else {
					all_source_nodes_map.put(val, possible_nodes_map.get(val));
					goalNodes.add(val);
				}
			} else {
				all_source_nodes_map.put(ID, possible_nodes_map.get(ID));
				goalNodes.add(ID);
			}
		}
		readerInAllTraversalNodes.close();
		// Adds every node from graph as possible source/target if file is empty
		if (all_source_nodes_map.size() == 0 && goalNodes.size() == 0) {
			debug("Adding entire graph to map...");
			for (Map.Entry<Integer, GraphNode<Integer>> e : possible_nodes_map.entrySet()) {
				all_source_nodes_map.put(e.getKey(), e.getValue());
				goalNodes.add(e.getKey());
			}
		} else {
			debug("Map Size: " + all_source_nodes_map.size() + " / Goal Size: " + goalNodes.size());
		}

		if (notFoundAttributes.size() > 0) {
			warn("Could not find [" + notFoundAttributes.size() + "] items.");
			warn("Outputting not found attributes to "
					+ alu.linking.config.constants.FilePaths.FILE_HOPS_SOURCE_NODES_NOT_FOUND.getPath(KG));
			for (String s : notFoundAttributes) {
				wrtNotFoundTraversalNodes.write(s + alu.linking.config.constants.Strings.NEWLINE.val);
			}
			warn("Finished outputting not found attributes. Continuing normally (try recovering later on by simply executing path building for the ones that weren't found properly. If they weren't found due to the graph though, you probably will have to run it for all of these files. Also watch out for the log files as they'll be needed for the indices)");
			wrtNotFoundTraversalNodes.close();
		} else {
			debug("Found all attributes through mapping!");
		}
		return concurrentBuild(possible_nodes_map, all_source_nodes_map, goalNodes, true, false, false, outputToList);
	}

	/**
	 * Builds paths concurrently
	 * 
	 * @param possible_nodes_map
	 *            All possible nodes within the graph, generally input is
	 *            Graph.getInstance().getNodes()
	 * @param all_source_nodes_map
	 *            All source nodes (a path only starts with one of these)
	 * @param goalNodes
	 *            All nodes that can be considered a 'goal' (aka. a path is only
	 *            valid if it ends in one of these nodes)
	 * @param outputPaths
	 *            whether to output paths
	 * @param outputEdges
	 *            whether to output edges
	 * @param outputDirections
	 *            whether to output directions
	 * @param outputToList
	 *            whether to output to a list or to a file
	 */
	public abstract List<String> concurrentBuild(final Map<Integer, GraphNode<Integer>> possible_nodes_map,
			final Map<Integer, GraphNode<Integer>> all_source_nodes_map, final Set<Integer> goalNodes,
			final boolean outputPaths, final boolean outputEdges, final boolean outputDirections,
			final boolean outputToList);
}
