import annotation.DependencyTree;
import annotation.DependencyTreeAnnotator;
import config.IniConfig;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;
import parser.DependencyParser;
import patterngenerator.Pattern;
import patterngenerator.PatternGenerator;
import properties.PropertyAnnotations;

import java.sql.SQLException;
import java.util.*;

/**
 * @author DANISH AHMED on 12/9/2018
 */
public class Main {
    public static void main(String[] args) {
        try {
            String annotationDirectory = IniConfig.configInstance.dptAnnotation2;
            List<String> properties = PropertyAnnotations.getAllProperties();
            DependencyParser dp = new DependencyParser();
            for (String property : properties) {
                HashMap<String, HashMap<String, String>> annotationsLabelMap = PropertyAnnotations.getAnnotationLabelMap(property);
                List<String> annotationFiles = PropertyAnnotations.getAnnotationFilesForProperty(property);

                if (annotationFiles != null && annotationFiles.size() > 0) {
                    for (String annotationFile : annotationFiles) {
                        if (annotationsLabelMap.containsKey(annotationFile)) {
                            String subjLabel = annotationsLabelMap.get(annotationFile).get("subjLabel");
                            String objLabel = annotationsLabelMap.get(annotationFile).get("objLabel");

                            Annotation annotation = DependencyTreeAnnotator.readAnnotationFromFile(annotationDirectory + property + "/" + annotationFile);
                            Set<SemanticGraph> distinctGraphs = new HashSet<>();

                            assert annotation != null;
                            List<CoreMap> sentences = DependencyTree.getSentences(annotation);
                            for (CoreMap sentence : sentences) {
                                SemanticGraph semanticGraph = DependencyTree.getDependencyParse(sentence);
                                List<IndexedWord> subjIWList = DependencyTree.getIndexedWordsFromString(semanticGraph, subjLabel);
                                List<IndexedWord> objIWList = DependencyTree.getIndexedWordsFromString(semanticGraph, objLabel);

                                for (IndexedWord subjIW : subjIWList) {
                                    for (IndexedWord objIW : objIWList) {
                                        SemanticGraph patternGraph = dp.getGraphBetweenNodes(semanticGraph, subjIW, objIW);
                                        if (patternGraph != null)
                                            distinctGraphs.add(patternGraph);
                                    }
                                }
                            }
                            System.out.println(annotationFile);
                            /*System.out.println(distinctGraphs);
                            System.out.println();*/


                            distinctGraphs = dp.removeDuplicatedGraphs(distinctGraphs);
                            Set<SemanticGraph> prunedAndDRReplacedGraphs = new HashSet<>();
                            for (SemanticGraph sg : distinctGraphs) {
                                SemanticGraph prunedGraph = PatternGenerator.pruneGraph(sg, subjLabel, objLabel);
                                SemanticGraph domainRangeReplaced = PatternGenerator.replaceDomainRange(prunedGraph, subjLabel, objLabel);
                                prunedAndDRReplacedGraphs.add(domainRangeReplaced);
                            }

                            Set<SemanticGraph> nonSubContainGraph = PatternGenerator.removeSubContainPatterns(prunedAndDRReplacedGraphs);
                            for (SemanticGraph sg : nonSubContainGraph) {
                                Pattern pattern = new Pattern(sg);
                                System.out.println(pattern.sgToSentence);
                                System.out.println(pattern.mergePatternStr);
                                System.out.println();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
