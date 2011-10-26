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
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.generic.GenericConnector;

/**
 * Factory class for JMX connections over JMS
 * 
 * @author Gerco Dries (gdr@progaia-rs.nl)
 *
 */
public abstract class AbstractClientProvider implements JMXConnectorProvider {
	/**
	 * Environment property name for a javax.jms.ConnectionFactory instance to be used by this ServerProvider
	 */
	public static final String CONNECTION_FACTORY = "nl.gdries.jmsjmx.jms.connectionfactory";
	
	/**
	 * Environment property name for the connection timeout in milliseconds (Long object)
	 */
	public static final String CONNECTION_TIMEOUT = "nl.gdries.jmsjmx.jms.connecttimeout";

	/**
	 * Override this method to provide the protocol name this provider uses
	 * 
	 * @return The protocol name (eg. activemq or sonicmq)
	 */
	protected abstract String getProtocol();
	
	public JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map<String, ?> environment)
			throws IOException {
		
		if(serviceURL != null && getProtocol().equals(serviceURL.getProtocol())) {
			if(!environment.containsKey(CONNECTION_FACTORY)) {
				throw new IllegalArgumentException("The property " + CONNECTION_FACTORY + " must be " +
						"present in the environment map!");
			}
			
			final Map<String, Object> env = new HashMap<String, Object>(environment);		
			
			final ClientMessageConnection messageConnection = 
				new ClientMessageConnection((ConnectionFactory)env.get(CONNECTION_FACTORY), serviceURL);
			env.remove(CONNECTION_FACTORY);

			// Create the GenericConnector and return it.
			env.put(GenericConnector.MESSAGE_CONNECTION, messageConnection);
			
			return new GenericConnector(env);
		}
		
		return null;
	}
}
