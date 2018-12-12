package alu.linking.disambiguation.hops.pathbuilding;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.jena.ext.com.google.common.collect.Lists;

import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.hops.graph.GraphNode;
import alu.linking.utils.Stopwatch;

/**
 * Concurrently builds paths between nodes based on Graph singleton
 */
public class ConcurrentPathBuilderBatch extends ConcurrentPathBuilderWrapper {

	@Override
	public List<String> concurrentBuild(final Map<Integer, GraphNode<Integer>> possible_nodes_map,
			final Map<Integer, GraphNode<Integer>> all_source_nodes_map, final Set<Integer> goalNodes,
			final boolean outputPaths, final boolean outputEdges, final boolean outputDirections,
			final boolean outputToList) {
		final List<String> files_created_paths = Lists.newArrayList();
		final EnumModelType KG = EnumModelType.DEFAULT;
		try {
			debug("File computations will be done for " + all_source_nodes_map.size()
					+ " STARTING attributes. (Instead of " + possible_nodes_map.size()
					+ " nodes (# of nodes in entire Graph). Difference (aka. nodes not found in graph or simply limited): "
					+ (possible_nodes_map.size() - all_source_nodes_map.size()));

			// #########################################
			// How tasks are split - START
			// #########################################
			int stepVal = (int) Math.floor(all_source_nodes_map.size()
					/ alu.linking.config.constants.Numbers.HOPS_THREAD_AMT.val.floatValue());

			final int step;
			if (stepVal != 0)
				step = stepVal;
			else
				step = 1;
			final int lastStep = (int) (Math.ceil(((double) all_source_nodes_map.size())
					/ (alu.linking.config.constants.Numbers.HOPS_THREAD_AMT.val.doubleValue())));
			final Integer[][] threadNodes = new Integer[alu.linking.config.constants.Numbers.HOPS_THREAD_AMT.val
					.intValue()][];
			// Allocate appropriate array space
			for (int i = 0; i < threadNodes.length; ++i) {
				final int size;
				if (i != threadNodes.length - 1) {
					size = step;
				} else {
					size = lastStep;
				}
				threadNodes[i] = new Integer[size];
			}
			int counter = 0;
			for (Map.Entry<Integer, GraphNode<Integer>> e : all_source_nodes_map.entrySet()) {
				int bucketVal = counter;
				int tn_index = bucketVal / step;
				if (tn_index >= threadNodes.length) {
					tn_index = threadNodes.length - 1;
				}
				int threadCapacity = threadNodes[tn_index].length;
				int x = tn_index;
				int y = bucketVal % threadCapacity;
				threadNodes[x][y] = e.getKey();
				counter++;
			}
			int sumSize = 0;
			for (Integer[] nodes : threadNodes) {
				sumSize += nodes.length;
			}
			if (sumSize != all_source_nodes_map.size() && (all_source_nodes_map
					.size() >= alu.linking.config.constants.Numbers.HOPS_THREAD_AMT.val.intValue())) {
				error("ERROR INITIALISING ARRAYS: MAP(" + all_source_nodes_map.size() + ") vs ARR(" + sumSize + "):");
				for (Integer[] nodes : threadNodes) {
					debug(nodes.length + ",");
				}
			}
			// #########################################
			// How tasks are split - END
			// #########################################

			// #########################################
			// Task preparation / execution
			// #########################################

			final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
			Stopwatch.start(this.getClass().getName());
			final List<String> foundPaths = Lists.newArrayList();
			final List<Future<List<String>>> futures = Lists.newArrayList();
			// Create tasks for the threads to execute on loop
			for (Integer[] srcNodes : threadNodes) {
				if (srcNodes == null || srcNodes.length == 0) {
					continue;
				}
				// Taking smallest and biggest nodes to know the range
				final List<Integer> nodeList = Arrays.asList(srcNodes);
				final Integer firstNode = Collections.min(nodeList);
				final Integer lastNode = Collections.max(nodeList);
				if (firstNode == null || lastNode == null) {
					// Doesn't make sense for one to be null without the other one being as worst
					// case, if it's a single element, max and min should be equal
					continue;
				}
				// Where to output the i-th range of paths to
				final String out_path = alu.linking.config.constants.FilePaths.FILE_HOPS_OUTPUT_PATHS_TEMPLATE
						.getPath(KG) + firstNode + "-" + lastNode + ".txt";
				// Where to output the i-th range of edges to
				final String out_edges = alu.linking.config.constants.FilePaths.FILE_HOPS_OUTPUT_EDGES_TEMPLATE
						.getPath(KG) + firstNode + "-" + lastNode + ".txt";
				// Where to output the i-th range of directions to
				final String out_directions = alu.linking.config.constants.FilePaths.FILE_HOPS_OUTPUT_DIRECTIONS_TEMPLATE
						.getPath(KG) + firstNode + "-" + lastNode + ".txt";
				// #########################################
				// Determining output / writer type
				// #########################################
				// If we just want the output in a list,
				// we do not need paths (null paths to the
				// factory are taken as StringListWriters)
				final String out_paths_path = outputToList ? null : out_path;
				final String out_edges_path = outputToList ? null : out_edges;
				final String out_dirs_path = outputToList ? null : out_directions;
				final Writer pathsWriter = outputPaths ? PathWriterFactory.create(out_paths_path) : null;
				final Writer edgesWriter = outputEdges ? PathWriterFactory.create(out_edges_path) : null;
				final Writer directionsWriter = outputDirections ? PathWriterFactory.create(out_dirs_path) : null;

				// #########################################
				// Instantiate Crawler + its wrapping
				// PathBuilder 'task'
				// #########################################
				final PathCrawler<Integer> crawler = new PathCrawlerDeepDiving<Integer>(possible_nodes_map, pathsWriter,
						edgesWriter, directionsWriter, goalNodes);
				final PathBuilder<Integer> task = new PathBuilder<Integer>(srcNodes, crawler, possible_nodes_map);
				// #########################################
				// Logging created files
				// #########################################
				if (!outputToList) {
					if (pathsWriter != null) {
						// There is a writer and it is not a list writer
						files_created_paths.add(out_path);
					}
					if (edgesWriter != null) {
						files_created_paths.add(out_edges);
					}
					if (directionsWriter != null) {
						files_created_paths.add(out_directions);
					}
				}
				debug("Started new PathBuilder - FROM(" + firstNode + ")-TO(" + lastNode + ")");
				// executor.execute(task);
				final Future<List<String>> future = executor.submit(task);
				futures.add(future);
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			for (Future<List<String>> f : futures) {
				// Grab all paths we found
				List<String> listPaths = f.get();
				if (listPaths != null) {
					foundPaths.addAll(listPaths);
					debug("Adding: " + listPaths.size());
				} else {
					debug("Null list found.");
				}
			}
			debug("Found total of paths: " + foundPaths.size());
			debug("[ConcurrentPathBuilder] Finished executing all threads. (Duration: "
					+ (Stopwatch.endDiff(this.getClass().getName())) + "ms)");
			return foundPaths;
		} catch (FileNotFoundException fnfe) {
			error("If exception is thrown due to " + alu.linking.config.constants.FilePaths.FILE_HOPS_SOURCE_NODES.getPath(KG)
					+ ", you need to first compute all the product attributes, then call this.");
			fnfe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(
						new FileWriter(alu.linking.config.constants.FilePaths.LOG_FILE_HOPS_FILES_CREATED.getPath(KG)));
				// Log which path files were created
				for (String s : files_created_paths) {
					bw.write(s + alu.linking.config.constants.Strings.NEWLINE.val);
				}
				bw.flush();
			} catch (IOException e) {
				e.printStackTrace();
				error("Could not log the path filenames to "
						+ alu.linking.config.constants.FilePaths.LOG_FILE_HOPS_FILES_CREATED.getPath(KG));
			} finally {
				if (bw != null) {
					try {
						bw.close();
					} catch (IOException e1) {
						e1.printStackTrace();
						error("Could not close writer.");
					}
				}
			}
		}
		return null;
	}
}
