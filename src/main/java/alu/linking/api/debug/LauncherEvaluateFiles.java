package alu.linking.api.debug;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.ext.com.google.common.collect.Lists;

import alu.linking.api.GERBILAPIAnnotator;
import alu.linking.config.kg.EnumModelType;

public class LauncherEvaluateFiles {

	public static void main(String[] args) {
		final GERBILAPIAnnotator annotator = new GERBILAPIAnnotator(EnumModelType.DBPEDIA_FULL);
		annotator.init();
		final String inputDir = "./evaluation/split_log/";
		final String outputDir = "./evaluation/split_output/";
		// final String inputDir = "./evaluation/min_example.ttl";
		// final String outputDir = "./evaluation/annotated/";

		final File inputDirObj = new File(inputDir);
		final List<File> fileList = Lists.newArrayList();
		if (inputDirObj.isFile()) {
			fileList.add(inputDirObj);
		} else {
			fileList.addAll(Arrays.asList(inputDirObj.listFiles()));
		}
		for (File file : fileList) {
			try {
				if (file.isFile()) {
					final FileInputStream inputStream = new FileInputStream(file);
					System.out.println("Evaluating:" + file.getAbsolutePath());
					final String results = annotator.annotateNIFInputStream(inputStream);
					final String outFilePath = outputDir + file.getName();
					try (BufferedWriter bwOut = new BufferedWriter(new FileWriter(new File(outFilePath)))) {
						System.out.println("Outputting results to:" + outFilePath);
						bwOut.write(results);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			}
		}
		System.out.println("Finished successfully!");
	}

}
