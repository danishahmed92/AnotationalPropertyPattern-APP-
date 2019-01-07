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
        /*There could be multiple IndexedWords for a single split*/
        List<IndexedWord> indexedWordList = new LinkedList<>();
        if (label == null || label.isEmpty())
            return indexedWordList;

        String[] labelSplit = Utils.getLabelSplit(label);

        for (String split : labelSplit) {
            try {
                List<IndexedWord> IWForSplit = semanticGraph.getAllNodesByWordPattern(split);
                if (IWForSplit != null && !IWForSplit.isEmpty()) {
                    IndexedWord firstIW = semanticGraph.getNodeByWordPattern(split);
                    if (!firstIW.tag().equals("IN"))
                        indexedWordList.addAll(IWForSplit);
                }
            } catch (PatternSyntaxException pse) {
                pse.getDescription();
            }
        }
        return indexedWordList;
    }
}
