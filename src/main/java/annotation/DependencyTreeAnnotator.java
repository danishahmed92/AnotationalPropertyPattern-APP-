package annotation;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.ProtobufAnnotationSerializer;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * @author DANISH AHMED on 12/7/2018
 */
public class DependencyTreeAnnotator {
    public static Annotation createAndWriteAnnotationToFile(String context, StanfordCoreNLP pipeline, String outputFile) throws IOException {
        Annotation document = new Annotation(context);
        pipeline.annotate(document);

        OutputStream out = new FileOutputStream(outputFile);

        ProtobufAnnotationSerializer serializer = new ProtobufAnnotationSerializer();
        serializer.write(document,out);
        out.close();

        return document;
    }

    public static Annotation readAnnotationFromFile(String annotationFile) {
        ProtobufAnnotationSerializer serializer = new ProtobufAnnotationSerializer();
        try {
            return serializer.readUndelimited(new File(annotationFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}