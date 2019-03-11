package alu.linking.config.constants;

import java.util.Comparator;

import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.Span;
import org.apache.commons.lang3.tuple.Pair;

import alu.linking.mentiondetection.Mention;

public class Comparators {
	public static final Comparator<Mention> mentionOffsetComp = new Comparator<Mention>() {
		@Override
		public int compare(Mention o1, Mention o2) {
			// Made so it accepts the smallest match as the used one
			final int diffLength = (o1.getOriginalMention().length() - o2.getOriginalMention().length());
			return (o1.getOffset() == o2.getOffset()) ? diffLength : ((o1.getOffset() > o2.getOffset()) ? 1 : -1);
		}
	};
	
	public static final Comparator<Marking> markingsOffsetComp = new Comparator<Marking>() {

		@Override
		public int compare(Marking o1, Marking o2) {
			Span spanLeft = null, spanRight = null;
			if (o1 instanceof Span) {
				spanLeft = (Span) o1;
			}

			if (o2 instanceof Span) {
				spanRight = (Span) o2;
			}

			if (spanLeft == null || spanRight == null) {
				return 0;
			}

			return spanLeft.getStartPosition() - spanRight.getStartPosition();
		}
	};
	
	public static final Comparator<Pair<? extends Comparable, ? extends Comparable>> pairRightComparator = new Comparator<Pair<? extends Comparable, ? extends Comparable>>() {

		@Override
		public int compare(Pair<? extends Comparable, ? extends Comparable> o1,
				Pair<? extends Comparable, ? extends Comparable> o2) {
			return o1.getRight().compareTo(o2.getRight());
		}

	};
	public static final Comparator<Pair<? extends Comparable, ? extends Comparable>> pairLeftComparator = new Comparator<Pair<? extends Comparable, ? extends Comparable>>() {

		@Override
		public int compare(Pair<? extends Comparable, ? extends Comparable> o1,
				Pair<? extends Comparable, ? extends Comparable> o2) {
			return o1.getLeft().compareTo(o2.getLeft());
		}

	};
	
	public static final Comparator<Mention> mentionOffsetComparator = new Comparator<Mention>() {
		@Override
		public int compare(Mention o1, Mention o2) {
			return o1.getOffset() - o2.getOffset();
		}
	};


}
