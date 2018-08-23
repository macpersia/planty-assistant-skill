package be.planty.skills.assistant;

import be.planty.skills.assistant.handlers.*;
import be.planty.skills.assistant.handlers.interceptors.MyRequestInterceptor;
import be.planty.skills.assistant.handlers.interceptors.MyResponseInterceptor;
import com.amazon.ask.Skill;
import com.amazon.ask.SkillStreamHandler;
import com.amazon.ask.Skills;
import com.amazon.ask.builder.SkillBuilder;

public class AssistantStreamHandler extends SkillStreamHandler {

    protected static SkillBuilder getSkillBuilder() {
        return Skills.standard()
                .addRequestHandlers(
                        new HelpIntentHandler(),
                        new LaunchRequestHandler(),
                        new HelloWorldIntentHandler(),
                        new EmailAddressIntentHandler(),
                        new FallbackIntentHandler(),
                        new CancelandStopIntentHandler(),
                        new SessionEndedRequestHandler())
                .addRequestInterceptor(new MyRequestInterceptor())
                .addResponseInterceptor(new MyResponseInterceptor())
                .addExceptionHandler(new MyExecptionHandler());
    }

    private static Skill getSkill() {
        return getSkillBuilder().build();
    }

    public AssistantStreamHandler() {
        super(getSkill());
    }

    public AssistantStreamHandler(Skill skill) {
        super(skill);
    }
 }
