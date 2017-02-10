package com.github.libgraviton.messaging.strategy.jms;

import com.github.libgraviton.messaging.AcknowledgingConsumer;
import com.github.libgraviton.messaging.Consumer;
import org.junit.Before;
import org.junit.Test;

import javax.jms.BytesMessage;

import static org.mockito.Mockito.*;

public class JmsConsumerTest {

    private JmsConsumer jmsConsumer;

    private Consumer consumer;

    private BytesMessage bytesMessage;

    @Before
    public void setUp() throws Exception {
        consumer = mock(Consumer.class);
        jmsConsumer = spy(new JmsConsumer(consumer));

        bytesMessage = mock(BytesMessage.class);
        doReturn("messageId").when(bytesMessage).getJMSMessageID();
        doReturn("message").when(jmsConsumer).extractBody(any(BytesMessage.class));
    }

    @Test
    public void testAutoAcknowledge() throws Exception {
        jmsConsumer.onMessage(bytesMessage);
        verify(jmsConsumer).acknowledge("messageId");

        jmsConsumer = spy(new JmsConsumer(mock(AcknowledgingConsumer.class)));
        jmsConsumer.onMessage(bytesMessage);
        verify(jmsConsumer, never()).acknowledge(anyString());
    }

    @Test
    public void testMessageDelegation() throws Exception {
        jmsConsumer.onMessage(bytesMessage);

        verify(consumer).consume("messageId", "message");
    }

}
