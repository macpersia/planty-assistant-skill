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

public class AgentSessionHandler extends StompSessionHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AgentSessionHandler.class);

    private static final ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();

    protected final HandlerInput input;
    protected final String messageId;
    protected final CompletableFuture<Optional<Response>> futureResponse;
    //protected final Optional<String> emailAddress;

    public AgentSessionHandler(HandlerInput input, String messageId,
                               CompletableFuture<Optional<Response>> futureResponse) {
        this.input = input;
        this.messageId = messageId;
        this.futureResponse = futureResponse;
        //this.emailAddress = getEmailAddress(input);
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        super.afterConnected(session, connectedHeaders);
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

        if (headers.getFirst("correlation-id").equals(messageId)
                && destination.startsWith("/user/queue/action-responses")
                //&& (!emailAddress.isPresent()
                //    || destination.endsWith(emailAddress.get()))
        ) {
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
