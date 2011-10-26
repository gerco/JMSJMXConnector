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
package nl.gdries.jmsjmx.sonicmq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.management.MBeanServer;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;

import nl.gdries.jmsjmx.jms.AbstractServerProvider;
import nl.gdries.jmsjmx.jms.WrappedJMSException;
import progress.message.jclient.ConnectionFactory;

public class ServerProvider extends AbstractServerProvider {
	
	@Override
	protected String getProtocol() {
		return "sonicmq";
	}

	/* (non-Javadoc)
	 * @see nl.gdries.jmsjmx.jms.AbstractServerProvider#newJMXConnectorServer(javax.management.remote.JMXServiceURL, java.util.Map, javax.management.MBeanServer)
	 */
	@Override
	public JMXConnectorServer newJMXConnectorServer(JMXServiceURL serviceURL,
			Map<String, ?> environment, MBeanServer mbeanServer)
			throws IOException {
		
		// Copy the environment
		Map<String, Object> newEnv = new HashMap<String, Object>(environment);
		
		try {
			// Create the connection to activemq
			ConnectionFactory connectionFactory = new ConnectionFactory(
					"tcp://" + serviceURL.getHost() + ":" + serviceURL.getPort());
			
			if(newEnv.containsKey(JMXConnector.CREDENTIALS)) {
				String[] cred = (String[])newEnv.get(JMXConnector.CREDENTIALS);
				connectionFactory.setDefaultUser(cred[0]);
				connectionFactory.setDefaultPassword(cred[1]);
			}
			
			newEnv.put(CONNECTION_FACTORY, connectionFactory);
			
			return super.newJMXConnectorServer(serviceURL, newEnv, mbeanServer);
		} catch (JMSException e) {
			throw new WrappedJMSException(e);
		}
	}

}
