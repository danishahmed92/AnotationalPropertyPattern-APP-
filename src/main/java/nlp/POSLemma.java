package nlp;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;

public class POSLemma implements CoreNLP {
    private StanfordCoreNLP pipelinePOSLemma;

    @Override
    public Properties setProperties() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        props.setProperty("coref.algorithm", "neural");
        return props;
    }

    @Override
    public StanfordCoreNLP setPipeLine(Properties props) {
        return new StanfordCoreNLP(props);
    }

    public static POSLemma PLInstance;
    static {
        PLInstance = new POSLemma();
    }

    public POSLemma() {
        this.pipelinePOSLemma = setPipeLine(setProperties());
    }

    public StanfordCoreNLP getPipeline() {
        return pipelinePOSLemma;
    }

    public Annotation annotateDocument(String context) {
        Annotation document = new Annotation(context);
        pipelinePOSLemma.annotate(document);

        return document;
    }
}
