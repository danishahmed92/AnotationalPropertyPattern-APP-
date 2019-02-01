package config;

import org.ini4j.Ini;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

public class IniConfig {
    public static IniConfig configInstance;
    public HashSet<String> stopWordsSet = new HashSet<>();

    static {
        try {
            configInstance = new IniConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String dptAnnotation1;
    public String dptAnnotation2;
    public String okeAnnotation;
    public String okeTrainDirectory;
    public String wordNet;

    public String dbHost;
    public String dbPort;
    public String database;
    public String dbUser;
    public String dbPassword;

    public String sparql;
    public int numRelationsPerProperty;

    public String esIp;
    public int esPort;
    public String esDataset;
    public String esDSType;

    /**
     * reading configuration from sameAs.ini
     * and set variables that are globally required
     * @throws IOException
     */
    private IniConfig() throws IOException {
        String CONFIG_FILE = "systemConfig.ini";
        Ini configIni = new Ini(IniConfig.class.getClassLoader().getResource(CONFIG_FILE));

        wordNet = configIni.get("data", "wordNet");
        String stopWords = configIni.get("data", "stopWords");
        dptAnnotation1 = configIni.get("data", "dptAnnotation1");
        dptAnnotation2 = configIni.get("data", "dptAnnotation2");
        okeAnnotation = configIni.get("data", "okeAnnotation");
        okeTrainDirectory = configIni.get("data", "okeTrainDirectory");

        dbHost = configIni.get("mysql", "dbHost");
        dbPort = configIni.get("mysql", "port");
        database = configIni.get("mysql", "database");
        dbUser = configIni.get("mysql", "dbUser");
        dbPassword = configIni.get("mysql", "dbPassword");

        sparql = configIni.get("environment", "sparql");
        numRelationsPerProperty = Integer.parseInt(configIni.get("environment", "numRelationsPerProperty"));

        esIp = configIni.get("elasticSearch", "ip");
        esPort = Integer.parseInt(configIni.get("elasticSearch", "port"));
        esDataset = configIni.get("elasticSearch", "indexDataset");
        esDSType = configIni.get("elasticSearch", "indexType");

//        loadStopWords(stopWords);
    }

    private void loadStopWords(String stopWordsPath) {
        try {
            BufferedReader input = new BufferedReader(new FileReader(stopWordsPath));
            String word;

            while ((word = input.readLine()) != null)
                stopWordsSet.add(word);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
