package de.dwslab.petar.walks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

public abstract class WalkResultProcessor {
	protected final String prefixSubject = "s";
	protected final String prefixPredicate = "p";
	protected final String prefixObject = "o";
	private long entityAmt = -1;
	public long startTime = System.currentTimeMillis();

	public static final String newline = System.getProperty("line.separator");

	public long processedEntities = 0;
	public long processedWalks = 0;
	public long fileProcessedLines = 0;
	protected int depth = -1;

	public abstract void processResultLines(final ResultSet results, final String entity, final BufferedWriter wrt,
			final boolean lineByLineOutput);

	public abstract StringBuilder processColumns(final StringBuilder walk, final QuerySolution solution);

	/**
	 * Adds new walks to the list; If the list is filled it is written to the file
	 * 
	 * @param str           Input string
	 * @param wrt           Where to output the string to
	 * @param stringProcess whether to process the string or not (aka. remove
	 *                      newline chars etc.). In short: set to TRUE if input is
	 *                      supposed to be a single line; FALSE if it is multiple
	 *                      lines
	 */
	public synchronized void writeToFile(String str, final BufferedWriter wrt, final boolean stringProcess) {
		processedWalks += 1;
		fileProcessedLines += 1;
		try {
			if (stringProcess) {
				str = str.replace(newline, " ").replace("\n", " ").replace("\r", " ").replace("  ", " ");
			}
			wrt.write(str);
			wrt.write(newline);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (processedWalks % 1_000_000 == 0) {
			System.out.println("TOTAL NUMBER OF PATHS(" + this.depth + " =?= "
					+ ((this.processedEntities / this.entityAmt) + 1) + " ; "
					+ (this.processedEntities % this.entityAmt) + " / " + this.entityAmt + ") : " + processedWalks);
			System.out.println("TOTAL TIME:" + ((System.currentTimeMillis() - startTime) / 1000));
		}
		// flush the file
		if (fileProcessedLines > 100_000) {
			fileProcessedLines = 0;
			try {
				wrt.flush();
				// writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Adds new walks to the list; If the list is filled it is written to the file
	 * 
	 * @param tmpList
	 */
	public synchronized void writeToFile(List<String> tmpList, final BufferedWriter wrt) {
		processedEntities++;
		processedWalks += tmpList.size();
		fileProcessedLines += tmpList.size();
		for (String str : tmpList)
			try {
				wrt.write(str + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		if (processedEntities % 1_000 == 0) {
			System.out.println("TOTAL PROCESSED ENTITIES: " + processedEntities);
			System.out.println("TOTAL NUMBER OF PATHS : " + processedWalks);
			System.out.println("TOTAL TIME:" + ((System.currentTimeMillis() - startTime) / 1000));
		}
		// flush the file
		if (fileProcessedLines > 3_000_000) {
			fileProcessedLines = 0;
			try {
				wrt.flush();
				// writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			int tmpNM = (processedWalks / 3000000);
//			String tmpFilename = fileName.replace(".txt", tmpNM + ".txt");
//			try {
//				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFilename, false), "utf-8"),
//						32 * 1024);
//			} catch (UnsupportedEncodingException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
	}

	public WalkResultProcessor updateEntityAmt(long entityAmt) {
		this.entityAmt = entityAmt;
		return this;
	}

	public long getEntityAmt() {
		return this.entityAmt;
	}

	/**
	 * One can update the depth (purely for logging/display purposes)
	 */
	public WalkResultProcessor updateDepth(int currDepth) {
		this.depth = currDepth;
		return this;
	}

	public synchronized WalkResultProcessor resetProcessedWalks() {
		this.processedWalks = 0;
		return this;
	}

}
