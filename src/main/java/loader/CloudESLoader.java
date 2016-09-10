package loader;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.shield.ShieldPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by samujjal on 10/09/16.
 */
public class CloudESLoader {
    static Logger LOGGER = LoggerFactory.getLogger(CloudESLoader.class);
    Client client;
    public void startup() throws UnknownHostException {
        // Build the settings for our client.
        String clusterId = "4e28cdda633998771c6875ad53211aaf"; // Your cluster ID here
        String region = "us-east-1"; // Your region here
        boolean enableSsl = true;

        Settings settings = Settings.settingsBuilder()
                .put("transport.ping_schedule", "5s")
                //.put("transport.sniff", false) // Disabled by default and *must* be disabled.
                .put("cluster.name", clusterId)
                .put("action.bulk.compress", false)
                .put("shield.transport.ssl", enableSsl)
                .put("request.headers.X-Found-Cluster", clusterId)
                .put("shield.user", "ashwani:bahutlatehotum") // your shield username and password
                .build();

        String hostname = clusterId + "." + region + ".aws.found.io";
// Instantiate a TransportClient and add the cluster to the list of addresses to connect to.
// Only port 9343 (SSL-encrypted) is currently supported.
        client = TransportClient.builder()
                .addPlugin(ShieldPlugin.class)
                .settings(settings)
                .build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostname), 9243));
    }

    public void insertDocument(String json, String indexName, String indexType){
        IndexResponse response = client.prepareIndex(indexName, indexType)
                .setSource(json)
                .get();

        LOGGER.info("Index: {}", response.getIndex());
        LOGGER.info("Type: {}", response.getType());
        LOGGER.info("ID: {}", response.getId());
        LOGGER.info("Version: {}", response.getVersion());
        LOGGER.info("IsCreated: {}", response.isCreated());
    }

    public void shutdown(){
        if(client != null){
            client.close();
        }
    }


}
