package alu.linking.launcher;

import java.io.File;

import alu.linking.config.kg.EnumModelType;
import alu.linking.utils.RDFUtils;

public class LauncherSFFromNTFile {
	public static void main(final String[] argv) {
		// Shetland input file for wikidata
		System.out.println("Get surface forms from a specified .NT file w/ a query");
		final String inPath = "/vol2/wikidata/dumps/20190213/wikidata-20190213-truthy-BETA_all-en-labels.nt";
		final File inputFile = new File(inPath);
		final String query = //
				"SELECT DISTINCT ?s (STR(?obj) AS ?o) WHERE "//
						+ "{ ?s ?p ?obj ." + " FILTER( isLiteral(?obj) ) ."//
						+ " FILTER( STRLEN(STR(?obj)) > 1 ) }"//
		;
		RDFUtils.getEntitiesFromNTFile(EnumModelType.WIKIDATA, inputFile, query);
	}
}
