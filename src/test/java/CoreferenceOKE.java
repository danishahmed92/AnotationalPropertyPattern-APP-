import annotation.DependencyTreeAnnotator;
import config.Database;
import config.IniConfig;
import edu.stanford.nlp.pipeline.Annotation;
import nlp.Coreference;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author DANISH AHMED on 1/26/2019
 */
public class CoreferenceOKE {
    public HashMap<String, String> getOKEFileSentences() {
        String selectQuery = "SELECT oke_file, sentence from `oke_triples` group by oke_file, sentence;";

        Statement statement = null;
        HashMap<String, String> fileSentenceMap = new HashMap<>();
        try {
            statement = Database.databaseInstance.conn.createStatement();
            ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                String file = rs.getString("oke_file");
                String sentence = rs.getString("sentence");

                fileSentenceMap.put(file, sentence);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return fileSentenceMap;
    }

    public HashMap<Integer, HashMap<String, String>> getOKESentences() {
        String selectQuery = "SELECT id_oke_sent, annotated_doc, sentence, entities from `oke_sentences` " +
                "ORDER BY id_oke_sent;";
        Statement statement = null;
        HashMap<Integer, HashMap<String, String>> okeSentenceDetailMap = new HashMap<>();
        try {
            statement = Database.databaseInstance.conn.createStatement();
            ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                int sentenceId = rs.getInt("id_oke_sent");

                HashMap<String, String> detailMap = new HashMap<>();
                detailMap.put("annotatedDoc", rs.getString("annotated_doc"));
                detailMap.put("sentence", rs.getString("sentence"));
                detailMap.put("entities", rs.getString("entities"));

                okeSentenceDetailMap.put(sentenceId, detailMap);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return okeSentenceDetailMap;
    }

    public Set<String> getDistinctEntitiesFromString(String entities) {
        Set<String> distinctEntities = new HashSet<>();
        String[] entitiesSplit = entities.split(",");
        for (String entity : entitiesSplit) {
            if (entity.length() > 0 && !entity.equals(" ")) {
                distinctEntities.add(entity.trim());
            }
        }
        return distinctEntities;
    }

    public List<String> getCorefSentencesHavingEntities(HashMap<String, String> detailMap) {
        Set<String> corefLabelSet = getDistinctEntitiesFromString(detailMap.get("entities"));
        String annotationFile = IniConfig.configInstance.okeAnnotation + detailMap.get("annotatedDoc");

        Annotation document = DependencyTreeAnnotator.readAnnotationFromFile(annotationFile);
        Coreference coreference = Coreference.CRInstance;
        return coreference.getCoreferenceReplacedSentences(document, corefLabelSet);
    }

    public void storeOKECorefSentences(int sentenceId, String okeFile, List<String> sentences) {
        for (String sentence : sentences) {
            String insertQuery = "INSERT INTO `oke_sentence_coref` (`id_oke_sent`, `oke_file`, `sentence`) " +
                    "VALUES (?, ?, ?);";
            PreparedStatement prepareStatement = null;
            try {
                prepareStatement = Database.databaseInstance.conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                prepareStatement.setInt(1, sentenceId);
                prepareStatement.setString(2, okeFile);
                prepareStatement.setString(3, sentence);

                prepareStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        /*CoreferenceOKE coreferenceOKE = new CoreferenceOKE();
        HashMap<Integer, HashMap<String, String>> okeSentenceDetailMap = coreferenceOKE.getOKESentences();
        for (int sentId : okeSentenceDetailMap.keySet()) {
            HashMap<String, String> detailMap = okeSentenceDetailMap.get(sentId);
            List<String> corefSentences = coreferenceOKE.getCorefSentencesHavingEntities(detailMap);

            if (corefSentences.isEmpty())
                corefSentences.add(detailMap.get("sentence"));
            String okeFile = detailMap.get("annotatedDoc").replaceAll("anno_", "");
            coreferenceOKE.storeOKECorefSentences(sentId, okeFile, corefSentences);
        }*/

        String annotationStorageDirectory = IniConfig.configInstance.okeTrainDirectory;

        CoreferenceOKE coreferenceOKE = new CoreferenceOKE();
        Coreference coreference = Coreference.CRInstance;
        HashMap<String, String> fileSentenceMap = coreferenceOKE.getOKEFileSentences();

        for (String file : fileSentenceMap.keySet()) {
            String sentence = fileSentenceMap.get(file);
            try {
                DependencyTreeAnnotator.createAndWriteAnnotationToFile(sentence, coreference.getPipeline(), annotationStorageDirectory + "anno_" + file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
