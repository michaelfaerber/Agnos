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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import alu.linking.config.constants.Comparators;
import alu.linking.utils.DecoderUtils;
import alu.linking.utils.EmbeddingsUtils;

public class EntitySimilarityService {
	private final Map<String, List<Number>> embeddings;
	private final Map<String, Number> distCache = new HashMap<>();
	public final Set<String> notFoundIRIs = new HashSet<>();
	public final AtomicInteger recovered = new AtomicInteger();

	public EntitySimilarityService(final Map<String, List<Number>> embeddings) {
		this.embeddings = embeddings;
	}

	public Number similarity(final String entity1, final String entity2) {
		final String keyStr = key(entity1, entity2);
		Number retVal;
		synchronized (this.distCache) {
			retVal = this.distCache.get(keyStr);
		}
		if (retVal == null) {
			List<Number> left = this.embeddings.get(entity1);
			List<Number> right = this.embeddings.get(entity2);
			if (left == null || right == null) {
				// Try with percentage decoding!
				// Attempt left recovery - if required
				if (left == null) {
					final String decodedEntity1 = DecoderUtils.escapePercentage(entity1);
					if (decodedEntity1 != null && decodedEntity1.length() > 0) {
						left = this.embeddings.get(decodedEntity1);
					}
				}

				// Attempt right recovery - if required
				if (right == null) {
					final String decodedEntity2 = DecoderUtils.escapePercentage(entity2);
					if (decodedEntity2 != null && decodedEntity2.length() > 0) {
						right = this.embeddings.get(decodedEntity2);
					}
				}

				if (left == null || right == null) {
					// -> couldn't be (completely) recovered
					if (left == null) {
						notFoundIRIs.add(entity1);
					}
					if (right == null) {
						notFoundIRIs.add(entity2);
					}
					return 0F;
				} else {
					recovered.incrementAndGet();
				}
			}
			retVal = EmbeddingsUtils.cosineSimilarity(left, right, true);
			synchronized (this.distCache) {
				if (this.distCache.get(keyStr) != null) {
					this.distCache.put(keyStr, retVal);
				}
			}
		}
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
		if (!allowSelfConnection && targets.contains(source)) {
			// Copies to a new list and removes the source
			final Set<String> copyTargets = new HashSet<String>(targets);
			copyTargets.remove(source);
			List<Pair<String, Double>> pairs = computeSortedSimilarities(source, copyTargets,
					Comparators.pairRightComparator.reversed());
			if (pairs != null && pairs.size() > 0) {
				return pairs.get(0);
			} else {
				return null;
			}
		} else {
			List<Pair<String, Double>> pairs = computeSortedSimilarities(source, targets,
					Comparators.pairRightComparator.reversed());
			if (pairs != null && pairs.size() > 0) {
				return pairs.get(0);
			} else {
				return null;
			}
		}
	}

	public List<Pair<String, Double>> computeSortedSimilarities(final String source, final Collection<String> targets,
			final Comparator<Pair<? extends Comparable, ? extends Comparable>> comparator) {
		final List<Pair<String, Double>> retList = computeSimilarities(source, targets);
		Collections.sort(retList, comparator);
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
		for (String target : targets) {
			retList.add(new ImmutablePair<String, Double>(target, similarity(source, target).doubleValue()));
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
