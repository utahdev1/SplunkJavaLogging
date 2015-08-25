package com.splunk.logging;

import java.net.URI;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.ConnectingIOReactor;

/**
 * Common HEC logic shared by all appenders/handlers
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */

public class SplunkHECInput extends SplunkInput {

	// connection props
	private HECTransportConfig config;

	private CloseableHttpAsyncClient httpClient;
	private URI uri;

	public SplunkHECInput(HECTransportConfig config) throws Exception {

		this.config = config;

		ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
		PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(
				ioReactor);
		cm.setMaxTotal(config.getPoolsize());

		HttpHost splunk = new HttpHost(config.getHost(), config.getPort());
		cm.setMaxPerRoute(new HttpRoute(splunk), config.getPoolsize());

		httpClient = HttpAsyncClients.custom().setConnectionManager(cm).build();

		uri = new URIBuilder().setScheme(config.isHttps() ? "https" : "http")
				.setHost(config.getHost()).setPort(config.getPort())
				.setPath("/services/collector").build();

		openStream();

	}

	/**
	 * open the stream
	 * 
	 */
	private void openStream() throws Exception {

		httpClient.start();

	}

	/**
	 * close the stream
	 */
	public void closeStream() {
		try {

			httpClient.close();
		} catch (Exception e) {
		}
	}

	private String wrapMessageInQuotes(String message) {

		return "\"" + message + "\"";
	}

	/**
	 * send an event via stream
	 * 
	 * @param message
	 */
	public void streamEvent(String message) {

		String currentMessage = message;
		try {

			if (!(message.startsWith("{") && message.endsWith("}"))
					&& !(message.startsWith("\"") && message.endsWith("\"")))
				message = wrapMessageInQuotes(message);

			// could use a JSON Object , but the JSON is so trivial , just
			// building it with a StringBuffer
			StringBuffer json = new StringBuffer();
			json.append("{\"").append("event\":").append(message).append(",\"")
					.append("index\":\"").append(config.getIndex())
					.append("\",\"").append("source\":\"")
					.append(config.getSource()).append("\",\"")
					.append("sourcetype\":\"").append(config.getSourcetype())
					.append("\"").append("}");

			HttpPost post = new HttpPost(uri);
			post.addHeader("Authorization", "Splunk " + config.getToken());

			StringEntity requestEntity = new StringEntity(json.toString(),
					ContentType.create("application/json", "UTF-8"));

			post.setEntity(requestEntity);
			httpClient.execute(post, null);

		} catch (Exception e) {

			// something went wrong , put message on the queue for retry
			enqueue(currentMessage);
			try {
				closeStream();
			} catch (Exception e1) {
			}

			try {
				openStream();
			} catch (Exception e2) {
			}
		}
	}
}
