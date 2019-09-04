package npcomplete;
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.


import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Laurenz Vorderwuelbecke on 05.08.16.
 * @author Laurenz Vorderwuelbecke
 */
public class POSBasedBaseNounPhraseExtractor {

    /**
     * This class represents the rejection rules conveniently
     */
    private class RejectionRule {
        private String key;
        private String operation;
        ArrayList<String> rules;

        public RejectionRule(String key, String operation) {
            this.key = key;
            this.operation = operation;
            this.rules = new ArrayList<String>();
        }

        public void addRule(String newRule) {
            rules.add(newRule);
        }

        public ArrayList<String> getRules() {
            return rules;
        }

        public String getKey() {
            return key;
        }

        public String getOperation() {
            return operation;
        }
    }


    private MaxentTagger POSTagger;
    List<List<TaggedWord>> taggedSentences;
    HashMap<BaseNounPhrase, List<TaggedWord>> dictionaryWithTaggedSentenceForBaseNP;
    ArrayList<BaseNounPhrase> extractedBaseNounPhrases;


    public POSBasedBaseNounPhraseExtractor(String pathToStanfordModel) {
        Properties props = new Properties();
        props.put("tokenize.options", "untokenizable=allKeep,normalizeParentheses=false"); // or noneKeep
        props.put("encoding", "utf-8");
        props.put("strictTreebank3", "true");

        this.POSTagger = new MaxentTagger(pathToStanfordModel, props);
    }


    /**
     * Preprocess data
     */
    private List<List<HasWord>> processString(String text) {
        List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new StringReader(text));
        return sentences;
    }

    /**
     * Tag data with POS Tags
     */
    public List<List<TaggedWord>> tagWithPOSTags(List<List<HasWord>> sentences) {
        List<List<TaggedWord>> taggedSentences = new ArrayList<List<TaggedWord>>();
        for (List<HasWord> sentence : sentences) {
            List<TaggedWord> taggedSentence = POSTagger.tagSentence(sentence);
            taggedSentences.add(taggedSentence);
        }
        return taggedSentences;
    }

    /**
     * Apply positive rules using REGEX
     */
    private ArrayList<BaseNounPhrase> applyPositiveRules(List<List<TaggedWord>> taggedSentences) {

        try {
            final ArrayList<BaseNounPhrase> extractedNounPhrases = new ArrayList<BaseNounPhrase>();

//            System.out.println("Use rules for ***GERMAN***.");
            String rulesFilePath =  "positiveRules.txt"; // "positiveRules_DE.txt";
            final String rulesRegEx = getRegExString(rulesFilePath);


            ExecutorService es = Executors.newFixedThreadPool(8); //newCachedThreadPool
            for (final List<TaggedWord> taggedSentence : taggedSentences) {


                Thread thread = new Thread() {
                    public void run() {


                        TaggedWord firstWord = taggedSentence.get(0);
                        int startOfTaggedSentence = firstWord.beginPosition() - 1;

                        int initialStartOffset = startOfTaggedSentence;

                        Pattern baseNPPositiveRulesPattern = Pattern.compile(rulesRegEx);
                        Matcher baseNPMatcher = baseNPPositiveRulesPattern.matcher("");

                        Pattern POSTagPattern = Pattern.compile("(?<!(?:/|\\)))/([A-Z,$,#,€]{1,4})");
                        Matcher POSTagMatcher = POSTagPattern.matcher("");

                        String sentence = Sentence.listToString(taggedSentence, false);
                        baseNPMatcher.reset(sentence);


                        while (baseNPMatcher.find()) {

                            String baseNPString = baseNPMatcher.group(0);

                            String POSTag = "";
                            POSTagMatcher.reset(baseNPString); //So Matcher does not have to be reinitialized every time

                            while (POSTagMatcher.find()) {
                                POSTag = POSTagMatcher.group(1);  //POS Tag of last token
                            }

                            String cleanBaseNPString = baseNPString.replaceAll("(?<!(?:\\/|\\\\))\\/([A-Z,$,#,€]{1,4})", "").trim();

                            if (!cleanBaseNPString.equals("")) {


                                BaseNounPhrase baseNP = createBaseNounPhrase(cleanBaseNPString, baseNPString, taggedSentence, POSTag, initialStartOffset);
                                //System.out.println("Tagged Sentence: " + taggedSentence + " with initialOffset: " + initialStartOffset + " produced: " + baseNP);
                                initialStartOffset = baseNP.getEndOffset();
                                dictionaryWithTaggedSentenceForBaseNP.put(baseNP, taggedSentence);
                                synchronized (extractedNounPhrases) {
                                    Collections.synchronizedCollection(extractedNounPhrases).add(baseNP);
                                }
                            }
                        }

                    }
                };
                //thread.start();
                es.execute(thread);

            }
            es.shutdown();
            boolean finshed = es.awaitTermination(1, TimeUnit.MINUTES);

            return extractedNounPhrases;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Apply rejection rules
     */
    private ArrayList<BaseNounPhrase> applyRejectionRules(ArrayList<BaseNounPhrase> baseNounPhrases) {

        try {

            ArrayList<RejectionRule> rules = getRejectionRules("rejectionRules.txt");

            for (RejectionRule rule : rules) {

                String delimiter = rule.getKey(); //The String to seperate the phrase on
                String operation = rule.getOperation();

                 //So every pass can start from the beginning again

                for (int i = 0; i < baseNounPhrases.size(); i++) {

                    BaseNounPhrase baseNP = baseNounPhrases.get(i);
                    String phrase = baseNP.getPhraseStringWithPOSTags();

                    int initialStartOffset = baseNP.getStartOffset()-1;

                    List<TaggedWord> taggedSentence = dictionaryWithTaggedSentenceForBaseNP.get(baseNP);

                    if (taggedSentence != null) {
                        String taggedSentenceString = Sentence.listToString(taggedSentence, false);



                        if (phrase.contains(delimiter)) {

                            ArrayList<String> checkStrings = rule.getRules();

                            boolean oneRejectionRuleMatched = false;

                            for (String checkString : checkStrings) {

                                String[] parts = phrase.split(delimiter);
                                String partone = parts[0];
                                String parttwo = parts[1];

                                checkString = checkString.replace(";@phrase@;", phrase);
                                checkString = checkString.replace(";@phrasepartone@;", partone);
                                checkString = checkString.replace(";@phraseparttwo@;", parttwo);
                                checkString = checkString.replace("$", "\\$");

                                Pattern baseNPRejectionRulesPattern = Pattern.compile(checkString);
                                Matcher baseNPMatcher = baseNPRejectionRulesPattern.matcher(taggedSentenceString);

                                if (baseNPMatcher.find()) {

                                    oneRejectionRuleMatched = true;

                                    dictionaryWithTaggedSentenceForBaseNP.remove(baseNP);
                                    baseNounPhrases.remove(i);

                                    for (int j = 0; j < parts.length; j++) {

                                        String subString = parts[j];

                                        switch (operation) {
                                            case "keepright":
                                                if (j == parts.length - 1) {
                                                    subString = delimiter + subString;
                                                }
                                                break;
                                            case "keepleft":
                                                if (j == 0) {
                                                    subString = subString + delimiter;
                                                }
                                                break;
                                            default:
                                        }

                                        String cleanSubstring = subString.replaceAll("(?<!/)/[A-Z,$,#,€]{1,4}", "").trim();

                                        BaseNounPhrase newBaseNP = createBaseNounPhrase(cleanSubstring, subString, taggedSentence, "", initialStartOffset);
                                        baseNounPhrases.add(i + j, newBaseNP);
                                        initialStartOffset = newBaseNP.getEndOffset();

                                    }

                                    baseNP = null;
                                }
                                if (oneRejectionRuleMatched) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baseNounPhrases;
    }
    
    
    /** added 2016-12-12:
     * assumes that the POS tagger model is already set.
     * 
     * @param text
     */
    public List<List<TaggedWord>> justPOSTaggingOfText(String text) {
    	List<List<HasWord>> sentences = processString(text);
        System.out.println("Finished Processing the text");

        System.out.println("Starting tagging");
        taggedSentences = tagWithPOSTags(sentences);
        System.out.println("Finished tagging the text");
        
        return taggedSentences;
    }

    /**
     * Extracts all base noun phrases from a given text.
     * The text can contain multiple sentences.
     * Results are saved internally and are available for output or saving
     *
     * @param text The input text
     */
    public void extractBaseNounPhrasesFromText(String text) {

        dictionaryWithTaggedSentenceForBaseNP = new HashMap<BaseNounPhrase, List<TaggedWord>>();

        List sentences = processString(text);
        System.out.println("Finished Processing the text");

        System.out.println("Starting tagging");
        taggedSentences = tagWithPOSTags(sentences);
        System.out.println("Finished tagging the text");

        System.out.println("Starting application of positive rules");
        //sentences = null;
        extractedBaseNounPhrases = applyPositiveRules(taggedSentences);
        System.out.println("Finished application of positive rules");


        System.out.println("Sorting the extracted phrases");
        sortExtractedPhrases();


        System.out.println("Starting application of rejection rules");
        extractedBaseNounPhrases = applyRejectionRules(extractedBaseNounPhrases);
        System.out.println("Finished application of rejection rules");

    }

    /**
     * Extracts all base noun phrases from a given file in the CoNLL data format.
     * Results are saved internally and are available for output or saving
     * The tokens have to be in the first column
     * Columns have to be either seperated by a whitespace or a tab
     *
     * @param path absolute path to the CoNLL File
     * @throws IOException
     */
    public void extractBaseNounPhrasesFromCoNLLData(String path) throws IOException {

        dictionaryWithTaggedSentenceForBaseNP = new HashMap<BaseNounPhrase, List<TaggedWord>>();

        List<List<HasWord>> sentences = new ArrayList();
        List<HasWord> sentence = new ArrayList<HasWord>();

        BufferedReader br = new BufferedReader(new FileReader(path));
        String currentLine;
        int currentStartPosition = 0;


        while (null != (currentLine = br.readLine())) {

            if (!currentLine.equals("") && !currentLine.contains("\t\t")) {

                String[] argumentsInLine = currentLine.split(" ");

                if (argumentsInLine.length <= 2) {
                    argumentsInLine = currentLine.split("\t");
                }

                String cleanToken = argumentsInLine[0]/*.replace("\\/", "//")*/;

                int currentEndPosition = currentStartPosition + cleanToken.length() - 1;

                sentence.add(new Word(cleanToken, currentStartPosition, currentEndPosition));

                currentStartPosition = currentEndPosition + 2;

            } else if (currentLine.equals("") || currentLine.equals("\t\t")) {
                sentences.add(sentence);
                sentence = new ArrayList<HasWord>();
            } else {
                System.out.println("Strange Line occured: " + currentLine);
            }
        }
        if (sentence.size() >= 0) {
            sentences.add(sentence); //saves last Sentence, when no empty line follows it
        }

        System.out.println("Finished Processing the text");

        System.out.println("Starting tagging");
        taggedSentences = tagWithPOSTags(sentences);
        System.out.println("Finished tagging the text");


        System.out.println("Starting application of positive rules");
        extractedBaseNounPhrases = applyPositiveRules(taggedSentences);
        System.out.println("Finished application of positive rules");


        System.out.println("Sorting extracted phrases");
        sortExtractedPhrases();



        System.out.println("Starting application of rejection rules");
        extractedBaseNounPhrases = applyRejectionRules(extractedBaseNounPhrases);
        System.out.println("Finished application of rejection rules");



    }

    private void sortExtractedPhrases() {
        Collections.sort(extractedBaseNounPhrases, new Comparator<BaseNounPhrase>() {
            @Override
            public int compare(BaseNounPhrase bnp1, BaseNounPhrase bnp2) {
                if (bnp1 == null) {
                    return -1;
                }
                if (bnp2 == null) {
                    return 1;
                }
                if (bnp1.getStartOffset() > bnp2.getStartOffset()) {
                    return 1;
                } else if (bnp1.getStartOffset() == bnp2.getStartOffset()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
    }

    /**
     * Returns the previously extracted base noun phrases as a List of BaseNounPhrase Objects
     *
     * @return ArrayList of BaseNounPhrase Objects
     */
    public ArrayList<BaseNounPhrase> getBaseNounPhrases() {
        return extractedBaseNounPhrases;
    }

    /**
     * Writes the previously extracted base noun phrases to the given absolute path in the CoNLL Format
     * 1. Column are the tokens
     * 2. Column are the created POS Tags
     * 3. Column are the chunk tags in the IOB2 format, only with baseNP information
     *
     * @param pathToWrite absolutePath
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public void writeBaseNounPhrasesAsCoNLLFile(String pathToWrite) throws FileNotFoundException, UnsupportedEncodingException {

        PrintWriter writer = new PrintWriter(pathToWrite, "UTF-8");

        int indexNPs = 0;
        int indexInNP = 0;

        int sizeOfExtractedNPsArray = extractedBaseNounPhrases.size();

        sentenceLoop:
        for (int indexOfSentences = 0; indexOfSentences < taggedSentences.size(); indexOfSentences++) {

            List<TaggedWord> sentence = taggedSentences.get(indexOfSentences);

            for (int i = 0; i < sentence.size(); i++) {

                if (i == 0 && indexOfSentences != 0) {
                    writer.println(""); //Create space bewteen two sentences
                }
                TaggedWord word = sentence.get(i);

                String token = word.word();
                String tag = word.tag();
                String assignedChunkTag = "";
                int startOffset = word.beginPosition();


                if (indexNPs < sizeOfExtractedNPsArray) {
                    BaseNounPhrase currentNP = extractedBaseNounPhrases.get(indexNPs);
                    String[] words = currentNP.getPhraseString().split(" ");
                    String currentWord = words[indexInNP];
                    int startOfCurrentWord = currentNP.getStartOffset();

                    for (int j = 1; j <= indexInNP; j++) {
                        startOfCurrentWord = startOfCurrentWord + words[j-1].length() + 1;
                    }

                    if (startOffset == startOfCurrentWord) {
                        if (indexInNP == 0) {
                            assignedChunkTag = "B";
                        } else {
                            assignedChunkTag = "I";
                        }
                        if (indexInNP + 1 < words.length) {
                            indexInNP++;
                        } else {
                            indexNPs++;
                            indexInNP = 0;
                        }
                    } else {
                        assignedChunkTag = "O";
                    }

                    /*if (startOffset > currentNP.getEndOffset()) {
                        indexNPs++;
                    }
                    */
                    String line = token + "\t" + tag + "\t" + assignedChunkTag;
                    writer.println(line);
                } else break sentenceLoop;
            }
        }

        writer.close();

    }


    /**
     * Creates BaseNounPhrase Object by finding the start and end offset and the head
     *
     * @param baseNP                  the string of the extracted baseNP
     * @param phraseStringWithPOSTags the string of the extracted baseNP with each POS appended to each token using /
     * @param taggedSentence          the sentence the phrase was extracted from
     * @param POSTag                  the POS Tag of the last token
     * @return BaseNounPhrase Object with its offsets
     */
    public BaseNounPhrase createBaseNounPhrase(String baseNP, String phraseStringWithPOSTags, List<TaggedWord> taggedSentence, String POSTag, int initialStartOffset) {

        int startOffset = -1;
        int endOffset = -1;

        String wordsInBaseNP[] = baseNP.split(" ");
        String firstWord = wordsInBaseNP[0];

        int baseNPLength = baseNP.length();


        for (int indexOTaggedWords = 0; indexOTaggedWords < taggedSentence.size() ; indexOTaggedWords++) {

            TaggedWord currentWord = taggedSentence.get(indexOTaggedWords);

            String cleanCurrentWord = currentWord.word()/*.replace("\\/", "//")*/;

            if (cleanCurrentWord.equals(firstWord)) { //Checks if word is the same as the first word of the baseNP
                int extra = 0;
                if (cleanCurrentWord.equals(firstWord + ".")) {
                    extra = 1;
                } else if (cleanCurrentWord.equals("'" + firstWord)) {
                    extra = 1;
                }
                if ((currentWord.beginPosition() > initialStartOffset) && startOffset < 0) { //Only sets startOffset if the word is after the beginning of the last baseNP and the startOffset has not been set yet
                    boolean correctStart = true;
                    for (int indexInBaseNP = 0; indexInBaseNP < wordsInBaseNP.length;indexInBaseNP++) {
                        String partWord = wordsInBaseNP[indexInBaseNP];
                        String compareWord = taggedSentence.get(indexOTaggedWords+indexInBaseNP).word();
                        if (!partWord.equals(compareWord)) {
                            correctStart = false;
                        }
                    }
                    if (correctStart) {
                        startOffset = currentWord.beginPosition();
                        endOffset = startOffset + baseNPLength + extra - 1;
                        break;
                    }
                }
            }
        }

        if (endOffset == -1) {
            System.out.println("Something went wrong while finding the appropriate offsets.");
        }
        return new BaseNounPhrase(baseNP, phraseStringWithPOSTags, startOffset, endOffset, POSTag);
    }

    /**
     * Extracts the RegEx String from the rules files
     * Substitues all placeholders accordingly
     *
     * @param rulesFilePath
     * @return Complete RegEx String
     * @throws IOException
     */
    private String getRegExString(String rulesFilePath) throws IOException {

        String rulesRegEx = "(";

        BufferedReader br = new BufferedReader(new FileReader(rulesFilePath));
        String currentLine;

        ArrayList<String> rules = new ArrayList<String>();
        HashMap<String, String> dictionaryOfRules = new HashMap<String, String>();

        Pattern ruleNamePattern = Pattern.compile("([^\\s]+)(?:[\\;]{2}(?=[^@]))");
        Matcher ruleNameMatcher = ruleNamePattern.matcher("");

        /**
         * Creates dictionary with rules and their names
         */

        while (null != (currentLine = br.readLine())) { //first rules in file are checked first
            if (!currentLine.substring(0, 1).equals("#")) {

                rules.add(currentLine);

                for (int i = 0; i < rules.size(); i++) {

                    String ruleLine = rules.get(i);
                    ruleNameMatcher.reset(ruleLine);

                    if (ruleNameMatcher.find()) {

                        String ruleName = ruleNameMatcher.group(0);
                        String rule = ruleLine.replaceAll(ruleName, "");

                        dictionaryOfRules.put(ruleName.replaceAll("\\;\\;", ""), rule);

                        rules.remove(i);
                        rules.add(i, rule);

                    }
                }
            }
        }

        /**
         * Replaces rule placeholders with the actual rules
         */
        Pattern ruleReplacementPattern = Pattern.compile(";@[^\\s]+?@;");
        Matcher ruleReplacementMatcher = ruleReplacementPattern.matcher("");

        for (int i = 0; i < rules.size(); i++) {

            String rule = rules.get(i);
            ruleReplacementMatcher.reset(rule);

            while (ruleReplacementMatcher.find()) {

                String ruleNameToken = ruleReplacementMatcher.group(0);
                String ruleName = ruleNameToken.replaceAll("(@;|;@)", "");

                String ruleToInsert = dictionaryOfRules.get(ruleName);

                rule = rule.replace(ruleNameToken, ruleToInsert);
                ruleReplacementMatcher.reset(rule);

            }

            /**
             * Creates final Regex
             */
            rule = "(" + rule + ")";
            if (!rulesRegEx.equals("(")) {
                rulesRegEx = rulesRegEx + "|" + rule;
            } else {
                rulesRegEx = rulesRegEx + rule;
            }
        }

        rulesRegEx = rulesRegEx + ")";
        return rulesRegEx;
    }

    /**
     * Extracts the List of Rejection Rules from the rejection rules files
     * Substitues all placeholders accordingly
     * Organizes Rules by their key/delimiter
     *
     * @param rulesFilePath
     * @return List of RejectionRule Objects
     * @throws IOException
     */
    private ArrayList<RejectionRule> getRejectionRules(String rulesFilePath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(rulesFilePath));
        String currentLine;

        ArrayList<RejectionRule> allRules = new ArrayList<RejectionRule>();
        RejectionRule rule = null;

        while (null != (currentLine = br.readLine())) { //first rules in file are checked first
            String firstCharacter = currentLine.substring(0, 1);
            if (!firstCharacter.equals("#")) {

                if (firstCharacter.equals("∞")) {
                    String key = currentLine.substring(1, currentLine.lastIndexOf("∞"));
                    String operation = currentLine.substring(currentLine.indexOf(";") + 1, currentLine.lastIndexOf(";"));

                    rule = new RejectionRule(key, operation);
                    allRules.add(rule);
                } else if (rule != null) {
                    rule.addRule(currentLine);
                }

            }
        }
        return allRules;
    }

    private List<List<TaggedWord>> getTaggedSentences() {
        return taggedSentences;
    }
    
//    public static void main(String[] args) {
//    	   String exampleText = "This text includes some noun phrases and some other phrases. I am only interested in the phrases, which are useful to my project.";
//    	   POSBasedBaseNounPhraseExtractor extractor = new POSBasedBaseNounPhraseExtractor("./lib/german-hgc.tagger"); // ("english-left3words-distsim.tagger");
//    	   extractor.extractBaseNounPhrasesFromText(exampleText);
//    	   ArrayList<BaseNounPhrase> baseNounPhrases = extractor.getBaseNounPhrases();
//    	   System.out.println(baseNounPhrases);
//    }
    
    public static void main(String[] args) {
 	   String exampleText = "This text includes some noun phrases and some other phrases. I am only interested in the phrases, which are useful to my project.";
// 	   String exampleText = "Die Kandidatin der Grünen in den USA, Jill Stein, hat seit der Trump-Wahl in einigen US-Bundesstaaten eine Neuauszählung der Stimmen beantragt. Vorwürfe der Wahlmanipulation gab es ja von beiden Seiten, aber Trump hatte es nach seinem Sieg plötzlich nicht mehr eilig mit der Überprüfung. Und wenn Hillary Clinton das angestoßen hätte, sähe es nach einer schlechten Verliererin aus. Jill Stein kam auf eine geringe einstellige Prozentzahl der Stimmen, die kann das also machen, ohne dass das sie da was zu gewinnen hätte, und sie hat daher ein Crowdfunding-Kampagne gefahren und in Michigan, Wisconsin und Pennsylvania eine Neuauszählung beantragt.";
    	POSBasedBaseNounPhraseExtractor extractor = new POSBasedBaseNounPhraseExtractor("./lib/english-left3words-distsim.tagger"); // ("german-hgc.tagger");
 	   List<List<TaggedWord>> tws = extractor.justPOSTaggingOfText(exampleText);
 	   for(List<TaggedWord> ctw : tws) {
 		   for(TaggedWord currentTaggedWord : ctw) {
 			   System.out.println(currentTaggedWord.value() + " " + currentTaggedWord.tag());
 		   }
 		   System.out.println();
 	   }
 	  
 }
}


