package alu.linking.executable.preprocessing.nounphrases;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.ext.com.google.common.collect.Lists;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.structure.Executable;
import edu.stanford.nlp.ling.TaggedWord;
import npcomplete.POSBasedBaseNounPhraseExtractor;

public enum NPExtractionManager implements Executable {
	INSTANCE(EnumModelType.DEFAULT);// enum-ensured singleton
	private final Set<String> wantedTags = new HashSet<>();
	private final EnumModelType KG;
	private final POSBasedBaseNounPhraseExtractor extractor;

	private NPExtractionManager(final EnumModelType KG) {
		this.KG = KG;
		this.extractor = new POSBasedBaseNounPhraseExtractor(FilePaths.FILE_NPCOMPLETE_ENGLISH_TAGGER.getPath(KG)); // ("german-hgc.tagger");
		init();
	}

	public List<TaggedWord> findTags(final String inputText, final Set<String> wantedTags) {
		final List<TaggedWord> retList = Lists.newArrayList();
		final List<List<TaggedWord>> tws = extractor.justPOSTaggingOfText(inputText);
		for (List<TaggedWord> ctw : tws) {
			for (TaggedWord currentTaggedWord : ctw) {
				if (wantedTags.contains(currentTaggedWord.tag())) {
					retList.add(currentTaggedWord);
				}
			}
		}
		return retList;
	}

	@Override
	public void init() {
		wantedTags.clear();
		wantedTags.add(Strings.NPCOMPLETE_TAG_NN.val);
		wantedTags.add(Strings.NPCOMPLETE_TAG_NNS.val);
	}

	@Override
	public boolean reset() {
		wantedTags.clear();
		return false;
	}

	@Override
	public List<TaggedWord> exec(Object... o) {
		if (o != null && o.length == 1 && (o[0] instanceof String)) {
			final String inputText = ((String) o[0]);
			return findTags(inputText, wantedTags);
		}
		throw new UncheckedIOException(new IOException("Invalid input arguments"));
	}

	@Override
	public boolean destroy() {
		wantedTags.clear();
		return false;
	}

	@Override
	public String getExecMethod() {
		return "findTags";
	}
}
