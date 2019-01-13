package datacollection;

import config.Database;
import config.IniConfig;
import org.apache.jena.query.*;
import properties.PropertyUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * @author DANISH AHMED on 1/10/2019
 */
public class PropertyTripleCollector extends DataStorage{

    public void getPropertyTriples(String property, Boolean storeToDB) {
        String queryString = String.format("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX vrank:<http://purl.org/voc/vrank#>\n" +
                "PREFIX dbo:<http://dbpedia.org/ontology/>\n" +
                "\n" +
                "SELECT distinct ?s ?subLabel ?o ?objLabel\n" +
                "FROM <http://dbpedia.org> \n" +
                "FROM <http://people.aifb.kit.edu/ath/#DBpedia_PageRank> \n" +
                "WHERE {\n" +
                "  ?s dbo:%s ?o .\n" +
                "  ?s rdfs:label ?subLabel .\n" +
                "  ?o rdfs:label ?objLabel .\n" +
                "  {\n" +
                "    SELECT distinct ?s ?v WHERE {\n" +
                "      ?s dbo:%s ?o .\n" +
                "      ?s vrank:hasRank/vrank:rankValue ?v.\n" +
                "    }\n" +
                "    ORDER BY DESC(?v) \n" +
                "\tLIMIT %s\n" +
                "  }\n" +
                " \n" +
                " FILTER (lang(?subLabel) = 'en' && lang(?objLabel) = 'en') \n" +
                "}\n" +
                "ORDER BY DESC(?v)", property, property, IniConfig.configInstance.numRelationsPerProperty);

        System.out.println(queryString);
        System.out.println();

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(IniConfig.configInstance.sparql, query);

        String insertQuery = "INSERT INTO `property_triple` (`property_uri`, `subj_uri`, `subj_label`, `obj_uri`, `obj_label`) " +
                "VALUES (?, ?, ?, ?, ?);";

        try {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();

                if (storeToDB) {
                    storePropertyTriple(soln, property);
                }
//
                System.out.println(property +
                        "\t" +  soln.getResource("s").toString() +
                        "\t" + soln.getResource("o").toString());
            }
        }finally {
            System.out.println(property);
        }
    }

    public static void main(String[] args) {
        try {
            List<String> properties = PropertyUtils.getAllProperties();
            PropertyTripleCollector ptc = new PropertyTripleCollector();

            for (String property : properties) {
                ptc.getPropertyTriples(property, true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
