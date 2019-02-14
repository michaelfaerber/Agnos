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

import alu.linking.utils.EmbeddingsUtils;

public class EntitySimilarityService {
	private final Comparator<Pair<? extends Comparable, ? extends Comparable>> rightComparator = new Comparator<Pair<? extends Comparable, ? extends Comparable>>() {

		@Override
		public int compare(Pair<? extends Comparable, ? extends Comparable> o1,
				Pair<? extends Comparable, ? extends Comparable> o2) {
			return o1.getRight().compareTo(o2.getRight());
		}

	};
	private final Comparator<Pair<? extends Comparable, ? extends Comparable>> leftComparator = new Comparator<Pair<? extends Comparable, ? extends Comparable>>() {

		@Override
		public int compare(Pair<? extends Comparable, ? extends Comparable> o1,
				Pair<? extends Comparable, ? extends Comparable> o2) {
			return o1.getLeft().compareTo(o2.getLeft());
		}

	};
	private final Map<String, List<Number>> embeddings;
	private final Map<String, Number> distCache = new HashMap<>();
	public final Set<String> notFoundIRIs = new HashSet<>();

	public EntitySimilarityService(final Map<String, List<Number>> embeddings) {
		this.embeddings = embeddings;
	}

	public Number similarity(final String entity1, final String entity2) {
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
		final String sbDistKeyStr = sbDistKey.toString();
		Number retVal;
		synchronized (this.distCache) {
			retVal = this.distCache.get(sbDistKeyStr);
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
				this.distCache.put(sbDistKeyStr, retVal);
			}
		}
		return retVal;
	}

	/**
	 * Returns the highest-rated pair (entity URL, score) based on cosine similarity
	 * rating from a specified entity to wanted targets
	 * 
	 * @param source
	 * @param targets
	 * @return
	 */
	public Pair<String, Double> topSimilarity(final String source, Collection<String> targets) {
		final List<Pair<String, Double>> retList = Lists.newArrayList();

		for (String target : targets) {
			retList.add(new ImmutablePair<String, Double>(target, similarity(source, target).doubleValue()));
		}
		Collections.sort(retList, rightComparator.reversed());
		return retList.get(0);
	}

}
