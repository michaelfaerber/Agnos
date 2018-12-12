package alu.linking.preprocessing.surfaceform.query;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.query.QuerySolution;

public class QuerySolutionIterator implements Iterator<String> {
	private final List<String> varNames = Lists.newArrayList();
	private int index = 0;
	private final int varNamesSize;

	public QuerySolutionIterator(QuerySolution result) {
		result.varNames().forEachRemaining(s -> varNames.add(s));
		Collections.sort(varNames);
		this.varNamesSize = varNames.size();
	}

	@Override
	public boolean hasNext() {
		return index < varNamesSize;
	}

	@Override
	public String next() {
		return varNames.get(index++);
	}

}
