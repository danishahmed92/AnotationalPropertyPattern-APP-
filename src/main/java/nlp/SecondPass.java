package nlp;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;

public class SecondPass implements CoreNLP {
    private StanfordCoreNLP pipelineSecondPass;

    @Override
    public Properties setProperties() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse");
        return props;
    }

    @Override
    public StanfordCoreNLP setPipeLine(Properties props) {
        return new StanfordCoreNLP(props);
    }

    public static SecondPass SPInstance;
    static {
        SPInstance = new SecondPass();
    }

    public SecondPass() {
        this.pipelineSecondPass = setPipeLine(setProperties());
    }

    public StanfordCoreNLP getPipeline() {
        return pipelineSecondPass;
    }

    public Annotation annotateDocument(String context) {
        Annotation document = new Annotation(context);
        pipelineSecondPass.annotate(document);

        return document;
    }
}
