package be.planty.skills.assistant.handlers.interceptors;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyRequestInterceptor implements RequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(MyRequestInterceptor.class.getName());

    private static final ObjectWriter prettyPrinter = new ObjectMapper().writerWithDefaultPrettyPrinter();

    @Override
    public void process(HandlerInput input) {
        // input.getAttributesManager().savePersistentAttributes();
        //final RequestEnvelope reqEnvelope = input.getRequestEnvelope();
        try {
            logger.info(">>>> input.context:\n" + prettyPrinter.writeValueAsString(input.getContext()));
            logger.info(">>>> input.reqEnvelope:\n" + prettyPrinter.writeValueAsString(input.getRequestEnvelope()));

        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
