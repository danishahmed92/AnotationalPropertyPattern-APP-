package patterngenerator;

import annotation.DependencyTree;
import annotation.DependencyTreeAnnotator;
import config.Database;
import config.IniConfig;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;
import parser.DependencyParser;
import properties.PropertyAnnotations;
import properties.PropertyUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class PatternStorage {
    private DependencyParser dp = new DependencyParser();

    private List<Pattern> getProcessedPatterns(Annotation annotation, String subjLabel, String objLabel) {
        Set<SemanticGraph> distinctGraphs = new HashSet<>();
        List<Pattern> patternsForAnnotation = new ArrayList<>();

        if (annotation != null) {
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
                patternsForAnnotation.add(pattern);
            }
        }

        return patternsForAnnotation;
    }

    private void storePatternToDB(String annotationFile, Pattern pattern) {
        String[] split = annotationFile.split("_");
        String propertyUri = split[0];
        int idPropTriple = Integer.parseInt(split[1]);
        int idPsRefined = Integer.parseInt(split[2]);

        String dbQuery = "INSERT INTO `property_pattern` (`id_ps_coref`, `prop_uri`, `id_prop_triple`, `orig_root`, `root_lemma`, `pattern`, `sg_pretty`, `sg_sentence`, `dist_nouns`) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?);";

        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(dbQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setInt(1, idPsRefined);
            prepareStatement.setString(2, propertyUri);
            prepareStatement.setInt(3, idPropTriple);

            try {
                prepareStatement.setString(4, pattern.root.label);
                prepareStatement.setString(5, (pattern.root.lemma).contains("%") ? null : pattern.root.lemma);
                prepareStatement.setString(6, pattern.mergePatternStr);
                prepareStatement.setString(7, pattern.sgPretty);
                prepareStatement.setString(8, pattern.sgToSentence);
                prepareStatement.setString(9, pattern.distinctNouns.toString());
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                return;
            }

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void storePatternsForAllPropertiesAnnotations() {
        String annotationDirectory = IniConfig.configInstance.dptAnnotation2;
        try {
            List<String> properties = PropertyUtils.getAllProperties();
            for (String property : properties) {
                System.out.println(property);
                HashMap<String, HashMap<String, String>> annotationsLabelMap = PropertyAnnotations.getAnnotationLabelMap(property);
                List<String> annotationFiles = PropertyAnnotations.getAnnotationFilesForProperty(property);

                if (annotationFiles != null && annotationFiles.size() > 0) {
                    for (String annotationFile : annotationFiles) {
                        if (annotationsLabelMap.containsKey(annotationFile)) {
                            String subjLabel = annotationsLabelMap.get(annotationFile).get("subjLabel");
                            String objLabel = annotationsLabelMap.get(annotationFile).get("objLabel");

                            Annotation annotation = DependencyTreeAnnotator.readAnnotationFromFile(annotationDirectory + property + "/" + annotationFile);
                            List<Pattern> patternsForAnnotation = getProcessedPatterns(annotation, subjLabel, objLabel);

                            for (Pattern pattern : patternsForAnnotation) {
                                storePatternToDB(annotationFile, pattern);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        PatternStorage patternStorage = new PatternStorage();
        patternStorage.storePatternsForAllPropertiesAnnotations();
    }
}
