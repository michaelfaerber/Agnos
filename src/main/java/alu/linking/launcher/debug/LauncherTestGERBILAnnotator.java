package alu.linking.launcher.debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import alu.linking.api.GERBILAPIAnnotator;
import alu.linking.config.kg.EnumModelType;

public class LauncherTestGERBILAnnotator {

	public static void main(String[] args) {
		final EnumModelType KG = EnumModelType.DBPEDIA_FULL;
		final GERBILAPIAnnotator gerbil = new GERBILAPIAnnotator(KG);
		gerbil.init();

		try (final FileInputStream inputStream = new FileInputStream(
				new File("./evaluation/kore50-nif_angelina.ttl"))) {
			System.out.println("Annotated stuff:");
			System.out.println(gerbil.annotateNIFInputStream(inputStream));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
