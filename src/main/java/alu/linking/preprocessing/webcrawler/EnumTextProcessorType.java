package alu.linking.preprocessing.webcrawler;

import java.lang.reflect.InvocationTargetException;

import alu.linking.structure.TextProcessor;

public enum EnumTextProcessorType {
	WEBCRAWLER_ONLINE(CrawlerConnection.class), WEBCRAWLER_OFFLINE(CrawlerOfflineTextProcessor.class);
	private final Class<? extends TextProcessor> clTextProc;

	EnumTextProcessorType(Class<? extends TextProcessor> cl) {
		this.clTextProc = cl;
	}

	public TextProcessor createTextProcess(final String idURL) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return this.clTextProc.getDeclaredConstructor(idURL.getClass()).newInstance(idURL);
	}

}
