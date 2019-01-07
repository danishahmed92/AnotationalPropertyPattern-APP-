package properties;

import config.Database;
import config.IniConfig;
import utils.Utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class PropertyAnnotations {
    public static HashMap<String, HashMap<String, String>> getAnnotationLabelMap(String propertyUri) {
        HashMap<String, HashMap<String, String>> annotationLabelMap = new LinkedHashMap<>();
        final String QUERY_TRIPLE_LABELS_FOR_PROPERTY = "SELECT psr.id_ps_refined, pt.id_prop_triple, psr.property_uri, pt.subj_label, pt.obj_label from property_sentence_refined AS psr \n" +
                "INNER JOIN property_triple as pt ON psr.id_prop_triple = pt.id_prop_triple \n" +
                "WHERE psr.property_uri = '%s' \n" +
                "ORDER BY psr.property_uri, pt.id_prop_triple;";

        String query = String.format(QUERY_TRIPLE_LABELS_FOR_PROPERTY, propertyUri);
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                String annotationFile = String.format("%s_%s_%s", propertyUri, rs.getString("id_prop_triple"), rs.getString("id_ps_refined"));
                String subjLabel = rs.getString("subj_label");
                String objLabel = rs.getString("obj_label");

                HashMap<String, String> annotationLabelAttrMap = new HashMap<>();
                annotationLabelAttrMap.put("subjLabel", subjLabel);
                annotationLabelAttrMap.put("objLabel", objLabel);

                annotationLabelMap.put(annotationFile, annotationLabelAttrMap);
            }
            return annotationLabelMap;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return annotationLabelMap;
    }

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

    public static List<String> getAnnotationFilesForProperty(String propertyUri) {
        String annotationFolder = IniConfig.configInstance.dptAnnotation2 + propertyUri + "/";
        if (Files.exists(Paths.get(annotationFolder))) {
            return Utils.getFilesInDirectory(annotationFolder);
        }
        return null;
    }
}
