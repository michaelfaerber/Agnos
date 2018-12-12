package alu.linking.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.MutablePair;

/**
 * Utility class allowing for execution time checking
 * 
 * @author Kris Noullet (kn65)
 *
 */
public class Stopwatch {
	private static final String def = "default";
	private static final Map<String, MutablePair<Long, Long>> stopwatchMap = new HashMap<>();

	/**
	 * Default 'start' action - updates starting from when the timer should be
	 * counted
	 * 
	 * @author Kris Noullet (kn65)
	 */
	public static void start() {
		start(def);
	}

	/**
	 * Default 'end' action - updates end time of timer
	 * 
	 * @author Kris Noullet (kn65)
	 */
	public static void end() {
		end(def);
	}

	/**
	 * Computes the difference of time between start and end times of the
	 * default timer
	 * 
	 * @author Kris Noullet (kn65)
	 * @return time difference
	 */
	public static long diff() {
		return diff(def);
	}

	/**
	 * Calls the default end implementation, followed by the default diff
	 * implementation
	 * 
	 * @author Kris Noullet (kn65)
	 * @return time difference
	 */
	public static long endDiff() {
		return endDiff(def);
	}

	/**
	 * Calls the default end implementation, followed by the default diff
	 * implementation, after which the counter is started again
	 * 
	 * @author Kris Noullet (kn65)
	 * @return time difference
	 */
	public static long endDiffStart() {
		return endDiffStart(def);
	}

	/**
	 * Calls default end and output implementations
	 * 
	 * @author Kris Noullet (kn65)
	 * @return default endOutput() ret value
	 */
	public static long endOutput() {
		return endOutput(def);
	}

	/**
	 * Calls default end, output and start implementations
	 * 
	 * @author Kris Noullet (kn65)
	 * @return default endOutputStart() ret value
	 */
	public static long endOutputStart() {
		return endOutputStart(def);
	}

	/**
	 * Starts timer for given watch
	 * 
	 * @author Kris Noullet (kn65)
	 * @param watch
	 *            String parameter used to store/retrieve this particular watch
	 */
	public static void start(final String watch) {
		MutablePair<Long, Long> p = null;
		if ((p = stopwatchMap.get(watch)) == null) {
			p = new MutablePair<Long, Long>();
			stopwatchMap.put(watch, p);
		}
		p.setLeft(System.currentTimeMillis());
	}

	/**
	 * Ends timer for given watch
	 * 
	 * @author Kris Noullet (kn65)
	 * @param watch
	 *            String parameter used to store/retrieve this particular watch
	 */
	public static void end(final String watch) {
		MutablePair<Long, Long> p = null;
		if ((p = stopwatchMap.get(watch)) == null) {
			p = new MutablePair<Long, Long>();
			stopwatchMap.put(watch, p);
		}
		if (p.getLeft() == null) {
			start(watch);
		}
		p.setRight(System.currentTimeMillis());
	}

	/**
	 * Gets time difference for given watch
	 * 
	 * @author Kris Noullet (kn65)
	 * @param watch
	 *            String parameter used to store/retrieve this particular watch
	 * @return difference between this watch's start and end
	 */
	public static long diff(final String watch) {
		final MutablePair<Long, Long> p = stopwatchMap.get(watch);
		return p.getRight() - p.getLeft();
	}

	/**
	 * Updates end time for given watch and returns time difference
	 * 
	 * @author Kris Noullet (kn65)
	 * @param watch
	 *            String parameter used to store/retrieve this particular watch
	 * @return time difference
	 */
	public static long endDiff(final String watch) {
		end(watch);
		return diff(watch);
	}

	/**
	 * Updates end time for given watch and returns time difference after which
	 * it starts the new watch
	 * 
	 * @author Kris Noullet (kn65)
	 * @param watch
	 *            String parameter used to store/retrieve this particular watch
	 * @return time difference
	 */
	public static long endDiffStart(final String watch) {
		end(watch);
		long diff = diff(watch);
		start(watch);
		return diff;
	}

	/**
	 * Ends watch timer and outputs the difference
	 * 
	 * @author Kris Noullet (kn65)
	 * @param watch
	 *            String parameter used to store/retrieve this particular watch
	 * @return time difference
	 */
	public static long endOutput(final String watch) {
		end(watch);
		long diff = diff(watch);
		System.out.println("[" + watch + "] Executed in " + diff + " ms.");
		return diff;
	}

	/**
	 * Ends watch timer, outputs the difference and starts it anew
	 * 
	 * @author Kris Noullet (kn65)
	 * @param watch
	 *            String parameter used to store/retrieve this particular watch
	 * @return time difference
	 */
	public static long endOutputStart(final String watch) {
		end(watch);
		long diff = diff(watch);
		System.out.println("[" + watch + "] Executed in " + diff + " ms.");
		start(watch);
		return diff;
	}
}
