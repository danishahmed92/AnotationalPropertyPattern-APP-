package properties;

import config.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @author DANISH AHMED on 1/10/2019
 */
public class PropertyUtils {
    public static List<String> getAllProperties() throws SQLException {
        String DISTINCT_PROPERTIES = "SELECT DISTINCT `prop_uri` FROM property ORDER BY `prop_uri` ASC";
        Statement statement = Database.databaseInstance.conn.createStatement();
        java.sql.ResultSet rs = statement.executeQuery(DISTINCT_PROPERTIES);

        List<String> properties = new LinkedList<>();
        while (rs.next())
            properties.add(rs.getString("prop_uri"));
        statement.close();
        return properties;
    }

    public static HashMap<Integer, HashMap<String, String>> getTriplesForProperty(String property) {
        HashMap<Integer, HashMap<String, String>> tripleIdDataMap = new LinkedHashMap<>();
        String queryString = String.format("SELECT * from property_triple where property_uri = \"%s\" order by id_prop_triple ASC;", property);

        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            ResultSet rs = statement.executeQuery(queryString);

            while (rs.next()) {
                int propTripleId = rs.getInt("id_prop_triple");

                String subUri = rs.getString("subj_uri");
                String subLabel = rs.getString("subj_label");
                String objUri = rs.getString("obj_uri");
                String objLabel = rs.getString("obj_label");

                HashMap<String, String> tripleData = new HashMap<>();
                tripleData.put("subUri", subUri);
                tripleData.put("subLabel", subLabel);
                tripleData.put("objUri", objUri);
                tripleData.put("objLabel", objLabel);

                tripleIdDataMap.put(propTripleId, tripleData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tripleIdDataMap;
    }

    public static HashMap<Integer, HashMap<String, String>> getSentencesForProperty(String property) {
        HashMap<Integer, HashMap<String, String>> sentenceTripleDataMap = new LinkedHashMap<>();
        String queryString = String.format("select ps.id_property_sentence, ps.id_prop_triple, pt.subj_label, pt.obj_label, sentence from property_sentence ps\n" +
                "INNER JOIN property_triple pt ON pt.id_prop_triple = ps.id_prop_triple\n" +
                "WHERE ps.property_uri = \"%s\";", property);

        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            ResultSet rs = statement.executeQuery(queryString);

            while (rs.next()) {
                int sentenceId = rs.getInt("id_property_sentence");

                int tripleId = rs.getInt("id_prop_triple");
                String subLabel = rs.getString("subj_label");
                String objLabel = rs.getString("obj_label");
                String sentence = rs.getString("sentence");

                HashMap<String, String> sentenceData = new HashMap<>();
                sentenceData.put("tripleId", String.valueOf(tripleId));
                sentenceData.put("subLabel", subLabel);
                sentenceData.put("objLabel", objLabel);
                sentenceData.put("sentence", sentence);

                sentenceTripleDataMap.put(sentenceId, sentenceData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sentenceTripleDataMap;
    }

    public static HashMap<Integer, String> getRefinedSentencesForProperty(String property) {
        HashMap<Integer, String> idSentenceMap = new LinkedHashMap<>();
        String queryString = String.format("select id_ps_coref, sentence from property_sentence_coref \n" +
                "WHERE property_uri = \"%s\";", property);

        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            ResultSet rs = statement.executeQuery(queryString);

            while (rs.next()) {
                int sentenceId = rs.getInt("id_ps_coref");
                String sentence = rs.getString("sentence");

                idSentenceMap.put(sentenceId, sentence);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return idSentenceMap;
    }

    /*public static HashMap<Integer, HashMap<String, String>> getAnnotationsForProperty(String property) {
        HashMap<Integer, HashMap<String, String>> sentenceTripleDataMap = new LinkedHashMap<>();
        String queryString = String.format("select id_property_sentence, id_prop_triple, annotated_doc from property_sentence \n" +
                "WHERE property_uri = \"%s\" and annotated_doc is not NULL;", property);

        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            ResultSet rs = statement.executeQuery(queryString);

            while (rs.next()) {
                int sentenceId = rs.getInt("id_property_sentence");
                int tripleId = rs.getInt("id_prop_triple");
                String annotatedDoc = rs.getString("annotated_doc");

                HashMap<String, String> sentenceData = new HashMap<>();
                sentenceData.put("tripleId", String.valueOf(tripleId));
                sentenceData.put("annotatedDoc", annotatedDoc);

                sentenceTripleDataMap.put(sentenceId, sentenceData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sentenceTripleDataMap;
    }*/

    public static HashMap<Integer, HashMap<String, String>> getAnnotationsForProperty(String property) {
        HashMap<Integer, HashMap<String, String>> sentenceTripleDataMap = new LinkedHashMap<>();
        String queryString = String.format("select ps.id_property_sentence, ps.id_prop_triple, pt.subj_label, pt.obj_label, ps.sentence, ps.annotated_doc from property_sentence ps\n" +
                "INNER JOIN property_triple pt ON pt.id_prop_triple = ps.id_prop_triple\n" +
                "WHERE ps.property_uri = \"%s\" and ps.annotated_doc IS NOT NULL;", property);

        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            ResultSet rs = statement.executeQuery(queryString);

            while (rs.next()) {
                int sentenceId = rs.getInt("id_property_sentence");
                int tripleId = rs.getInt("id_prop_triple");
                String annotatedDoc = rs.getString("annotated_doc");
                String subLabel = rs.getString("subj_label");
                String objLabel = rs.getString("obj_label");
                String sentence = rs.getString("sentence");

                HashMap<String, String> sentenceData = new HashMap<>();
                sentenceData.put("tripleId", String.valueOf(tripleId));
                sentenceData.put("annotatedDoc", annotatedDoc);
                sentenceData.put("subLabel", subLabel);
                sentenceData.put("objLabel", objLabel);
                sentenceData.put("sentence", sentence);

                sentenceTripleDataMap.put(sentenceId, sentenceData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sentenceTripleDataMap;
    }

    public static HashMap<Integer, HashMap<String, String>> getCorefSentencesForProperty(String property) {
        HashMap<Integer, HashMap<String, String>> sentenceTripleDataMap = new LinkedHashMap<>();
        String queryString = String.format("select id_ps_coref, id_prop_triple, sentence from property_sentence_coref \n" +
                "WHERE property_uri = \"%s\";", property);

        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            ResultSet rs = statement.executeQuery(queryString);

            while (rs.next()) {
                int sentenceId = rs.getInt("id_ps_coref");

                int tripleId = rs.getInt("id_prop_triple");
                String sentence = rs.getString("sentence");

                HashMap<String, String> sentenceData = new HashMap<>();
                sentenceData.put("tripleId", String.valueOf(tripleId));
                sentenceData.put("sentence", sentence);

                sentenceTripleDataMap.put(sentenceId, sentenceData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sentenceTripleDataMap;
    }

}
