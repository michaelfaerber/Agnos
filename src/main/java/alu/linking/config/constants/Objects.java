package alu.linking.config.constants;

import alu.linking.preprocessing.webcrawler.EnumTextProcessorType;

/**
 * Class containing more complex constants for the framework
 * 
 * @author Kristian Noullet
 *
 */
public class Objects {
	// EnumTextProcessorType.WEBCRAWLER_OFFLINE -> takes offline websites
	// EnumTextProcessorType.WEBCRAWLER_ONLINE -> crawls the content from websites
	public static final EnumTextProcessorType CONNECTION_OR_OFFLINE = EnumTextProcessorType.WEBCRAWLER_OFFLINE;
	public static final EnumConnection VIRTUOSO_SERVER = EnumConnection.CELEBES;
}
