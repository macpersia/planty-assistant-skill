package be.planty.skills.assistant.handlers.agent;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static be.planty.skills.assistant.handlers.AssistantUtils.getEmailAddress;

public class AgentSessionHandler extends StompSessionHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AgentSessionHandler.class);
    private final HandlerInput input;
    private final CompletableFuture<Optional<Response>> futureResponse;
    private final String emailAddress;

    public AgentSessionHandler(HandlerInput input, CompletableFuture<Optional<Response>> futureResponse) {
        this.input = input;
        this.futureResponse = futureResponse;
        final Session session = input.getRequestEnvelope().getSession();
        final Optional<String> foundEmail = getEmailAddress(session);
        assert foundEmail.isPresent();
        this.emailAddress = foundEmail.get();
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {

        final String destination = headers.getDestination();

        if (destination.startsWith("/user/queue/action-responses")
                && destination.endsWith(emailAddress)) {

            final String response = String.valueOf(payload);
            logger.info("Here's the action response: " + response);
            final String report = response.equals("Pong!") ?
                    "Agent pong!"
                    : "All right! I'm done!";
            futureResponse.complete(
                    input.getResponseBuilder()
                            .withSpeech(report)
                            .build());
        }
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        logger.error(exception.toString(), exception);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        logger.error(exception.toString(), exception);
    }
}
