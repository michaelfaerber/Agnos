package alu.linking.config.constants;

import alu.linking.mentiondetection.EnumDetectionType;

public enum Numbers {
	// Contains all numeric constants
	// Attempting to keep a specific order
	//
	// ##################################
	// # MAIN NUMERIC CONSTANTS
	// ##################################
	THRESHOLD_NOUN_PHRASE_MIN_LEN(50,
			"Minimal character length of a string literal to be considered for noun-phrase extraction (to get helping surface forms)"), //
	WEBCRAWLER_CONNECTIONS(10240,
			"Number of threads/connections used for crawling the web. Note that it can/should be 'high' due to the sometimes long waiting times for web responses"), //
	WEBCRAWLER_CONNECTIONS_TIMEOUT_MS(30_000L, "Timeout for connections, generally 300s is way more than enough"), //
	HOPS_PATH_LENGTH(1, "Maximum length of paths to be computed"), //
	HOPS_THREAD_AMT(10, "Number of threads computing hops"), //
	SCORER_THREAD_AMT(10, "Number of threads computing scores"), //
	// Disambiguation
	VICINITY_SCORING_WEIGHT_SIGMA(0.5,
			"Generally a value between 0 and 1. Represents the loss of information per hop beyond a direct link. Weight used to decrease the weight of longer hop-paths"), //
	VICINITY_WEIGHT(5, "How much vicinity scoring affects final score"), //
	PAGERANK_WEIGHT(10, "How much pagerank scoring affects final score"), //
	// Mention Detection
	MENTION_DETECTION_WINDOW_SIZE(3, "Size of the sliding window for mention detection when using "
			+ EnumDetectionType.BOUND_DYNAMIC_WINDOW.name()
			+ " mode. Note that this number generally represents the (maximum) 'number of words' used for word detection"), //
	MENTION_DETECTION_THREAD_AMT(8, "Number of threads used for mention detection"), //
	MENTION_DETECTION_DEFAULT_THRESHOLD(0.85,
			"Default threshold for mention detection. Exists just out of ease of use in case unsure of good choice of threshold."), //
	// LSH Variables
	LSH_BANDS(20, "Number of bands used for LSH/MinHash"), //
	LSH_BUCKETS(1000, "Number of buckets used for LSH/MinHash"),//
	// ##################################
	// DATASET NUMERIC CONSTANTS
	// ##################################

	// ##################################
	// # TEST
	// ##################################
	;
	public final Number val;

	Numbers(final Number val) {
		this(val, "");
	}

	Numbers(final Number val, final String desc) {
		this.val = val;
	}

	@Override
	public String toString() {
		return String.valueOf(val);
	}

}
