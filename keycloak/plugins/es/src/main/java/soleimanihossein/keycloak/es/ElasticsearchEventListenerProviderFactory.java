package soleimanihossein.keycloak.es;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchEventListenerProviderFactory implements EventListenerProviderFactory {
  private static final Logger logger = LoggerFactory.getLogger(ElasticsearchEventListenerProviderFactory.class);
  private static final String PROVIDER_ID = "elasticsearch-event-listener";

  private ElasticsearchConfig config;
  private RestHighLevelClient client;

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    return new ElasticsearchEventListenerProvider(client, config);
  }

  @Override
  public void init(Config.Scope configScope) {
    this.config = new ElasticsearchConfig(configScope);
    this.client = ElasticsearchClientFactory.createClient(this.config);
    logger.info("Initialized Elasticsearch event listener with config: {}", this.config.getConnectionString());
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    // No post-init actions needed
  }

  @Override
  public void close() {
    try {
      ElasticsearchClientFactory.close();
    } catch (Exception e) {
      logger.error("Failed to close Elasticsearch client", e);
    }
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }
}
