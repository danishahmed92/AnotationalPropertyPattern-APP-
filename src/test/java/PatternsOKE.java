import annotation.DependencyTree;
import annotation.DependencyTreeAnnotator;
import config.Database;
import config.IniConfig;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;
import parser.DependencyParser;
import patterngenerator.Pattern;
import patterngenerator.PatternGenerator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @author DANISH AHMED on 1/27/2019
 */
public class PatternsOKE {
    DependencyParser dp = new DependencyParser();
    public HashMap<Integer, HashMap<String, String>> getOKETriples() {
        String selectQuery = "SELECT id_oke_triple, oke_file, subj_label, obj_label " +
                " FROM `oke_triples` ot " +
                " INNER JOIN `property` p ON ot.prop_uri = p.prop_uri " +
                " ORDER BY id_oke_triple ASC;";
        Statement statement = null;
        HashMap<Integer, HashMap<String, String>> okeTripleDetailMap = new HashMap<>();
        try {
            statement = Database.databaseInstance.conn.createStatement();
            ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                int tripleId = rs.getInt("id_oke_triple");

                HashMap<String, String> detailMap = new HashMap<>();
                detailMap.put("annotatedDoc", "anno_"+ (rs.getString("oke_file")));
                detailMap.put("subjLabel", rs.getString("subj_label"));
                detailMap.put("objLabel", rs.getString("obj_label"));

                okeTripleDetailMap.put(tripleId, detailMap);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return okeTripleDetailMap;
    }

    public List<Pattern> generatePatters(Annotation annotation, String subject, String object) {
        List<Pattern> patternsForAnnotation = new ArrayList<>();
        Set<SemanticGraph> distinctGraphs = new HashSet<>();
        assert annotation != null;
        List<CoreMap> sentences = DependencyTree.getSentences(annotation);

        for (CoreMap sentence : sentences) {
            SemanticGraph semanticGraph = DependencyTree.getDependencyParse(sentence);
            List<IndexedWord> subjIWList = DependencyTree.getIndexedWordsFromString(semanticGraph, subject);
            List<IndexedWord> objIWList = DependencyTree.getIndexedWordsFromString(semanticGraph, object);

            if (subjIWList.isEmpty() || objIWList.isEmpty()) {
                System.out.println("Subject: " + subject + "\t\tObject: " + object);
                System.out.println("Sentence: " + semanticGraph.toRecoveredSentenceString());
                System.out.println();
            }

            for (IndexedWord subjIW : subjIWList) {
                for (IndexedWord objIW : objIWList) {
                    SemanticGraph patternGraph = dp.getGraphBetweenNodes(semanticGraph, subjIW, objIW);
                    if (patternGraph != null)
                        distinctGraphs.add(patternGraph);
                }
            }
        }

        Set<SemanticGraph> annoGraphs = new HashSet<>(dp.removeDuplicatedGraphs(distinctGraphs));
        Set<SemanticGraph> prunedAndDRReplacedGraphs = new HashSet<>();
        for (SemanticGraph sg : annoGraphs) {
            PatternGenerator pg = new PatternGenerator();
            SemanticGraph prunedGraph = pg.pruneGraph(sg, subject, object);

            SemanticGraph domainRangeReplaced = pg.replaceDomainRange(prunedGraph, subject, object);
            prunedAndDRReplacedGraphs.add(domainRangeReplaced);
        }

        Set<SemanticGraph> nonSubContainGraph = PatternGenerator.removeSubContainPatterns(prunedAndDRReplacedGraphs);
        for (SemanticGraph sg : nonSubContainGraph) {
            Pattern pattern = new Pattern(sg);
            patternsForAnnotation.add(pattern);
        }

        return patternsForAnnotation;
    }

    public void storeOKEPattern(int tripleId, Pattern pattern) {
        String dbQuery = "INSERT INTO `oke_patterns` (`id_oke_triple`, `orig_root`, `root_lemma`, `pattern`, `sg_pretty`, `sg_sentence`, `dist_nouns`, `dist_verbs`) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?);";

        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(dbQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setInt(1, tripleId);

            try {
                prepareStatement.setString(2, pattern.root.label);
                prepareStatement.setString(3, (pattern.root.lemma).contains("%") ? null : pattern.root.lemma);
                prepareStatement.setString(4, pattern.mergePatternStr);
                prepareStatement.setString(5, pattern.sgPretty);
                prepareStatement.setString(6, pattern.sgToSentence);
                prepareStatement.setString(7, pattern.distinctNouns.toString());
                prepareStatement.setString(8, pattern.distinctVerbs.toString());
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                return;
            }

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        PatternsOKE patternsOKE = new PatternsOKE();
        HashMap<Integer, HashMap<String, String>> okeTripleDetailMap = patternsOKE.getOKETriples();

        for (int tripleId : okeTripleDetailMap.keySet()) {
            HashMap<String, String> tripleDetailMap = okeTripleDetailMap.get(tripleId);
            String annotationFile = IniConfig.configInstance.okeAnnotation + tripleDetailMap.get("annotatedDoc");
            String subject = tripleDetailMap.get("subjLabel");
            String object = tripleDetailMap.get("objLabel");

            System.out.println(tripleId + "\t" + tripleDetailMap.get("annotatedDoc"));

            Annotation annotation = DependencyTreeAnnotator.readAnnotationFromFile(annotationFile);
            List<Pattern> patterns = patternsOKE.generatePatters(annotation, subject, object);

            for (Pattern pattern : patterns) {
                patternsOKE.storeOKEPattern(tripleId, pattern);
            }
        }
    }
}
