/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 * @author Netzbegruenung e.V.
 * @author verdigado eG
 */

package netzbegruenung.keycloak.authenticator.gateway;

import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;

import org.jboss.logging.Logger;
import java.util.Base64;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;

public class ApiSmsService implements SmsService{

	private final String apitokenurl;
	private final String apitokenscope;
	private final String apiurl;
	private final Boolean urlencode;

	private final String apitoken;
	private final String apiuser;

	private final String senderId;
	private final String countrycode;

	private final String apitokenattribute;
	private final String messageattribute;
	private final String priorityattribute;
	private final String ttlattribute;
	private final String receiverattribute;
	private final String senderattribute;

	private final boolean hideResponsePayload;
	private final String prefixToken = "Bearer ";
	private static final Logger logger = Logger.getLogger(SmsServiceFactory.class);

	ApiSmsService(Map<String, String> config) {
		apitokenurl = config.get("apitokenurl");
		apitokenscope = config.get("apitokenscope");
		apiurl = config.get("apiurl");

		urlencode = Boolean.parseBoolean(config.getOrDefault("urlencode", "false"));

		apitoken = config.getOrDefault("apitoken", "");
		apiuser = config.getOrDefault("apiuser", "");

		countrycode = config.getOrDefault("countrycode", "");
		senderId = config.get("senderId");

		apitokenattribute = config.getOrDefault("apitokenattribute", "");
		messageattribute = config.get("messageattribute");
		priorityattribute = config.get("priorityattribute");
		ttlattribute = config.get("ttlattribute");
		receiverattribute = config.get("receiverattribute");
		senderattribute = config.get("senderattribute");

		hideResponsePayload = Boolean.parseBoolean(config.get("hideResponsePayload"));

	}

	public void send(String phoneNumber, String message) {
		phoneNumber = clean_phone_number(phoneNumber, countrycode);
		Builder request_builder;
		HttpRequest request = null;
		var client = HttpClient.newHttpClient();
		try {
			if (urlencode) {
				request_builder = urlencoded_request(phoneNumber, message);
			} else {
//				request_builder = json_request(phoneNumber, message);
				request_builder = text_plain_request(phoneNumber, message,priorityattribute,ttlattribute);
			}
			if (!Objects.equals(apiuser, "")) {
				request = request_builder.setHeader("Authorization", get_auth_header(apiuser, apitoken)).build();
			} else {
				request = request_builder.build();
			}
			if(!Objects.equals(apitokenurl, "")){
				TokenManager.API_TOKEN_URL = apitokenurl;
				TokenManager.API_TOKEN_SCOPE = apitokenscope;
				TokenManager.API_BASIC_AUTH_HEADER = get_auth_header(apiuser,apitoken);
				request = request_builder.setHeader("Authorization", prefixToken + TokenManager.getAccessToken()).build();
			}

			logger.infof("info =========> request uri: %s, request bodyPublisher: %s", request,request.bodyPublisher());

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			int statusCode = response.statusCode();
			String payload = hideResponsePayload ? "redacted" : "Response: " + response.body();
			logger.infof("info =========> status code: %s, payload size: %s", statusCode, payload.length());

			if (statusCode >= 200 && statusCode < 300) {
				logger.infof("Sent SMS to %s [%s]", phoneNumber, payload);
			} else {
				logger.errorf("Failed to send message to %s [%s]. Validate your config.", phoneNumber, payload);
			}
		} catch (Exception e){
			logger.errorf(e, "Failed to send message to %s with request: %s. Validate your config.", phoneNumber, request != null ? request.toString() : "null");
		}
	}

	public Builder text_plain_request(String phoneNumber, String message,String priority, String ttl) {
		String sendJson = "{"
			.concat(String.format("\"%s\":\"%s\",", messageattribute, message))
			.concat(String.format("\"%s\":\"%s\",", receiverattribute, phoneNumber))
			.concat(String.format("\"%s\":\"%s\",", priorityattribute, priority))
			.concat(String.format("\"%s\":\"%s\",", ttlattribute, ttl))
			.concat("}");


		return HttpRequest.newBuilder()
			.uri(URI.create(apiurl))
			.header("Content-Type", "text/plain")
			.header("Accept", "*/*")
			.POST(HttpRequest.BodyPublishers.ofString(sendJson));
	}

	public Builder json_request(String phoneNumber, String message) {
		String sendJson = "{"
			.concat(apitokenattribute != "" ? String.format("\"%s\":\"%s\",", apitokenattribute, apitoken): "")
			.concat(String.format("\"%s\":\"%s\",", messageattribute, message))
			.concat(String.format("\"%s\":\"%s\",", receiverattribute, phoneNumber))
			.concat(String.format("\"%s\":\"%s\"", senderattribute, senderId))
			.concat("}");

		 return HttpRequest.newBuilder()
			.uri(URI.create(apiurl))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(sendJson));
	}

	public Builder urlencoded_request(String phoneNumber, String message) throws JsonProcessingException {
		String body = ""
			.concat(apitokenattribute != "" ? String.format("%s=%s&", apitokenattribute, URLEncoder.encode(apitoken, Charset.defaultCharset())) : "" )
			.concat(String.format("%s=%s&", messageattribute, URLEncoder.encode(message, Charset.defaultCharset())))
			.concat(String.format("%s=%s&", receiverattribute, URLEncoder.encode(phoneNumber, Charset.defaultCharset())))
			.concat(String.format("%s=%s", senderattribute, URLEncoder.encode(senderId, Charset.defaultCharset())));

		return HttpRequest.newBuilder()
				.uri(URI.create(apiurl))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body));
	}

	private static String get_auth_header(String apiuser, String apitoken) {
		String authString = apiuser + ":" + apitoken;
		String b64_cred = new String(Base64.getEncoder().encode(authString.getBytes()));
		return "Basic " + b64_cred;
	}

	private static String clean_phone_number(String phone_number, String countrycode) {
		/*
		 * This function tries to correct several common user errors. If there is no default country
		 * prefix, this function does not dare to touch the phone number.
		 * https://en.wikipedia.org/wiki/List_of_mobile_telephone_prefixes_by_country
		 */
		if (countrycode == "") {
			logger.infof("Clean phone number: no country code set, return %s", phone_number);
			return phone_number;
		}
		String country_number = countrycode.replaceFirst("\\+", "");
		// convert 49 to +49
		if (phone_number.startsWith(country_number)) {
			phone_number = phone_number.replaceFirst(country_number, countrycode);
			logger.infof("Clean phone number: convert 49 to +49, set phone number to %s", phone_number);
		}
		// convert 0049 to +49
		if (phone_number.startsWith("00"+country_number)) {
			phone_number = phone_number.replaceFirst("00"+country_number, countrycode);
			logger.infof("Clean phone number: convert 0049 to +49, set phone number to %s", phone_number);
		}
		// convert +490176 to +49176
		if (phone_number.startsWith(countrycode+"0")) {
			phone_number = phone_number.replaceFirst("\\+"+country_number+"0", countrycode);
			logger.infof("Clean phone number: convert +490176 to +49176, set phone number to %s", phone_number);
		}
		// convert 0 to +49
		if (phone_number.startsWith("0")) {
			phone_number = phone_number.replaceFirst("0", countrycode);
			logger.infof("Clean phone number: convert 0 to +49, set phone number to %s", phone_number);
		}
		return phone_number;
	}
}
