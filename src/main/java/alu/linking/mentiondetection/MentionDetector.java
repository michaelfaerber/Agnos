package alu.linking.mentiondetection;

import java.util.List;

public interface MentionDetector<T> {
	public void init() throws Exception;
	
	public List<Mention<T>> detect(final String input);

	public List<Mention<T>> detect(String input, String source);
}
