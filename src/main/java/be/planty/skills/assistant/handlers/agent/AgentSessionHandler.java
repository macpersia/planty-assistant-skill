package be.planty.skills.assistant.handlers.agent;

import be.planty.models.assistant.ActionResponse;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static be.planty.models.assistant.Constants.PAYLOAD_TYPE_KEY;
import static be.planty.skills.assistant.handlers.AssistantUtils.getEmailAddress;
import static com.amazonaws.util.json.Jackson.toJsonPrettyString;

public class AgentSessionHandler extends StompSessionHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AgentSessionHandler.class);

    private static final ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();

    private final HandlerInput input;
    private final CompletableFuture<Optional<Response>> futureResponse;
    private final String emailAddress;

    public AgentSessionHandler(HandlerInput input, CompletableFuture<Optional<Response>> futureResponse) {
        this.input = input;
        this.futureResponse = futureResponse;
        final Optional<String> foundEmail = getEmailAddress(input);
        assert foundEmail.isPresent();
        this.emailAddress = foundEmail.get();
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        final String typeName = headers.getFirst(PAYLOAD_TYPE_KEY);
        if (typeName == null)
            return super.getPayloadType(headers);
        try {
            return Class.forName(typeName);

        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
            return ActionResponse.class;
        }
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {

        final String destination = headers.getDestination();

        if (destination.startsWith("/user/queue/action-responses")
                && destination.endsWith(emailAddress)) {

            final String response = payload instanceof String ?
                    (String) payload
                    : toPrettyJson(payload);
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

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        logger.error(exception.toString(), exception);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        logger.error(exception.toString(), exception);
    }
}
