package annotation;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import utils.Utils;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * @author DANISH AHMED on 12/9/2018
 */
public class DependencyTree {
    public static SemanticGraph getDependencyParse(CoreMap sentence) {
        return sentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class);
    }

    public static List<CoreMap> getSentences(Annotation annotation) {
        return annotation.get(CoreAnnotations.SentencesAnnotation.class);
    }

    public static List<IndexedWord> getIndexedWordsFromString(SemanticGraph semanticGraph, String label) {
        List<IndexedWord> indexedWordList = new LinkedList<>();
        if (label == null || label.isEmpty())
            return indexedWordList;

        String[] labelSplit = Utils.getLabelSplit(label);
        List<IndexedWord> IWForSplit = semanticGraph.getAllNodesByWordPattern(labelSplit[0]);

        if (labelSplit.length == 1) {
            indexedWordList.addAll(IWForSplit);
            return indexedWordList;
        }

        for (IndexedWord iw : IWForSplit) {
            indexedWordList.add(iw);
            int position = iw.index();
            for (int i = 1; i < labelSplit.length; i++) {
                try {
                    IndexedWord indexedWord = semanticGraph.getNodeByIndex(position + 1);
                    String splitWord = labelSplit[i];
                    String currentWord = indexedWord.originalText();

                    if (currentWord.equals(splitWord))
                        indexedWordList.add(indexedWord);
                    else {
                        indexedWordList.clear();
                        break;
                    }
                    position++;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    break;
                }
            }
            if (indexedWordList.size() == labelSplit.length)
                break;
        }
        return indexedWordList;
    }
}
