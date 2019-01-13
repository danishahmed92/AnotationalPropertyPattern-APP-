package datacollection;

import annotation.DependencyTreeAnnotator;
import config.IniConfig;
import edu.stanford.nlp.pipeline.Annotation;
import nlp.Coreference;
import properties.PropertyUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author DANISH AHMED on 1/13/2019
 */
public class MentionSentence extends DataStorage{
    public Annotation getCorefSentencesAnnotation(String sourceSentence, String fileName, Boolean writeAnnotation) {
        String[] fileSplit = fileName.split("_");
        String property = fileSplit[0];
        int tripleId = Integer.parseInt(fileSplit[1]);
        int sentenceId = Integer.parseInt(fileSplit[2]);

        Coreference coreference = Coreference.CRInstance;
        Annotation document = null;
        String outputFile = null;

        if (writeAnnotation) {
            try {
                outputFile = IniConfig.configInstance.dptAnnotation1 + property + "/" + fileName;
                document = DependencyTreeAnnotator.createAndWriteAnnotationToFile(sourceSentence, coreference.getPipeline(), outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            document = coreference.annotateDocument(sourceSentence);
        }

        if (document != null)
            updateAnnotationFile(sentenceId, fileName);

        return document;
    }

    public List<String> getMentionedSentences(Annotation document) {
        Coreference coreference = Coreference.CRInstance;
        List<String> corefSentences = new ArrayList<>();
        if (coreference.getClusterIdCorefChainMap(document) == null)
            return corefSentences;
        else
            return coreference.getCoreferenceReplacedSentences(document);
    }

    public List<String> filterSentencesWithSubjObj(List<String> corefSentences, String subj, String obj) {
        List<String> newSentences = new ArrayList<>();
        for (String sentence : corefSentences) {
            if (sentence.contains(subj) && sentence.contains(obj)) {
                newSentences.add(sentence);
            }
        }
        return newSentences;
    }

    public static void main(String[] args) {
        try {
            List<String> properties = PropertyUtils.getAllProperties();
            for (String property : properties) {
                HashMap<Integer, HashMap<String, String>> sentenceTripleDataMap = PropertyUtils.getSentencesForProperty(property);
                for (Integer sentenceId : sentenceTripleDataMap.keySet()) {
                    int tripleId = Integer.parseInt(sentenceTripleDataMap.get(sentenceId).get("tripleId"));
                    String subLabel = sentenceTripleDataMap.get(sentenceId).get("subLabel");
                    String objLabel = sentenceTripleDataMap.get(sentenceId).get("objLabel");
                    String sentence = sentenceTripleDataMap.get(sentenceId).get("sentence");

                    MentionSentence ms = new MentionSentence();
                    String fileName = property + "_" + tripleId + "_" + sentenceId;

                    Annotation document = ms.getCorefSentencesAnnotation(sentence, fileName, true);
                    List<String> corefSentences = ms.getMentionedSentences(document);
                    if (corefSentences.isEmpty()) {
                        ms.insertRefinedSentenceToDB(sentenceId, tripleId, property, sentence);
                    } else {
                        List<String> filteredSentences = ms.filterSentencesWithSubjObj(corefSentences, subLabel, objLabel);
                        for (String sent : filteredSentences) {
                            ms.insertRefinedSentenceToDB(sentenceId, tripleId, property, sent);
                        }
                    }
//                    break;
                }
//                break;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
