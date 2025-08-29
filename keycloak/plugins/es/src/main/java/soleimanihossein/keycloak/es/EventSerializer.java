package soleimanihossein.keycloak.es;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;

import java.util.HashMap;
import java.util.Map;

public class EventSerializer {
  private static final ObjectMapper mapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  public static String serialize(Event event) throws JsonProcessingException {
    Map<String, Object> eventMap = new HashMap<>();
    eventMap.put("type", "user_event");
    eventMap.put("time", event.getTime());
    eventMap.put("realmId", event.getRealmId());
    eventMap.put("clientId", event.getClientId());
    eventMap.put("userId", event.getUserId());
    eventMap.put("sessionId", event.getSessionId());
    eventMap.put("ipAddress", event.getIpAddress());
    eventMap.put("eventType", event.getType().name());
    eventMap.put("details", event.getDetails());
    eventMap.put("error", event.getError());
    return mapper.writeValueAsString(eventMap);
  }

  public static String serialize(AdminEvent event) throws JsonProcessingException {
    Map<String, Object> eventMap = new HashMap<>();
    eventMap.put("type", "admin_event");
    eventMap.put("time", event.getTime());
    eventMap.put("realmId", event.getRealmId());
    eventMap.put("authDetails", event.getAuthDetails());
    eventMap.put("resourceType", event.getResourceType().name());
    eventMap.put("operationType", event.getOperationType().name());
    eventMap.put("resourcePath", event.getResourcePath());
    eventMap.put("representation", event.getRepresentation());
    eventMap.put("error", event.getError());
    return mapper.writeValueAsString(eventMap);
  }
}
