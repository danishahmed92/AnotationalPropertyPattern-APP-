package datacollection;

import config.IniConfig;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author DANISH AHMED on 1/10/2019
 */
public class ElasticSearch {
    public static ElasticSearch elasticSearchInstance;
    static {
        elasticSearchInstance = new ElasticSearch();
    }
    private TransportClient client;

    public ElasticSearch() {
        IniConfig config = IniConfig.configInstance;
        Settings settings = Settings.builder().put("client.transport.ignore_cluster_name", true)
//                .put( "cluster.name", "elasticsearch" )
                .put("client.transport.sniff", true)
                .build();
        client = new PreBuiltTransportClient(settings);
        try {
            client.addTransportAddress(new TransportAddress(InetAddress.getByName(config.esIp), config.esPort));
            clusterAlive();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private void clusterAlive() {
        ClusterHealthResponse response = client.admin().cluster().prepareHealth()
                .setWaitForGreenStatus()
                .setTimeout(TimeValue.timeValueSeconds(1))
                .execute().actionGet();
        if (!response.isTimedOut()) {
            Boolean clusterHealthy = true;
        }
    }

    public GetResponse getDocumentById(String uri) {
        IniConfig config = IniConfig.configInstance;
        String index = config.esDataset;
        String type = config.esDSType;
        return client.prepareGet(index, type, uri).get();
    }
}
