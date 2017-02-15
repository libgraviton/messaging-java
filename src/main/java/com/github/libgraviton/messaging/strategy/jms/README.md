# JMS Strategy

Allows interaction with a JMS based queue. It's recommended to extend this class in order to get rid of the need of creating a JMS `ConnectionFactory`.

## Configuration

The JMS strategy uses the builder pattern, which allows you to configure the connection as follows:
```java

QueueConnection connection = new JmsConnection.Builder().queueName("your-queue").build();

```

Moreover, you can even pass a property file and a property context: 
```java
Properties properties = new Properties();
properties.setProperty("context.queue.name", "your-queue");
QueueConnection connection = new JmsConnection.Builder().applyProperties(properties, "context").build();

```
This will use all `context.*` properties to configure the connection.

The following builder methods / properties are currently supported:

| builder method           | equivalent property      | default value |
|--------------------------|--------------------------|---------------|
| host()                   | host                     | `localhost`   |
| port()                   | port                     | `61616`       |
| user()                   | user                     | `anonymous`   |
| password()               | password                 | `null`        |
| queueName()              | queueName                | `null`        |
| connectionAttempts()     | connection.attempts      | `-1`          |
| connectionAttemptsWait() | connection.attempts.wait | `1`           |
| connectionFactory()      |                          | `null`        |

The use of `connectionFactory()` may cause the builder to ignore / override other values like `host`.

See the [API doc](https://www.javadoc.io/doc/com.github.libgraviton/messaging/) for further details on the builder methods.