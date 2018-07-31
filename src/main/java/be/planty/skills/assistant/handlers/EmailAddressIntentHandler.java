package be.planty.skills.assistant.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import com.fasterxml.jackson.core.JsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

public class EmailAddressIntentHandler implements RequestHandler {

    private final static Logger logger = LoggerFactory.getLogger(EmailAddressIntentHandler.class);

    private final static JsonFactory jsonFactory = new JsonFactory();

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
         if (accessToken == null && apiAccessToken == null) {
             final String text = "Please login with Amazon, and then try again.";
             return input.getResponseBuilder()
                     .withLinkAccountCard()
                     .withSpeech(text)
                     .withSimpleCard("LWA Required", text)
                     .build();
         } else {
             emailAddress = getEmailAddress(accessToken != null ? accessToken : apiAccessToken);
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