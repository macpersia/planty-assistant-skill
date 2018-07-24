package be.planty.skills.assistant.handlers.agent;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AgentSessionHandler extends StompSessionHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AgentSessionHandler.class);
    private final HandlerInput input;
    private final CompletableFuture<Optional<Response>> futureResponse;

    public AgentSessionHandler(HandlerInput input, CompletableFuture<Optional<Response>> futureResponse) {
        this.input = input;
        this.futureResponse = futureResponse;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {

        if (headers == null || headers.getDestination() == null) {
            futureResponse.complete(Optional.empty());

        } else if (headers.getDestination().equals("/topic/action.res")) {
            final String response = String.valueOf(payload);
            logger.info("Here's the action response: " + response);
            futureResponse.complete(
                    input.getResponseBuilder()
                            .withSpeech("I'm done! Your app is ready.")
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
