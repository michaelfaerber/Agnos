package alu.linking.launcher.eval.cb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.google.common.io.Files;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.utils.CBURLTransformer;

/**
 * Filter news based on URLs
 * 
 * @author Kwizzer
 *
 */
public class LauncherFilterNews {
	public static void main(String[] argv) {
		final EnumModelType KG = EnumModelType.CRUNCHBASE2;

		try {
			// Load the entities
			final File sfEntities = new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG));
			final Set<String> entities = new HashSet<>();
			try (final BufferedReader brIn = new BufferedReader(new FileReader(sfEntities))) {
				String line = null;
				final String delim = Strings.ENTITY_SURFACE_FORM_LINKING_DELIM.val;
				while ((line = brIn.readLine()) != null) {
					final String[] tokens = line.split(delim);
					entities.add(tokens[0]);
				}
			}
			System.out.println("Finished loading entities(" + entities.size() + ")!");

			// Links directory
			final File linkDirectory = new File(FilePaths.DIR_NEWS_LINKS.getPath(KG));

			// Body directory
			final File bodyDirectory = new File(FilePaths.DIR_NEWS_BODY.getPath(KG));

			for (File f : linkDirectory.listFiles()) {
				if (!f.isFile()) {
					continue;
				}
				try (final BufferedReader brIn = new BufferedReader(new FileReader(f))) {
					String line = null;
					boolean keep = false;
					while ((line = brIn.readLine()) != null) {
						if (wantedLine(line, entities)) {
							counter++;
							keep = true;
							break;
						}
					}
					if (keep) {
						System.out.println("KEEP("+counter+"): " + f.getAbsolutePath());
						// Copy them
						// Copy links
						Files.copy(f, new File(FilePaths.DIR_NEWS_FILTERED_LINKS.getPath(KG) + "/" + f.getName()));
						// Copy body
						Files.copy(new File(bodyDirectory.getAbsolutePath() + "/" + f.getName()),
								new File(FilePaths.DIR_NEWS_FILTERED_BODY.getPath(KG) + "/" + f.getName()));
					}
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static int counter = 0;
	private static boolean wantedLine(String line, Set<String> entities) {
		/**
		 * 
			Für das Beispiel von IBM:
			Von: https://crunchbase.com/organization/ibm
			Nach: http://linked-crunchbase.org/api/organizations/ibm#id
		 */
		return entities.contains(CBURLTransformer.toCustomKG(line));
		// return line.contains("crunch");
	}
}
