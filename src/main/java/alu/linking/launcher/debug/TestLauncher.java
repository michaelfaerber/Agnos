package alu.linking.launcher.debug;

import java.util.List;

import org.apache.jena.ext.com.google.common.collect.Lists;

import alu.linking.utils.EmbeddingsUtils;

public class TestLauncher {

	public static void main(String[] args) {
		// final List<List<String>> result = Lists.newArrayList();
		final List<List<String>> clusters = Lists.newArrayList();
		final List<String> cluster1 = Lists.newArrayList(new String[] { "1", "2" });
		final List<String> cluster2 = Lists.newArrayList(new String[] { "3", "4", "5" });
		final List<String> cluster3 = Lists.newArrayList(new String[] { "6" });
		final List<String> cluster4 = Lists.newArrayList(new String[] { "7", "8", "9" });
		// EmbeddingsUtils.findPermutations(0, 0, new ArrayList<>(), result, cluster1,
		// cluster2);
		clusters.add(cluster1);
		clusters.add(cluster2);
		clusters.add(cluster3);
		clusters.add(cluster4);
		final List<List<String>> result = EmbeddingsUtils.findPermutations(clusters);
		System.out.println("Result:" + result.size());
		System.out.println(result);

		final List<Number> embedding1 = Lists.newArrayList(new Number[] { 2.5, 3.6, 4.2 });
		final List<Number> embedding2 = Lists.newArrayList(new Number[] { 2.1, 2.6, 3.2 });
		System.out.println("CoSim:" + EmbeddingsUtils.cosineSimilarity(embedding1, embedding2));

	}

}
