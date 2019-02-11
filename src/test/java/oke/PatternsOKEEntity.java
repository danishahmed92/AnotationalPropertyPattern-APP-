package oke;

import annotation.DependencyTree;
import config.Database;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;
import nlp.Coreference;
import parser.DependencyParser;
import patterngenerator.Pattern;
import patterngenerator.PatternGenerator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @author DANISH AHMED on 2/10/2019
 */
public class PatternsOKEEntity {
    DependencyParser dp = new DependencyParser();
    public HashMap<Integer, HashMap<String, String>> getOKEEntitySentence() {
        String selectQuery = "SELECT id_oke_sent_entity, id_oke_coref, " +
                "subj, obj, coref_sentence " +
                " FROM `oke_sent_entity` ;";
        Statement statement = null;
        HashMap<Integer, HashMap<String, String>> okeEntitySentenceMap = new HashMap<>();
        try {
            statement = Database.databaseInstance.conn.createStatement();
            ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                int entityId = rs.getInt("id_oke_sent_entity");

                HashMap<String, String> entityDetailMap = new HashMap<>();
                entityDetailMap.put("idCoref", String.valueOf(rs.getInt("id_oke_coref")));
                entityDetailMap.put("subj", rs.getString("subj"));
                entityDetailMap.put("obj", rs.getString("obj"));
                entityDetailMap.put("corefSentence", rs.getString("coref_sentence"));

                okeEntitySentenceMap.put(entityId, entityDetailMap);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return okeEntitySentenceMap;
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

    public void storeOKEPattern(int entitySentId, int corefSentId, Pattern pattern) {
        String dbQuery = "INSERT INTO `oke_coref_pattern` (`id_oke_sent_entity`, `id_oke_coref`, " +
                "`orig_root`, `root_lemma`, " +
                "`pattern`, `sg_pretty`, `sg_sentence`, " +
                "`dist_nouns`, `dist_verbs`) " +
                "values (?, ?, " +
                "?, ?, " +
                "?, ?, ?, " +
                "?, ?);";

        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(dbQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setInt(1, entitySentId);
            prepareStatement.setInt(2, corefSentId);

            try {
                prepareStatement.setString(3, pattern.root.label);
                prepareStatement.setString(4, (pattern.root.lemma).contains("%") ? null : pattern.root.lemma);
                prepareStatement.setString(5, pattern.mergePatternStr);
                prepareStatement.setString(6, pattern.sgPretty);
                prepareStatement.setString(7, pattern.sgToSentence);
                prepareStatement.setString(8, pattern.distinctNouns.toString());
                prepareStatement.setString(9, pattern.distinctVerbs.toString());
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
        PatternsOKEEntity okeEntity = new PatternsOKEEntity();

        HashMap<Integer, HashMap<String, String>> okeEntitySentenceMap = okeEntity.getOKEEntitySentence();
        for (int entityId : okeEntitySentenceMap.keySet()) {
            HashMap<String, String> entityDetailMap = okeEntitySentenceMap.get(entityId);

            int corefSentId = Integer.parseInt(entityDetailMap.get("idCoref"));
            String subject = entityDetailMap.get("subj");
            String object = entityDetailMap.get("obj");
            String corefSentence = entityDetailMap.get("corefSentence");

            Coreference coreference = Coreference.CRInstance;
            Annotation annotation = coreference.annotateDocument(corefSentence);

            List<Pattern> generatedPatterns = okeEntity.generatePatters(annotation, subject, object);
            for (Pattern pattern : generatedPatterns) {
                okeEntity.storeOKEPattern(entityId, corefSentId, pattern);
            }
        }
    }
}
