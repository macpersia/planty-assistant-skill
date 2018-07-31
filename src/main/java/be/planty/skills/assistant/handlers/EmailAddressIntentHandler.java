package be.planty.skills.assistant.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

public class EmailAddressIntentHandler implements RequestHandler {

    private final static Logger logger = LoggerFactory.getLogger(EmailAddressIntentHandler.class);

    private static final ObjectWriter prettyPrinter = new ObjectMapper().writerWithDefaultPrettyPrinter();

    @Override
     public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.intentName("EmailAddressIntent"));
     }

     @Override
     public Optional<Response> handle(HandlerInput input) {
         final String emailAddress;
         final RequestEnvelope reqEnvelope = input.getRequestEnvelope();
         final String accessToken = reqEnvelope.getSession().getUser().getAccessToken();
         final String apiAccessToken = reqEnvelope.getContext().getSystem().getApiAccessToken();
         logger.info(">>>> accessToken: " + accessToken);
         logger.info(">>>> apiAccessToken: " + apiAccessToken);
         if (accessToken == null) {
             final String text = "Please link this skill, and try again.";
             return input.getResponseBuilder()
                     .withLinkAccountCard()
                     .withSpeech(text)
                     .withSimpleCard("Account Linking Needed!", text)
                     .build();
         } else {
             emailAddress = getEmailAddress(accessToken);
         }
         final String speechText = "Your registered email address is " + emailAddress;
         return input.getResponseBuilder()
                 .withSpeech(speechText)
                 .withSimpleCard("EmailAddress", speechText)
                 .build();
     }

    private String getEmailAddress(String accessToken) {
        final String baseUrl = "https://api.amazon.com/user/profile?access_token=" + accessToken;
        final ResponseEntity<String> response = new RestTemplate()
                .getForEntity(baseUrl, String.class);
        try {
            logger.info(">>>> Profile API response.statusCode: " + prettyPrinter.writeValueAsString(response.getStatusCode()));
            logger.info(">>>> Profile API response.body:\n" + prettyPrinter.writeValueAsString(response.getBody()));
            logger.info(">>>> Profile API response.headers:\n" + prettyPrinter.writeValueAsString(response.getHeaders()));

        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
        if (response.getStatusCode().isError()) {
            logger.error(response.toString());
            return null;
        }
        final Map<String, Object> body = JsonParserFactory.getJsonParser()
                .parseMap(response.getBody());
        final Map<String, Object> data = (Map) body.get("data");
        return (String) data.get("email");
    }

}