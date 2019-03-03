package alu.linking.mentiondetection.exact;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import alu.linking.config.constants.Numbers;
import alu.linking.mentiondetection.EnumDetectionType;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.structure.Loggable;

public class MentionDetectorMap implements MentionDetector, Loggable {
	private final Set<String> keys;
	private final String tokenSeparator = " ";// space
	private final EnumDetectionType detectionType = EnumDetectionType.BOUND_DYNAMIC_WINDOW;
	private final String mentionLock = "mentionsList";
	private final InputProcessor inputProcessor;

	public MentionDetectorMap(final Map<String, Collection<String>> map, final InputProcessor inputProcessor) {
		Set<String> inKeys = map.keySet();
		// Adds everything in lower case
		this.keys = new HashSet<>();
		for (String key : inKeys) {
			this.keys.add(InputProcessor.combineProcessedInput(InputProcessor.processToStr(key)));
		}
		this.inputProcessor = inputProcessor;
	}

	/**
	 * Short-hand call to {@link #detect(String, String)} with {@param source=null}
	 * 
	 * @param input input text/corpus to detect mentions from
	 */
	@Override
	public List<Mention> detect(String input) {
		return detect(input, null);
	}

	/**
	 * @param input  input text/corpus to detect mentions from
	 * @param source where this text comes from or what it is linked to
	 */
	@Override
	public List<Mention> detect(final String input, final String source) {
		try {
			final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
					.newFixedThreadPool(Numbers.MENTION_DETECTION_THREAD_AMT.val.intValue());
			AtomicInteger doneCounter = new AtomicInteger(0);

			final List<Mention> mentions = inputProcessor.createMentions(input, source, detectionType);
			for (Mention mention : mentions) {
				execFind(executor, mention, doneCounter);
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
	 * @param mention
	 * @param doneCounter
	 */
	private void execFind(final ThreadPoolExecutor executor, final Mention mention, final AtomicInteger doneCounter) {
		executor.submit(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				final boolean found = find(mention.getOriginalWithoutStopwords());
				if (found) {
					mention.setMention(mention.getOriginalMention());
					mention.setDetectionConfidence(1d);
				}
				return doneCounter.incrementAndGet();
			}
		});
	}

	/**
	 * Finds a mention for a given input token
	 * 
	 * @param input  token or word
	 * @param source what entity this word is linked to
	 * 
	 * @return mention with the closest possible mate
	 * 
	 */
	public boolean find(final String input) {
		if (!this.keys.contains(input)) {
			System.out.println("Could not match w/:" + input);
			final int showAmt = 100;
			int showCounter = 0;
			System.out.println("Number of keys:" + this.keys.size());
			for (String key : this.keys) {
				if (showCounter++ < showAmt) {
					System.out.println("Key:'" + key.toLowerCase() + "'");
				}
			}
			return false;
		}
		// Create a mention with the best-found word
		return true;
	}

	@Override
	public void init() throws Exception {
	}
}
