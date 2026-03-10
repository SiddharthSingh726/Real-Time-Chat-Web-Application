package com.example.chat.chat.messaging;

import com.example.chat.config.MessagingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@ConditionalOnProperty(prefix = "app.messaging", name = "provider", havingValue = "rabbitmq")
public class RabbitMessagingConfig {

  @Bean
  public FanoutExchange chatExchange(MessagingProperties messagingProperties) {
    return new FanoutExchange(messagingProperties.getRabbit().getExchange(), true, false);
  }

  @Bean
  public Queue gatewayQueue(MessagingProperties messagingProperties,
                            @Value("${HOSTNAME:local}") String hostName) {
    String queueName = messagingProperties.getRabbit().getGatewayQueuePrefix() + "." + hostName;
    return QueueBuilder.durable(queueName).build();
  }

  @Bean
  public Binding gatewayBinding(Queue gatewayQueue, FanoutExchange chatExchange) {
    return BindingBuilder.bind(gatewayQueue).to(chatExchange);
  }

  @Bean
  public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                       MessageConverter rabbitMessageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(rabbitMessageConverter);
    return template;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory,
      MessageConverter rabbitMessageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(rabbitMessageConverter);
    return factory;
  }
}
