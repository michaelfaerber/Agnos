package npcomplete;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;



public class FileUtility {
	/**
	 * 
	 * @param file
	 *            Path to file
	 * @return a set of all stripped lines of the file except comments (#) or
	 *         empty lines
	 * @throws IOException
	 */
	public static Set<String> getSetFromFile(String file) throws IOException {
		Set<String> output = new HashSet<String>();
		InputStream fis;
		BufferedReader br;
		String line;

		fis = new FileInputStream(file);
		br = new BufferedReader(new InputStreamReader(fis,
				Charset.forName("UTF-8")));
		String tmp;
		while ((line = br.readLine()) != null) {
			tmp = line.trim();
			if (!tmp.startsWith("#") && !tmp.isEmpty()) {
				output.add(tmp);
			}
		}
		br.close();
		br = null;
		fis = null;
		return output;
	}

	
	public static void createDir(String path) throws IOException {
		File dir = new File(path);
		if(!dir.exists()) {
			dir.mkdir();
		}
	}
	
	
	 //append
	  public static void writeToFile(String txt) throws IOException { 
		    PrintWriter out = null;
		    BufferedWriter bufWriter;

		    try{
		        bufWriter =
		            Files.newBufferedWriter(
		                Paths.get("res/debug.txt"),
		                Charset.forName("UTF8"),
		                StandardOpenOption.WRITE, 
		                StandardOpenOption.APPEND,
		                StandardOpenOption.CREATE);
		        out = new PrintWriter(bufWriter, true);
		    }catch(IOException e){
		    }
		    out.println(txt);
		    out.close();
		}
	  
	  public static void writeToFile(String absolutfilepath, String txt) throws IOException { 
		    PrintWriter out = null;
		    BufferedWriter bufWriter;

		    try{
		        bufWriter =
		            Files.newBufferedWriter(
		                Paths.get(absolutfilepath),
		                Charset.forName("UTF8"),
		                StandardOpenOption.WRITE, 
		                StandardOpenOption.CREATE);
		        out = new PrintWriter(bufWriter, true);
		    }catch(IOException e){
		    }
		    out.println(txt);
		    out.close();
		}
	  
	  public static void writeToFile(String absolutfilepath, Collection<String> txt) throws IOException { 
		    PrintWriter out = null;
		    BufferedWriter bufWriter;

		    try{
		        bufWriter =
		            Files.newBufferedWriter(
		                Paths.get(absolutfilepath),
		                Charset.forName("UTF8"),
		                StandardOpenOption.WRITE, 
		                StandardOpenOption.CREATE);
		        out = new PrintWriter(bufWriter, true);
		    }catch(IOException e){
		    }
		    for (String str : txt) {
		    	out.println(str);
		    }

		    out.close();
		}
	  
	  
	  // added 20150107: store whole HashMap at once, so faster than always appending. (used e.g. 
	  // to store ngram HashMap table)
	  public static void writeToFileWholeHashMapSeparatedByTab(String absolutfilepath, HashMap map) throws IOException { 
		    PrintWriter out = null;
		    BufferedWriter bufWriter;

		    try{
		        bufWriter =
		            Files.newBufferedWriter(
		                Paths.get(absolutfilepath),
		                Charset.forName("UTF8"),
		                StandardOpenOption.WRITE, 
		                StandardOpenOption.CREATE);
		        out = new PrintWriter(bufWriter, true);
		    }catch(IOException e){
		    }
		    for (Iterator<Map.Entry> iterator = map.entrySet().iterator(); iterator.hasNext();) { // keep generic to be more flexible reg. input
				Map.Entry entry = iterator.next();
				out.println(entry.getKey() + "\t" + entry.getValue());
			}
		    out.close();
		}
	  
	  public static void appendToFileWithoutLinebreak(String absolutfilepath, String txt) throws IOException { 
		    PrintWriter out = null;
		    BufferedWriter bufWriter;

		    try{
		        bufWriter =
		            Files.newBufferedWriter(
		                Paths.get(absolutfilepath),
		                Charset.forName("UTF8"),
		                StandardOpenOption.WRITE, 
		                StandardOpenOption.APPEND,
		                StandardOpenOption.CREATE);
		        
		        out = new PrintWriter(bufWriter, true);
		    }catch(IOException e){
		    }
		    out.print(txt);
		    out.close();
		}
	  
	  public static synchronized void appendToFile(String absolutfilepath, String txt) throws IOException { 
		    PrintWriter out = null;
		    BufferedWriter bufWriter;

		    try{
		        bufWriter =
		            Files.newBufferedWriter(
		                Paths.get(absolutfilepath),
		                Charset.forName("UTF8"),
		                StandardOpenOption.WRITE, 
		                StandardOpenOption.APPEND,
		                StandardOpenOption.CREATE);
		        out = new PrintWriter(bufWriter, true);
		    }catch(IOException e){
		    }
		    out.println(txt);
		    out.close();
		}
	  
	  public static List<String> readLinesFromTextFile(String path) throws IOException {
		  List<String> listOfLines = new ArrayList<String>();
		  BufferedReader br = new BufferedReader(new FileReader(path));
		  String line;
		  while ((line = br.readLine()) != null) {
		     listOfLines.add(line);
		  }
		  br.close();
		  return listOfLines;
	  }
	  
	  public static List<String> readLinesFromTextFileWithoutSep(String path) throws IOException {
		  List<String> listOfLines = new ArrayList<String>();
		  BufferedReader br = new BufferedReader(new FileReader(path));
		  String line;
		  while ((line = br.readLine()) != null) {
			 if(!line.startsWith("===")) {
				 listOfLines.add(line);
			 }
		  }
		  br.close();
		  return listOfLines;
	  }
	  
	  public static List<Integer> readIntegerLinesFromTextFile(String path) throws IOException {
		  List<Integer> listOfLines = new ArrayList<Integer>();
		  BufferedReader br = new BufferedReader(new FileReader(path));
		  try {
			  String line;
			  while ((line = br.readLine()) != null) {
			     listOfLines.add(Integer.valueOf(line));
			  }
		  } catch(Exception e) {
			  e.printStackTrace();
		  }
		  br.close();
		  return listOfLines;
	  }
	  
	  
	  public static String readLinesWithWSFromTextFile(String path) throws IOException {
		  BufferedReader br = new BufferedReader(new FileReader(path));
		  String line;
		  String output = "";
		  while ((line = br.readLine()) != null) {
		     output = output + " " + line;
		  }
		  br.close();
		  return output.trim();
	  }
	  
	  public static String readLinesWithoutWSFromTextFile(String path) throws IOException {
		  BufferedReader br = new BufferedReader(new FileReader(path));
		  String line;
		  String output = "";
		  while ((line = br.readLine()) != null) {
		     output = output + line;
		  }
		  br.close();
		  return output.trim();
	  }
	  
	  public static String readLinesWithLineSepFromTextFile(String path) throws IOException {
		  BufferedReader br = new BufferedReader(new FileReader(path));
		  String line;
		  String output = "";
		  while ((line = br.readLine()) != null) {
		     output = output + System.lineSeparator() + line;
		  }
		  br.close();
		  return output.substring(1); // because of line sep. TODO check
	  }
	  
//	  // especially for xml: only line break in content such as text
//	  public static String readLinesWithLineSepFromTextFile(String path) throws IOException {
//		  BufferedReader br = new BufferedReader(new FileReader(path));
//		  String line;
//		  String output = "";
//		  while ((line = br.readLine()) != null) {
//		     output = output + System.lineSeparator() + line;
//		  }
//		  br.close();
//		  return output.substring(1); // because of line sep. TODO check
//	  }
	  
	  
	  public static List<String> readLinesFromTextFileTrimmed(File file) throws IOException {
		  List<String> listOfLines = new ArrayList<String>();
		  BufferedReader br = new BufferedReader(new FileReader(file));
		  String line;
		  while ((line = br.readLine()) != null) {
		     listOfLines.add(line.trim()); // trimed here.
		  }
		  br.close();
		  return listOfLines;
	  }
	  
	  public static List<String> readLinesFromTextFile(File file) throws IOException {
		  List<String> listOfLines = new ArrayList<String>();
		  BufferedReader br = new BufferedReader(new FileReader(file));
		  String line;
		  while ((line = br.readLine()) != null) {
		     listOfLines.add(line);
		  }
		  br.close();
		  return listOfLines;
	  }
	 
	  
	  public static void deleteFile() throws IOException {
		  File myFile = new File("res/debug.txt");
		  if(myFile.exists())
		      myFile.delete();
	  }
	  
	  public static void deleteFile(String absolutepath) throws IOException {
		  File myFile = new File(absolutepath);
		  if(myFile.exists())
		      myFile.delete();
	  }
	  
	  public static File[] getAllFilesInDir(File pathToDir) {
//			File folder = new File(pathToDir);
			// all files except hidden files (such as .svn):
			File[] filesAsInput = pathToDir.listFiles(new FileFilter() {
			    @Override
			    public boolean accept(File file) {
			        return (!file.isHidden() && file.isFile());
			    }
			});
			return filesAsInput;
	  }
	  
	  public static File[] getAllFilesInDirWithEnding(File pathToDir, String ending) {
//			File folder = new File(pathToDir);
			// all files except hidden files (such as .svn):
			File[] filesAsInput = pathToDir.listFiles(new FileFilter() {
			    @Override
			    public boolean accept(File file) {
			        return (!file.isHidden() && file.isFile() && file.getAbsoluteFile().toString().endsWith(ending));
			    }
			});
			return filesAsInput;
	  }
	  
	  public static File[] getAllDirsInDir(String pathToDir) {
			File folder = new File(pathToDir);
			// all "public" dirs (not .svn etc.)
			File[] filesAsInput = folder.listFiles(new FileFilter() {
			    @Override
			    public boolean accept(File file) {
			        return (!file.isHidden() && file.isDirectory());
			    }
			});
			return filesAsInput;
	  }
	  
	  
}
