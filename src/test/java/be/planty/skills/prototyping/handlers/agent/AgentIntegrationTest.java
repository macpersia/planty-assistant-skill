package be.planty.skills.prototyping.handlers.agent;

import be.planty.skills.assistant.handlers.agent.AgentClient;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.ui.OutputSpeech;
import org.apache.http.auth.AuthenticationException;
import org.junit.AfterClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for the {@link AgentClient}.
 */
//@RunWith(SpringRunner.class)
public class AgentIntegrationTest {

    static final AgentClient agentClient = new AgentClient();

    @Test
    public void messageAgent() throws ExecutionException, InterruptedException, TimeoutException, AuthenticationException {

        final HandlerInput mockInput = Mockito.mock(HandlerInput.class);
        // Oops! final classes cannot be mocked
        //final RequestEnvelope mockEnvelope = mock(RequestEnvelope.class);
        //final Session mockSession = mock(Session.class);
        final RequestEnvelope mockEnvelope = RequestEnvelope.builder()
                .withSession(Session.builder()
                        .withAttributes(new HashMap() {{
                            //put("email", "agent.prototyper@localhost");
                            put("email", "agent.prototyper");
                        }}).build())
                .build();
        when(mockInput.getRequestEnvelope())
                .thenReturn(mockEnvelope);

        final String message = "Ping!";
        final CompletableFuture<Optional<Response>> futureSession = agentClient.messageAgent(mockInput, message);
        assertNotNull(futureSession);
        final Optional<Response> optResponse = futureSession.get(5, SECONDS);
        assertTrue("No outputSpeech is present!", optResponse.isPresent());
        final OutputSpeech outputSpeech = optResponse.get().getOutputSpeech();
        assertNotNull(outputSpeech);
        //assertTrue(outputSpeech.toString().contains("Pong!"));
    }

    @AfterClass
    public static void tearDown() throws InterruptedException {
        Thread.sleep(2000);
    }
}
