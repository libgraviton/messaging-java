# messaging-java
[![Build Status](https://travis-ci.org/libgraviton/messaging-java.svg?branch=develop)](https://travis-ci.org/libgraviton/messaging-java)[![Coverage Status](https://coveralls.io/repos/libgraviton/messaging-java/badge.svg?branch=develop&service=github)](https://coveralls.io/github/libgraviton/messaging-java?branch=develop)[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.libgraviton/messaging/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.libgraviton/messaging)[![javadoc.io](https://javadocio-badges.herokuapp.com/com.github.libgraviton/messaging/badge.svg)](https://javadocio-badges.herokuapp.com/com.github.libgraviton/messaging) 


Message Queue Integration Library for Java

## Supported Message Brokers

* RabbitMQ
* JMS based Message Brokers


## Using the library

### Setup

In order to use the library, include the following in the `pom.xml` of your project:

```xml
<dependencies>
	<dependency>
		<groupId>com.github.libgraviton</groupId>
		<artifactId>messaging</artifactId>
		<version>LATEST</version>
	</dependency>
</dependencies>
```

Make sure that `version` points to the newest release on maven central (see badge above).

### Publish Messages

To publish messages you need an instance of `QueueConnection` which represents the connection to the Message Queue.
Once you have a `QueueConnection`, you can simply do the following to publish a message:
```java
try {
    // connection is an instance of QueueConnection
    connection.publish("the message");
} catch (CannotPublishMessage e) {
    // Message publishment failed for some reason.
}
```

### Consume Messages
To consume messages you need an instance of `QueueConnection` which represents the connection to the Message Queue.

Once you have a `QueueConnection`, you can simply do the following to consume a message:
```java
Consumer consumer = new Consumer() {

    @Override
    public void consume(String messageId, String message) throws CannotConsumeMessage {
        System.out.println(String.format("Received message with id '%s': '%s'", messageId, message));
    }

};

try {
    // connection is an instance of QueueConnection
    connection.consume(consumer);
} catch (CannotRegisterConsumer e) {
    // Consumer registration failed for some reason.
}
```

In this case, each message gets automatically acknowledged. If you want to handle message acknowledgment yourself, you need to register an `AcknowledginConsumer`:
```java
Consumer consumer = new AcknowledgingConsumer() {

    private MessageAcknowledger acknowledger;

    @Override
    public void setAcknowledger(MessageAcknowledger acknowledger) {
        this.acknowledger = acknowledger;
    }

    @Override
    public void consume(String messageId, String message) throws CannotConsumeMessage {
        System.out.println(String.format("Received message with id '%s': '%s'", messageId, message));
        try {
            acknowledger.acknowledge(messageId);
        } catch (CannotAcknowledgeMessage e) {
            // Message Acknowledgment failed for some reason
            throw new CannotConsumeMessage(messageId, message, e);
        }
    }

};

try {
    // connection is an instance of QueueConnection
    connection.consume(consumer);
} catch (CannotRegisterConsumer e) {
    // Consumer registration failed for some reason.
}
```
