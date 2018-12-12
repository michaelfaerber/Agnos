package alu.linking.structure;

import org.apache.log4j.Logger;

public interface Loggable {
	public default Logger getLogger() {
		return Logger.getLogger(getClass());
	}

	public default void warn(final String msg) {
		getLogger().warn(msg);
	}

	public default void error(final String msg) {
		getLogger().error(msg);
	}

}
