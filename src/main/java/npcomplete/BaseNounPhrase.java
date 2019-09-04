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


import java.util.ArrayList;

/**
 * This class is used to store information about a found or annotated noun phrase
 * Created by Laurenz Vorderwülbecke on 02.08.16.
 * @author Laurenz Vorderwuelbecke
 */
public class BaseNounPhrase {

    private String phraseString;
    private String phraseStringWithPOSTags;

    private String posTag;

    private int startOffset;
    private int endOffset;

    private String head;


    /**
     *
     * @param phraseString The String of the phrase
     * @param phraseStringWithPOSTags The String of the phrase with the respective POS tags behind each token
     * @param startOffset Start-offset of phrase in characters, no annotation tokens, in file
     * @param endOffset End-offset of phrase in characters, no annotation tokens, in file
     * @param posTag
     */
    public BaseNounPhrase(String phraseString, String phraseStringWithPOSTags, int startOffset, int endOffset, String posTag) {
        this.phraseString = phraseString;
        this.phraseStringWithPOSTags = phraseStringWithPOSTags;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.posTag = posTag;
    }

    /**
     * Returns a string with the token, which is considered to be the head of the noun phrase
     * Rules by Collins, Michael. "Head-driven statistical models for natural language parsing." Computational linguistics 29.4 (2003)
     * @return String The extracted head
     */
    public String getHead() {
        if (head == null) {
            ArrayList<String> tokens = new ArrayList<String>();
            ArrayList<String> POSTags = new ArrayList<String>();

            String wordsWIthTagsInBaseNP[] = phraseStringWithPOSTags.split(" ");

            for (String word : wordsWIthTagsInBaseNP) {

                String token = word.replaceAll("(?<!/)/[A-Z,$,#,€]{1,4}", "").trim();
                String POSTag = word.replace(token, "").trim();
                POSTag = POSTag.replace("/", "");

                tokens.add(token);
                POSTags.add(POSTag);
            }

            if (POSTags.get(tokens.size() - 1).equals("POS")) {
                head = tokens.get(tokens.size() - 1);
                return head;
            }
            for (int i = tokens.size() - 1; i >= 0; i--) {
                String POSTag = POSTags.get(i);
                if (POSTag.equals("NN") || POSTag.equals("NNP") || POSTag.equals("NNPS") || POSTag.equals("NNS") || POSTag.equals("NX") || POSTag.equals("POS") || POSTag.equals("JJR")) {
                    head = tokens.get(i);
                    return head;
                }
            }
            for (int j = 0; j < tokens.size(); j++) {
                String POSTag = POSTags.get(j);
                if (POSTag.equals("NP")) {
                    head = tokens.get(j);
                    return head;
                } else {

                }
            }
            for (int i = tokens.size() - 1; i >= 0; i--) {
                String POSTag = POSTags.get(i);
                if (POSTag.equals("$") || POSTag.equals("ADJP") || POSTag.equals("PRN")) {
                    head = tokens.get(i);
                    return head;
                }
            }
            for (int i = tokens.size() - 1; i >= 0; i--) {
                String POSTag = POSTags.get(i);
                if (POSTag.equals("CD")) {
                    head = tokens.get(i);
                    return head;
                }
            }
            for (int i = tokens.size() - 1; i >= 0; i--) {
                String POSTag = POSTags.get(i);
                if (POSTag.equals("JJ") || POSTag.equals("JJS") || POSTag.equals("RB") || POSTag.equals("QP")) {
                    head = tokens.get(i);
                    return head;
                }
            }
            head = tokens.get(tokens.size() - 1);
            return head;
        }
        else {
            return head;
        }
    }

    /**
     * Returns the phrase string of the noun phrase
     * @return phraseString as String.
     */
    public String getPhraseString() {
        return phraseString;
    }

    /**
     * Returns the phrase string of the noun phrase
     * @return phraseString as String.
     */
    public String getPhraseStringWithPOSTags() {
        return phraseStringWithPOSTags;
    }

    /**
     * Returns the start Offset as counted in characters of the noun phrase
     * @return startOffsetInTokens in the file as int.
     */
    public int getEndOffset() {
        return endOffset;
    }

    /**
     * Returns the end Offset as counted in characters of the noun phrase
     * @return startOffsetInTokens in the file as int.
     */
    public int getStartOffset() {
        return startOffset;
    }

    /**
     * Returns the start Offset as counted in tokens of the noun phrase
     * @return startOffsetInTokens in the file as int.
     */
    public String getPosTag() {
        if (posTag != null) {
            return  posTag;
        } else {
            return "";
        }
    }

    public boolean equals(Object object) {
        if (object instanceof BaseNounPhrase) {
            BaseNounPhrase bNP = (BaseNounPhrase) object;
            if (bNP.getPhraseString().equals(this.getPhraseString())/* && bNP.getStartOffset() == this.getStartOffset() && bNP.getEndOffset() == this.getEndOffset()*/) {
                return true;
            } else {
                return false;
            }
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "BaseNounPhrase{" +
                "phraseString='" + phraseString +
                ", startOffset=" + startOffset +
                ", endOffset=" + endOffset +
                '}';
    }
    
    public String toTabOutputString() {
        return phraseString + "\t" + startOffset + "\t" + endOffset;
    }
}