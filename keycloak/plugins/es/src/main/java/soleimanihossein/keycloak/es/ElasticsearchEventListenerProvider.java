package soleimanihossein.keycloak.es;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ElasticsearchEventListenerProvider implements EventListenerProvider {
  private static final Logger logger = LoggerFactory.getLogger(ElasticsearchEventListenerProvider.class);

  private final RestHighLevelClient client;
  private final String indexPrefix;
  private final BlockingQueue<IndexRequest> eventQueue;
  private final Thread processingThread;
  private volatile boolean running = true;
  private final int bulkSize;

  public ElasticsearchEventListenerProvider(RestHighLevelClient client, ElasticsearchConfig config) {
    this.client = client;
    this.indexPrefix = config.getIndexPrefix();
    this.bulkSize = config.getBulkSize();
    this.eventQueue = new LinkedBlockingQueue<>(1000);

    this.processingThread = new Thread(this::processEvents);
    this.processingThread.setName("Keycloak-Elasticsearch-Processor");
    this.processingThread.setDaemon(true);
    this.processingThread.start();
  }

  @Override
  public void onEvent(Event event) {
    try {
      String json = EventSerializer.serialize(event);
      String indexName = indexPrefix + "-" + getDateSuffix(event.getTime());

      IndexRequest request = new IndexRequest(indexName)
          .source(json, XContentType.JSON);

      if (!eventQueue.offer(request, 100, TimeUnit.MILLISECONDS)) {
        logger.warn("Event queue full, dropping event: {}", event.getType());
      }
    } catch (Exception e) {
      logger.error("Failed to process event", e);
    }
  }

  @Override
  public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
    try {
      String json = EventSerializer.serialize(adminEvent);
      String indexName = indexPrefix + "-admin-" + getDateSuffix(adminEvent.getTime());

      IndexRequest request = new IndexRequest(indexName)
          .source(json, XContentType.JSON);

      if (!eventQueue.offer(request, 100, TimeUnit.MILLISECONDS)) {
        logger.warn("Event queue full, dropping admin event: {}", adminEvent.getOperationType());
      }
    } catch (Exception e) {
      logger.error("Failed to process admin event", e);
    }
  }

  private String getDateSuffix(long timestamp) {
    // Implement date-based index suffix (e.g., "yyyy.MM.dd")
    return java.time.Instant.ofEpochMilli(timestamp)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .toString()
        .replace("-", ".");
  }

  private void processEvents() {
    BulkRequest bulkRequest = new BulkRequest();

    while (running || !eventQueue.isEmpty()) {
      try {
        IndexRequest request = eventQueue.poll(1, TimeUnit.SECONDS);
        if (request != null) {
          bulkRequest.add(request);
        }

        if (bulkRequest.numberOfActions() >= bulkSize ||
            (bulkRequest.numberOfActions() > 0 && eventQueue.isEmpty())) {
          sendBulkRequest(bulkRequest);
          bulkRequest = new BulkRequest();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        logger.error("Error processing events", e);
      }
    }

    // Send any remaining events
    if (bulkRequest.numberOfActions() > 0) {
      sendBulkRequest(bulkRequest);
    }
  }

  private void sendBulkRequest(BulkRequest bulkRequest) {
    try {
      client.bulkAsync(bulkRequest, RequestOptions.DEFAULT, new ActionListener<BulkResponse>() {
        @Override
        public void onResponse(BulkResponse bulkResponse) {
          if (bulkResponse.hasFailures()) {
            logger.error("Bulk request had failures: {}", bulkResponse.buildFailureMessage());
          }
        }

        @Override
        public void onFailure(Exception e) {
          logger.error("Failed to send bulk request", e);
        }
      });
    } catch (Exception e) {
      logger.error("Failed to send bulk request", e);
    }
  }

  @Override
  public void close() {
    running = false;
    processingThread.interrupt();
    try {
      processingThread.join(5000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
