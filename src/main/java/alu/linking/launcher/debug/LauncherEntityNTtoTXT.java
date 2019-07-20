package alu.linking.launcher.debug;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.utils.IterableEntity;
import alu.linking.utils.IterableFileEntity;

/**
 * Class to simply find and output entities based on a .NT file, assuming each
 * subject is an entity
 * 
 * @author Kristian Noullet
 *
 */
public class LauncherEntityNTtoTXT {

	public static void main(String[] args) {
		final EnumModelType kg = EnumModelType.MAG;
		final boolean skipDuplicates = true;
		final String inputFile = FilePaths.FILE_NT_ENTITIES.getPath(kg);
		final String outputFile = FilePaths.FILE_TXT_ENTITIES.getPath(kg);
		try (final BufferedWriter bwOut = new BufferedWriter(new FileWriter(new File(outputFile)));
				final IterableEntity iterable = new IterableFileEntity(new File(inputFile), skipDuplicates)) {
			for (String entity : iterable) {
				bwOut.write(entity);
				bwOut.newLine();
			}
		} catch (

		Exception e) {
			e.printStackTrace();
		}

	}

}
