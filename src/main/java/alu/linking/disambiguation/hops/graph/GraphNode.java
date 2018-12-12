package alu.linking.disambiguation.hops.graph;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.beust.jcommander.internal.Lists;

public class GraphNode<T> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7011294609068585108L;

	private List<T> next = null;
	private List<T> prev = null;
	private List<T> nextEdges = null;
	private List<T> prevEdges = null;
	private final String name;
	private boolean category = false;
	private final T id;

	private boolean surfaceform = false;

	private boolean helpingSurfaceform = false;

	private boolean blanknode = false;

	private boolean entity = false;

	GraphNode(final T id) {
		this(id, null);
	}

	GraphNode(final T id, final String name) {
		this(id, name, null);
	}

	GraphNode(final T id, final String name, final List<T> next) {
		this(id, name, next, null, null, null);
	}

	GraphNode(final T id, final String name, final List<T> next, final List<T> prev, final List<T> nextEdges,
			final List<T> prevEdges) {
		this.id = id;
		this.name = name;
		// this.name = null;<- causes plenty of NPEs, check Graph.java for
		// dependencies in later stages in case you want it removed
		this.next = next;
		this.prev = prev;
		if (this.next == null) {
			this.next = Lists.newArrayList();
		}
		if (this.prev == null) {
			this.prev = Lists.newArrayList();
		}
		// Edges
		this.nextEdges = nextEdges;
		this.prevEdges = prevEdges;
		if (this.nextEdges == null) {
			this.nextEdges = Lists.newArrayList();
		}
		if (this.prevEdges == null) {
			this.prevEdges = Lists.newArrayList();
		}
	}

	public GraphNode(T id, String name, List<T> next, List<T> prev, final List<T> nextEdges, final List<T> prevEdges,
			boolean category) {
		this(id, name, next, prev, nextEdges, prevEdges);
		this.category = category;
	}

	public void addSuccessor(final T succ, final T predicate) {
		this.next = addToArray(succ, next);
		this.nextEdges = addToArray(predicate, nextEdges);
	}

	public void addPredecessor(final T predecessor, final T predicate) throws Exception {
		if (predecessor == null) {
			throw new Exception("Passed predecessor is null. (" + getID() + "," + getLabel() + ")");
		}
		this.prev = addToArray(predecessor, prev);
		this.prevEdges = addToArray(predicate, prevEdges);
	}

	private List<T> addToArray(final T succ, final List<T> arr) {
		List<T> retArr = arr;
		if (retArr == null) {
			retArr = Lists.newArrayList();
		}
		retArr.add(succ);
		return retArr;
	}

	private T[] addToArray(final T succ, final T[] arr) {
		int arrLen;
		if (arr == null) {
			arrLen = 0;
		} else {
			arrLen = arr.length;
		}
		final T[] newArray;
		if (succ instanceof Integer) {
			newArray = (T[]) (new Integer[arrLen + 1]);
		} else if (succ instanceof String) {
			newArray = (T[]) (new String[arrLen + 1]);
		} else {
			throw new RuntimeException("Invalid type: " + succ.getClass());
		}

		for (int i = 0; i < arrLen; ++i) {
			newArray[i] = arr[i];
		}
		newArray[newArray.length - 1] = succ;
		return newArray;
	}

	/**
	 * Adds multiple successors as given from a String[] Pretty much just calls
	 * addSuccessors after transforming the String[] into a List<String>
	 * 
	 * @param successors
	 */
	public void addSuccessor(final T[] successors) {
		addSuccessors(Arrays.asList(successors));
	}

	/**
	 * Adds multiple successors to this node
	 * 
	 * @param successors
	 */
	public void addSuccessors(final List<T> successors) {
		List<T> listNext = next;
		listNext.addAll(successors);
		this.next = listNext;
	}

	public List<T> getSuccessors() {
		return this.next;
	}

	public List<T> getPredecessors() {
		return this.prev;
	}

	public List<T> getSuccessorEdges() {
		return this.nextEdges;
	}

	public List<T> getPredecessorEdges() {
		return this.prevEdges;
	}

	public T getID() {
		return this.id;
	}

	public String getLabel() {
		return this.name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public void flagCategory() {
		this.category = true;
	}

	public boolean isCategory() {
		return this.category;
	}

	/**
	 * Removes node with given ID from both successor and predecessor arrays (if
	 * present)
	 * 
	 * @param ID
	 * @return whether something was removed successfully from either NEXT or PREV
	 *         arrays
	 */
	public boolean removeFromSuccessorsAndOrPredecessors(final T ID) {
		boolean ret = false;
		ret |= removeNodeFromPredecessors(ID);
		ret |= removeNodeFromSuccessors(ID);
		return ret;
	}

	/**
	 * Removes nodes with given IDs from both successor and predecessor arrays (if
	 * present)
	 * 
	 * @param IDs
	 *            IDs to be removed from successor/predecessor lists
	 * @return whether nodes were removed successfully from NEXT and PREV arrays
	 */
	public boolean removeNodeFromSuccessorsAndOrPredecessors(final Set<T> IDs) {
		boolean ret = true;
		long threshold = 10_000, endNodeTime, endNodeTimeSubSum = 0, endNodeTimeSum = 0,
				startNodeTime = System.currentTimeMillis();
		ret &= removeNodesFromPredecessors(IDs);
		ret &= removeNodesFromSuccessors(IDs);
		endNodeTime = System.currentTimeMillis();
		if ((endNodeTimeSubSum = (endNodeTime - startNodeTime)) > threshold) {
			endNodeTimeSum += endNodeTimeSubSum;
			System.out.println("Node(" + getID() + "): - Processing... for " + (endNodeTimeSum / 1000.0) + "seconds");
			startNodeTime = System.currentTimeMillis();
		}
		return ret;
	}

	/**
	 * Removes node with given ID from this node's successor list
	 * 
	 * @param ID
	 *            Identifier of node to be removed
	 * @return Whether something was removed from the successor array
	 */
	public boolean removeNodeFromSuccessors(final T ID) {
		// List<T> nextList = new ArrayList<T>(Arrays.asList(this.next));
		List<T> nextList = this.next;
		List<T> nextEdgesList = null;
		if (nextList.indexOf(ID) != -1) {
			// nextEdgesList = new ArrayList<T>(Arrays.asList(this.nextEdges));
			nextEdgesList = this.nextEdges;
			removeNodeFromEdgesAndNodes(nextList, nextEdgesList, ID);
		}
		if (nextEdgesList != null) {
			// The indices were not -1 at some point -> means stuff got changed,
			// so we should update!
			this.next = nextList;
			// this.next = nextList.toArray((T[])
			// Array.newInstance(id.getClass().getComponentType(), 0));
			this.nextEdges = nextEdgesList;
			// this.nextEdges = nextEdgesList.toArray((T[])
			// Array.newInstance(id.getClass().getComponentType(), 0));
		}
		return nextEdgesList != null;
	}

	/**
	 * Removes multiple IDs from a node's successor list
	 * 
	 * @param IDs
	 *            IDs to be removed.
	 * @return true if everything is removed successfully
	 */
	public boolean removeNodesFromSuccessors(final Set<T> IDs) {
		boolean ret = true;
		final List<T> nextList = this.next;
		List<T> nextEdgesList = null;
		nextEdgesList = this.nextEdges;
		for (T ID : IDs) {
			ret &= removeNodeFromEdgesAndNodes(nextList, nextEdgesList, ID);
		}
		if (nextEdgesList != null) {
			// The indices were not -1 at some point -> means stuff got changed,
			// so we should update!
			this.next = nextList;
			this.nextEdges = nextEdgesList;
		}
		return ret;
	}

	/**
	 * Removes node with given ID from this node's predecessor list
	 * 
	 * @param ID
	 *            Identifier of node to be removed
	 * @return Whether something was removed from the predecessor array
	 */
	public boolean removeNodeFromPredecessors(final T ID) {
		List<T> prevList = this.prev;
		List<T> prevEdgesList = null;
		int prevPos = -1;
		if (prevList.indexOf(ID) != -1) {
			prevEdgesList = this.prevEdges;
			removeNodeFromEdgesAndNodes(prevList, prevEdgesList, ID);
		}
		if (prevEdgesList != null) {
			// The indices were not -1 at some point -> means stuff got changed,
			// so we should update!
			this.prev = prevList;
			this.prevEdges = prevEdgesList;
		}
		return prevEdgesList != null;
	}

	/**
	 * Removes multiple IDs from a node's predecessor list
	 * 
	 * @param IDs
	 *            IDs to be removed.
	 * @return true if everything is removed successfully
	 */
	public boolean removeNodesFromPredecessors(final Set<T> IDs) {
		boolean ret = true;
		final List<T> prevList = this.prev;
		List<T> prevEdgesList = null;
		prevEdgesList = this.prevEdges;
		for (T ID : IDs) {
			ret &= removeNodeFromEdgesAndNodes(prevList, prevEdgesList, ID);
		}
		if (prevEdgesList != null) {
			// The indices were not -1 at some point -> means stuff got changed,
			// so we should update!
			this.prev = prevList;
			this.prevEdges = prevEdgesList;
		}
		return ret;
	}

	public boolean removeNodeFromEdgesAndNodes(final List<T> nodeList, final List<T> edgesList, final T ID) {
		boolean ret = true;
		int pos = -1;

		while ((pos = nodeList.indexOf(ID)) != -1) {
			ret &= (edgesList.remove(pos) != null);
			ret &= (nodeList.remove(pos) != null);
		}
		return ret;
	}

	/**
	 * Removes specified edges from this node if any exist
	 * 
	 * @param ID
	 *            IDs of edges to remove
	 * @return how many connections were removed from this node
	 */
	public int removeEdges(final Set<T> IDs) {
		int ret = 0;
		for (T ID : IDs) {
			ret += removeEdge(ID);
		}
		return ret;
	}

	/**
	 * Removes specified edge from this node if it exists
	 * 
	 * @param ID
	 *            id of edge to remove
	 * @return how many connections were removed from this node
	 */
	public int removeEdge(final T ID) {
		int ret = 0;
		int pos = -1;
		boolean removed = false;
		// First from 'previous' nodes
		while ((pos = this.prevEdges.indexOf(ID)) != -1) {
			removed = (this.prevEdges.remove(pos) != null);
			this.prev.remove(pos);
			if (removed) {
				ret++;
				removed = false;
			}
		}
		// Second from 'next' nodes
		removed = false;
		while ((pos = this.nextEdges.indexOf(ID)) != -1) {
			removed = (this.nextEdges.remove(pos) != null);
			this.next.remove(pos);
			if (removed) {
				ret++;
				removed = false;
			}
		}
		return ret;
	}

	public void flagSurfaceform() {
		this.surfaceform = true;
	}

	public void flagHelpingSurfaceform() {
		this.helpingSurfaceform = true;
	}

	public boolean isBlanknode() {
		return blanknode;
	}

	public void flagBlanknode() {
		this.blanknode = true;
	}

	public boolean isSurfaceform() {
		return surfaceform;
	}

	public boolean isHelpingSurfaceform() {
		return helpingSurfaceform;
	}

	public boolean isEntity() {
		return entity;
	}

	public void flagEntity() {
		this.entity = true;
	}

}
