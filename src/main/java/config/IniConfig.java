package config;

import org.ini4j.Ini;

import java.io.IOException;

public class IniConfig {
    public static IniConfig configInstance;

    static {
        try {
            configInstance = new IniConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String dptAnnotation1;
    public String dptAnnotation2;
    public String wordNet;

    public String dbHost;
    public String dbPort;
    public String database;
    public String dbUser;
    public String dbPassword;

    /**
     * reading configuration from sameAs.ini
     * and set variables that are globally required
     * @throws IOException
     */
    private IniConfig() throws IOException {
        String CONFIG_FILE = "systemConfig.ini";
        Ini configIni = new Ini(IniConfig.class.getClassLoader().getResource(CONFIG_FILE));

        wordNet = configIni.get("data", "wordNet");
        dptAnnotation1 = configIni.get("data", "dptAnnotation1");
        dptAnnotation2 = configIni.get("data", "dptAnnotation2");

        dbHost = configIni.get("mysql", "dbHost");
        dbPort = configIni.get("mysql", "port");
        database = configIni.get("mysql", "database");
        dbUser = configIni.get("mysql", "dbUser");
        dbPassword = configIni.get("mysql", "dbPassword");
    }
}