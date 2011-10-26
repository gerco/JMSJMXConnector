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
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.generic.MessageConnection;
import javax.management.remote.generic.MessageConnectionServer;

/**
 * Connects the platform MBeanServer to a JMS Topic
 * 
 * @author Gerco Dries (gdr@progaia-rs.nl)
 *
 */
public class ServerMessageConnectionServer implements MessageConnectionServer {
	private final ConnectionFactory connectionFactory;
	private final JMXServiceURL serviceURL;
	private final String topicPrefix;
	
	private Connection connection;
	private Session session;
	private MessageConsumer consumer;
	
	public ServerMessageConnectionServer(ConnectionFactory connectionFactory, JMXServiceURL serviceURL) {
		this.connectionFactory = connectionFactory;
		this.serviceURL = serviceURL;
		
		String topic = serviceURL.getURLPath();
		if(topic.length() == 0) {
			throw new IllegalArgumentException("The topic name must be provided in the service URL");
		}
		
		// Use the part after the / or ; as the topic name
		this.topicPrefix = topic.substring(1);
	}

	/**
	 * Listen for connect messages on the connect topic
	 */
	public MessageConnection accept() throws IOException {
		try {		
			// Open a consumer on the connect topic and receive a message to initiate the connection
			Message msg = consumer.receive();
			if(msg == null)
				throw new IOException("Received null message");

			// Use the received message to designate a communications destination (the JMSReplyTo)
			return new ServerMessageConnection(session, msg.getJMSReplyTo());
		} catch (JMSException e) {
			throw new WrappedJMSException(e);
		} 
	}

	public JMXServiceURL getAddress() {
		return serviceURL;
	}

	@SuppressWarnings("unchecked")
	public void start(Map arg0) throws IOException {
		try {
			// Open a JMS Connection & Session if none is already open
			if(connection == null) {
					connection = connectionFactory.createConnection();
					connection.start();
					session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			}
	
			// Create the message consumer if not already created
			if(consumer == null) {
				consumer = session.createConsumer(
					session.createTopic(topicPrefix),
					ServerMessageConnection.PROP_REQUESTTYPE + " = '" + ServerMessageConnection.REQUESTTYPE_CONNECT + "'");
			}
		} catch (JMSException e) {
			throw new WrappedJMSException(e);
		}
	}

	public void stop() throws IOException {
		if(session != null) {
			try {
				session.close();
				connection.close();
			} catch (JMSException e) {
				throw new WrappedJMSException(e);
			}
		}
	}
}
