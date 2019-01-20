package datacollection;

import config.Database;
import org.apache.jena.query.QuerySolution;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author DANISH AHMED on 1/12/2019
 */
public class DataStorage {
    private String propertyTripleInsertQuery = "INSERT INTO `property_triple` (`property_uri`, `subj_uri`, `subj_label`, `obj_uri`, `obj_label`) " +
            "VALUES (?, ?, ?, ?, ?);";
    private String insertSentenceQuery = "INSERT INTO `property_sentence` (`id_prop_triple`, `property_uri`, `sentence`) " +
            "VALUES (?, ?, ?);";
    private String updateSentenceQuery = "UPDATE `property_sentence` SET `sentence` = ? WHERE `id_property_sentence` = ?;";
    private String updateAnnotationFileQuery = "UPDATE `property_sentence` SET `annotated_doc` = ? WHERE `id_property_sentence` = ?;";
    private String updateCorefAnnotationFileQuery = "UPDATE `property_sentence_coref` SET `annotated_doc` = ? WHERE `id_ps_coref` = ?;";
    private String insertRefinedSentenceQuery = "INSERT INTO `property_sentence_coref` (`id_prop_sentence`, `id_prop_triple`, `property_uri`, `sentence`) " +
            "VALUES (?, ?, ?, ?);";

    protected void storePropertyTriple(QuerySolution soln, String property) {
        String subUri = soln.getResource("s").toString();
        String objUri = soln.getResource("o").toString();

        if (!subUri.contains("http://dbpedia.org/resource/") || !objUri.contains("http://dbpedia.org/resource/"))
            return;

        subUri = subUri.replaceAll("http://dbpedia.org/resource/", "");
        objUri = objUri.replaceAll("http://dbpedia.org/resource/", "");

        String subLabel = soln.getLiteral("subLabel").toString();
        String objLabel = soln.getLiteral("objLabel").toString();

        subLabel = subLabel.substring(0, subLabel.length() - 3);
        objLabel = objLabel.substring(0, objLabel.length() - 3);

        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(propertyTripleInsertQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setString(1, property);
            prepareStatement.setString(2, subUri);
            prepareStatement.setString(3, subLabel);
            prepareStatement.setString(4, objUri);
            prepareStatement.setString(5, objLabel);

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void insertSentenceToDB(int propTripleId, String property, String sentence) {
        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(insertSentenceQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setInt(1, propTripleId);
            prepareStatement.setString(2, property);
            prepareStatement.setString(3, sentence);

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void updateTripleSentence(int sentenceId, String sentence) {
        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(updateSentenceQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setString(1, sentence);
            prepareStatement.setInt(2, sentenceId);

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void updateAnnotationFile(int sentenceId, String annotatedFile) {
        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(updateAnnotationFileQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setString(1, annotatedFile);
            prepareStatement.setInt(2, sentenceId);

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void updateCorefAnnotationFile(int sentenceId, String annotatedFile) {
        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(updateCorefAnnotationFileQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setString(1, annotatedFile);
            prepareStatement.setInt(2, sentenceId);

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void insertRefinedSentenceToDB(int sentenceId, int propTripleId, String property, String sentence) {
        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(insertRefinedSentenceQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setInt(1, sentenceId);
            prepareStatement.setInt(2, propTripleId);
            prepareStatement.setString(3, property);
            prepareStatement.setString(3, sentence);

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
