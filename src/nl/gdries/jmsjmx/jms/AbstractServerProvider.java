/*
Copyright (c) 2009, Gerco Dries (gerco@gdries.nl)
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * The name of the contributors may not be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY GERCO DRIES ''AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL GERCO DRIES BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package nl.gdries.jmsjmx.jms;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerProvider;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.generic.GenericConnectorServer;

/**
 * Factory class for JMX connections over JMS. Service URL must have the following structure:
 * <p>
 * service:jmx:<i>protocol</i>://<i>hostname</i>:<i>portnumber<i>/<i>topic</i>
 * <p>
 * <i>protocol</i> = The JMS provider to use (activemq/sonicmq/etc).<br/>
 * <i>hostname</i> = The hostname of the JMS broker<br/>
 * <i>portnumber</i> = The port number for the JMS broker<br/>
 * <i>topic</i> = The prefix to use to create Topic publishers/subscribers<br/>
 * <p>
 * The ServerProvider will listen for messages on <i>topic</i> from connecting clients. The
 * client is required to provide a JMSReplyTo Destination for it's connect message. The server will
 * reply to that Destination and the client must send all subsequent messages to the JMSReplyTo
 * destination the server will provide in all it's messages.
 * <p>
 * 
 * @author Gerco Dries (gdr@progaia-rs.nl)
 *
 */
public abstract class AbstractServerProvider implements JMXConnectorServerProvider {
	/**
	 * Environment property name for a javax.jms.Connection instance to be used by this ServerProvider
	 */
	public static final String CONNECTION_FACTORY = "nl.gdries.jmsjmx.jms.connectionfactory";

	/**
	 * Override this method to provide the protocol name this provider uses
	 * 
	 * @return The protocol name (eg. activemq or sonicmq)
	 */
	protected abstract String getProtocol();
	
	/**
	 * Create a new JMXConnectorServer
	 */
	public JMXConnectorServer newJMXConnectorServer(JMXServiceURL serviceURL,
			Map<String, ?> environment, MBeanServer mbeanServer)
			throws IOException {
		
		if(serviceURL != null && getProtocol().equals(serviceURL.getProtocol())) {
			if(!environment.containsKey(CONNECTION_FACTORY)) {
				throw new IllegalArgumentException("The property " + CONNECTION_FACTORY + " must be " +
						"present in the environment map!");
			}
			
			final Map<String, Object> env = new HashMap<String, Object>(environment);
			
			// Create the connectorserver instance using the ConnectionFactory from the environment
			final ServerMessageConnectionServer messageConnectionServer = 
				new ServerMessageConnectionServer((ConnectionFactory)env.get(CONNECTION_FACTORY), serviceURL);
			env.remove(CONNECTION_FACTORY);

			// Create the GenericConnectorServer and return it.
			env.put(GenericConnectorServer.MESSAGE_CONNECTION_SERVER, messageConnectionServer);
			
			return new GenericConnectorServer(env, mbeanServer);
		}
		
		return null;
	}
}
