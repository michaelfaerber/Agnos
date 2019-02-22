package alu.linking.executable.preprocessing.setup;

import java.io.File;
import java.util.Map;
import java.util.Set;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.loader.MentionPossibilityLoader;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.mentiondetection.fuzzy.MentionDetectorLSH;
import alu.linking.structure.Executable;

public class MentionDetectionSetup implements Executable {
	final EnumModelType KG;

	public MentionDetectionSetup(final EnumModelType KG) {
		this.KG = KG;
	}

	@Override
	public void init() {
	}

	@Override
	public boolean reset() {
		return false;
	}

	@Override
	public <T> T exec(Object... o) throws Exception {
		// Initialize & setup LSH (precompute signatures and sparse vectors)
		// Note: requires (surface form) dictionary to be ready
		// Extract the possible mentions properly from the surface form
		getLogger().debug("Setting up mention detection files");
		final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
		final Set<String> stopwords = stopwordsLoader.getStopwords();
		final MentionPossibilityLoader mpl = new MentionPossibilityLoader(KG, stopwordsLoader);
		// Map<String, Set<String>> map = mpe.exec(new
		// File(FilePaths.FILE_EXTENDED_GRAPH.path));
		Map<String, Set<String>> map = mpl.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG)));
		System.out.println("Map size:" + map.size());
		final InputProcessor inputProcessor = new InputProcessor(stopwords);
		final MentionDetectorLSH md = new MentionDetectorLSH(KG, inputProcessor);
		md.setup(map);
		getLogger().debug("Backing up computed files!");
		md.backup();
		getLogger().debug("Finished setting up mention detector stuff");
		return null;
	}

	@Override
	public boolean destroy() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getExecMethod() {
		// TODO Auto-generated method stub
		return null;
	}

}
