package alu.linking.api.debug;

public class TestMarking {
	final String uri;
	final String mention;

	TestMarking(final String mention, final String uri) {
		this.uri = uri;
		this.mention = mention;
	}

	@Override
	public boolean equals(Object obj) {
		boolean ret = false;
		if (obj instanceof TestMarking) {
			ret = true;
			final TestMarking otherObj = ((TestMarking) obj);
			ret &= this.uri.equals(otherObj.uri);
			ret &= this.mention.equals(otherObj.mention);
		}
		// return super.equals(obj);
		return ret;
	}

	@Override
	public int hashCode() {
		final int superHash = super.hashCode();
		return (this.uri.hashCode() + this.mention.hashCode()) * 89 + superHash;
	}
}
