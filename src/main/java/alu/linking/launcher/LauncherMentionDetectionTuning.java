package alu.linking.launcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Lists;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.loader.MentionPossibilityLoader;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.fuzzy.MentionDetectorLSH;
import alu.linking.utils.Stopwatch;

public class LauncherMentionDetectionTuning {
	public static void main(String[] args) {
		try {
			final EnumModelType KG = EnumModelType.DBPEDIA_FULL;
			new LauncherMentionDetectionTuning(KG).run();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	final EnumModelType KG;

	public LauncherMentionDetectionTuning(final EnumModelType KG) {
		this.KG = KG;
	}

	public void run() throws Exception {
		final Map<String, Collection<String>> map;
		final MentionPossibilityLoader mpl = new MentionPossibilityLoader(KG);
		map = mpl.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG)));
		System.out.println("Startup completed.");
		System.out.println("Number of entries: " + map.size());
		final String source = "_:subject_source";
		// final String text = "Im August blieb ich mit meinem BMW (Tachostand 66000km)
		// in Frankreich auf einer Campingsurlaubsreise liegen. Die Reparatur zog sich
		// von Montag bis Freitag hin, Ursache war eine Einspritzdüse. Dank ADAC bekam
		// ich einen Mietwagen, 2 Übernachtungen und den Taxitransfer zum nächsten
		// Mietwagen (42km einfache Strecke!) bezahlt. Da wir auf einer Rundreise waren,
		// konnten wir zumindest einen Teil dank Mietwagen wie geplant durchführen. Nach
		// meiner Rückkehr hatte ich eigentlich vor, den Wagen zu verkaufen und ein
		// jüngeres gebrauchtes Modell des 1er BMWs zu kaufen. Ich war gerade dabei den
		// Wagen schätzen zu lassen, da begann etwas zu klappern. Meine Werkstatt
		// meinte, es sei die Steuerkette. Als ich gegen 11:45 bei BMW noch ohne mein
		// Auto eintraf, war mir bewusst, dass Mittagszeit war; ich wartete also
		// geduldig; doch als um 13:25 noch immer niemand auf mich zukam, wurde ich dann
		// doch ungeduldig und reklamierte. Daraufhin wurde ich sofort bedient. Man
		// hatte mich also offensichtlich vergessen. Zu den Problemen meines Wagens
		// befragt, erklärte man mir, dass noch eine kostenlose Überprüfung der
		// Steuerkette offen sei und diese im Schadensfall kostenlos repariert werden
		// würde. Ich fragte nach, warum man als BMW-Kunde nicht automatisch
		// angeschrieben werden würde wie vor ein paar Jahren, als BMW die Zündspule
		// auswechselte, da beim 1er Modell viele Probleme aufgetreten waren. Im
		// Internet sind diesbezüglich viele Beschwerden nachzulesen. Daraufhin ließ ich
		// mein Auto zu BMW in Stuttgart-Vaihingen bringen, wo man feststellte, dass es
		// nicht die Steuerkette war. Die Ursachensuche des Geräuschs wurde mit Kosten
		// von 300€ veranschlagt. Schließlich wurde mir mitgeteilt, dass im Motor etwas
		// gebrochen sei und das Auto einen neuen Motor benötigen würde. Die gute
		// Nachricht für mich war, dass BMW diese Reparatur auf Kulanz übernahm.
		// Allerdings zog sich das ganze Prozedere auf 2 1/2 Wochen hin, in denen ich
		// ohne Auto war und anderweitig jeden Tag nach Echterdingen, wo ich arbeite,
		// kommen musste. Ich habe inzwischen beschlossen, den Wagen erst mal nicht zu
		// verkaufen und selbst weiter zu fahren; hoffe jedoch, dass in nächster Zeit
		// keine weiteren bösen Überraschungen auf mich zu kommen. Was mich weiter
		// überraschte, war der sehr niedrige Schätzwert meines alten Wagens von 6600€.
		// Demnach hätte ich in 5 Jahren einen Werteverlust von 10000€ gehabt. Für ein
		// deutsches Auto empfinde ich das als sehr hoch. Ob ich nun BMW an Freunde
		// weiterempfehlen würde, hängt auch davon ab, wie sich mein jetziges Modell
		// bewährt. Einen sehr positiven Eindruck hat auf alle Fälle die Kulanzhaltung
		// von BMW beim Austausch des Motors bei mir hinterlassen.";
		final String text = "Hindus throng to Ganges for bathing festival  Pilgrims believe dip in river during six week celebration will cleanse sin    ALLAHABAD, India - Nearly half a million Hindus braved near-freezing temperatures to wash away their sins in the icy waters of the Ganges river in northern India on Wednesday, the first day of a six-week festival.    As many as 70 million people from India and abroad are expected over the whole “Ardh Kumbh Mela” or Half Pitcher Festival, billed as one of the largest gatherings on earth.    Men, women, children, holy men in saffron and the infirm gathered at the confluence of the Ganges, the Yamuna and a mythical third river in Allahabad city well before dawn, waiting for the sun to rise for the auspicious bath on the first day of the 42-day event.    They chanted verses from Hindu scriptures and sang holy songs as they walked towards the bathing areas, some lying prostrate after every few steps to salute the gods.    The festival falls midway between the “Maha Kumbh Mela” or the Great Pitcher Festival, celebrated once every 12 years.    Hindus believe that bathing in the Ganges during the festivals cleanses them of sin, speeding the way to the attainment of nirvana or the afterlife.    'Like being with God'    After dipping in the polluted but sacred waters, many filled cans, bottles and steel containers for relatives and friends who could not make it. Others sprinkled it on their dry clothes.     “It was a long-cherished desire to take a dip here during the Kumbh Mela,” said Naba Kumar Ghosh, a young school teacher from the eastern Indian state of West Bengal. “The experience has been one of fulfillment, a complete cleansing of the inner self.”    Shakuntala, a 70-year-old woman who gave only one name, said she traveled all night from the central Indian state of Madhya Pradesh to bathe in the Ganges, just as she has done at every Kumbh Mela over the last 25 years.    “It was a divine experience, a dip in the holy waters is like being with God,” she said. “God willing, I will be here for the next 'Kumbh' too”.    Rama Devi, an old woman from Allahabad who could not remember her age, has not missed a single Kumbh and was determined to make it this time despite her inability to walk.    With roads closed to traffic, her 35-year-old son, a soldier in the Indian army, carried her on his back for the 6-mile walk from their house to the waters.    Tight security  Allahabad, in the Hindi heartland state of Uttar Pradesh, is one of four spots where Garuda, the winged steed of Hindu god Vishnu, is said to have rested during a titanic battle with demons over a pitcher of divine nectar of immortality.    Garuda’s flight lasted 12 divine days, or 12 years of mortal time, hence the celebration of “Maha Kumbh Mela” every 12 years.    The midway point between two such celebrations is also considered highly favorable because the position of the sun and the moon are the same as during the “Maha Kumbh”.    The “Maha Kumbh Mela” in 1989 attracted 15 million pilgrims and the Guinness Book of Records dubbed it the largest gathering of human beings for a single purpose. The festival in 2001 drew between 50 and 70 million.    Thousands of tents and camps have been built to house pilgrims across the 4,000 acre  festival area and more than 10,000 policemen, including specially-trained “terrorist spotters”, have been deployed, authorities said.";
		Stopwatch.start(getClass().getName());
		final double threshold = 0.8;
		int[] bandsArr = new int[] { 10, 100, 500, 1000, 2000 };
		int[] bucketsArr = new int[] { // 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1200,
				1500, 2000, 2500, 5000, 10_000 //
		};
		long mDuration = Long.MAX_VALUE;
		Integer mCollisions = Integer.MAX_VALUE;
		int[] minDurationArray = null;
		int[] minCollisionArray = null;
		final List<Long> durations = Lists.newArrayList();
		try (final BufferedWriter bwTuning = new BufferedWriter(new FileWriter("./tuning.txt", true))) {
			bwTuning.newLine();
			bwTuning.write("#New launch!");
			bwTuning.newLine();
			final InputProcessor inputProcessor = new InputProcessor(null);
			for (int j = 0; j < bandsArr.length; ++j) {
				final int bands = bandsArr[j];
				for (int i = 0; i < bucketsArr.length; ++i) {
					final int buckets = bucketsArr[i];
					final MentionDetectorLSH md = new MentionDetectorLSH(KG, threshold, bands, buckets, inputProcessor);
					md.setup(map);
					md.backup();
					Stopwatch.start(getClass().getName());
					final List<Mention> mentions = md.detect(text, source);
					final long duration = Stopwatch.endDiffStart(getClass().getName());
					durations.add(duration);
					final int collisions = md.getCollisionCounter().get();
					final String outString = "Collisions(" + md.getCollisionCounter().get() + ") Bands(" + bands
							+ "), Buckets(" + buckets + "), Threshold(" + threshold + "): " + duration;
					System.out.println(outString);
					bwTuning.write(outString);
					bwTuning.newLine();
					if (duration < mDuration) {
						minDurationArray = new int[] { (int) duration, bands, buckets, collisions };
						mDuration = duration;
					}
					if (collisions < mCollisions) {
						minCollisionArray = new int[] { (int) duration, bands, buckets, collisions };
						mCollisions = collisions;
					}
					int minDurationDuration = minDurationArray[0];
					int minDurationBands = minDurationArray[1];
					int minDurationBuckets = minDurationArray[2];
					int minDurationCollisions = minDurationArray[3];

					int minCollisionDuration = minCollisionArray[0];
					int minCollisionBands = minCollisionArray[1];
					int minCollisionBuckets = minCollisionArray[2];
					int minCollisionCollisions = minCollisionArray[3];

					System.out.println("[CURRENT BEST - DURATION] Duration(" + minDurationDuration + "ms), Collisions("
							+ minDurationCollisions + "), Buckets(" + minDurationBuckets + "), Bands("
							+ minDurationBands + ")");
					System.out.println("[CURRENT BEST - COLLISION] Duration(" + minCollisionDuration + "ms), Collision("
							+ minCollisionCollisions + "), Buckets(" + minCollisionBuckets + "), Bands("
							+ minCollisionBands + ")");
				}
			}
			// Each mention is linked to a single (top-scored) assignment
			System.out.println("All durations: " + durations);
			int minDurationDuration = minDurationArray[0];
			int minDurationBands = minDurationArray[1];
			int minDurationBuckets = minDurationArray[2];
			int minDurationCollisions = minDurationArray[3];

			final String bestDurationString = "Best bands(" + minDurationBands + ") and buckets(" + minDurationBuckets
					+ ") w/ Duration(" + minDurationDuration + ") and Collisions(" + minDurationCollisions + ")";
			int minCollisionDuration = minDurationArray[0];
			int minCollisionBands = minDurationArray[1];
			int minCollisionBuckets = minDurationArray[2];
			int minCollisionCollisions = minDurationArray[3];

			final String bestCollisionString = "Best bands(" + minCollisionBands + ") and buckets("
					+ minCollisionBuckets + ") w/ Duration(" + minCollisionDuration + ") and Collisions("
					+ minCollisionCollisions + ")";
			bwTuning.write("## RESULTS ##");
			bwTuning.newLine();
			bwTuning.write(bestDurationString);
			bwTuning.newLine();
			bwTuning.write(bestCollisionString);
			bwTuning.newLine();
		}
	}
}
