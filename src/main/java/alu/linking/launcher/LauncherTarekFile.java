package alu.linking.launcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.semanticweb.yars.nx.Node;

import com.beust.jcommander.internal.Lists;
import com.google.common.io.Files;

import alu.linking.mentiondetection.Mention;
import alu.linking.utils.MentionUtils;

public class LauncherTarekFile {

	public static void main(String[] args) {
		try {
			final String DEFAULT_OUTPUT_FILE = "./out_linking.txt";
			final String DEFAULT_OUTPUT_DIR = "./out_linking/";
			if (args.length == 0) {
				System.out.println("Usage (1): <inFile> <outFile>");
				System.out.println("Takes <inFile> as input and outputs detected linked mentions to <outFile>");
				System.out.println("Usage (2): <inFile>");
				System.out.println("Note: Output will be set to:" + DEFAULT_OUTPUT_FILE);
				System.out.println("Usage (3): <inDirectory> <outDirectory>");
				System.out.println(
						"Takes all files from <inDirectory> as separate inputs and outputs detected linked mentions to the <outDirectory> directory with the corresponding names");
				System.out.println("Note: Any subdirectories within <inDirectory> will be IGNORED.");
				System.out.println("Usage (4): <inDirectory>");
				System.out.println("Note: Output directory will be set to:" + DEFAULT_OUTPUT_DIR);
				return;
			}

			File outputFile = null;
			final LauncherInputLinking linking = new LauncherInputLinking();
			if (args.length > 0) {
				final File inputFile = new File(args[0]);
				if (args.length > 1) {
					outputFile = new File(args[1]);
				} else {
					outputFile = new File(inputFile.isDirectory() ? DEFAULT_OUTPUT_DIR : DEFAULT_OUTPUT_FILE);
				}
				List<File> toProcessFiles = Lists.newArrayList();
				if (inputFile.isDirectory()) {

					for (File f : inputFile.listFiles()) {
						if (f.isFile()) {
							toProcessFiles.add(f);
						}
					}
				} else {
					toProcessFiles.add(inputFile);
				}

				for (File f : toProcessFiles) {
					// Read stuff from input file/dir
					final StringBuilder sb = new StringBuilder();
					try (final BufferedReader br = new BufferedReader(new FileReader(f))) {
						String line = null;
						while ((line = br.readLine()) != null) {
							sb.append(line + System.getProperty("line.separator"));
						}
					}
					// One-time linking
					final List<Mention<Node>> mentions = linking.run(sb.toString());
					final String formattedOutput = MentionUtils.formatMentionsTabbedLines(mentions, sb.toString());
					final File f_out = inputFile.isDirectory()
							? new File(outputFile.getAbsolutePath() + "/" + f.getName())
							: outputFile;
					if (!f_out.exists()) {
						Files.createParentDirs(f_out);
					}
					try (final BufferedWriter bw = new BufferedWriter(new FileWriter(f_out))) {
						bw.write(formattedOutput);
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

}
