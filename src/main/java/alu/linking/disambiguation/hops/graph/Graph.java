package alu.linking.disambiguation.hops.graph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;

/**
 * Graph implementation. Used to generate graph from Triple data
 *
 * @author Kris Noullet (kn65)
 *
 */
public class Graph<T> {
	private static Logger logger = Logger.getLogger(Graph.class);

	private final Incrementable<T> idCounterInit;
	private HashMap<T, GraphNode<T>> nodes = new LinkedHashMap<T, GraphNode<T>>();
	private DualHashBidiMap<T, String> idPredicates = new DualHashBidiMap<T, String>();
	private DualHashBidiMap<T, String> idNodes = new DualHashBidiMap<T, String>();
	private Incrementable<T> idCounter;
	private Incrementable<T> idPredicateCounter;
	private String source = null;
	public static final String dumpSeparator = alu.linking.config.constants.Strings.HOPS_GRAPH_DUMP_SEPARATOR.val;
	private static Set<String> subjectBlacklist = new HashSet<String>();
	private static Set<String> predicateBlacklist = new HashSet<String>();
	private static Set<String> objectBlacklist = new HashSet<String>();
	private EnumModelType KG = null;

	/**
	 * Private wrapper instance used to ensure singleton creation (ensured by Java's
	 * class loader)
	 * 
	 * @author Kristian Noullet
	 *
	 */
	private static class GraphWrapper {
		static Graph<Integer> INSTANCE = new Graph<Integer>(EnumModelType.DEFAULT, new IntegerIncrementable(0l),
				new IntegerIncrementable(0l), new IntegerIncrementable(0l));
	}

	private Graph(final EnumModelType KG, Incrementable<T> counterInit, Incrementable<T> idCounter,
			Incrementable<T> idPredicateCounter) {
		this.idCounterInit = counterInit;
		this.idCounter = idCounter;
		this.idPredicateCounter = idPredicateCounter;
		this.KG = KG;
	}

	public static Graph<Integer> getInstance() {
		return GraphWrapper.INSTANCE;
	}

	/**
	 * Generates a unique node ID and keeps track of the NODE_ID<->NODE_STR
	 * mapping.<br/>
	 * 
	 * @param node
	 * @return
	 */
	private T generateID(final String node) {
		idCounter.increase();
		idNodes.put(idCounter.getVal(), node);
		return idCounter.getVal();
	}

	/**
	 * Generates a unique predicate ID and keeps track of the NODE_ID<->NODE_STR
	 * mapping.<br/>
	 * 
	 * @param node
	 * @return
	 */
	private T generatePredicateID(final String predicate) {
		idPredicateCounter.increase();
		idPredicates.put(idPredicateCounter.getVal(), predicate);
		return idPredicateCounter.getVal();
	}

	int selfLoopCounter = 0;

	/**
	 * Adds a node to the graph if it doesn't exist yet and it updates the
	 * connectivities to the their successors. </br>
	 * Additionally creates the successor node(s), one per call (due to information
	 * being sent in triples)</br>
	 * So there's pretty much 2 approaches:</br>
	 * 1. Create a node and give it a list of its successor(s) (one per call)</br>
	 * 2. Same as above, but additionally create a node for the successor in order
	 * to set whether it's an attribute or category</br>
	 * Initially option 1. was implemented, currently option 2. is through calls to
	 * createToNode(...)
	 * 
	 * @param from
	 * @param to
	 */
	public void addNode(final org.semanticweb.yars.nx.Node s, final org.semanticweb.yars.nx.Node p,
			final org.semanticweb.yars.nx.Node o) {
		addNode(s.toString(), p.toString(), o.toString());
	}

	/**
	 * Pretty much the entry point
	 * 
	 * @param from
	 * @param predicate
	 * @param to
	 */
	public void addNode(final String from, final String predicate, final String to) {
		final boolean selfLoop = from.equals(to);
		if (selfLoop) {
			// Avoid self loops
			return;
		}

		if (subjectBlacklist.contains(from) || predicateBlacklist.contains(predicate) || objectBlacklist.contains(to)) {
			// Jump out if any are blacklisted
			return;
		}
		T idFrom = getOrGenerateID(from);
		T idTo = getOrGenerateID(to);
		GraphNode<T> fromNode;
		final GraphNode<T> toNode;
		if ((fromNode = nodes.get(idFrom)) == null) {
			// Doesn't exist yet
			fromNode = createFromNode(from, to, predicate);
		} else {
			updateSuccessors(fromNode, to, predicate);
		}
		if ((toNode = nodes.get(idTo)) == null) {
			// Doesn't exist yet
			createToNode(to, predicate, from);
		} else {
			try {
				updatePredecessors(to, from, predicate);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// FLAGGING
		doFlags(fromNode, from, predicate, toNode, to);

	}

	/**
	 * Activates all the appropriate flags for the passed 'triple' where the subject
	 * and object have already been transformed into appropriate GraphNodes (with
	 * the exception of flags).<br>
	 * <b>Note</b>: The predicate only serves to determine some flags.<br>
	 * <b>Note</b>: Requires the labels separately due to the possibility of
	 * disabling labels from graphnodes (they are merely kept to make file debugging
	 * more human-readable)
	 * 
	 * @param fromNode
	 * @param fromLabel
	 * @param predicate
	 * @param toNode
	 * @param toLabel
	 */
	private void doFlags(GraphNode<T> fromNode, String fromLabel, String predicate, GraphNode<T> toNode,
			String toLabel) {
		fromLabel = fromLabel == null && fromNode != null ? fromNode.getLabel() : fromLabel;
		toLabel = toLabel == null && toNode != null ? toNode.getLabel() : toLabel;
		// Nothing should be null at this point

		if (predicate.contains(Strings.PRED_HELPING_SURFACE_FORM.val) && toNode != null) {
			// They're connected through this predicate, so the object node is a helping
			// surface form
			toNode.flagHelpingSurfaceform();
		} else if (toNode != null && toLabel != null) {
			if (toLabel.contains(Strings.RDF_TYPED_LITERAL_STRING.val)) {
				// It is a STRING type
				toNode.flagSurfaceform();
			}
		}

		if (fromLabel != null && fromNode != null) {
			if (fromLabel.contains(Strings.RDF_BLANK_NODE_PREFIX.val)) {
				// It's a blank node...
				fromNode.flagBlanknode();
			}
		}

		if (toLabel != null && toNode != null) {
			if (toLabel.contains(Strings.RDF_BLANK_NODE_PREFIX.val)) {
				// It's a blank node...
				toNode.flagBlanknode();
			}
		}

	}

	/**
	 * When do we need to update precessors? When it is created, its direct
	 * predecessor is added right away. Meaning: we have to check if it already
	 * exists!
	 * 
	 * @param toNode
	 * @param fromNode
	 * @throws Exception when passed predecessor somehow turns out to be null
	 */
	private void updatePredecessors(final String toNode, final String fromNode, final String predicate)
			throws Exception {
		GraphNode<T> n;
		if ((n = nodes.get(getOrGenerateID(toNode))) != null && nodes.get(getOrGenerateID(fromNode)) != null) {
			boolean found = false;
			// Checks if the successor is already known
			final T idNodeFrom = getOrGenerateID(fromNode);
			final T predicateId = getOrGeneratePredicateID(predicate);

			int i = 0;

			for (T s : n.getPredecessors()) {
				if (s.equals(idNodeFrom) && n.getPredecessorEdges().get(i).equals(predicateId)) {
					found = true;
					break;
				}
				i++;
			}
			if (!found) {
				// DEBUGGING - START
				// logger.debug(n.getID() + ":<-" + idNodeFrom);
				// DEBUGGING - END
				// Predecessor wasn't found in array, so add it
				n.addPredecessor(idNodeFrom, predicateId);
				// ADDED FOR TESTING - START
				createFromNode(fromNode, toNode, predicate);
				// ADDED FOR TESTING - END

			} else {
				// Predecessor is already known in prev-array, so pretty much
				// don't do anything!
			}
		}
	}

	/**
	 * Updates successors for a GraphNode 'from'
	 * 
	 * @author Kris Noullet (kn65)
	 * @param n         from node
	 * @param to        successor of n
	 * @param predicate how 'to' and 'n' are linked
	 */
	private void updateSuccessors(final GraphNode<T> n, final String to, final String predicate) {
		boolean found = false;
		final T idNodeTo = getOrGenerateID(to);
		final T predicateId = getOrGeneratePredicateID(predicate);
		// Checks if the successor is already known aka. in successor list
		int i = 0;
		for (T s : n.getSuccessors()) {
			if (s.equals(idNodeTo) && n.getSuccessorEdges().get(i).equals(predicateId)) {
				found = true;
				break;
			}
			i++;
		}
		if (!found) {
			// Successor wasn't found, so add it & create the resulting
			// node!
			n.addSuccessor(idNodeTo, predicateId);
			createToNode(to, predicate, n.getLabel());
		} else {
			// It already exists, so pretty much don't do anything!
		}
	}

	/**
	 * Creates node only if it doesn't exist yet and adds it to the HashMap
	 * 
	 * @param from 'from' node
	 * @param to   'to' node
	 */
	private GraphNode<T> createFromNode(final String from, final String to, final String predicate) {
		final T idNodeFrom = getOrGenerateID(from);
		GraphNode<T> ret;
		if ((ret = nodes.get(idNodeFrom)) == null) {
			try {
				final ArrayList<T> toNode = new ArrayList<T>();
				toNode.add(getOrGenerateID(to));
				final ArrayList<T> pred = new ArrayList<T>();
				pred.add(getOrGeneratePredicateID(predicate));
				GraphNode<T> newNodeFrom = new GraphNode<T>(idNodeFrom, from, toNode, null, pred, null);
				nodes.put(idNodeFrom, newNodeFrom);
				ret = newNodeFrom;
			} catch (NullPointerException npe) {
				logger.error("createFromNode - " + from + " / " + to + " / " + predicate);
				throw npe;
			}
		}
		return ret;
	}

	/**
	 * Creates node only if it doesn't exist yet and adds it to the HashMap
	 * 
	 * @param to        node to be created
	 * @param predicate needs predicate to check for category/attribute
	 * @param from      node that it is coming from
	 */
	private GraphNode<T> createToNode(final String to, final String predicate, final String from) {
		final T idNodeTo = getOrGenerateID(to);
		GraphNode<T> ret;
		if ((ret = nodes.get(idNodeTo)) == null) {
			ArrayList<T> fromNodeList = new ArrayList<T>();
			fromNodeList.add(getOrGenerateID(from));
			ArrayList<T> predicateList = new ArrayList<T>();
			predicateList.add(getOrGeneratePredicateID(predicate));

			GraphNode<T> newNodeTo = new GraphNode<T>(idNodeTo, to, null, fromNodeList, null, predicateList);
			nodes.put(idNodeTo, newNodeTo);
			ret = newNodeTo;
		}
		return ret;
	}

	/**
	 * Checks whether a given string's node has been given an ID yet.<br/>
	 * If it has, it returns the ID it was given, else it generates a new one and
	 * registers it within the mapping through generateID(String)
	 * 
	 * @param node
	 * @return
	 */
	private T getOrGenerateID(final String node) {
		final T idNode = idNodes.getKey(node);
		if (idNode != null) {
			return idNode;
		} else {
			return generateID(node);
		}
	}

	/**
	 * Generates IDs for predicates
	 * 
	 * @param node
	 * @return
	 */
	private T getOrGeneratePredicateID(final String predicate) {
		final T idPredicate = idPredicates.getKey(predicate);
		if (idPredicate != null) {
			return idPredicate;
		} else {
			return generatePredicateID(predicate);
		}
	}

	/**
	 * Returns all node IDs and their related GraphNodes
	 * 
	 * @author Kris Noullet (kn65)
	 * @return all node IDs and related GraphNodes
	 */
	public HashMap<T, GraphNode<T>> getNodes() {
		return this.nodes;
	}

	public HashMap<T, GraphNode<T>> getNodes(T type) {
		return this.nodes;
	}

	/**
	 * Returns mapping of node ID to node's string representation
	 * 
	 * @author Kris Noullet (kn65)
	 * @return two-sided mapping for node IDs and their respective string
	 *         representations
	 */
	public DualHashBidiMap<T, String> getIDMapping() {
		return this.idNodes;
	}

	/**
	 * Returns mapping of predicate ID to its string representation
	 * 
	 * @author Kris Noullet (kn65)
	 * @return two-sided mapping for predicate IDs and their respective string
	 *         representation
	 */
	public DualHashBidiMap<T, String> getPredicateIDMapping() {
		return this.idPredicates;
	}

	/**
	 * Resets the ID counters, empties the lists etc. Overall, it just resets the
	 * graph's entire state
	 * 
	 * @author Kris Noullet (kn65)
	 */
	public void reset() {
		source = "reset";
		this.nodes.clear();
		this.idNodes.clear();
		this.idPredicates.clear();
		this.idCounter = idCounterInit;
		this.idPredicateCounter = idCounterInit;
	}

	private static int lineCounter = 0;

	/**
	 * Populates Graph with nodes and the mappings of ID<->Name from given reader
	 * sources.
	 * 
	 * @param rdGraph             Reader for actual graph
	 * @param rdGraphIDs          Reader for nodes' mappings
	 * @param rdGraphPredicateIDs Reader for predicates' mappings
	 */
	public void readIn(final Reader rdGraph, final Reader rdGraphIDs, final Reader rdGraphPredicateIDs) {
		source = "readIn";
		if (rdGraph != null) {
			readIn(rdGraph);
		} else {
			logger.warn("Null reader passed for Graph readIn");
		}
		if (rdGraphIDs != null) {
			readInNodeIDs(rdGraphIDs);
		} else {
			logger.warn("Null reader passed for Node IDs readIn");
		}
		if (rdGraphPredicateIDs != null) {
			readInPredicateIDs(rdGraphPredicateIDs);
		} else {
			logger.warn("Null reader passed for Predicate IDs readIn");
		}
	}

	/**
	 * Reads the graph from a Reader and adds the nodes.<br>
	 * Note: Does NOT populate the ID mappings maps
	 */
	public static void readIn(final Reader rd) {
		Graph.GraphWrapper.INSTANCE.nodes.clear();
		lineCounter = 0;
		try {
			String line;
			final BufferedReader br;
			if (rd instanceof BufferedReader) {
				br = (BufferedReader) rd;
			} else {
				br = new BufferedReader(rd);
			}
			while ((line = br.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, dumpSeparator);
				int counter = 0;
				Integer id = -1;
				String name = null;
				ArrayList<Integer> next = null, prev = null, nextEdges = null, prevEdges = null;
				boolean category = false, blankNode = false, surfaceform = false, helpingSurfaceform = false,
						entity = false;
				while (st.hasMoreTokens()) {
					// ID;name;next;prev;category
					final String token = st.nextToken();
					switch (counter) {
					case 0:
						id = new Integer(token);
						break;
					case 1:
						name = token;
						break;
					case 2:
						next = new ArrayList<Integer>(Arrays.asList(fromStringToArray(token)));
						break;
					case 3:
						prev = new ArrayList<Integer>(Arrays.asList(fromStringToArray(token)));
						break;
					case 4:
						nextEdges = new ArrayList<Integer>(Arrays.asList(fromStringToArray(token)));
						break;
					case 5:
						prevEdges = new ArrayList<Integer>(Arrays.asList(fromStringToArray(token)));
						break;
					case 6:
						category = new Boolean(token).booleanValue();
						break;
					case 7:
						blankNode = new Boolean(token).booleanValue();
						break;
					case 8:
						surfaceform = new Boolean(token).booleanValue();
						break;
					case 9:
						helpingSurfaceform = new Boolean(token).booleanValue();
						break;
					case 10:
						entity = new Boolean(token).booleanValue();
						break;
					default:
						logger.debug("ID(" + id + ") ERROR (" + lineCounter
								+ ") - Counter exceeded value. Found token: " + token);
						break;
					}
					counter++;

				}
				lineCounter++;
				// Bypass addNode(...) b/c we want to absolutely have the same IDs etc as
				// specified in the file for consistency
				Graph.GraphWrapper.INSTANCE.nodes.put(id,
						new GraphNode<Integer>(id, name, next, prev, nextEdges, prevEdges, category));
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Could not read graph from file");
		} finally {
			try {
				rd.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Reads in the node ID mapping into the map
	 * 
	 * @param rd
	 */
	public static void readInNodeIDs(final Reader rd) {

		Graph.GraphWrapper.INSTANCE.idNodes.clear();
		Graph.GraphWrapper.INSTANCE.idCounter = Graph.GraphWrapper.INSTANCE.idCounterInit;
		lineCounter = 0;
		Integer maxID = Graph.GraphWrapper.INSTANCE.idCounter.getVal();
		try {
			String line;
			final BufferedReader br;
			if (rd instanceof BufferedReader) {
				br = (BufferedReader) rd;
			} else {
				br = new BufferedReader(rd);
			}
			while ((line = br.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, dumpSeparator);
				int counter = 0;
				Integer id = -1;
				String name = null;
				while (st.hasMoreTokens()) {
					// ID;name
					final String token = st.nextToken();
					switch (counter) {
					case 0:
						id = new Integer(token);
						maxID = id;
						break;
					case 1:
						name = token;
						break;
					default:
						logger.debug("ID(" + id + ") ERROR (" + lineCounter
								+ ") - Counter exceeded value. Found token: " + token);
						break;
					}
					counter++;
				}
				lineCounter++;
				Graph.GraphWrapper.INSTANCE.idNodes.put(id, name);
			}
			// Works on the principle of "last line has highest ID"
			Graph.GraphWrapper.INSTANCE.idCounter = new IntegerIncrementable(maxID);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Could not read node IDs from file");
		} finally {
			try {
				rd.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Reads the predicate mappings into the map
	 * 
	 * @param rd
	 */
	public static void readInPredicateIDs(final Reader rd) {
		Graph.GraphWrapper.INSTANCE.idPredicates.clear();
		Graph.GraphWrapper.INSTANCE.idPredicateCounter = Graph.GraphWrapper.INSTANCE.idCounterInit;
		lineCounter = 0;
		int maxID = Graph.GraphWrapper.INSTANCE.idPredicateCounter.getNumberVal().intValue();
		try {
			String line;
			final BufferedReader br;
			if (rd instanceof BufferedReader) {
				br = (BufferedReader) rd;
			} else {
				br = new BufferedReader(rd);
			}
			while ((line = br.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, dumpSeparator);
				int counter = 0;
				Integer id = -1;
				String name = null;
				while (st.hasMoreTokens()) {
					// ID;name
					final String token = st.nextToken();
					switch (counter) {
					case 0:
						id = new Integer(token);
						maxID = id;
						break;
					case 1:
						name = token;
						break;
					default:
						logger.debug("ID(" + id + ") ERROR (" + lineCounter
								+ ") - Counter exceeded value. Found token: " + token);
						break;
					}
					counter++;
				}
				lineCounter++;
				Graph.GraphWrapper.INSTANCE.idPredicates.put(id, name);
			}
			// Works on the principle of "last line has highest ID"
			Graph.GraphWrapper.INSTANCE.idPredicateCounter = new IntegerIncrementable(maxID);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Could not read predicate IDs from file");
		} finally {
			try {
				rd.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Dumps the graph to a BufferedWriter
	 * 
	 * @param bw
	 */
	public void dump(final BufferedWriter bw) {
		try {
			for (Map.Entry<T, GraphNode<T>> e : nodes.entrySet()) {
				/*
				 * Save it as: ID(Integer), name(String), next(Integer[]), prev (Integer[]),
				 * category(boolean)
				 */
				final T ID = e.getKey();
				final List<T> next = e.getValue().getSuccessors();
				final List<T> prev = e.getValue().getPredecessors();
				final List<T> nextEdges = e.getValue().getSuccessorEdges();
				final List<T> prevEdges = e.getValue().getPredecessorEdges();

				final String name = e.getValue().getLabel();
				final boolean category = e.getValue().isCategory();
				final boolean blankNode = e.getValue().isBlanknode();
				final boolean surfaceform = e.getValue().isSurfaceform();
				final boolean helpingSurfaceform = e.getValue().isHelpingSurfaceform();
				final boolean entity = e.getValue().isEntity();
				final String nextStr, prevStr, nextEdgesStr, prevEdgesStr;

				if (next != null) {
					nextStr = Arrays.toString(next.toArray());
				} else {
					nextStr = "";
				}
				if (prev != null) {
					prevStr = Arrays.toString(prev.toArray());
				} else {
					prevStr = "";
				}
				if (nextEdges != null) {
					nextEdgesStr = Arrays.toString(nextEdges.toArray());
				} else {
					nextEdgesStr = "";
				}
				if (prevEdges != null) {
					prevEdgesStr = Arrays.toString(prevEdges.toArray());
				} else {
					prevEdgesStr = "";
				}
				bw.write(generateDumpLine(ID.toString(), name, nextStr, prevStr, nextEdgesStr, prevEdgesStr, category,
						blankNode, surfaceform, helpingSurfaceform, entity));
			}
		} catch (IOException e) {
			System.err.println("Could not dump graph.");
			e.printStackTrace();
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private String generateDumpLine(Object... args) {
		String format = "";
		for (int i = 0; i < args.length; ++i) {
			format += "%s";
			if (i != args.length - 1) {
				format += dumpSeparator;
			}
		}
		format += "\n";
		return String.format(format, args);
	}

	private static Integer[] fromStringToArray(final String arr) {
		final List<Integer> l = Lists.newArrayList();
		final StringTokenizer st = new StringTokenizer(arr, ",");
		while (st.hasMoreTokens()) {
			final String token = st.nextToken();
			final String sanitisedString = token.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "");
			if (sanitisedString != null && !sanitisedString.equals("")) {
				try {
					l.add(new Integer(sanitisedString));
				} catch (NumberFormatException nfe) {
					logger.debug("ERROR(" + lineCounter + ") @ '" + sanitisedString + "': " + arr);
				}
			}
		}
		return l.toArray(new Integer[0]);
	}

	public void outputNodeIDs() {
		outputNodeIDs(alu.linking.config.constants.FilePaths.FILE_HOPS_GRAPH_DUMP_PATH_IDS.getPath(KG));
	}

	public void outputPredicateIDs() {
		outputPredicateIDs(alu.linking.config.constants.FilePaths.FILE_HOPS_GRAPH_DUMP_EDGE_IDS.getPath(KG));
	}

	public void outputPredicateIDs(final String OUT_PRED_IDS) {
		BufferedWriter out_node_ids = null;
		try {
			out_node_ids = new BufferedWriter(new FileWriter(OUT_PRED_IDS));
			int counter = 0;
			final long startTime = System.currentTimeMillis();
			final DualHashBidiMap<T, String> ids = getPredicateIDMapping();
			for (Map.Entry<T, String> e : ids.entrySet()) {
				out_node_ids.write(String.format("%d" + dumpSeparator + "%s%s", e.getKey(), e.getValue(),
						alu.linking.config.constants.Strings.NEWLINE.val));
				counter++;
			}
			out_node_ids.close();
			long endTime = System.currentTimeMillis();
			logger.debug("[OUTPUT PRED IDS] Outputting Predicate IDs Ended");
			logger.debug("[OUTPUT PRED IDS] No. of predicates output: " + counter);
			logger.debug("[OUTPUT PRED IDS] Execution time: " + (endTime - startTime) + "ms");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out_node_ids != null) {
					out_node_ids.close();
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	public void outputNodeIDs(final String OUT_PATH_IDS) {
		BufferedWriter out_node_ids = null;
		try {
			out_node_ids = new BufferedWriter(new FileWriter(OUT_PATH_IDS));
			int counter = 0;
			long startTime = System.currentTimeMillis();
			DualHashBidiMap<T, String> ids = getIDMapping();
			for (Map.Entry<T, String> e : ids.entrySet()) {
				out_node_ids.write(String.format("%d" + dumpSeparator + "%s%s", e.getKey(), e.getValue(),
						alu.linking.config.constants.Strings.NEWLINE.val));
				counter++;
			}
			out_node_ids.close();
			long endTime = System.currentTimeMillis();
			logger.debug("[OUTPUT NODE IDS] Outputting Path IDs Ended");
			logger.debug("[OUTPUT NODE IDS] No. of nodes output: " + counter);
			logger.debug("[OUTPUT NODE IDS] Execution time: " + (endTime - startTime) + "ms");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out_node_ids != null) {
					out_node_ids.close();
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	/**
	 * @author Kris Noullet (kn65)
	 * @param graphDumpPath
	 * @param graphDumpPathIds
	 * @param graphDumpEdgeIds
	 * @throws FileNotFoundException If any of the passed paths does not lead to a
	 *                               file
	 */
	public void readIn(String graphDumpPath, String graphDumpPathIds, String graphDumpEdgeIds)
			throws FileNotFoundException {
		readIn(graphDumpPath);
		readInNodeIDs(graphDumpPathIds);
		readInPredicateIDs(graphDumpEdgeIds);
	}

	/**
	 * @author Kris Noullet (kn65)
	 * @param graphDumpEdgeIds
	 * @throws FileNotFoundException
	 */
	public void readInPredicateIDs(String graphDumpEdgeIds) throws FileNotFoundException {
		readInPredicateIDs(new BufferedReader(new FileReader(graphDumpEdgeIds)));
	}

	/**
	 * @author Kris Noullet (kn65)
	 * @param graphDumpPathIds
	 * @throws FileNotFoundException
	 */
	public void readInNodeIDs(String graphDumpPathIds) throws FileNotFoundException {
		readInNodeIDs(new BufferedReader(new FileReader(graphDumpPathIds)));
	}

	/**
	 * @author Kris Noullet (kn65)
	 * @param graphDumpPath
	 * @throws FileNotFoundException
	 */
	public void readIn(final String graphDumpPath) throws FileNotFoundException {
		readIn(new BufferedReader(new FileReader(graphDumpPath)));
	}

}
