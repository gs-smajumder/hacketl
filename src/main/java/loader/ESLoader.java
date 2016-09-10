package loader;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by samujjal on 08/09/16.
 */
public class ESLoader {
    static Logger LOGGER = LoggerFactory.getLogger(ESLoader.class);

    TransportClient client;

    public void startup() throws UnknownHostException {
        Settings settings = Settings.settingsBuilder()
                .put("cluster.name", "samujjal_elastic_dev").build();
        client = TransportClient.builder().settings(settings).build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("127.0.0.1"), 9300));

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
