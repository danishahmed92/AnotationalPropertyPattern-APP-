package wordnet;

import config.IniConfig;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author DANISH AHMED on 12/10/2018
 */
public class WordNet {
    public static WordNet wordNet;
    private IDictionary dict;
    static {
        try {
            wordNet = new WordNet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WordNet() throws IOException {
        String path = IniConfig.configInstance.wordNet;
        URL url = null;
        try {
            url = new URL("file", null, path);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (url == null)
            return;

        dict = new Dictionary(url);
        dict.open();
    }

    public String getVerbForNoun(String noun) {
        IIndexWord idxWord = dict.getIndexWord(noun, edu.mit.jwi.item.POS.NOUN);
        try {
            IWordID wordID = idxWord.getWordIDs().get(0);
            IWord word = dict.getWord(wordID);
            String nounLemma = word.getLemma();

            for (IWordID iWordID : word.getRelatedWords()) {
                IWord relWord = dict.getWord(iWordID);
                if (relWord.getPOS() == edu.mit.jwi.item.POS.VERB) {
                    String verb = relWord.toString().split("-")[4];

                    IIndexWord iinWord = dict.getIndexWord(verb, POS.VERB);
                    IWordID iwordID = iinWord.getWordIDs().get(0);
                    IWord iword = dict.getWord(iwordID);

                    return iword.getLemma();
                }
            }
            return nounLemma;
        } catch (NullPointerException ne) {
            return noun;
        }
    }
}
