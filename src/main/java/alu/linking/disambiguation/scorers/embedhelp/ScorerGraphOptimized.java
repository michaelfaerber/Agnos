package alu.linking.disambiguation.scorers.embedhelp;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScorerGraphOptimized extends AbstractScorerGraph {
	private Map<String, List<String>> clusters = null;
	private double amtEntities = -1d;
	private final String separatorSFEntity = "//__//";
	private HashMap<String, Number> iterMap = null;

	ScorerGraphOptimized(final EntitySimilarityService similarityService) {
		super(similarityService);
	}

	public ScorerGraphOptimized clusters(final Map<String, List<String>> clusters) {
		this.clusters = clusters;
		int sumItems = 0;
		for (Map.Entry<String, List<String>> e : clusters.entrySet()) {
			sumItems += e.getValue().size();
		}
		this.amtEntities = (double) sumItems;
		this.iterMap = new HashMap<>((int) this.amtEntities);

		return this;
	}

	public void pagerank() {
		for (int i = 0; i < this.iter; ++i) {
			iteration(iterMap);
		}
	}

	public Collection<String> getTop() {
		// Sort by value of scores
		final Map<String, String> sfEntityTrackerMap = new HashMap<>(this.clusters.size());
		final Map<String, Number> topScores = new HashMap<>(this.clusters.size());
		for (Map.Entry<String, Number> e : iterMap.entrySet()) {
			final String iterKey = e.getKey();
			final String clusterKey = getClusterKey(iterKey);
			final Number entitySFScore = e.getValue();
			final Number currBestScore = topScores.getOrDefault(clusterKey, -10F);
			// Issues with the topScores data structure
			// Need to insert by specific cluster (aka. by surface form, so they are unique)
			// but need to be able to link the score to the entity
			// Idea: use a second map to keep track of the surfaceForm/topEntity link and
			// update it when changes to topScores are made
			if (currBestScore.doubleValue() < entitySFScore.doubleValue()) {
				topScores.put(clusterKey, entitySFScore);
				sfEntityTrackerMap.put(clusterKey, getClusterVal(iterKey));
			}
		}

		Set<String> ret = new HashSet<String>();
		for (Map.Entry<String, String> e : sfEntityTrackerMap.entrySet()) {
			ret.add(e.getValue());
		}
		return ret;// sfEntityTrackerMap.keySet();
	}

	private void iteration(final Map<String, Number> iterMap) {
		final double N = this.amtEntities;
		final double d = this.damping;
		for (Map.Entry<String, List<String>> entryToCluster : this.clusters.entrySet()) {
			for (Map.Entry<String, List<String>> entryFromCluster : this.clusters.entrySet()) {
				if (!entryToCluster.getKey().equals(entryFromCluster.getKey())) {
					// Only if it's not the same key (aka. different surface forms)
					for (String toNode : entryToCluster.getValue()) {
						double predecessorPRSum = 0d;
						for (String fromNode : entryFromCluster.getValue()) {
							final Number weight = this.similarityService.similarity(fromNode, toNode);
							final Number prevNodePR = iterMap.getOrDefault(iterKey(entryFromCluster.getKey(), fromNode),
									this.startValue);
							// This only works due to N-to-N connectivity (excluding own cluster's entities)
							final double prevNodeConnections = (double) (N - entryFromCluster.getValue().size());
							predecessorPRSum += weight.doubleValue() * (prevNodePR.doubleValue() / prevNodeConnections);
						}
						final Number PR = (double) ((1.0d - d) / N + d * predecessorPRSum);
						iterMap.put(iterKey(entryToCluster.getKey(), toNode), PR);
					}
				}
			}
		}
	}

	private String iterKey(String surfaceForm, String entity) {
		return surfaceForm + this.separatorSFEntity + entity;
	}

	private String getClusterKey(final String iterKey) {
		return iterKey.split(this.separatorSFEntity)[0];
	}

	private String getClusterVal(final String iterKey) {
		return iterKey.split(this.separatorSFEntity)[1];
	}

	public Set<String> getNotFoundIRIs() {
		return this.similarityService.notFoundIRIs;
	}

}
