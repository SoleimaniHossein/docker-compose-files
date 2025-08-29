package soleimanihossein.keycloak.es;

import org.keycloak.Config;

import java.util.Map;

public class ElasticsearchConfig {
  private final String host;
  private final int port;
  private final String scheme;
  private final String username;
  private final String password;
  private final String indexPrefix;
  private final int bulkSize;
  private final int connectTimeout;
  private final int socketTimeout;
  private final int maxRetryTimeout;

  public ElasticsearchConfig(Config.Scope config) {
    this.host = config.get("host", "localhost");
    this.port = config.getInt("port", 9200);
    this.scheme = config.get("scheme", "http");
    this.username = config.get("username");
    this.password = config.get("password");
    this.indexPrefix = config.get("indexPrefix", "keycloak-events");
    this.bulkSize = config.getInt("bulkSize", 100);
    this.connectTimeout = config.getInt("connectTimeout", 5000);
    this.socketTimeout = config.getInt("socketTimeout", 10000);
    this.maxRetryTimeout = config.getInt("maxRetryTimeout", 30000);
  }

  // Getters
  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getScheme() {
    return scheme;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getIndexPrefix() {
    return indexPrefix;
  }

  public int getBulkSize() {
    return bulkSize;
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  public int getSocketTimeout() {
    return socketTimeout;
  }

  public int getMaxRetryTimeout() {
    return maxRetryTimeout;
  }

  public String getConnectionString() {
    return scheme + "://" + host + ":" + port;
  }
}
