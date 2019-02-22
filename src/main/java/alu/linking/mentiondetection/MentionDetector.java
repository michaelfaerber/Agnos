package alu.linking.mentiondetection;

import java.util.List;

public interface MentionDetector {
	public void init() throws Exception;

	public List<Mention> detect(final String input);

	public List<Mention> detect(String input, String source);

}
