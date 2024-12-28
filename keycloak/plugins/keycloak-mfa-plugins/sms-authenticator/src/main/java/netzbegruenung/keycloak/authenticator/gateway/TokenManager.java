package netzbegruenung.keycloak.authenticator.gateway;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.jboss.logging.Logger;

public class TokenManager {
	private static final Logger logger = Logger.getLogger(SmsServiceFactory.class);

	public static String API_TOKEN_URL = "https://example.com/oauth/token";
	public static String API_BASIC_AUTH_HEADER = "user:pass";
	public static String API_TOKEN_SCOPE = "scope";
	private final static String API_TOKEN_URL_CONTENT_TYPE = "application/x-www-form-urlencoded";
	private final static String API_TOKEN_URL_GRANT_TYPE = "client_credentials";

	private static final ConcurrentHashMap<String, TokenData> tokenCache = new ConcurrentHashMap<>();

	public static synchronized String getAccessToken() throws IOException, InterruptedException {
		TokenData cachedToken = tokenCache.get("accessToken");

		// Check if cached token exists and is still valid
		if (cachedToken != null && Instant.now().isBefore(cachedToken.getExpiry())) {
			logger.infof("access token cached =========> %s", cachedToken.getToken());
			return cachedToken.getToken();
		}

		String formEncodedBody = "grant_type=" + URLEncoder.encode(API_TOKEN_URL_GRANT_TYPE, Charset.defaultCharset())
			+ "&scope=" + URLEncoder.encode(API_TOKEN_SCOPE, Charset.defaultCharset());

		// No valid cached token, request a new one
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(API_TOKEN_URL))
			.header("Content-Type", API_TOKEN_URL_CONTENT_TYPE)
			.header("Authorization", API_BASIC_AUTH_HEADER)
			.POST(HttpRequest.BodyPublishers.ofString(formEncodedBody))
			.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		int statusCode = response.statusCode();
		if (statusCode != 200) {
			throw new IOException("Failed to get token. Status code: " + statusCode);
		}

		String responseBody = response.body();
		try (JsonReader jsonReader = Json.createReader(new StringReader(responseBody))) {
			JsonObject jsonResponse = jsonReader.readObject();

			if (jsonResponse.containsKey("access_token") && jsonResponse.containsKey("expires_in")) {
				String accessToken = jsonResponse.getString("access_token");
				int expiresIn = jsonResponse.getInt("expires_in");

				Instant expiryTime = Instant.now().plusSeconds(expiresIn);

				tokenCache.put("accessToken", new TokenData(accessToken, expiryTime));

				logger.infof("access token =========> %s", accessToken);
				return accessToken;
			} else {
				throw new IOException("Required token fields not found in the response");
			}
		}
	}

	private static class TokenData {
		private final String token;
		private final Instant expiry;

		public TokenData(String token, Instant expiry) {
			this.token = token;
			this.expiry = expiry;
		}

		public String getToken() {
			return token;
		}

		public Instant getExpiry() {
			return expiry;
		}
	}
}
