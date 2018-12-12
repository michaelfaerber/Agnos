package alu.linking.disambiguation.hops.pathbuilding;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

public interface PathCrawler<T> {
	
	public List<T> filter(final List<T> next, final Collection<T> path);

	public LinkedHashSet<LinkedList<T>> crawlPaths(final List<T> v);

	public LinkedHashSet<LinkedList<T>> recursiveCrawlPaths(final LinkedList<T> path);

	public void storePath(LinkedList<T> l);

	public List<T> expandVertex(final T v);

	public void storePath(LinkedHashSet<LinkedList<T>> ret, LinkedList<T> l);

	public void closeOutputs();
}
