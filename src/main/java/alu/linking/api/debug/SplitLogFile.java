package alu.linking.api.debug;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

public class SplitLogFile {

	public static void main(String[] args) {
		new SplitLogFile().process();
	}

	public void process() {
		final String inputFile = "./web_app.log";
		final String outputDir = "./split_log/";
		int fileCounter = 0;
		final String[] splitConditions = new String[] { "Input from GERBIL - START:", "Input from GERBIL - END" };
		final HashSet<String> splitLines = new HashSet<String>(Arrays.asList(splitConditions));
		try (final BufferedReader brIn = new BufferedReader(new FileReader(new File(inputFile)))) {
			String line = null;
			File outFile = new File(outputDir + fileCounter++ + ".log");
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
			while ((line = brIn.readLine()) != null) {
				if (splitLines.contains(line)) {
					bw.close();
					outFile = new File(outputDir + fileCounter++ + ".log");
					bw = new BufferedWriter(new FileWriter(outFile));
					continue;
				}
				bw.write(line);
				bw.newLine();
			}
			bw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
