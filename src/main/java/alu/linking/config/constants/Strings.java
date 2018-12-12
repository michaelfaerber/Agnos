package alu.linking.config.constants;

public enum Strings {
	// Attempting to keep a specific order
	// From most-common to most-specific
	NEWLINE(System.getProperty("line.separator")), //
	CSV_DELIM(";"), //
	CRM_PRED_NEWS("http://km.aifb.kit.edu/services/crunchbase/api-vocab#news",
			"Crummymatch's predicate for finding news"), //
	CRM_PRED_CONTENT("http://rdfs.org/sioc/ns#content"), //
	CRM_PRED_TITLE("http://purl.org/dc/terms/title"), //
	PRED_RDF_LABEL("http://www.w3.org/2000/01/rdf-schema#label"), //
	PRED_SURFACE_FORM_CRUMMYMATCH("http://www.w3.org/2000/01/rdf-schema#label"), //
	PRED_SURFACE_FORM_CRUNCHBASE("http://ontologycentral.com/2010/05/cb/vocab#name"), //
	PRED_HELPING_SURFACE_FORM("http://own.org/helpingSurfaceFormExtractedNP"), //
	RDF_TYPED_LITERAL_STRING("^^<http://www.w3.org/2001/XMLSchema#string>"), //
	RDF_FILE_PREFIX("file://"), //
	RDF_BLANK_NODE_PREFIX("http://own.org/"), //
	NPCOMPLETE_TAG_NN("NN", "NN tag from RBBNPE"), //
	NPCOMPLETE_TAG_NNS("NNS", "NNS tag from RBBNPE"), //

	// Hops-related settings
	HOPS_GRAPH_DUMP_SEPARATOR("\t"), //
	HOPS_NODE_PATH_SEPARATOR(" "), //
	HOPS_PATH_BUILDER_DELIMITER(" "), //
	PATH_EDGE_EDGE_DELIM(" "), //
	EDGE_DIR_CODE_NEXT_STR(">"), //
	EDGE_DIR_CODE_PREV_STR("<"), //
	EDGE_DIR_CODE_BOTH_STR("o"), //
	EDGE_DIR_CODE_NONE_STR("x"), //
	PATH_EDGE_DELIM_START("--"), //
	PATH_EDGE_DELIM_END("->"), //
	// Query-related settings
	ENTITY_SURFACE_FORM_LINKING_DELIM(" %%%%% "), //" %%%%% "//"\t"
	QUERY_RESULT_DELIMITER("\t;\t"),//
	
	// Embeddings- and Walks-related settings
	EMBEDDINGS_SENTENCES_DELIM("\t", "Delimiter used for outputting sentences to the appropriate files"),//
	EMBEDDINGS_TRAINED_DELIM("\t", "Delimiter used in python to output the trained word embeddings into word:embedding lines"),//
	EMBEDDINGS_TRAINED_SENTENCES_DELIM("\t", "Delimiter used for the combination of the word embeddings into sentence embeddings (e.g. through summing, etc)"),//
	EMBEDDINGS_ENTITY_EMBEDDINGS_DUMP_DELIMITER("\t", "Delimiter for the dumping of entity embeddings"),
	;
	public final String val;
	public final String desc;

	private Strings(final String value) {
		this(value, "");
	}

	private Strings(final String value, final String desc) {
		this.val = value;
		this.desc = desc;
	}

	@Override
	public String toString() {
		return val;
	}

}
