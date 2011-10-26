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
import java.util.UUID;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.management.remote.generic.MessageConnection;
import javax.management.remote.message.Message;

public abstract class AbstractMessageConnection implements MessageConnection {
	public static final long MAX_TIMEOUT = 3600000; // One hour

	protected static final String PROP_REQUESTTYPE    = "nl_gdries_jmsjmx_requesttype";
	protected static final String REQUESTTYPE_CONNECT = "CONNECT";
	
	protected final String connectionId;
	
	protected Session session;
	
	protected Destination myDestination;
	protected MessageConsumer consumer;
	
	protected Destination peerDestination;
	protected MessageProducer producer;
	
	protected long receiveTimeout = MAX_TIMEOUT;

	public AbstractMessageConnection() {
		this(null, null);
	}
	
	/**
	 * 
	 * @param session
	 * @param peerDestination The destination to send messages to
	 */
	public AbstractMessageConnection(Session session, Destination peerDestination) {
		this.connectionId = UUID.randomUUID().toString();
		this.session = session;
		this.peerDestination = peerDestination;
	}
	
	public String getConnectionId() {
		return connectionId;
	}
	
	/**
	 * Create the consumer and producer for the topics
	 */
	@SuppressWarnings("unchecked")
	public void connect(Map env) throws IOException {
		try {
			if(myDestination == null)
				myDestination = session.createTemporaryTopic();
			consumer = session.createConsumer(myDestination);
			
			if(peerDestination != null) {
				producer = session.createProducer(peerDestination);
				producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			}
		} catch (JMSException e) {
			throw new WrappedJMSException(e);
		}
	}
	
	public void close() throws IOException {
		try {
			if(consumer != null) {
				consumer.close();
			}
			
			if(producer != null) {
				producer.close();
			}
		} catch (JMSException e) {
			throw new WrappedJMSException(e);
		}
	}
	
	public Message readMessage() throws IOException, ClassNotFoundException {
		if(consumer == null)
			throw new IOException("The connection is closed");
		
		try {
			ObjectMessage om = (ObjectMessage)consumer.receive(receiveTimeout);
			if(om == null)
				throw new IOException("Receive timeout expired");

			// If the peer destination is still unknown. Use this message's
			// JMSReplyTo as the peer destination.
			if(peerDestination == null && producer == null)
				createProducer(om.getJMSReplyTo());
			
			return (Message)om.getObject();
		} catch (JMSException e) {
			throw new WrappedJMSException(e);
		}
	}

	public void writeMessage(Message message) throws IOException {
		if(producer == null)
			throw new IOException("The connection is closed");
		
		try {
			ObjectMessage msg = session.createObjectMessage();
			msg.setJMSReplyTo(myDestination);
			msg.setObject(message);
			producer.send(msg);
		} catch (JMSException e) {
			throw new WrappedJMSException(e);
		}
	}
	
	private void createProducer(Destination dst) throws JMSException {
		peerDestination = dst;
		producer = session.createProducer(dst);
	}	
}
