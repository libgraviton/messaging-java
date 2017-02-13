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
QueueConnection connection = new RabbitMqConnection.Builder(properties, "context");

```
This will use all `config.*` properties to configure the connection.

The following properties / builder methods are currently supported:

| property         | invoked builder method         | default value |
|------------------|--------------------------------|---------------|
| host             | host(propertyValue)            | `localhost`   |
| port             | port(propertyValue)            | `5672`        |
| user             | user(propertyValue)            | `guest`       |
| password         | password(propertyValue)        | `guest`       |
| queue.name       | queueName(propertyValue)       | `null`        |
| queue.durable    | queueDurable(propertyValue)    | `true`        |
| queue.exclusive  | queueExclusive(propertyValue)  | `false`       |
| queue.autodelete | queueAutoDelete(propertyValue) | `false`       |
| exchange.name    | exchangeName(propertyValue)    | `null`        |
| exchange.type    | exchangeType(propertyValue)    | `direct`      |
| exchange.durable | exchangeDurable(propertyValue) | `false`       |
| routingkey       | routingKey(propertyValue)      | `routingkey`  |

See the [API doc](https://www.javadoc.io/doc/com.github.libgraviton/messaging/) for further details on the builder methods.