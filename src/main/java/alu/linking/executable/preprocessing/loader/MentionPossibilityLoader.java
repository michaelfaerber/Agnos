package alu.linking.executable.preprocessing.loader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.MentionPossibilityExtractor;
import alu.linking.mentiondetection.StopwordsLoader;
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
	private MentionPossibilityExtractor mpe = null;
	private StopwordsLoader stopwordsLoader = null;
	private InputProcessor inputProcessor = null;
	final EnumModelType KG;

	public MentionPossibilityLoader(final EnumModelType KG) {
		this(KG, new StopwordsLoader(KG));
	}

	public MentionPossibilityLoader(final EnumModelType KG, final StopwordsLoader stopwordsLoader) {
		this.KG = KG;
		this.stopwordsLoader = stopwordsLoader;
		init();
	}

	@Override
	public void init() {
		if (this.stopwordsLoader != null) {
			try {
				this.mpe = new MentionPossibilityExtractor(this.stopwordsLoader);
			} catch (IOException e) {
				getLogger()
						.error("Could not instantiate MPExtractor w/ StopwordsLoader. Instantiating with KG instead.");
				this.mpe = new MentionPossibilityExtractor(this.KG);
			}
			try {
				this.inputProcessor = new InputProcessor(this.stopwordsLoader.getStopwords());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			this.mpe = new MentionPossibilityExtractor(this.KG);
		}
	}

	@Override
	public boolean reset() {
		init();
		return true;
	}

	@Override
	public Map<String, Collection<String>> exec(Object... o) throws IOException {
		Map<String, Collection<String>> ret = null;
		if (o != null) {
			final File blacklistFile, entityLinkingFile;
			if (o.length == 2 && o[0] instanceof File && o[1] instanceof File) {
				// o[0] == file containing blacklist
				// o[1] == file containing S,P,O triples where O is the literal that S will be
				// linked to
				// Returned map is of the sort Map<O, Set<S>>
				mpe.populateBlacklist((File) (o[0]));
				ret = this.inputProcessor.processCollection(mpe.addPossibilities((File) (o[1])), this.inputProcessor);
			} else if (o.length == 1 && o[0] instanceof File) {
				// Takes the blacklist from the default location
				mpe.populateBlacklist(new File(FilePaths.FILE_MENTIONS_BLACKLIST.getPath(KG)));
				ret = this.inputProcessor.processCollection(
						mpe.addPossibilities((File) (o[0]))
						, this.inputProcessor)
				;
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
