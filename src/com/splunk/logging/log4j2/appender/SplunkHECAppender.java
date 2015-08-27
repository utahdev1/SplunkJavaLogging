package com.splunk.logging.log4j2.appender;

import java.io.Serializable;
import java.util.concurrent.locks.*;

import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.*;

import com.splunk.logging.HECTransportConfig;
import com.splunk.logging.SplunkHECInput;

@Plugin(name = "SplunkHECAppender", category = "Core", elementType = "appender", printObject = true)
public final class SplunkHECAppender extends AbstractAppender {

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Lock readLock = rwLock.readLock();

	// connection settings
	private HECTransportConfig config;

	// queuing settings
	private String maxQueueSize;
	private boolean dropEventsOnQueueFull;

	private SplunkHECInput shi;

	protected SplunkHECAppender(String name, HECTransportConfig config,
			Filter filter, Layout<? extends Serializable> layout,
			final boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions);
		this.config = config;
		try {
			this.shi = new SplunkHECInput(config);
			this.shi.setMaxQueueSize(maxQueueSize);
			this.shi.setDropEventsOnQueueFull(dropEventsOnQueueFull);
		} catch (Exception e) {
		}
	}

	@Override
	public void append(LogEvent event) {
		readLock.lock();
		try {

			try {
				if (shi == null) {
					shi = new SplunkHECInput(config);
					shi.setMaxQueueSize(maxQueueSize);
					shi.setDropEventsOnQueueFull(dropEventsOnQueueFull);
				}
			} catch (Exception e) {
				throw new AppenderLoggingException(
						"Couldn't establish connection for SplunkHECAppender named \""
								+ this.getName() + "\".");
			}

			final byte[] bytes = getLayout().toByteArray(event);
			String formatted = new String(bytes);

			shi.streamEvent(formatted);

		} catch (Exception ex) {
			if (!ignoreExceptions()) {
				throw new AppenderLoggingException(ex);
			}
		} finally {
			readLock.unlock();
		}
	}

	@PluginFactory
	public static SplunkHECAppender createAppender(
			@PluginAttribute("name") String name,
			@PluginElement("Layout") Layout<? extends Serializable> layout,
			@PluginElement("Filter") final Filter filter,
			@PluginAttribute("token") String token,
			@PluginAttribute("host") String host,
			@PluginAttribute("port") int port,
			@PluginAttribute("poolsize") int poolsize,
			@PluginAttribute("https") boolean https,
			@PluginAttribute("index") String index,
			@PluginAttribute("source") String source,
			@PluginAttribute("sourcetype") String sourcetype) {

		if (name == null) {
			LOGGER.error("No name provided for SplunkHECAppender");
			return null;
		}
		if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}

		HECTransportConfig config = new HECTransportConfig();

		if (token == null) {
			LOGGER.error("No token provided for SplunkHECAppender");
			return null;
		}

		config.setHost(host);
		config.setPort(port);
		config.setToken(token);
		config.setPoolsize(poolsize);
		config.setHttps(https);
		config.setIndex(index);
		config.setSource(sourcetype);
		config.setSourcetype(sourcetype);

		return new SplunkHECAppender(name, config, filter, layout, true);
	}
}