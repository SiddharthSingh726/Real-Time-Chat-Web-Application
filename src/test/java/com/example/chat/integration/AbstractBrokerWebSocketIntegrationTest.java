package com.example.chat.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.example.chat.chat.api.ChatMessageResponse;
import com.example.chat.chat.api.ChatSendRequest;
import com.example.chat.chat.api.ConversationResponse;
import com.example.chat.chat.api.CreateConversationRequest;
import com.example.chat.chat.api.MessageAckResponse;
import com.example.chat.chat.domain.ConversationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

public abstract class AbstractBrokerWebSocketIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  protected void runBrokerFanoutFlow() throws Exception {
    UUID conversationId = createConversation();

    WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());

    StompSession sender = connect(stompClient, "user-1");
    StompSession receiver = connect(stompClient, "user-2");

    AtomicReference<ChatMessageResponse> receivedMessage = new AtomicReference<>();
    AtomicReference<MessageAckResponse> ack = new AtomicReference<>();
    CountDownLatch messageLatch = new CountDownLatch(1);
    CountDownLatch ackLatch = new CountDownLatch(1);

    receiver.subscribe(
        "/topic/conversations." + conversationId,
        typedHandler(ChatMessageResponse.class, receivedMessage, messageLatch));

    sender.subscribe(
        "/user/queue/acks",
        typedHandler(MessageAckResponse.class, ack, ackLatch));

    Thread.sleep(Duration.ofMillis(300));

    String clientMessageId = "it-" + UUID.randomUUID();
    String payload = "hello through broker";
    sender.send("/app/chat.send", new ChatSendRequest(conversationId, clientMessageId, payload));

    assertThat(ackLatch.await(12, TimeUnit.SECONDS)).isTrue();
    assertThat(messageLatch.await(12, TimeUnit.SECONDS)).isTrue();

    MessageAckResponse ackValue = ack.get();
    ChatMessageResponse messageValue = receivedMessage.get();
    assertThat(ackValue).isNotNull();
    assertThat(messageValue).isNotNull();
    assertThat(ackValue.status()).isEqualTo("PERSISTED");
    assertThat(ackValue.clientMessageId()).isEqualTo(clientMessageId);
    assertThat(messageValue.text()).isEqualTo(payload);
    assertThat(messageValue.senderId()).isEqualTo("user-1");
    assertThat(messageValue.conversationId()).isEqualTo(conversationId);

    sender.disconnect();
    receiver.disconnect();
    stompClient.stop();
  }

  private UUID createConversation() {
    CreateConversationRequest request = new CreateConversationRequest(
        "Integration Room",
        ConversationType.GROUP,
        List.of("user-2"));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add("X-User-Id", "user-1");

    ResponseEntity<ConversationResponse> response = restTemplate.exchange(
        "/api/conversations",
        HttpMethod.POST,
        new HttpEntity<>(request, headers),
        ConversationResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    return response.getBody().id();
  }

  private StompSession connect(WebSocketStompClient stompClient, String userId) throws Exception {
    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("x-user-id", userId);

    String url = "ws://localhost:" + port + "/ws";
    return stompClient.connectAsync(
            url,
            new WebSocketHttpHeaders(),
            connectHeaders,
            new StompSessionHandlerAdapter() {
            })
        .get(8, TimeUnit.SECONDS);
  }

  private <T> StompFrameHandler typedHandler(Class<T> payloadType,
                                             AtomicReference<T> sink,
                                             CountDownLatch latch) {
    return new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return payloadType;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        sink.set(payloadType.cast(payload));
        latch.countDown();
      }
    };
  }
}
