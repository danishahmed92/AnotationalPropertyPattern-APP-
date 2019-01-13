package nlp;

import annotation.DependencyTreeAnnotator;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author DANISH AHMED on 1/13/2019
 */
public class Coreference implements CoreNLP{
    private StanfordCoreNLP pipelineCoreference;

    @Override
    public Properties setProperties() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,mention,coref");
        props.setProperty("coref.algorithm", "neural");
        return props;
    }

    @Override
    public StanfordCoreNLP setPipeLine(Properties props) {
        return new StanfordCoreNLP(props);
    }

    public static Coreference CRInstance;
    static {
        CRInstance = new Coreference();
    }

    public Coreference() {
        this.pipelineCoreference = setPipeLine(setProperties());
    }

    public StanfordCoreNLP getPipeline() {
        return pipelineCoreference;
    }

    public Annotation annotateDocument(String context) {
        Annotation document = new Annotation(context);
        pipelineCoreference.annotate(document);
        return document;
    }

    public List<CoreMap> getSentences(Annotation document) {
        return document.get(CoreAnnotations.SentencesAnnotation.class);
    }

    public Map<Integer, CorefChain> getClusterIdCorefChainMap(Annotation document) {
        return document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
    }

    public List<String> getCoreferenceReplacedSentences(Annotation document) {
        List<String> corefSentences = new ArrayList<>();
        Map<Integer, CorefChain> clusterIdCorefChainMap = getClusterIdCorefChainMap(document);

        if (clusterIdCorefChainMap == null)
            return null;

        for (CoreMap sentence : getSentences(document)) {
            List<String> sentenceWords = new ArrayList<>();
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

            for (int i = 0; i < tokens.size(); i++) {
                CoreLabel token = tokens.get(i);

                if (token.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals("."))
                    continue;

                if (token.get(CoreAnnotations.PartOfSpeechAnnotation.class).contains("PRP")) {
                    Integer corefClustId = token.get(CorefCoreAnnotations.CorefClusterIdAnnotation.class);
                    CorefChain corefChain = clusterIdCorefChainMap.get(corefClustId);

                    if (corefChain == null)
                        continue;
                    CorefChain.CorefMention mention = corefChain.getRepresentativeMention();

                    if ((i + 1) < tokens.size()) {
                        CoreLabel nextToken = tokens.get(i + 1);
                        if (nextToken.get(CoreAnnotations.PartOfSpeechAnnotation.class).contains("NN")
                                || nextToken.get(CoreAnnotations.PartOfSpeechAnnotation.class).contains("JJ"))
                            sentenceWords.add(mention.mentionSpan + "'s");
                        else
                            sentenceWords.add(mention.mentionSpan);
                    }
                } else {
                    sentenceWords.add(token.word());
                }
            }
            corefSentences.add(String.join(" ", sentenceWords).trim() + ".");
        }
        return corefSentences;
    }

    public static void main(String[] args) {
        String context = "Karachi is also called the city of lights, and it bears Ahmed.";
        Coreference coreference = Coreference.CRInstance;
        Annotation document = coreference.annotateDocument(context);

        List<String> corefSentences = coreference.getCoreferenceReplacedSentences(document);
        StringBuilder corefSentence = new StringBuilder();
        for (String sent : corefSentences) {
            corefSentence.append(sent).append(" ");
        }
        corefSentence = new StringBuilder(corefSentence.toString().trim());
        System.out.println(corefSentence);
    }
}
