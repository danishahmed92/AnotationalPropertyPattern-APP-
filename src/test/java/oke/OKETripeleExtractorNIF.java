package oke;

import config.Database;
import config.IniConfig;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
import utils.Utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author DANISH AHMED on 1/30/2019
 */
public class OKETripeleExtractorNIF {
    private final String TRIPLE_LABEL_SENTENCE_QUERY = "prefix dbo:   <http://dbpedia.org/ontology/> \n" +
            "prefix oa:    <http://www.w3.org/ns/oa#> \n" +
            "prefix aksw:  <http://aksw.org/notInWiki/> \n" +
            "prefix dbr:   <http://dbpedia.org/resource/> \n" +
            "prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
            "prefix xsd:   <http://www.w3.org/2001/XMLSchema#> \n" +
            "prefix itsrdf: <http://www.w3.org/2005/11/its/rdf#> \n" +
            "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
            "prefix nif:   <http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#> \n" +

            "SELECT ?subjLabel ?subjType ?pred ?objLabel ?objType ?sentence WHERE {" +
                "[] nif:isString ?sentence . \n" +
                "?triple oa:hasTarget ?target . \n" +
                "?triple rdf:subject ?subj . \n" +
                "?triple rdf:predicate ?pred . \n" +
                "?triple rdf:object ?obj . \n" +

                "?subjDetail itsrdf:taIdentRef ?subj . \n" +
                "?subjDetail nif:anchorOf ?subjLabel . \n" +
                "?subjDetail itsrdf:taClassRef ?subjType . \n" +

                "?objDetail itsrdf:taIdentRef ?obj . \n" +
                "?objDetail nif:anchorOf ?objLabel . \n" +
                "?objDetail itsrdf:taClassRef ?objType . \n" +
            "}";

    public HashMap<Integer, HashMap<String, String>> getTripleFromTurtle(String filePath) {
        Model model = FileManager.get().loadModel(filePath);
        Query query = QueryFactory.create(TRIPLE_LABEL_SENTENCE_QUERY);
        HashMap<Integer, HashMap<String, String>> triplesMap = new LinkedHashMap<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            int count = 0;
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();

                Literal sentence = soln.getLiteral("sentence");
                Literal subj = soln.getLiteral("subjLabel");
                Literal obj = soln.getLiteral("objLabel");
                String pred = soln.getResource("pred").getURI();

                String subjClass = soln.getResource("subjType").getURI();
                String objClass = soln.getResource("objType").getURI();

                subjClass = subjClass.replaceAll("http://dbpedia.org/ontology/", "");
                subjClass = subjClass.replaceAll("http://dbpedia.org/resource/", "");
                objClass = objClass.replaceAll("http://dbpedia.org/ontology/", "");
                objClass = objClass.replaceAll("http://dbpedia.org/resource/", "");

                HashMap<String, String> tripleMap = new HashMap<>();

                tripleMap.put("subjLabel", subj.getLexicalForm());
                tripleMap.put("objLabel", obj.getLexicalForm());
                tripleMap.put("subjClass", subjClass);
                tripleMap.put("objClass", objClass);
                tripleMap.put("property", pred);
                tripleMap.put("sentence", sentence.getLexicalForm());

                triplesMap.put(count, tripleMap);
                count++;
            }
        }
        return triplesMap;
    }

    public void storeOKETripleToDB(String okeFile, HashMap<String, String> tripleMap) {
        String subjLabel = tripleMap.get("subjLabel");
        String objLabel = tripleMap.get("objLabel");
        String subjClass = tripleMap.get("subjClass");
        String objClass = tripleMap.get("objClass");
        String property = tripleMap.get("property").replaceAll("http://dbpedia.org/ontology/", "");
        String sentence = tripleMap.get("sentence");

        String dbQuery = "INSERT INTO `oke_triples` (`oke_file`, `subj_label`, `prop_uri`, `obj_label`, `sentence`, `subj_class`, `obj_class`)" +
                "values (?, ?, ?, ?, ?, ?, ?);";
        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(dbQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setString(1, okeFile);

            prepareStatement.setString(2, subjLabel);
            prepareStatement.setString(3, property);
            prepareStatement.setString(4, objLabel);
            prepareStatement.setString(5, sentence);
            prepareStatement.setString(6, subjClass);
            prepareStatement.setString(7, objClass);

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String okeTrainDirectory = IniConfig.configInstance.okeTrainDirectory;
        List<String> okeTrainFilesList = Utils.getFilesInDirectory(okeTrainDirectory);

        OKETripeleExtractorNIF extractorNIF = new OKETripeleExtractorNIF();
        for (String file : okeTrainFilesList) {
            HashMap<Integer, HashMap<String, String>> triplesMap = extractorNIF.getTripleFromTurtle(okeTrainDirectory + file);
            for (Integer tripleId : triplesMap.keySet()) {
                HashMap<String, String> tripleMap = triplesMap.get(tripleId);
                extractorNIF.storeOKETripleToDB(file, tripleMap);
            }
        }
    }
}
