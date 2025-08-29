package soleimanihossein.keycloak.es;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.keycloak.Config;

import java.io.IOException;
import java.util.Objects;

public class ElasticsearchClientFactory {
  private static RestHighLevelClient client;

  public static synchronized RestHighLevelClient createClient(ElasticsearchConfig config) {
    if (client != null) {
      return client;
    }

    RestClientBuilder builder = RestClient.builder(
        new HttpHost(config.getHost(), config.getPort(), config.getScheme()));

    // Configure credentials if provided
    if (config.getUsername() != null && config.getPassword() != null) {
      final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(AuthScope.ANY,
          new UsernamePasswordCredentials(config.getUsername(), config.getPassword()));

      builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
          .setDefaultCredentialsProvider(credentialsProvider));
    }

    // Configure timeouts
    builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
        .setConnectTimeout(config.getConnectTimeout())
        .setSocketTimeout(config.getSocketTimeout()));

    client = new RestHighLevelClient(builder);

    return client;
  }

  public static synchronized void close() throws IOException {
    if (client != null) {
      client.close();
      client = null;
    }
  }
}
