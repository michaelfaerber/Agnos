package alu.linking.utils;

import java.util.Collection;
import java.util.Iterator;

public class IterableCollectionEntity extends IterableEntity {
	Collection<String> coll = null;
	public IterableCollectionEntity(Collection<String> coll) {
		// Pass a collection (list, set, whatever)
		this.coll = coll;
	}

	@Override
	public void close() throws Exception {
		//Nothing to do for a collection
	}

	@Override
	public Iterator<String> iterator() {
		return this.coll.iterator();
	}

}
