package datacollection;

import annotation.DependencyTree;
import annotation.DependencyTreeAnnotator;
import config.IniConfig;
import edu.stanford.nlp.pipeline.Annotation;
import nlp.Coreference;
import nlp.SecondPass;
import properties.PropertyUtils;
import utils.Utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * @author DANISH AHMED on 1/13/2019
 */
public class MentionSentence extends DataStorage{
    public Annotation getCorefSentencesAnnotation(String sourceSentence, String fileName, Boolean writeAnnotation) {
        String[] fileSplit = fileName.split("_");
        String property = fileSplit[0];
//        int tripleId = Integer.parseInt(fileSplit[1]);
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

    public List<String> getMentionedSentences(Annotation document, Set<String> corefLabelSet) {
        Coreference coreference = Coreference.CRInstance;
        List<String> corefSentences = new ArrayList<>();
        if (coreference.getClusterIdCorefChainMap(document) == null)
            return corefSentences;
        else
            return coreference.getCoreferenceReplacedSentences(document, corefLabelSet);
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

    public void generateRefinedSentence(String property, int sentenceId, HashMap<String, String> sentenceMap, Annotation document) {
        int tripleId = Integer.parseInt(sentenceMap.get("tripleId"));
        String subLabel = sentenceMap.get("subLabel");
        String objLabel = sentenceMap.get("objLabel");
        String sentence = sentenceMap.get("sentence");

        Set<String> corefLabelSet = new HashSet<>();
        corefLabelSet.add(subLabel);
        corefLabelSet.add(objLabel);

        List<String> corefSentences = getMentionedSentences(document, corefLabelSet);
        if (corefSentences.isEmpty()) {
            insertRefinedSentenceToDB(sentenceId, tripleId, property, sentence);
        } else {
            List<String> filteredSentences = filterSentencesWithSubjObj(corefSentences, subLabel, objLabel);
            for (String sent : filteredSentences) {
                insertRefinedSentenceToDB(sentenceId, tripleId, property, sent);
            }
        }
    }

    public void generateRefinedCorefSentencesForAllProperties() {
        try {
            List<String> properties = PropertyUtils.getAllProperties();
            for (String property : properties) {
                String annotationDirectory = IniConfig.configInstance.dptAnnotation1 + property + "/";

                HashMap<Integer, HashMap<String, String>> sentenceTripleDataMap = PropertyUtils.getAnnotationsForProperty(property);
                for (Integer sentenceId : sentenceTripleDataMap.keySet()) {
                    HashMap<String, String> sentenceMap = sentenceTripleDataMap.get(sentenceId);
                    String annotatedDoc = sentenceMap.get("annotatedDoc");

                    Annotation document = DependencyTreeAnnotator.readAnnotationFromFile(annotationDirectory + annotatedDoc);
                    if (document != null)
                        generateRefinedSentence(property, sentenceId, sentenceTripleDataMap.get(sentenceId), document);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveRefinedCorefSentenceAnnotationForAllProperties() {
        try {
            List<String> properties = PropertyUtils.getAllProperties();
            for (String property : properties) {
                property = "trainer";
                String annotationDirectory = IniConfig.configInstance.dptAnnotation2 + property + "/";

                HashMap<Integer, HashMap<String, String>> sentenceTripleDataMap = PropertyUtils.getCorefSentencesForProperty(property);
                for (Integer sentenceId : sentenceTripleDataMap.keySet()) {
                    HashMap<String, String> sentenceMap = sentenceTripleDataMap.get(sentenceId);
                    int tripleId = Integer.parseInt(sentenceMap.get("tripleId"));
                    String sentence = sentenceMap.get("sentence");

                    SecondPass secondPass = SecondPass.SPInstance;
                    Annotation document = null;
                    String outputFile = null;
                    String fileName = property + "_" + tripleId + "_" + sentenceId;
                    try {
                        outputFile = annotationDirectory + fileName;
                        document = DependencyTreeAnnotator.createAndWriteAnnotationToFile(sentence, secondPass.getPipeline(), outputFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (document != null)
                        updateCorefAnnotationFile(sentenceId, fileName);
                }
                System.out.println(property);
                break;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            List<String> properties = PropertyUtils.getAllProperties();
            MentionSentence ms = new MentionSentence();
            for (String property : properties) {
                Utils.createFolderIfNotExist(IniConfig.configInstance.dptAnnotation1 + property);
                Utils.createFolderIfNotExist(IniConfig.configInstance.dptAnnotation2 + property);

                HashMap<Integer, HashMap<String, String>> sentenceTripleDataMap = PropertyUtils.getSentencesForProperty(property);
                for (Integer sentenceId : sentenceTripleDataMap.keySet()) {
                    HashMap<String, String> sentenceMap = sentenceTripleDataMap.get(sentenceId);
                    int tripleId = Integer.parseInt(sentenceMap.get("tripleId"));
                    String sentence = sentenceMap.get("sentence");
                    String fileName = property + "_" + tripleId + "_" + sentenceId;

                    Annotation document = ms.getCorefSentencesAnnotation(sentence, fileName, true);
                    ms.generateRefinedSentence(property, sentenceId, sentenceTripleDataMap.get(sentenceId), document);
                }
                System.out.println(property);
            }
            ms.saveRefinedCorefSentenceAnnotationForAllProperties();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
