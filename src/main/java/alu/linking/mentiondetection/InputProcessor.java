package alu.linking.mentiondetection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;

import alu.linking.config.constants.Numbers;

/**
 * Centralised processing class for input strings (also used for data structure
 * text processing
 * 
 * @author Kris
 *
 */
public class InputProcessor {
	final Collection<String> blacklist;

	public InputProcessor(final Collection<String> blacklist) {
		this.blacklist = blacklist;
	}

	public static void main(String[] args) {
		final String inString = "I have a cat that I like playing with!";
		final EnumDetectionType detectionMode = EnumDetectionType.UNBOUND_DYNAMIC_WINDOW;
		final List<Mention> mentions = new InputProcessor(null).createMentions(inString, null, detectionMode);
		for (Mention m : mentions) {
			System.out.println(m.getOriginalMention() + " - " + m.getOffset());
		}
		System.out.println("#################");
		System.out.println("inString:");
		System.out.println(inString);
	}

	private static final String tokenSeparator = " ";// space
	private static final Pattern spacePattern = Pattern.compile("\\p{Space}");

	public List<Mention> createMentions(final String input, final String source,
			final EnumDetectionType detectionMode) {
		return createMentions(input, source, detectionMode, this.blacklist);
	}

	/**
	 * Used to create mentions without too much trouble in regards to offsets when
	 * removing stopwords and whatnot
	 * 
	 * @param input
	 * @return
	 */
	public static List<Mention> createMentions(final String input, final String source,
			final EnumDetectionType detectionMode, final Collection<String> blacklist) {
		final List<Mention> retList = Lists.newArrayList();
		final Matcher matcher = spacePattern.matcher(input);

		final List<Integer> spacedIndices = Lists.newArrayList();
		// Add initial index so substring starts from beginning of text
		spacedIndices.add(0);
		while (matcher.find()) {
			spacedIndices.add(matcher.start() + matcher.group().length());
		}
		spacedIndices.add(input.length());

		switch (detectionMode) {
		case BOUND_DYNAMIC_WINDOW:
			final int windowSize = Numbers.MENTION_DETECTION_WINDOW_SIZE.val.intValue();

			for (int i = 0; i < spacedIndices.size(); ++i) {
				for (int j = 1; j <= windowSize && ((i + j) < spacedIndices.size()); ++j) {
					final int startIndex = spacedIndices.get(i), endIndex = spacedIndices.get(i + j) - 1;
					retList.add(createMention(input.substring(startIndex, endIndex), startIndex, source, blacklist));
				}
			}
			break;
		case SINGLE_WORD:
			// Just Single words
			for (int i = 0; i < spacedIndices.size() - 1; ++i) {
				final int startIndex = spacedIndices.get(i), endIndex = spacedIndices.get(i + 1) - 1;
				retList.add(createMention(input.substring(startIndex, endIndex), startIndex, source, blacklist));
			}
			break;
		case UNBOUND_DYNAMIC_WINDOW:
			/*
			 * Example: Input: I have a cat Processed as: I; I have; I have a; I have a cat;
			 * have; have a; have a cat; ...
			 */
			for (int i = 0; i < spacedIndices.size(); ++i) {
				for (int j = i + 1; j < spacedIndices.size(); ++j) {
					final int startIndex = spacedIndices.get(i), endIndex = spacedIndices.get(j) - 1;
					retList.add(createMention(input.substring(startIndex, endIndex), startIndex, source, blacklist));
				}
			}
			break;
		case UNBOUND_DYNAMIC_WINDOW_STRICT_MULTI_WORD:
//			// Multi-words (excluding single words)
//			combinedWords.setLength(0);
//			stringPos = 0;
//			for (int i = 0; i < words.length; ++i) {
//				combinedWords.append(words[i]);
//				int subPos = stringPos;
//				for (int j = i + 1; j < words.length; ++j) {
//					combinedWords.append(tokenSeparator + words[j]);
//					execFind(executor, mentions, doneCounter, combinedWords.toString(), source, threshold,
//							stringPos);
//					subPos = tokenSeparator.length() + words[j].length();
//				}
//				stringPos += words[i].length();
//				combinedWords.setLength(0);
//			}
			break;

		default:
			break;
		}

		return retList;
	}

	private static Mention createMention(String original, int startIndex, final String source,
			Collection<String> blacklist) {
		final String processedInput = combineProcessedInput(processAndRemoveStopwords(original, blacklist));
		return new Mention(null, source, null, startIndex, 0f, original, processedInput);
	}

	public static String[] process(final String input) {
		return input.replaceAll("\\p{Punct}", "").toLowerCase().split("\\p{Space}");// POSIX class
	}

	public String[] processAndRemoveStopwords(final String input) {
		return processAndRemoveStopwords(input, blacklist);
	}

	public static String[] processAndRemoveStopwords(final String input, final Collection<String> blacklist) {
		final String[] inputArr = process(input);// POSIX class
		final List<String> ret = Lists.newArrayList();
		if (blacklist == null) {
			return inputArr;
		}
		for (String str : inputArr) {
			if (!blacklist.contains(str)) {
				ret.add(str);
			}
		}
		return ret.toArray(new String[ret.size()]);
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
	 * Note: Does not modify passed Map instance
	 * 
	 * @param map
	 * @return
	 */
	public static <V> HashMap<String, V> processCollection(final Map<String, V> map,
			final InputProcessor inputProcessor) {
		final HashMap<String, V> ret = new HashMap<>(map.keySet().size());
		map.forEach((key, value) -> {
			if (key != null) {
				ret.put(InputProcessor.combineProcessedInput(inputProcessor.processAndRemoveStopwords(key)), value);
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
	public static TreeSet<String> processCollection(final SortedSet<String> inputSet,
			final InputProcessor inputProcessor) {
		final TreeSet<String> ret = new TreeSet<String>(inputSet.comparator());
		inputSet.forEach(item -> {
			if (item != null) {
				ret.add(InputProcessor.combineProcessedInput(inputProcessor.processAndRemoveStopwords(item)));
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
	public static ArrayList<String> processCollection(final List<String> inputList,
			final InputProcessor inputProcessor) {
		final ArrayList<String> ret = Lists.newArrayList();
		inputList.forEach(item -> {
			if (item != null) {
				ret.add(InputProcessor.combineProcessedInput(inputProcessor.processAndRemoveStopwords(item)));
			}
		});
		return ret;
	}
}
