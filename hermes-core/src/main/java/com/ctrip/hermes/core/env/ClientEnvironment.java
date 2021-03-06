package com.ctrip.hermes.core.env;

import java.io.IOException;
import java.util.Properties;

public interface ClientEnvironment {

	Properties getProducerConfig(String topic) throws IOException;

	Properties getConsumerConfig(String topic) throws IOException;
	
	Properties getGlobalConfig() throws IOException;
}
