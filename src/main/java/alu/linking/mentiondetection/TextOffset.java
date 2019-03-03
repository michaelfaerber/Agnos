package alu.linking.mentiondetection;

public class TextOffset {
	public int offset = 0;
	public String text = null;

	public TextOffset text(final String text) {
		this.text = text;
		return this;
	}

	public TextOffset offset(final int offset) {
		this.offset = offset;
		return this;
	}
}
