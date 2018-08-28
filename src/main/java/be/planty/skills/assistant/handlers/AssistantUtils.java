package be.planty.skills.assistant.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Session;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public final class AssistantUtils {

    private final static Logger logger = LoggerFactory.getLogger(AssistantUtils.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectWriter prettyPrinter = objectMapper.writerWithDefaultPrettyPrinter();

    public static final String EMAIL_KEY = "email";

    public static Optional<String> getEmailAddress(HandlerInput input) {
        final Session session = input.getRequestEnvelope().getSession();
        final Optional<String> emailAddress = getEmailAddress(session);
        emailAddress.ifPresent( ea ->
            ofNullable(input.getAttributesManager())
                    .map(am -> am.getSessionAttributes())
                    .orElse(new HashMap<>())
                    .put(EMAIL_KEY, ea));
        return emailAddress;
    }

    public static Optional<String> getEmailAddress(Session session) {
        final Map<String, Object> attributes = session.getAttributes();
        final Optional<String> emailAttribute = ofNullable(attributes)
                .map(atts -> atts.get(EMAIL_KEY)).map(Object::toString);
        if (emailAttribute.isPresent())
            return emailAttribute;

        final String accessToken = session.getUser().getAccessToken();
        logger.info(">>>> accessToken: " + accessToken);
        return (accessToken != null) ? getEmailAddress(accessToken) : empty();
    }

    public static Optional<String> getEmailAddress(String accessToken) {
        final String baseUrl = "https://api.amazon.com/user/profile?access_token=" + accessToken;
        final ResponseEntity<String> response = new RestTemplate()
                .getForEntity(baseUrl, String.class);
        try {
            logger.info(">>>> Profile API response.statusCode: " + prettyPrinter.writeValueAsString(response.getStatusCode()));
            logger.info(">>>> Profile API response.profile:\n" + prettyPrinter.writeValueAsString(response.getBody()));
            logger.info(">>>> Profile API response.headers:\n" + prettyPrinter.writeValueAsString(response.getHeaders()));

        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
        if (response.getStatusCode().isError()) {
            logger.error(response.toString());
            return empty();
        }
        try {
            final Profile profile;
            profile = objectMapper.readValue(response.getBody(), Profile.class);
            return ofNullable(profile.email);

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return empty();
        }
    }

    public static class Profile {

        @JsonProperty("user_id") public final String userId;
        @JsonProperty("name") public final String name;
        @JsonProperty("email") public final String email;

        @JsonCreator
        public Profile(
                @JsonProperty("user_id") String userId,
                @JsonProperty("name") String name,
                @JsonProperty("email") String email
        ) {
            this.userId = userId;
            this.name = name;
            this.email = email;
        }
    }
}
