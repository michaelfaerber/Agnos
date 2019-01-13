package alu.linking.launcher;

import java.io.File;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.setup.surfaceform.processing.url.webcrawl.NP_HSFURLContentSaver;

public class LauncherDownloadHTML {

	public static void main(String[] args) {
		final EnumModelType KG = EnumModelType.CRUNCHBASE;
		final NP_HSFURLContentSaver contentSaver = new NP_HSFURLContentSaver(KG);
		final File urlFolder = new File(FilePaths.DIR_QUERY_OUT_NP_URL_HELPING_SURFACEFORM.getPath(KG));
//		try {
//			System.out.println("Google: " + new Crawler().crawlSimple("http://www.google.com"));
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		if (true)
//			return;
		try {
			contentSaver.exec(urlFolder, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Done querying all websites!");
	}

}
