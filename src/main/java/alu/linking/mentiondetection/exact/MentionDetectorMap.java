package alu.linking.mentiondetection.exact;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.semanticweb.yars.nx.Node;

import com.google.common.collect.Lists;

import alu.linking.config.constants.Numbers;
import alu.linking.mentiondetection.EnumDetectionType;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.structure.Loggable;

public class MentionDetectorMap implements MentionDetector<Node>, Loggable {
	private final Set<String> keys;
	private final String tokenSeparator = " ";// space
	private final EnumDetectionType detectionType = EnumDetectionType.BOUND_DYNAMIC_WINDOW;
	private final String mentionLock = "mentionsList";

	public MentionDetectorMap(final Map<String, Set<String>> map) {
		Set<String> inKeys = map.keySet();
		// Adds everything in lower case
		this.keys = new HashSet<>();
		for (String key : inKeys) {
			this.keys.add(InputProcessor.combineProcessedInput(InputProcessor.process(key)));
		}

	}

	/**
	 * Short-hand call to {@link #detect(String, String)} with {@param source=null}
	 * 
	 * @param input input text/corpus to detect mentions from
	 */
	@Override
	public List<Mention<Node>> detect(String input) {
		return detect(input, null);
	}

	/**
	 * @param input  input text/corpus to detect mentions from
	 * @param source where this text comes from or what it is linked to
	 */
	@Override
	public List<Mention<Node>> detect(final String input, final String source) {
		try {
			final String[] words = InputProcessor.process(input);
			// Synchronized list
			final List<Mention<Node>> mentions = Lists.newArrayList();

			final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
					.newFixedThreadPool(Numbers.MENTION_DETECTION_THREAD_AMT.val.intValue());
			AtomicInteger doneCounter = new AtomicInteger(0);

			final StringBuilder combinedWords = new StringBuilder();
			int stringPos = 0;
			switch (detectionType) {
			case BOUND_DYNAMIC_WINDOW:
				final int windowSize = Numbers.MENTION_DETECTION_WINDOW_SIZE.val.intValue();
				combinedWords.setLength(0);
				stringPos = 0;
				for (int i = 0; i < words.length; ++i) {
					int subPos = stringPos;// + i*tokenSeparator.length();//+i due to spaces;
					for (int j = 0; j < windowSize; ++j) {
						if (i + j > words.length - 1) {
							break;
						}
						// final int index = Math.min(words.length - 1, i + j);
						final int index = i + j;
						if (j != 0) {
							combinedWords.append(tokenSeparator);
							subPos += tokenSeparator.length();
						}
						combinedWords.append(words[index]);
						execFind(executor, mentions, doneCounter, combinedWords.toString(), source, subPos);
						subPos += words[index].length();
					}
					combinedWords.setLength(0);
					stringPos += words[i].length() + tokenSeparator.length();
				}
				break;
			case SINGLE_WORD:
				// Just Single words
				stringPos = 0;
				for (String token : words) {
					execFind(executor, mentions, doneCounter, token, source, stringPos);
					stringPos += token.length();
				}
				break;
			case UNBOUND_DYNAMIC_WINDOW:
				/*
				 * Example: Input: I have a cat Processed as: I I have I have a I have a cat
				 * have have a have a cat ...
				 */
				// Just Single words
				stringPos = 0;
				for (String token : words) {
					execFind(executor, mentions, doneCounter, token, source, stringPos);
					stringPos += token.length();
				}
				// Multi-words (excluding single words)
				combinedWords.setLength(0);
				stringPos = 0;
				for (int i = 0; i < words.length; ++i) {
					combinedWords.append(words[i]);
					int subPos = stringPos;
					for (int j = i + 1; j < words.length; ++j) {
						combinedWords.append(tokenSeparator + words[j]);
						execFind(executor, mentions, doneCounter, combinedWords.toString(), source, stringPos);
						subPos += tokenSeparator.length() + words[j].length();
					}
					stringPos += words[i].length();
					combinedWords.setLength(0);
				}
				break;
			case UNBOUND_DYNAMIC_WINDOW_STRICT_MULTI_WORD:
				// Multi-words (excluding single words)
				combinedWords.setLength(0);
				stringPos = 0;
				for (int i = 0; i < words.length; ++i) {
					combinedWords.append(words[i]);
					int subPos = stringPos;
					for (int j = i + 1; j < words.length; ++j) {
						combinedWords.append(tokenSeparator + words[j]);
						execFind(executor, mentions, doneCounter, combinedWords.toString(), source, stringPos);
						subPos = tokenSeparator.length() + words[j].length();
					}
					stringPos += words[i].length();
					combinedWords.setLength(0);
				}
				break;
			default:
				break;
			}

			// No more tasks will be added
			executor.shutdown();
			do {
				// No need for await termination as this is pretty much it already...
				Thread.sleep(50);
				// getLogger().debug("Finished executing: " + doneCounter.get() + "
				// processes.");
			} while (!executor.isTerminated());
			// Shouldn't wait at all generally, but in order to avoid unexpected behaviour -
			// especially relating to logic changes on the above busy-waiting loop
			final boolean terminated = executor.awaitTermination(10L, TimeUnit.MINUTES);
			if (!terminated) {
				getLogger().error("Executor has not finished terminating");
			}
			return mentions;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Submits a find task to the executor, adding the result to the mentions list,
	 * incrementing the 'done' counter<br>
	 * <b>Note</b>: Mentions list MUST be synchronized (e.g. through
	 * Collections.synchronizedList(List))
	 * 
	 * @param executor
	 * @param mentions
	 * @param doneCounter
	 * @param wordCombination
	 * @param source
	 * @param threshold
	 */
	private void execFind(final ThreadPoolExecutor executor, final List<Mention<Node>> mentions,
			final AtomicInteger doneCounter, final String wordCombination, final String source, final int indexPos) {
		executor.submit(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				final Mention<Node> mention = find(wordCombination, source, indexPos);
				if (mention != null) {
					synchronized (mentionLock) {
						mentions.add(mention);
					}
				}
				return doneCounter.incrementAndGet();
			}
		});
	}

	/**
	 * Finds a mention for a given input token
	 * 
	 * @param input     token or word
	 * @param source    what entity this word is linked to
	 * 
	 * @param threshold minimum similarity threshold for it to be accepted
	 * @return mention with the closest possible mate
	 * 
	 */
	public Mention<Node> find(final String input, final String source, final int offset) {
		if (!this.keys.contains(input.toLowerCase())) {
			System.out.println("Could not match w/:" + input.toLowerCase());
			final int showAmt = 100;
			int showCounter = 0;
			System.out.println("Number of keys:" + this.keys.size());
			for (String key : this.keys) {
				if (showCounter++ < showAmt) {
					System.out.println("Key:'" + key.toLowerCase() + "'");
				}
			}
			return null;
		}
		// Create a mention with the best-found word
		final Mention<Node> mention = new Mention<Node>(input, source, null, offset, 1, input);
		return mention;
	}

	@Override
	public void init() throws Exception {
	}
}
