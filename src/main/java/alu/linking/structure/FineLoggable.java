package alu.linking.structure;

public interface FineLoggable extends Loggable {
	public static final boolean fineLogging = false;

	public default void debug(final String msg) {
		if (fineLogging) {
			getLogger().debug(msg);
		}
	}

	
}
