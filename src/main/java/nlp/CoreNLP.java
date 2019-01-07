package nlp;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;

public interface CoreNLP {
    Properties setProperties();
    StanfordCoreNLP setPipeLine(Properties props);
}
