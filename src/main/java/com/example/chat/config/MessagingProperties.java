package com.example.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
public class MessagingProperties {

  public enum Provider {
    LOCAL,
    KAFKA,
    RABBITMQ
  }

  private Provider provider = Provider.LOCAL;
  private Kafka kafka = new Kafka();
  private Rabbit rabbit = new Rabbit();

  public Provider getProvider() {
    return provider;
  }

  public void setProvider(Provider provider) {
    this.provider = provider;
  }

  public Kafka getKafka() {
    return kafka;
  }

  public void setKafka(Kafka kafka) {
    this.kafka = kafka;
  }

  public Rabbit getRabbit() {
    return rabbit;
  }

  public void setRabbit(Rabbit rabbit) {
    this.rabbit = rabbit;
  }

  public static class Kafka {

    private String topic = "chat.messages";
    private String gatewayGroupId = "chat-gateway-${random.uuid}";
    private int partitions = 6;

    public String getTopic() {
      return topic;
    }

    public void setTopic(String topic) {
      this.topic = topic;
    }

    public String getGatewayGroupId() {
      return gatewayGroupId;
    }

    public void setGatewayGroupId(String gatewayGroupId) {
      this.gatewayGroupId = gatewayGroupId;
    }

    public int getPartitions() {
      return partitions;
    }

    public void setPartitions(int partitions) {
      this.partitions = partitions;
    }
  }

  public static class Rabbit {

    private String exchange = "chat.messages.exchange";
    private String gatewayQueuePrefix = "chat.gateway";

    public String getExchange() {
      return exchange;
    }

    public void setExchange(String exchange) {
      this.exchange = exchange;
    }

    public String getGatewayQueuePrefix() {
      return gatewayQueuePrefix;
    }

    public void setGatewayQueuePrefix(String gatewayQueuePrefix) {
      this.gatewayQueuePrefix = gatewayQueuePrefix;
    }
  }
}
