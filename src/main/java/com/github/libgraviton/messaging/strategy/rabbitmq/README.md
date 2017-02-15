# RabbitMQ Strategy

Allows interaction with a RabbitMQ queue.

## Configuration

The RabbitMQ strategy uses the builder pattern, which allows you to configure the connection as follows:
```java

QueueConnection connection = new RabbitMqConnection.Builder().queueName("your-queue").build();

```

Moreover, you can even pass a property file and a property context: 
```java
Properties properties = new Properties();
properties.setProperty("context.queue.name", "your-queue");
QueueConnection connection = new RabbitMqConnection.Builder().applyProperties(properties, "context").build();

```
This will use all `context.*` properties to configure the connection.

The following properties / builder methods are currently supported:

| invoked builder method   | equivalent property      | default value |
|--------------------------|--------------------------|---------------|
| host()                   | host                     | `localhost`   |
| port()                   | port                     | `5672`        |
| user()                   | user                     | `guest`       |
| password()               | password                 | `guest`       |
| queueName()              | queueName                | `null`        |
| connectionAttempts()     | connection.attempts      | `-1`          |
| connectionAttemptsWait() | connection.attempts.wait | `1`           |
| queueDurable()           | queueDurable             | `true`        |
| queueExclusive()         | queueExclusive           | `false`       |
| queueAutodelete()        | queueAutoDelete          | `false`       |
| exchangeName()           | exchangeName             | `null`        |
| exchangeType()           | exchangeType             | `direct`      |
| exchangeDurable()        | exchangeDurable          | `false`       |
| routingkey()             | routingKey               | `routingkey`  |

See the [API doc](https://www.javadoc.io/doc/com.github.libgraviton/messaging/) for further details on the builder methods.