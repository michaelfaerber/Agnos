package alu.linking.disambiguation.hops.pathbuilding;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.hops.graph.GraphNode;
import alu.linking.structure.BufferedSeparableWriter;

public abstract class PathCrawlerBasic<T> implements PathCrawler<T> {
	private Logger logger = Logger.getLogger(getClass());
	public String newLine = Strings.NEWLINE.val;
	public String path_delim = Strings.HOPS_PATH_BUILDER_DELIMITER.val;

	public static long pathBuildStartTime = 0l;
	private final Writer out_nodes;
	private final Writer out_edges;
	private final Writer out_directions;

	private static AtomicInteger saveCounter = new AtomicInteger(0);
	protected final long timeOutputThreshold = 100;
	private final int saveCounterStep = 1_000_000;

	// Deep diving stuff
	private HashMap<T, Set<T>> alreadyExpanded;
	// LinkedListToString stuff

	// Path length stuff - searches paths up to length threshold
	protected final int pathLengthThreshold;

	// All paths
	private final HashSet<T> computedFor;
	protected final Map<T, GraphNode<T>> allGraphNodes;

	public PathCrawlerBasic(final EnumModelType KG, Map<T, GraphNode<T>> allNodes) throws IOException {
		this(allNodes,
				new FileWriter(alu.linking.config.constants.FilePaths.FILE_HOPS_OUTPUT_PATHS_TEMPLATE.getPath(KG)),
				new FileWriter(alu.linking.config.constants.FilePaths.FILE_HOPS_OUTPUT_EDGES_TEMPLATE.getPath(KG)),
				new FileWriter(
						alu.linking.config.constants.FilePaths.FILE_HOPS_OUTPUT_DIRECTIONS_TEMPLATE.getPath(KG)));
	}

	public PathCrawlerBasic(final Map<T, GraphNode<T>> allNodes, final Writer OUT_WRITER_NODES,
			final Writer OUT_WRITER_EDGES, final Writer OUT_WRITER_DIRECTIONS) {
		this(allNodes, OUT_WRITER_NODES, OUT_WRITER_EDGES, OUT_WRITER_DIRECTIONS,
				alu.linking.config.constants.Numbers.HOPS_PATH_LENGTH.val.intValue());
	}

	public PathCrawlerBasic(final Map<T, GraphNode<T>> allNodes, final Writer OUT_WRITER_NODES,
			final Writer OUT_WRITER_EDGES, final Writer OUT_WRITER_DIRECTIONS, final int pathLengthThreshold) {
		this.allGraphNodes = allNodes;
		this.pathLengthThreshold = pathLengthThreshold;
		computedFor = new HashSet<T>();
		out_nodes = OUT_WRITER_NODES == null ? null : OUT_WRITER_NODES;
		out_edges = OUT_WRITER_EDGES == null ? null : OUT_WRITER_EDGES;
		out_directions = OUT_WRITER_DIRECTIONS == null ? null : OUT_WRITER_DIRECTIONS;
	}

	/**
	 * Adds an item and its subitem to a map.<br>
	 * If an item does not exist yet, it adds the entry to the map. If an entry
	 * already exists, does not change anything (adds subItem to the set, but since
	 * it's a set nobody cares)
	 * 
	 * @param map     map to be added to
	 * @param item    Main node that was expanded
	 * @param subItem Subnode that is being gone through
	 */
	private void addToMap(HashMap<T, Set<T>> map, T item, T subItem) {
		Set<T> s;
		if ((s = map.get(item)) == null) {
			Set<T> set = new HashSet<T>();
			set.add(subItem);
			map.put(item, set);
		} else {
			// Sets contain unique items so it's k
			s.add(subItem);
		}
	}

	/**
	 * Transforms a linkedList into a string format to be output to a file <br>
	 * Trying to minimize characters used in order to minimize file size
	 * 
	 * @param l linkedlist to be transformed into a string
	 * @return
	 * @throws IOException
	 */
	private <O> String linkedListToString(final LinkedList<O> l) throws IOException {
		// Integer[] pathsIntArr = l.toArray(new Integer[0]);
		// return saveCounter+" - "+Arrays.toString(pathsIntArr) + newLine
		final BufferedWriter linkedListStringBuilder = new BufferedWriter(new StringWriter());
		write(linkedListStringBuilder, l);
		final String ret = linkedListStringBuilder.toString();
		return ret;
	}

	/**
	 * Stores path if first and last items are amazon products
	 * 
	 * @param l path
	 */
	public void storeAmazonPath(LinkedList<T> l) {
		try {
			write(out_nodes, l);
			write(out_edges, getEdgesForPathAndWriteDirections(l, this.allGraphNodes));
			if (saveCounter.incrementAndGet() % saveCounterStep == 0) {
				logger.info((System.currentTimeMillis() - pathBuildStartTime) + " - Counter: " + saveCounter);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns edges for given paths and writes out the directions appropriately
	 * into the appropriate directions file
	 * 
	 * @author Kris Noullet (kn65)
	 * @param path  sequence of nodes
	 * @param nodes ID to GraphNode mapping
	 * @return sequence of edges for passed path
	 */
	private LinkedList<String> getEdgesForPathAndWriteDirections(LinkedList<T> path, Map<T, GraphNode<T>> nodes) {
		if (out_edges == null && out_directions == null) {
			return null;
		}
		final LinkedList<String> l_edges = new LinkedList<>();
		try {
			// Populate edges
			T[] l_arr = (T[]) path.toArray();
			for (int i = 0; i < l_arr.length; ++i) {
				/*
				 * We have a node, is the 'i+1'-th node a 'previous' or a 'next' node?
				 */
				String edge = null;
				if (i < l_arr.length - 1) {
					final GraphNode<T> n1 = nodes.get((T) (l_arr[i]));
					final GraphNode<T> n2 = nodes.get((T) (l_arr[i + 1]));
					final List<T> predList = n1.getPredecessors();
					final List<T> nextList = n1.getSuccessors();
					final int predIndex = predList.indexOf((T) (l_arr[i + 1]));
					final int nextIndex = nextList.indexOf((T) (l_arr[i + 1]));
					String writeDirection = null;
					if (predIndex == -1 && nextIndex != -1) {
						// It's in Next
						edge = n1.getSuccessorEdges().get(nextIndex).toString();
						writeDirection = Strings.EDGE_DIR_CODE_NEXT_STR.val;
					} else if (predIndex != -1 && nextIndex == -1) {
						// It's in prev
						edge = n1.getPredecessorEdges().get(predIndex).toString();
						writeDirection = Strings.EDGE_DIR_CODE_PREV_STR.val;
					} else if (predIndex == -1 && nextIndex == -1) {
						// It's in neither
						edge = "";
						System.err.println(n1.getID() + "->" + n2.getID());
						writeDirection = Strings.EDGE_DIR_CODE_NONE_STR.val;
					} else {
						// It's in both -> unexpected
						/*
						 * System.err.println("ERROR: Node(" + n2.getID() +
						 * ") in both in PRED and SUCC for node(" + n1.getID() + ")");
						 */
						edge = n1.getPredecessorEdges().get(predIndex).toString() + "/"
								+ n1.getSuccessorEdges().get(nextIndex).toString();
						writeDirection = Strings.EDGE_DIR_CODE_BOTH_STR.val;
					}
					write(out_directions, writeDirection, Strings.HOPS_PATH_BUILDER_DELIMITER.val);
					l_edges.add(edge);
				}
			}
			newLine(out_directions);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return l_edges;
	}

	/**
	 * Returns edges for given path
	 * 
	 * @author Kris Noullet (kn65)
	 * @param path  sequence of nodes
	 * @param nodes Mapping of IDs to GraphNode objects
	 * @return edges for given path
	 */
	private LinkedList<String> getEdgesForPath(LinkedList<T> path, Map<T, GraphNode<T>> nodes) {
		final LinkedList<String> l_edges = new LinkedList<>();
		try {
			// Populate edges
			Object[] l_arr = path.toArray();
			for (int i = 0; i < l_arr.length; ++i) {
				/*
				 * We have a node, is the 'i+1'-th node a 'previous' or a 'next' node?
				 */
				String edge = "";
				if (i < l_arr.length - 1) {
					final GraphNode<T> n1 = nodes.get((T) (l_arr[i]));
					// GraphNode n2 = nodes.get((T) (l_arr[i + 1]));
					final List<T> predList = n1.getPredecessors();
					final List<T> nextList = n1.getSuccessors();
					final List<T> predEdgesList = n1.getPredecessorEdges();
					final List<T> nextEdgesList = n1.getSuccessorEdges();

					// ------------ Multiple Occurrences - START
					for (int x = 0; x < predList.size(); ++x) {
						if (predList.get(x).equals((T) (l_arr[i + 1]))) {
							edge += predEdgesList.get(x) + Strings.PATH_EDGE_EDGE_DELIM.val;
						}
					}
					for (int x = 0; x < nextList.size(); ++x) {
						if (nextList.get(x).equals((T) (l_arr[i + 1]))) {
							edge += nextEdgesList.get(x) + Strings.PATH_EDGE_EDGE_DELIM.val;
						}
					}
					// ------------ Multiple Occurrences - END
					edge = edge.substring(0, edge.length() - Strings.PATH_EDGE_EDGE_DELIM.val.length());
					l_edges.add(edge);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return l_edges;
	}

	/**
	 * Stores any path passed as a linkedlist
	 * 
	 * @param l Single path
	 */
	public void storeAnyPath(LinkedList<T> l) {
		try {
			write(out_nodes, l);
			write(out_edges, getEdgesForPathAndWriteDirections(l, this.allGraphNodes));
			if (saveCounter.incrementAndGet() % saveCounterStep == 0) {
				System.out.println((System.currentTimeMillis() - pathBuildStartTime) + " - Counter: " + saveCounter);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private final StringBuilder output = new StringBuilder();

	/**
	 * Stores any path (including edges) passed as a linkedlist
	 * 
	 * @param l Single path
	 */
	public void storeAnyPathWEdges(LinkedList<T> l) {
		try {
			LinkedList<String> edges = getEdgesForPath(l, this.allGraphNodes);
			final Iterator<String> it_edges = edges.iterator();
			final Iterator<T> it_nodes = l.iterator();
			output.setLength(0);
			List<StringBuilder> multiPathBuilders = Lists.newArrayList();
			multiPathBuilders.add(output);
			while (it_nodes.hasNext()) {
				final T nextNode = it_nodes.next();
				outputToBuilders(multiPathBuilders, nextNode);
				if (it_edges.hasNext()) {
					final String possibleMultiEdge = it_edges.next().toString();
					if (possibleMultiEdge.contains(Strings.PATH_EDGE_EDGE_DELIM.val)) {
						// It's got multiple possible edges, so duplicate the
						// path at this point
						final String[] multiEdges = possibleMultiEdge.split(Strings.PATH_EDGE_EDGE_DELIM.val);
						List<StringBuilder> tmpBuilders = Lists.newArrayList();
						for (String edge : multiEdges) {
							for (StringBuilder sb : multiPathBuilders) {
								tmpBuilders.add(new StringBuilder(sb.toString()
										+ Strings.HOPS_PATH_BUILDER_DELIMITER.val + Strings.PATH_EDGE_DELIM_START.val
										+ edge + Strings.PATH_EDGE_DELIM_END.val));
							}
						}
						multiPathBuilders = tmpBuilders;
					} else {
						outputToBuilders(multiPathBuilders,
								Strings.HOPS_PATH_BUILDER_DELIMITER.val + Strings.PATH_EDGE_DELIM_START.val
										+ possibleMultiEdge + Strings.PATH_EDGE_DELIM_END.val);
					}
				}
				outputToBuilders(multiPathBuilders, Strings.HOPS_PATH_BUILDER_DELIMITER.val);
			}
			if (it_nodes.hasNext() || it_edges.hasNext()) {
				outputToBuilders(multiPathBuilders, "[ERROR]");
			}
			outputToBuilders(multiPathBuilders, newLine);
			for (StringBuilder sb : multiPathBuilders) {
				write(out_nodes, sb.toString());
			}
			output.setLength(0);
			if (saveCounter.addAndGet(multiPathBuilders.size()) % saveCounterStep < 2) {
				System.out.println((System.currentTimeMillis() - pathBuildStartTime) + " - Counter: " + saveCounter);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void outputToBuilders(final List<StringBuilder> l, final Object str) {
		for (StringBuilder sb : l) {
			sb.append(str);
		}
	}

	/**
	 * Stores any path (NOT EDGES NOR DIRECTIONS) passed as a linkedlist<br>
	 * Does the same as {@link #storeAnyPath(LinkedList)}, except that this method
	 * does not write out the directions nor edges
	 * 
	 * @param l Single path
	 */
	public void storeAnyJustPath(LinkedList<T> l) {
		try {
			write(out_nodes, l);
			if (saveCounter.incrementAndGet() % saveCounterStep == 0) {
				System.out.println((System.currentTimeMillis() - pathBuildStartTime) + " - Counter: " + saveCounter);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Clones given array list and adds passed item to the newly created one
	 * 
	 * @param src  Linkedlist containing path to be extended
	 * @param item Item by which to extend linkedlist by
	 * @return new linkedlist object containing all previous items additionally to
	 *         the passed one
	 */
	protected LinkedList<T> extend(final LinkedList<T> src, final T item) {
		/*
		 * final LinkedList<T> l_ext = new LinkedList<T>(); Iterator<T> iter =
		 * src.iterator(); while (iter.hasNext()) { l_ext.add(iter.next()); }
		 * l_ext.add(item); return l_ext;
		 */

		long extendStartTime = System.currentTimeMillis();
		final LinkedList<T> retList = (LinkedList<T>) src.clone();
		retList.add(item);
		long extendTime = System.currentTimeMillis() - extendStartTime;
		if (extendTime > timeOutputThreshold) {
			logger.warn(Arrays.toString(retList.toArray()) + " - Extension: " + extendTime + ", " + src.size());
		}
		return retList;
	}

	/**
	 * Closes the outputs defined during PathCrawler instantiation
	 */
	@Override
	public void closeOutputs() {
		try {
			if (out_nodes != null) {
				out_nodes.close();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		try {
			if (out_edges != null) {
				out_edges.close();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * PathCrawlerBasic's implementation of storePath.<br>
	 * Stores a path by adding it to the passed return list
	 */
	@Override
	public void storePath(LinkedHashSet<LinkedList<T>> ret, LinkedList<T> l) {
		ret.add(l);
	}

	/**
	 * PathCrawlerBasic's implementation of storePath.<br>
	 * Short-hand method for calling storeNonAmazonPath(LinkedList<T>)
	 */
	@Override
	public void storePath(LinkedList<T> l) {
		storeAnyPath(l);
	}

	protected void write(Writer wrt, String... content) throws IOException {
		if (wrt != null && content != null) {
			for (String str : content) {
				if (str != null) {
					wrt.write(str);
					wrt.write(alu.linking.config.constants.Strings.HOPS_PATH_BUILDER_DELIMITER.val);
				}
			}
			newLine(wrt);
		}
	}

	protected void write(Writer wrt, LinkedList l) throws IOException {
		if (wrt != null && l != null) {
			// l = one path
			for (Object str : l) {
				// Print out each element of the path
				wrt.write(str.toString());
				wrt.write(path_delim);
			}
			// Once one path is written out, add a new line
			newLine(wrt);
		}
	}

	protected void write(Writer wrt, String content) throws IOException {
		if (wrt != null && content != null) {
			wrt.write(content);
		}
	}

	public PathCrawlerBasic<T> noDelimiter() {
		this.path_delim = "";
		return this;
	}

	public PathCrawlerBasic<T> noNewline() {
		this.newLine = "";
		return this;
	}

	protected void newLine(Writer wrt) throws IOException {
		if (wrt != null) {
			if (wrt instanceof BufferedSeparableWriter) {
				((BufferedSeparableWriter) wrt).newEntry();
			} else if (wrt instanceof BufferedWriter) {
				((BufferedWriter) wrt).newLine();
			} else {
				write(wrt, newLine);
			}
		}
		// write(wrt, newLine);
	}

	public Writer getOut_nodes() {
		return out_nodes;
	}

	public Writer getOut_edges() {
		return out_edges;
	}

	public Writer getOut_directions() {
		return out_directions;
	}

}
