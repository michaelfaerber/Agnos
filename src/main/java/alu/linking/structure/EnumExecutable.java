package alu.linking.structure;

import java.lang.reflect.InvocationTargetException;

public enum EnumExecutable {
	//TAGTOG(alu.linking.executable.TagtogCommunicator.class,"Class for communicating with the tagtog.net website's REST API to extract wanted information"), //
	PAGERANK_RDF(alu.linking.executable.preprocessing.deprecated.PageRankComputer.class, "Computes PageRank for RDF triples")//
	;
	private final Class<? extends Executable> execClass;
	private final String desc;

	EnumExecutable(final Class<? extends Executable> execClass, final String desc) {
		this.execClass = execClass;
		this.desc = desc;
	}

	/**
	 * Execute the Enum's associated class with the passed parameters, detecting the
	 * appropriate declared constructor
	 * 
	 * @param params
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public Object execute(Object... params) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, Exception {
		final Object ret;
		if (params != null) {
			final Class[] types = new Class<?>[params.length];
			for (int i = 0; i < params.length; ++i) {
				types[i] = params[i].getClass();
			}
			ret = execClass.getDeclaredConstructor(null).newInstance(null).exec(params);
			// execClass.getDeclaredConstructor(types).newInstance(params).ex;
		} else {
			ret = execClass.getDeclaredConstructor(null).newInstance(null).exec();
		}
		return ret;
	}
}
