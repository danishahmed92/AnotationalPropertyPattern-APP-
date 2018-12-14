import annotation.DependencyTree;
import annotation.DependencyTreeAnnotator;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;
import parser.DependencyParser;
import patterngenerator.PatternGenerator;

import java.util.*;

/**
 * @author DANISH AHMED on 12/9/2018
 */
public class Main {
    public static void main(String[] args) {
        String annoPath = "data/anno/";
        HashMap<String, HashMap<String, String>> annotationSubjObjMap = new LinkedHashMap<String, HashMap<String, String>>();
        HashMap<String, String> subjObjMap = new HashMap<String, String>();

        subjObjMap.put("subj", "Albert Einstein");
//        subjObjMap.put("obj", "German Empire");
        subjObjMap.put("obj", "Kingdom of Württemberg");
        annotationSubjObjMap.put("Einstein-born", subjObjMap);
        subjObjMap = new HashMap<>();

        subjObjMap.put("subj", "Ahmed");
        subjObjMap.put("obj", "Pakistan");
        annotationSubjObjMap.put("subj2Obj2", subjObjMap);
        subjObjMap = new HashMap<>();

        subjObjMap.put("subj", "Ahmed");
        subjObjMap.put("obj", "Ricardo");
        annotationSubjObjMap.put("subj2Obj1", subjObjMap);
        subjObjMap = new HashMap<>();

        subjObjMap.put("subj", "Ahmed");
        subjObjMap.put("obj", "Karachi");
        annotationSubjObjMap.put("obj2Subj1", subjObjMap);

        HashMap<String, Set<SemanticGraph>> annotationSGMap = new LinkedHashMap<>();
        DependencyParser dp = new DependencyParser();
        for (String annoFile : annotationSubjObjMap.keySet()) {
            String path = annoPath + annoFile;
            Annotation annotation = DependencyTreeAnnotator.readAnnotationFromFile(path);

            Set<SemanticGraph> distinctGraphs = new HashSet<>();
            String subj = annotationSubjObjMap.get(annoFile).get("subj");
            String obj = annotationSubjObjMap.get(annoFile).get("obj");

            assert annotation != null;
            List<CoreMap> sentences = DependencyTree.getSentences(annotation);
            for (CoreMap sentence : sentences) {
                SemanticGraph semanticGraph = DependencyTree.getDependencyParse(sentence);
                List<IndexedWord> subjIWList = DependencyTree.getIndexedWordsFromString(semanticGraph, subj);
                List<IndexedWord> objIWList = DependencyTree.getIndexedWordsFromString(semanticGraph, obj);

                for (IndexedWord subjIW : subjIWList) {
                    for (IndexedWord objIW : objIWList) {
                        SemanticGraph patternGraph = dp.getGraphBetweenNodes(semanticGraph, subjIW, objIW);
                        if (patternGraph != null)
                            distinctGraphs.add(patternGraph);
                    }
                }
            }
            annotationSGMap.put(annoFile, distinctGraphs);
        }

        for (String annoFile : annotationSGMap.keySet()) {
            Set<SemanticGraph> annoGraphs = annotationSGMap.get(annoFile);
            System.out.println(annoFile);
            String subj = annotationSubjObjMap.get(annoFile).get("subj");
            String obj = annotationSubjObjMap.get(annoFile).get("obj");

            annoGraphs = dp.removeDuplicatedGraphs(annoGraphs);
            Set<SemanticGraph> prunedAndDRReplacedGraphs = new HashSet<>();
            for (SemanticGraph sg : annoGraphs) {
                PatternGenerator pg = new PatternGenerator(sg);
                SemanticGraph prunedGraph = pg.pruneGraph(sg, subj, obj);
                SemanticGraph domainRangeReplaced = pg.replaceDomainRange(prunedGraph, subj, obj);
                prunedAndDRReplacedGraphs.add(domainRangeReplaced);
//                System.out.println("leaves:\t" + domainRangeReplaced.getLeafVertices());
//                prunedGraph.prettyPrint();
                System.out.println(prunedGraph.toRecoveredSentenceString());
                System.out.println(prunedGraph.toCompactString(true));
                System.out.println();
            }
            System.out.println();


        }
    }
}
