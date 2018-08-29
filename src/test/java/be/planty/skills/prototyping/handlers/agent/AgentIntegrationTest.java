package be.planty.skills.prototyping.handlers.agent;

import be.planty.skills.assistant.handlers.agent.AgentClient;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.ui.OutputSpeech;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import com.amazon.ask.response.ResponseBuilder;
import org.apache.http.auth.AuthenticationException;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test class for the {@link AgentClient}.
 */
//@RunWith(SpringRunner.class)
public class AgentIntegrationTest {

    private static final AgentClient agentClient = new AgentClient();

//    @Test
    public void messageAgentWithString() throws ExecutionException, InterruptedException, TimeoutException, AuthenticationException {

        final HandlerInput mockInput = Mockito.mock(HandlerInput.class);
        // Oops! final classes cannot be mocked
        //final RequestEnvelope mockEnvelope = mock(RequestEnvelope.class);
        //final Session mockSession = mock(Session.class);
        final RequestEnvelope mockEnvelope = RequestEnvelope.builder()
                .withSession(Session.builder()
                        .withAttributes(new HashMap() {{
                            put("email", "agent.prototyper@localhost");
                        }}).build())
                .build();
        when(mockInput.getRequestEnvelope())
                .thenReturn(mockEnvelope);
        when(mockInput.getResponseBuilder())
                .thenReturn(new ResponseBuilder());

        final String message = "Ping!";
        final CompletableFuture<Optional<Response>> futureSession = agentClient.messageAgent(mockInput, message);
        assertNotNull(futureSession);
        final Optional<Response> optResponse = futureSession.get(30, SECONDS);
        assertTrue("No outputSpeech is present!", optResponse.isPresent());
        final OutputSpeech outputSpeech = optResponse.get().getOutputSpeech();
        assertNotNull(outputSpeech);
        assertNotNull("SSML", outputSpeech.getType());
        assertEquals("<speak>Agent pong!</speak>", ((SsmlOutputSpeech)outputSpeech).getSsml());
    }

    @Test
    public void messageAgentWithObject() throws ExecutionException, InterruptedException, TimeoutException, AuthenticationException {

        final HandlerInput mockInput = Mockito.mock(HandlerInput.class);
        // Oops! final classes cannot be mocked
        //final RequestEnvelope mockEnvelope = mock(RequestEnvelope.class);
        //final Session mockSession = mock(Session.class);
        final RequestEnvelope mockEnvelope = RequestEnvelope.builder()
                .withSession(Session.builder()
                        .withAttributes(new HashMap() {{
                            put("email", "agent.prototyper@localhost");
                        }}).build())
                .build();
        when(mockInput.getRequestEnvelope())
                .thenReturn(mockEnvelope);
        when(mockInput.getResponseBuilder())
                .thenReturn(new ResponseBuilder());

        final SimpleEntry<String, String> message = new SimpleEntry<>("data", "Ping!");
        final CompletableFuture<Optional<Response>> futureSession = agentClient.messageAgent(mockInput, message);
        assertNotNull(futureSession);
        final Optional<Response> optResponse = futureSession.get(30, SECONDS);
        assertTrue("No outputSpeech is present!", optResponse.isPresent());
        final OutputSpeech outputSpeech = optResponse.get().getOutputSpeech();
        assertNotNull(outputSpeech);
        assertNotNull("SSML", outputSpeech.getType());
        assertEquals("<speak>Agent pong!</speak>", ((SsmlOutputSpeech)outputSpeech).getSsml());
    }
}
