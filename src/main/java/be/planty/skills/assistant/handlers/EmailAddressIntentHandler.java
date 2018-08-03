package be.planty.skills.assistant.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Session;
import com.amazon.ask.request.Predicates;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class EmailAddressIntentHandler implements RequestHandler {

    private final static Logger logger = LoggerFactory.getLogger(EmailAddressIntentHandler.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
     public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.intentName("EmailAddressIntent"));
     }

     @Override
     public Optional<Response> handle(HandlerInput input) {
         final Optional<String> emailAddress = AssistantUtils.getEmailAddress(input);
         if (!emailAddress.isPresent()) {
             final String text = "Please link this skill, and try again.";
             return input.getResponseBuilder()
                     .withLinkAccountCard()
                     .withSpeech(text)
                     .withSimpleCard("Account Linking Needed!", text)
                     .build();
         }
         final String speechText = "Your registered email address is " + emailAddress.get();
         return input.getResponseBuilder()
                 .withSpeech(speechText)
                 .withSimpleCard("EmailAddress", speechText)
                 .build();
     }

}