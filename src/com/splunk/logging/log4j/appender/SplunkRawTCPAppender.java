package com.splunk.logging.log4j.appender;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

import com.splunk.logging.SplunkRawTCPInput;

/**
 * Log4j Appender for sending events to Splunk via Raw TCP
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class SplunkRawTCPAppender extends AppenderSkeleton {

	// connection settings
	private String host = "";
	private int port = 5150;

	// queuing settings
	private String maxQueueSize;
	private boolean dropEventsOnQueueFull;

	private SplunkRawTCPInput sri;

	private String activationKey;

	/**
	 * Constructor
	 */
	public SplunkRawTCPAppender() {
	}

	/**
	 * Constructor
	 * 
	 * @param layout
	 *            the layout to apply to the log event
	 */
	public SplunkRawTCPAppender(Layout layout) {

		this.layout = layout;
	}

	/**
	 * Log the message
	 */
	@Override
	protected void append(LoggingEvent event) {

		try {
			if (sri == null) {
				sri = new SplunkRawTCPInput(host, port, this.activationKey);
				sri.setMaxQueueSize(maxQueueSize);
				sri.setDropEventsOnQueueFull(dropEventsOnQueueFull);
			}
		} catch (Exception e) {
			errorHandler.error(
					"Couldn't establish Raw TCP connection for SplunkRawTCPAppender named \"" + this.name + "\".");
			return;
		}

		String formatted = layout.format(event);

		// send error stack traces to splunk
		if (layout.ignoresThrowable()) {
			String[] s = event.getThrowableStrRep();
			StringBuilder stackTrace = new StringBuilder();
			if (s != null) {
				int len = s.length;
				for (int i = 0; i < len; i++) {
					stackTrace.append(Layout.LINE_SEP);
					stackTrace.append(s[i]);
				}
			}
			formatted += stackTrace.toString();
		}

		sri.streamEvent(formatted);

	}

	/**
	 * Clean up resources
	 */
	@Override
	synchronized public void close() {

		closed = true;
		if (sri != null) {
			try {
				sri.closeStream();
				sri = null;
			} catch (Exception e) {
				Thread.currentThread().interrupt();
				sri = null;
			}
		}

	}

	@Override
	public boolean requiresLayout() {
		return true;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getMaxQueueSize() {
		return maxQueueSize;
	}

	public void setMaxQueueSize(String maxQueueSize) {
		this.maxQueueSize = maxQueueSize;
	}

	public boolean isDropEventsOnQueueFull() {
		return dropEventsOnQueueFull;
	}

	public void setDropEventsOnQueueFull(boolean dropEventsOnQueueFull) {
		this.dropEventsOnQueueFull = dropEventsOnQueueFull;
	}

	public String getActivationKey() {
		return this.activationKey;
	}

	public void setActivationKey(String activationKey) {
		this.activationKey = activationKey;
	}

}
