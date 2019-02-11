package oke;

import config.Database;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author DANISH AHMED on 2/10/2019
 */
public class CorefOKE {
    public Set<String> mergeLocationType = new HashSet<String>(){{
        add("CITY");
        add("STATE_OR_PROVINCE");
        add("COUNTRY");
        add("NATIONALITY");
    }};

    public Set<String> keepEntityTypes = new HashSet<String>() {{
        add("ORGANIZATION");
        add("PERSON");
        add("LOCATION");
    }};

    public HashMap<Integer, String> getOKEFileSentences() {
        String selectQuery = "SELECT id_oke_sent, sentence from `oke_sent`;";

        Statement statement = null;
        HashMap<Integer, String> idSentenceMap = new HashMap<>();
        try {
            statement = Database.databaseInstance.conn.createStatement();
            ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                int id = rs.getInt("id_oke_sent");
                String sentence = rs.getString("sentence");

                idSentenceMap.put(id, sentence);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return idSentenceMap;
    }

    public HashMap<Integer, String> getOKECorefSentences() {
        String selectQuery = "SELECT id_oke_coref, coref_sentence from `oke_coref`;";

        Statement statement = null;
        HashMap<Integer, String> idCorefSentenceMap = new HashMap<>();
        try {
            statement = Database.databaseInstance.conn.createStatement();
            ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                int id = rs.getInt("id_oke_coref");
                String sentence = rs.getString("coref_sentence");

                idCorefSentenceMap.put(id, sentence);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return idCorefSentenceMap;
    }

    public List<String> getCorefedSentences(String sentence) {
        RestTemplate restTemplate = new RestTemplate();
        Set<String> corefLabelSet = new HashSet<>();

        MultiValueMap<String, Object> requestMap = new LinkedMultiValueMap<String, Object>();
        requestMap.add("context", sentence);
        requestMap.add("corefLabelSet", corefLabelSet);

        String corefAnnoURL = "http://localhost:8080/nlp/coreference/mention/sentences/";
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestMap);
        ResponseEntity<List<String>> response =
                restTemplate.exchange(
                        corefAnnoURL,
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<List<String>>(){});
        List<String> corefedSentences = response.getBody();
        return corefedSentences;
    }

    public HashMap<String, Set<String>> getNERMap(String sentence, String url) {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, Object> requestMap = new LinkedMultiValueMap<String, Object>();
        requestMap.add("sentence", sentence);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestMap);
        ResponseEntity<HashMap<String, Set<String>>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<HashMap<String, Set<String>>>(){});
        HashMap<String, Set<String>> nerMap = response.getBody();
        return nerMap;
    }

    public void storeOKECorefSentence(int sentId, String corefSent) {
        String insertQuery = "INSERT INTO `oke_coref` (`id_oke_sent`, `coref_sentence`) " +
                "VALUES (?, ?);";

        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setInt(1, sentId);
            prepareStatement.setString(2, corefSent);

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateNERInDB(HashMap<String, Set<String>> nerMap, int okeCorefId) {
        String updateQuery = "UPDATE `oke_coref` SET `ner_categorized` = ? WHERE `id_oke_coref` = ?;";
        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(updateQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setString(1, nerMap.toString());
            prepareStatement.setInt(2, okeCorefId);

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CorefOKE corefOKE = new CorefOKE();
        HashMap<Integer, String> idSentenceMap = corefOKE.getOKEFileSentences();

//        Generating and storing corefed sentence
        for (int sentId : idSentenceMap.keySet()) {
            List<String> corefedSentences = corefOKE.getCorefedSentences(idSentenceMap.get(sentId));
            for (String corefSent : corefedSentences) {
                corefOKE.storeOKECorefSentence(sentId, corefSent);
            }
        }

//        Retrieving corefed sentences and getting NER map
        HashMap<Integer, String> idCorefSentenceMap = corefOKE.getOKECorefSentences();
        for (int sentId : idCorefSentenceMap.keySet()) {
            String dbpediaUrl = "http://localhost:8080/nlp/ner/DBpedia/";
            String stanfordUrl = "http://localhost:8080/nlp/ner/stanford/";

            HashMap<String, Set<String>> stanfordNERMap = corefOKE.getNERMap(idCorefSentenceMap.get(sentId), stanfordUrl);
            Set<String> toRemoveEntityTypes = new HashSet<>();
            for (String entityTpe : stanfordNERMap.keySet()) {
                if (corefOKE.mergeLocationType.contains(entityTpe)) {
                    Set<String> entities = stanfordNERMap.get(entityTpe);
                    if (stanfordNERMap.containsKey("LOCATION")) {
                        Set<String> locationEntities = stanfordNERMap.get("LOCATION");
                        locationEntities.addAll(entities);
                        stanfordNERMap.put("LOCATION", locationEntities);
                    } else {
                        stanfordNERMap.put("LOCATION", entities);
                    }
                }

//                Since you have merged. Delete unnecessary entity types
                if (!corefOKE.keepEntityTypes.contains(entityTpe)) {
                    toRemoveEntityTypes.add(entityTpe);
                }
            }

            for (String toRemove : toRemoveEntityTypes) {
                stanfordNERMap.remove(toRemove);
            }

            int entityCount = 0;
            for (String entityTpe : stanfordNERMap.keySet()) {
                entityCount = entityCount + stanfordNERMap.get(entityTpe).size();
            }

            if (entityCount <= 1) {
                HashMap<String, Set<String>> dbpediaNERMap = corefOKE.getNERMap(idCorefSentenceMap.get(sentId), dbpediaUrl);
                for (String validEntityTpe : corefOKE.keepEntityTypes) {
                    if (dbpediaNERMap.containsKey(validEntityTpe)) {
                        if (stanfordNERMap.containsKey(validEntityTpe)) {
                            Set<String> entities = stanfordNERMap.get(validEntityTpe);
                            entities.addAll(dbpediaNERMap.get(validEntityTpe));
                            stanfordNERMap.put(validEntityTpe, entities);
                        } else {
                            stanfordNERMap.put(validEntityTpe, dbpediaNERMap.get(validEntityTpe));
                        }
                    }
                }
            }
            corefOKE.updateNERInDB(stanfordNERMap, sentId);

            System.out.println(sentId + ":\t" + idCorefSentenceMap.get(sentId));
            System.out.println("NER Combined:\t" + stanfordNERMap);

            System.out.println();
        }
    }
}
