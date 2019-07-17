package alu.linking.disambiguation.hops.pathbuilding;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Interface for different ways of crawling paths
 * @author Kristian Noullet
 *
 * @param <T>
 */
public interface PathCrawler<T> {
	
	/**
	 * Filters choices of the passed "next" list based on the passed path (usually used to avoid cycles)
	 * @param next possibilities to go to
	 * @param path currently chosen elements of path
	 * @return remaining choices
	 */
	public List<T> filter(final List<T> next, final Collection<T> path);

	/**
	 * Possible entry point to crawling 
	 * @param v
	 * @return
	 */
	public LinkedHashSet<LinkedList<T>> crawlPaths(final List<T> v);

	public LinkedHashSet<LinkedList<T>> recursiveCrawlPaths(final LinkedList<T> path);

	/**
	 * Store passed path for persistence
	 * @param path path to store
	 */
	public void storePath(LinkedList<T> path);

	/**
	 * Expanding a vertex to find what the possible successors are
	 * @param v node to expand
	 * @return possible successors from given node
	 */
	public List<T> expandVertex(final T v);

	/**
	 * Stores the path to the passed 'ret' list from the linkedlist l
	 * @param ret return list to store path into
	 * @param path path to store
	 */
	public void storePath(LinkedHashSet<LinkedList<T>> ret, LinkedList<T> path);

	/**
	 * Closes all potential outputs for paths
	 */
	public void closeOutputs();
}
