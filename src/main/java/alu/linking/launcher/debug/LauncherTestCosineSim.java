package alu.linking.launcher.debug;

import java.util.Arrays;

import alu.linking.utils.EmbeddingsUtils;

public class LauncherTestCosineSim {

	public static void main(String[] args) {
		System.out.println(EmbeddingsUtils.cosineSimilarity(Arrays.asList(new Double[] {5.0, 10.0}), Arrays.asList(new Double[] {2.0, 5.0})));
	}

}
