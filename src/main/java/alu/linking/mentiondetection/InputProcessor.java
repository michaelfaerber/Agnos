package alu.linking.mentiondetection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.collect.Lists;

/**
 * Centralised processing class for input strings (also used for data structure
 * text processing
 * 
 * @author Kris
 *
 */
public class InputProcessor {
	public static String[] process(final String input) {
		return input.replaceAll("\\p{Punct}", "").toLowerCase().split("\\p{Space}");// POSIX class
	}

	public static String combineProcessedInput(final String[] inputTokens) {
		if (inputTokens == null || inputTokens.length == 0) {
			return "";
		}
		final StringBuilder sb = new StringBuilder(inputTokens[0]);
		for (int i = 1; i < inputTokens.length; ++i) {
			sb.append(" ");
			sb.append(inputTokens[i]);
		}
		return sb.toString();
	}

	/**
	 * Treats the key as it would any other input and saves it to a new HashMap with
	 * the associated previous key value (meant for use for [Keys=surfaceForm;
	 * Values=entities]<br>
	 * Note: Does not change passed Map instance
	 * 
	 * @param map
	 * @return
	 */
	public static <V> HashMap<String, V> processCollection(final Map<String, V> map) {
		final HashMap<String, V> ret = new HashMap<>(map.keySet().size());
		map.forEach((key, value) -> {
			if (key != null) {
				ret.put(InputProcessor.combineProcessedInput(InputProcessor.process(key)), value);
			}
		});
		return ret;
	}

	/**
	 * Takes same comparator as the passed one and uses it for the creation of a new
	 * treeset which contains the sanitized versions of the contents of the
	 * previously-passed one.<br>
	 * Note: Does not change passed SortedSet instance
	 * 
	 * @param inputSet
	 * @return
	 */
	public static TreeSet<String> processCollection(final SortedSet<String> inputSet) {
		final TreeSet<String> ret = new TreeSet<String>(inputSet.comparator());
		inputSet.forEach(item -> {
			if (item != null) {
				ret.add(InputProcessor.combineProcessedInput(InputProcessor.process(item)));
			}
		});
		return ret;
	}

	/**
	 * Sanitizes passed list items<br>
	 * Note: does not modify passed List instance
	 * 
	 * @param inputList
	 * @return
	 */
	public static ArrayList<String> processCollection(final List<String> inputList) {
		final ArrayList<String> ret = Lists.newArrayList();
		inputList.forEach(item -> {
			if (item != null) {
				ret.add(InputProcessor.combineProcessedInput(InputProcessor.process(item)));
			}
		});
		return ret;
	}
}
