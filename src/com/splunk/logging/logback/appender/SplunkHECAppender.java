package com.splunk.logging.logback.appender;

import com.splunk.logging.HECTransportConfig;
import com.splunk.logging.SplunkHECInput;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;

/**
 * LogBack Appender for sending events to Splunk via HEC Endpoint
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class SplunkHECAppender extends AppenderBase<ILoggingEvent> {

	// connection settings
	private HECTransportConfig config = new HECTransportConfig();

	// queuing settings
	private String maxQueueSize;
	private boolean dropEventsOnQueueFull;

	private SplunkHECInput shi;

	private Layout<ILoggingEvent> layout;

	/**
	 * Constructor
	 */
	public SplunkHECAppender() {
	}

	/**
	 * Log the message
	 */
	@Override
	protected void append(ILoggingEvent event) {

		if (shi != null) {

			String formatted = layout.doLayout(event);

			shi.streamEvent(formatted);

		}
	}

	/**
	 * Initialisation logic
	 */
	@Override
	public void start() {

		if (this.layout == null) {
			addError("No layout set for the appender named [" + name + "].");
			return;
		}

		if (shi == null) {
			try {
				shi = new SplunkHECInput(config);
				shi.setMaxQueueSize(maxQueueSize);
				shi.setDropEventsOnQueueFull(dropEventsOnQueueFull);
			} catch (Exception e) {
				addError("Couldn't establish connection for SplunkHECAppender named \""
						+ this.name + "\".");
			}
		}
		super.start();
	}

	/**
	 * Clean up resources
	 */
	@Override
	public void stop() {
		if (shi != null) {
			try {
				shi.closeStream();
				shi = null;
			} catch (Exception e) {
				Thread.currentThread().interrupt();
				shi = null;
			}
		}
		super.stop();
	}

	public String getToken() {
		return config.getToken();
	}

	public void setToken(String token) {
		config.setToken(token);
	}

	public String getHost() {
		return config.getHost();
	}

	public void setHost(String host) {
		config.setHost(host);
	}

	public int getPort() {
		return config.getPort();
	}

	public void setPort(int port) {
		config.setPort(port);
	}

	public boolean isHttps() {
		return config.isHttps();
	}

	public void setHttps(boolean https) {
		config.setHttps(https);
	}

	public int getPoolsize() {
		return config.getPoolsize();
	}

	public void setPoolsize(int poolsize) {
		config.setPoolsize(poolsize);
	}

	public String getIndex() {
		return config.getIndex();
	}

	public void setIndex(String index) {
		config.setIndex(index);
	}

	public String getSource() {
		return config.getSource();
	}

	public void setSource(String source) {
		config.setSource(source);
	}

	public String getSourcetype() {
		return config.getSourcetype();
	}

	public void setSourcetype(String sourcetype) {
		config.setSourcetype(sourcetype);
	}

	public long getMaxBatchSizeEvents() {
		return config.getMaxBatchSizeEvents();
	}

	public void setMaxBatchSizeEvents(long maxBatchSizeEvents) {
		config.setMaxBatchSizeEvents(maxBatchSizeEvents);
	}

	public long getMaxInactiveTimeBeforeBatchFlush() {
		return config.getMaxInactiveTimeBeforeBatchFlush();
	}

	public void setMaxInactiveTimeBeforeBatchFlush(
			long maxInactiveTimeBeforeBatchFlush) {
		config.setMaxInactiveTimeBeforeBatchFlush(maxInactiveTimeBeforeBatchFlush);
	}

	public boolean isBatchMode() {
		return config.isBatchMode();
	}

	public void setBatchMode(boolean batchMode) {
		config.setBatchMode(batchMode);
	}

	
	public String getMaxBatchSizeBytes() {
		return String.valueOf(config.getMaxBatchSizeBytes());
	}

	public void setMaxBatchSizeBytes(String maxBatchSizeBytes) {
		config.setMaxBatchSizeBytes(maxBatchSizeBytes);
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

	public Layout<ILoggingEvent> getLayout() {
		return layout;
	}

	public void setLayout(Layout<ILoggingEvent> layout) {
		this.layout = layout;
	}

}
