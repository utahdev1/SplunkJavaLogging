package com.splunk.logging.jdk.handler;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import com.splunk.logging.HECTransportConfig;
import com.splunk.logging.SplunkHECInput;

/**
 * java.util.logging handler for sending events to Splunk via HEC endpoint
 * 
 * @author Damien Dallimore damien@baboonbones.com
 * 
 */
public class SplunkHECHandler extends Handler {

	// connection settings
	private HECTransportConfig config = new HECTransportConfig();

	// queuing settings
	private String maxQueueSize;
	private boolean dropEventsOnQueueFull;

	private SplunkHECInput shi;

	private String activationKey;

	/**
	 * Constructor
	 */
	public SplunkHECHandler() {

		configure();

		try {

			shi = new SplunkHECInput(config, this.activationKey);
			shi.setMaxQueueSize(maxQueueSize);
			shi.setDropEventsOnQueueFull(dropEventsOnQueueFull);
		} catch (Exception e) {

		}

	}

	/**
	 * Read in the handler properties from the config file
	 */
	private void configure() {

		LogManager manager = LogManager.getLogManager();
		String cname = getClass().getName();

		this.activationKey = manager.getProperty(cname + ".activationKey");
		config.setHost(manager.getProperty(cname + ".host"));
		config.setPort(Integer.parseInt(manager.getProperty(cname + ".port")));
		config.setPoolsize(Integer.parseInt(manager.getProperty(cname + ".poolsize")));
		config.setHttps(Boolean.parseBoolean(manager.getProperty(cname + ".https")));
		config.setToken(manager.getProperty(cname + ".token"));
		config.setIndex(manager.getProperty(cname + ".index"));
		config.setSource(manager.getProperty(cname + ".source"));
		config.setSourcetype(manager.getProperty(cname + ".sourcetype"));

		config.setBatchMode(Boolean.parseBoolean(manager.getProperty(cname + ".batchMode")));
		config.setMaxBatchSizeBytes(manager.getProperty(cname + ".maxBatchSizeBytes"));
		config.setMaxBatchSizeEvents(Long.parseLong(manager.getProperty(cname + ".maxBatchSizeEvents")));
		config.setMaxInactiveTimeBeforeBatchFlush(
				Long.parseLong(manager.getProperty(cname + ".maxInactiveTimeBeforeBatchFlush")));

		setMaxQueueSize(manager.getProperty(cname + ".maxQueueSize"));
		setDropEventsOnQueueFull(Boolean.parseBoolean(manager.getProperty(cname + ".dropEventsOnQueueFull")));
		setLevel(Level.parse(manager.getProperty(cname + ".level")));
		setFilter(null);
		setFormatter(new SplunkFormatter());

		try {
			setEncoding(manager.getProperty(cname + ".encoding"));
		} catch (Exception ex) {
			try {
				setEncoding(null);
			} catch (Exception ex2) {
			}
		}

	}

	/**
	 * Clean up resources
	 */
	@Override
	synchronized public void close() throws SecurityException {

		if (shi != null) {
			try {
				shi.closeStream();
				shi = null;
			} catch (Exception e) {
				Thread.currentThread().interrupt();
				shi = null;
			}
		}

	}

	/**
	 * Log the message
	 */
	@Override
	public void publish(LogRecord record) {

		if (!isLoggable(record)) {
			return;
		}
		if (shi == null) {
			return;
		}

		String formatted = getFormatter().format(record);

		shi.streamEvent(formatted);

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

	@Override
	public void flush() {
	}

}
