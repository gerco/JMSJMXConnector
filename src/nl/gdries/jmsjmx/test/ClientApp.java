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
package nl.gdries.jmsjmx.test;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import nl.gdries.jmsjmx.sonicmq.ClientProvider;

public class ClientApp {

	public static void main(String[] args) throws Exception {
		Map<String, Object> environment = new HashMap<String, Object>();
		environment.put(JMXConnectorServerFactory.PROTOCOL_PROVIDER_PACKAGES, "nl.gdries.jmsjmx");
		environment.put(ClientProvider.CONNECTION_TIMEOUT, 60000L);
		environment.put(JMXConnector.CREDENTIALS, new String[] {"Administrator", "Administrator"});
		
		JMXServiceURL serviceURL = new JMXServiceURL(args[0]);
		JMXConnector connector = JMXConnectorFactory.newJMXConnector(serviceURL, environment);
		connector.connect();
		
		MBeanServerConnection server = connector.getMBeanServerConnection();
		ObjectName objectName = new ObjectName("nl.gdries.jmsjmx.test", "name", "Hello");
		
		if(server.isRegistered(objectName)) {
			server.invoke(objectName, "sayHello", null, null);
		} else {
			System.out.println("WTF? " + objectName + " is not registered!");
		}
		
		connector.close();
	}
}
