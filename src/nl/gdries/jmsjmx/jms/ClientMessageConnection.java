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
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.message.Message;

public class ClientMessageConnection extends AbstractMessageConnection {
	public static final Long DEFAULT_CONNECT_TIMEOUT = 10000L;

	protected final ConnectionFactory connectionFactory;
	protected final String topic;
	
	protected Connection connection;
	
	public ClientMessageConnection(ConnectionFactory connectionFactory, JMXServiceURL serviceURL) {
		this.connectionFactory = connectionFactory;
		
		String topic = serviceURL.getURLPath();
		if(topic.length() == 0) {
			throw new IllegalArgumentException("The topic name must be provided in the service URL");
		}
		
		// Use the part after the / or ; as the topic name
		this.topic = topic.substring(1);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void connect(Map env) throws IOException {
		try {
			// Create the connection
			connection = connectionFactory.createConnection();
			
			// Create the session 
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			// Create a topic for communication
			myDestination = session.createTemporaryTopic();
			
			// Open the required consumer and producer
			super.connect(env);
			
			// Send the initial connect message to the connect destination.
			javax.jms.Message msg = session.createMessage();
			msg.setStringProperty(PROP_REQUESTTYPE, REQUESTTYPE_CONNECT);
			msg.setJMSReplyTo(myDestination);
			
			MessageProducer p = session.createProducer(session.createTopic(topic));
			p.send(msg);
			p.close();
			
			// Set connect timeout
			if(env.containsKey(AbstractClientProvider.CONNECTION_TIMEOUT))
				receiveTimeout = (Long)env.get(AbstractClientProvider.CONNECTION_TIMEOUT);
			else
				receiveTimeout = DEFAULT_CONNECT_TIMEOUT;
		} catch (JMSException e) {
			throw new WrappedJMSException(e);
		}		
	}
	
	/**
	 * Receives a message and sets the timeout back to MAX_TIMEOUT. This makes sure 
	 * that the connect timeout will only be used on the initial HandshakeBegin message.
	 */
	@Override
	public Message readMessage() throws IOException, ClassNotFoundException {
		try {
			return super.readMessage();
		} finally {
			receiveTimeout = MAX_TIMEOUT;
		}
	}
	
	@Override
	public void close() throws IOException {
		try {
			super.close();
			session.close();
			connection.close();
		} catch (JMSException e) {
			throw new WrappedJMSException(e);
		}
	}
}
