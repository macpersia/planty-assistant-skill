package be.planty.skills.assistant.handlers.agent;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.http.auth.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static be.planty.models.assistant.Constants.PAYLOAD_TYPE_KEY;
import static be.planty.skills.assistant.handlers.AssistantUtils.getEmailAddress;
import static java.util.Arrays.asList;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN;
import static org.springframework.util.StringUtils.isEmpty;

public class AgentClient {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentClient.class);

    private static final MappingJackson2MessageConverter jacksonMessageConverter = new MappingJackson2MessageConverter();
    private static final ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();

    private final String baseUrl = System.getProperty("be.planty.assistant.login.url");
    private final String username = System.getProperty("be.planty.assistant.access.id");
    private final String password = System.getProperty("be.planty.assistant.access.key");

    private WebSocketStompClient createStompClient() {
        final WebSocketClient socketClient = new StandardWebSocketClient();

        //final WebSocketStompClient stompClient = new WebSocketStompClient(socketClient);
        final SockJsClient sockJsClient = new SockJsClient(asList(
                new WebSocketTransport(socketClient)
        ));
        final WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);

        stompClient.setMessageConverter(
                new CompositeMessageConverter(
                        asList(jacksonMessageConverter, new StringMessageConverter())));
        return stompClient;
    }

    private String login(String baseUrl, String username, String password) throws AuthenticationException {
        final Map<String, String> request = new HashMap(){{
            put("username", username);
            put("password", password);
        }};
        final ResponseEntity<String> response = new RestTemplate()
                .postForEntity(baseUrl, request, String.class);

        if (response.getStatusCode().isError()) {
            logger.error(response.toString());
            throw new AuthenticationException(response.toString());
        }
        final HttpHeaders respHeaders = response.getHeaders();
        final String authHeader = respHeaders.getFirst("Authorization");
        if (isEmpty(authHeader)) {
            final String msg = "No 'Authorization header found!";
            logger.error(msg + " : " + response.toString());
            throw new AuthenticationException(msg);
        }
        if (!authHeader.startsWith("Bearer ")) {
            final String msg = "The 'Authorization header does not start with 'Bearer '!";
            logger.error(msg + " : " + authHeader);
            throw new AuthenticationException(msg);
        }
        return authHeader.substring(7);
    }

    public CompletableFuture<Optional<Response>> messageAgent(HandlerInput input, Object payload) throws AuthenticationException {

        final CompletableFuture<Optional<Response>> futureResponse = new CompletableFuture<>();

        final String accessToken = login(baseUrl, username, password);
        final String wsUrl = System.getProperty("be.planty.assistant.ws.url");
        final String url = wsUrl + "/action?access_token=" + accessToken;
        final WebSocketStompClient stompClient = createStompClient();
        final String messageId = UUID.randomUUID().toString();
        final StompSessionHandler handler = new AgentSessionHandler(input, messageId, futureResponse);

        logger.info("Connecting to: " + url + " ...");
        final ListenableFuture<StompSession> futureSession = stompClient.connect(url, handler);
        futureSession.addCallback(
            session -> {
                logger.info("Connected!");
                final Optional<String> emailAddress = getEmailAddress(input);
                final String resDest = "/user/queue/action-responses/" + emailAddress.orElse(null);
                session.subscribe(resDest, handler);
                final String reqDest = "/topic/action-requests/" + emailAddress.orElse(null);
                final StompHeaders headers = new StompHeaders();
                headers.setDestination(reqDest);
                headers.setMessageId(messageId);
                if (payload instanceof String) {
                    headers.setContentType(TEXT_PLAIN);
                    logger.info("Sending a string payload to '" + reqDest + "' : " + payload);
                    session.send(headers, payload);
                } else {
                    headers.setContentType(APPLICATION_JSON);
                    headers.set(PAYLOAD_TYPE_KEY, payload.getClass().getTypeName());
                    logger.info("Sending an object payload to '" + reqDest + "' : " + toPrettyJson(payload));
                    session.send(headers, payload);
                }
            },
            err -> logger.error(err.getMessage(), err));
        return futureResponse;
    }

    private String toPrettyJson(Object payload) {
        String prettyPayload;
        try {
            prettyPayload = payload instanceof String ?
                (String) payload
                : (payload instanceof byte[] ?
                    new String((byte[]) payload)
                    : objectWriter.writeValueAsString(payload));

        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            prettyPayload = String.valueOf(payload);
        }
        return prettyPayload;
    }
}
