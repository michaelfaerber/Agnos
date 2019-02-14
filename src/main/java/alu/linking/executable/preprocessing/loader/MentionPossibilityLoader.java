package alu.linking.executable.preprocessing.loader;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.preprocessing.surfaceform.MentionPossibilityExtractor;
import alu.linking.structure.Executable;

/**
 * This class extracts literals and maps them as Map<Literal, Set<Source>> The
 * point is to be able to give the MentionDetector an input map to match against
 * text occurrences.
 * 
 * @author Kwizzer
 *
 */
public class MentionPossibilityLoader implements Executable {
	private MentionPossibilityExtractor mpe;

	final EnumModelType KG;

	public MentionPossibilityLoader(final EnumModelType KG) {
		this.KG = KG;
		init();
	}

	@Override
	public void init() {
		this.mpe = new MentionPossibilityExtractor(this.KG);
	}

	@Override
	public boolean reset() {
		init();
		return true;
	}

	@Override
	public Map<String, Set<String>> exec(Object... o) throws IOException {
		Map<String, Set<String>> ret = null;
		if (o != null) {
			final File blacklistFile, entityLinkingFile;
			if (o.length == 2 && o[0] instanceof File && o[1] instanceof File) {
				// o[0] == file containing blacklist
				// o[1] == file containing S,P,O triples where O is the literal that S will be
				// linked to
				// Returned map is of the sort Map<O, Set<S>>
				mpe.populateBlacklist((File) (o[0]));
				ret = InputProcessor.processCollection(mpe.addPossibilities((File) (o[1])));
			} else if (o.length == 1 && o[0] instanceof File) {
				// Takes the blacklist from the default location
				mpe.populateBlacklist(new File(FilePaths.FILE_MENTIONS_BLACKLIST.getPath(KG)));
				ret = InputProcessor.processCollection(mpe.addPossibilities((File) (o[0])));
			}
		}

		return ret;
	}

	@Override
	public boolean destroy() {
		this.mpe = null;
		return false;
	}

	@Override
	public String getExecMethod() {
		return null;
	}
}
