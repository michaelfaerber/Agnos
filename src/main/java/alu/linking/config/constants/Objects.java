package alu.linking.config.constants;

import alu.linking.preprocessing.webcrawler.EnumTextProcessorType;

public class Objects {
	// EnumTextProcessorType.WEBCRAWLER_OFFLINE -> takes offline websites
	// EnumTextProcessorType.WEBCRAWLER_ONLINE -> crawls the content from websites
	public static EnumTextProcessorType CONNECTION_OR_OFFLINE = EnumTextProcessorType.WEBCRAWLER_OFFLINE;
	public static EnumConnection VIRTUOSO_SERVER = EnumConnection.CELEBES;
}
