package be.planty.skills.assistant.handlers.agent;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import org.apache.http.auth.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
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
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static org.springframework.util.StringUtils.isEmpty;

public class AgentClient {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentClient.class);

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

        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
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

    public CompletableFuture<Optional<Response>> messageAgent(HandlerInput input, Object message) throws AuthenticationException {

        final CompletableFuture<Optional<Response>> futureResponse = new CompletableFuture<>();

        final String accessToken = login(baseUrl, username, password);
        final String wsUrl = System.getProperty("be.planty.assistant.ws.url");
        final String url = wsUrl + "/action?access_token=" + accessToken;
        final WebSocketStompClient stompClient = createStompClient();
        final StompSessionHandler handler = new AgentSessionHandler(input, futureResponse);

        logger.info("Connecting to: " + url + " ...");
        final ListenableFuture<StompSession> futureSession = stompClient.connect(url, handler);
        futureSession.addCallback(
            session -> {
                logger.info("Connected!");
                session.subscribe("/topic/action.res", handler);
                logger.info("Sending a message to /topic/action.req...");
                session.send("/topic/action.req", message);
            },
            err -> logger.error(err.getMessage(), err));
        return futureResponse;
    }
}
