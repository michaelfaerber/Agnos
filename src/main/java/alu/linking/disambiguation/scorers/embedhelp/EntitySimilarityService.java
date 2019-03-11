package alu.linking.disambiguation.scorers.embedhelp;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import alu.linking.config.constants.Comparators;
import alu.linking.utils.EmbeddingsUtils;

public class EntitySimilarityService {
	private final Map<String, List<Number>> embeddings;
	private final Map<String, Number> distCache = new HashMap<>();
	public final Set<String> notFoundIRIs = new HashSet<>();

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
			final List<Number> left = this.embeddings.get(entity1);
			final List<Number> right = this.embeddings.get(entity2);
			if (left == null || right == null) {
				if (left == null) {
					notFoundIRIs.add(entity1);
				}
				if (right == null) {
					notFoundIRIs.add(entity2);
				}
				return 0F;
			}
			retVal = EmbeddingsUtils.cosineSimilarity(left, right, true);
			synchronized (this.distCache) {
				this.distCache.put(keyStr, retVal);
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
					Comparators.pairRightComparator);
			if (pairs != null && pairs.size() > 0) {
				return pairs.get(0);
			} else {
				return null;
			}
		} else {
			List<Pair<String, Double>> pairs = computeSortedSimilarities(source, targets,
					Comparators.pairRightComparator);
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
		Collections.sort(retList, comparator.reversed());
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

}
