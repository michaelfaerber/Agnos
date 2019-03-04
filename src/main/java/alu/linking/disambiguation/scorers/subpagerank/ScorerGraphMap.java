package alu.linking.disambiguation.scorers.subpagerank;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.internal.Lists;

import alu.linking.disambiguation.pagerank.AssignmentScore;
import alu.linking.disambiguation.scorers.embedhelp.EntitySimilarityService;
import alu.linking.structure.Loggable;
import alu.linking.utils.Stopwatch;

public class ScorerGraphMap extends AbstractScorerGraph implements Loggable {
	private Map<String, Double> prScores = new HashMap<>();

	ScorerGraphMap(final EntitySimilarityService similarityService) {
		super(similarityService);
	}

	public ScorerGraphMap startValue(Number num) {
		this.startValue = num.doubleValue();
		return this;
	}

	public ScorerGraphMap iterations(final int iterations) {
		this.iter = iterations;
		return this;
	}

	public ScorerGraphMap dampingFactor(final Number damping) {
		this.damping = damping.floatValue();
		return this;
	}

	public void pagerank(final Map<String, List<String>> clusters) {
		// Compute the number of outgoing edges for each entity
		final HashMap<String, Integer> mapOutEntityAmt = new HashMap<>();
		// Which ones are incoming to the entity?
		final HashMap<String, List<String>> mapIncToEntity = new HashMap<>();
		Stopwatch.start(getClass().getName());
		for (Map.Entry<String, List<String>> eSource : clusters.entrySet()) {
			for (Map.Entry<String, List<String>> eTarget : clusters.entrySet()) {
				if (!eTarget.getKey().equals(eSource.getKey())) {
					// Connections from eSource to eTarget (making sure none go from the cluster to
					// itself)
					for (String sourceEntity : eSource.getValue()) {
						for (String targetEntity : eTarget.getValue()) {
							final double sim = this.similarityService.similarity(sourceEntity, targetEntity)
									.doubleValue();
							if (sim < MIN_SIMILARITY) {
								// skip if too small
								continue;
							}

							// Incoming connections to the target
							List<String> incTarget = mapIncToEntity.get(targetEntity);
							if (incTarget == null) {
								incTarget = Lists.newArrayList();
								mapIncToEntity.put(targetEntity, incTarget);
							}

							// Incoming connections to the source
							final List<String> incSource = mapIncToEntity.get(sourceEntity);
							if (incSource == null) {
								mapIncToEntity.put(sourceEntity, Lists.newArrayList());
							}
							incTarget.add(sourceEntity);

							final Integer numberOut = mapOutEntityAmt.get(sourceEntity);
							if (numberOut == null) {
								mapOutEntityAmt.put(sourceEntity, 1);
							} else {
								mapOutEntityAmt.put(sourceEntity, numberOut + 1);
							}
						}
					}
				}
			}
		}

		final Set<String> entities = mapIncToEntity.keySet();
		final double initDampingPR = 1.0d - this.damping;
		Stopwatch.endOutputStart(getClass().getName());
		for (int i = 0; i < this.iter; i++) {
			for (final String entity : entities) {
				final List<String> incLinks = mapIncToEntity.get(entity);

				double pageRank = initDampingPR;
				for (final String inc : incLinks) {
					Double pageRankIn = prScores.get(inc);
					if (pageRankIn == null) {
						pageRankIn = this.startValue;
					}
					final double outAmt = mapOutEntityAmt.get(inc).doubleValue();
					pageRank += this.damping * (pageRankIn.doubleValue() / outAmt);
				}
				prScores.put(entity, pageRank);
			}
		}
		Stopwatch.endOutputStart(getClass().getName());

		// Now sort them so that it's easy to get...

	}

	/**
	 * Grabs the best item PR-wise for this cluster
	 * 
	 * @param cluster list of entities for which PR was computed
	 * @return the best assignment score
	 */
	public AssignmentScore grabBest(final List<String> cluster) {
		return grabBest(cluster, 1).get(0);
	}

	/**
	 * Grabs the TOP_K best items PR-wise for the given cluster
	 * 
	 * @param cluster list of entities for which PR was computed
	 * @return list of best assignment scores
	 */
	public List<AssignmentScore> grabBest(final List<String> cluster, final int TOP_K) {
		final List<AssignmentScore> assignmentScores = Lists.newArrayList();
		for (String entity : cluster) {
			assignmentScores
					.add(new AssignmentScore().assignment(entity).score(this.prScores.getOrDefault(entity, 0d)));
		}
		Collections.sort(assignmentScores, Comparator.reverseOrder());
		return assignmentScores.subList(0, TOP_K);
	}
}
