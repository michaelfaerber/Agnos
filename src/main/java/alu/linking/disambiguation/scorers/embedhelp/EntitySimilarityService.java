package alu.linking.disambiguation.scorers.embedhelp;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import alu.linking.config.constants.Comparators;
import alu.linking.structure.Loggable;
import alu.linking.utils.EmbeddingsUtils;
import alu.linking.utils.Stopwatch;

public class EntitySimilarityService implements Loggable {
	private final Map<String, List<Number>> embeddings;
	private final Map<String, Number> distCache;
	public final Set<String> notFoundIRIs = new HashSet<>();
	public final AtomicInteger recovered = new AtomicInteger();
	public final int threadCount;

	public EntitySimilarityService(final Map<String, List<Number>> embeddings) {
		this(embeddings, 40);
	}

	public EntitySimilarityService(final Map<String, List<Number>> embeddings, final int threadCount) {
		this.embeddings = embeddings;
		this.threadCount = threadCount;
		this.distCache = new HashMap<>(embeddings.keySet().size());
	}

	public Number similarity(final String entity1, final String entity2) {
		return similarity(entity1, entity2, false);
	}

	public Number similarity(final String entity1, final String entity2, final boolean normalize) {
		final String keyStr = key(entity1, entity2);
		Stopwatch.start(keyStr);

		Number retVal;
		synchronized (this.distCache) {
			retVal = this.distCache.get(keyStr);
		}
		if (retVal == null) {
			List<Number> left = this.embeddings.get(entity1);
			List<Number> right = this.embeddings.get(entity2);
//				if (left == null || right == null) {
//					// Try with percentage decoding!
//					// Attempt left recovery - if required
//					if (left == null) {
//						final String decodedEntity1 = DecoderUtils.escapePercentage(entity1);
//						if (decodedEntity1 != null && decodedEntity1.length() > 0) {
//							left = this.embeddings.get(decodedEntity1);
//						}
//					}
//
//					// Attempt right recovery - if required
//					if (right == null) {
//						final String decodedEntity2 = DecoderUtils.escapePercentage(entity2);
//						if (decodedEntity2 != null && decodedEntity2.length() > 0) {
//							right = this.embeddings.get(decodedEntity2);
//						}
//					}
//
//					if (left == null || right == null) {
//						// -> couldn't be (completely) recovered
//						if (left == null) {
//							notFoundIRIs.add(entity1);
//						}
//						if (right == null) {
//							notFoundIRIs.add(entity2);
//						}
//						return 0F;
//					} else {
//						recovered.incrementAndGet();
//					}
//				}
			Stopwatch.start("embeddCosineSim" + keyStr);
			retVal = EmbeddingsUtils.cosineSimilarity(left, right, normalize);
			synchronized (this.distCache) {
				if (this.distCache.get(keyStr) == null) {
					this.distCache.put(keyStr, retVal);
				}
			}
			Stopwatch.endOutput("embeddCosineSim" + keyStr, 5, "Slowdown is due to cosine similarity computation!");
		}
		Stopwatch.endOutput(keyStr, 5, "Computed: " + keyStr);
		return retVal;
	}

	private String key(String entity1, String entity2) {
		final StringBuilder sbDistKey;
		final int compareRes = entity1.compareTo(entity2);
		if (compareRes > 0) {
			sbDistKey = new StringBuilder(entity1);
			sbDistKey.append(entity2);
		} else {
			// If they're both equal... it means it's the same entity, so doesn't matter
			// which one is first and which second
			sbDistKey = new StringBuilder(entity2);
			sbDistKey.append(entity1);
		}
		return sbDistKey.toString();
	}

	/**
	 * Returns the highest-rated pair (entity URL, score) based on cosine similarity
	 * rating from a specified entity to wanted targets
	 * 
	 * @param source
	 * @param targets
	 * @return
	 */
	public Pair<String, Double> topSimilarity(final String source, Collection<String> targets,
			final boolean allowSelfConnection) {
		final Collection<String> copyTargets;
		final String watch = "topSimilarity - source -> targets";
		if (!allowSelfConnection && targets.contains(source)) {
			// Copies to a new list and removes the source
			copyTargets = new HashSet<String>(targets);
			copyTargets.remove(source);
		} else {
			copyTargets = targets;
		}
		Stopwatch.start(watch);
		final List<Pair<String, Double>> pairs = computeSortedSimilarities(source, copyTargets,
				Comparators.pairRightComparator.reversed());
		Stopwatch.endOutputStart(watch, 5_000, "topSimilarities - compute + sort similarities");
		if (pairs != null && pairs.size() > 0) {
			return pairs.get(0);
		} else {
			return null;
		}

	}

	public List<Pair<String, Double>> computeSortedSimilarities(final String source, final Collection<String> targets,
			final Comparator<Pair<? extends Comparable, ? extends Comparable>> comparator) {
		final String watch = "computeSortedSim - call";
		Stopwatch.start(watch);
		final List<Pair<String, Double>> retList = computeSimilarities(source, targets);
		Stopwatch.endOutputStart(watch, 5_000, "executed computeSimilarities(source, targets)");
		Collections.sort(retList, comparator);
		Stopwatch.endOutputStart(watch, 5_000, "Time it took to sort similarities");
		return retList;
	}

	/**
	 * Computes similarities from a source to the given targets and returns a list
	 * with all of them
	 * 
	 * @param source  from where
	 * @param targets to where
	 * @return
	 */
	public List<Pair<String, Double>> computeSimilarities(final String source, final Collection<String> targets) {
		List<Pair<String, Double>> retList = Lists.newArrayList();

		AtomicInteger doneCounter = new AtomicInteger(0);
		final int todoSimilarities = targets.size();
		final int threads = Math.min(this.threadCount, todoSimilarities);
		final boolean SINGLE = false;

		if (SINGLE) {
			Iterator<String> itTarget = targets.iterator();
			while (itTarget.hasNext()) {
				final String target = itTarget.next();
				final Pair<String, Double> pair = new ImmutablePair<String, Double>(target,
						similarity(source, target).doubleValue());
				synchronized (retList) {
					retList.add(pair);
				}
			}
		} else {
			final ThreadPoolExecutor pool = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.SECONDS,
					new java.util.concurrent.LinkedBlockingQueue<Runnable>());
			final long sleepStep = 10l;
			try {
				Iterator<String> itTarget = targets.iterator();
				do {
					while (itTarget.hasNext() && pool.getActiveCount() < threads) {
						final String target = itTarget.next();
						// Multi thread here
						pool.execute(new Runnable() {
							@Override
							public void run() {
								final Pair<String, Double> pair = new ImmutablePair<String, Double>(target,
										similarity(source, target).doubleValue());
								synchronized (retList) {
									retList.add(pair);
								}
								doneCounter.incrementAndGet();
							}
						});
					}
					Thread.sleep(sleepStep);

				} while (itTarget.hasNext());
				pool.shutdown();
				long sleepCounter = 0l;
				final String watch = "entitySim - Sleep";
				Stopwatch.start(watch);
				do {
					// No need for await termination as this is pretty much it already...
					Thread.sleep(sleepStep);
					sleepCounter += sleepStep;
					Stopwatch.endOutputRestart(watch, 5_000, "Similarities - Completed: " + doneCounter.get() + "/"
							+ todoSimilarities + " in " + sleepCounter + "ms.");
				} while (!pool.isTerminated());
				pool.awaitTermination(30L, TimeUnit.DAYS);
				Stopwatch.endOutputStart(watch, 5_000, "done...");
			} catch (InterruptedException ie) {
				getLogger().error("Exception during thread execution...", ie);
			}
		}

		return retList;
	}

	/**
	 * Check that the passed iterable's items really have associated embeddings.
	 * Removes item from collection otherwise.
	 * 
	 * @param collection
	 */
	public void ascertainSimilarityExistence(final Iterable<String> collection) {
		final Iterator<String> iter = collection.iterator();
		while (iter.hasNext()) {
			final String key = iter.next();
			if (!this.embeddings.containsKey(key)) {
				iter.remove();
			}
		}
	}

}
