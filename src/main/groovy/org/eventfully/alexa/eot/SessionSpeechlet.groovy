package org.eventfully.alexa.eot

import com.amazon.speech.slu.Intent
import com.amazon.speech.slu.Slot
import com.amazon.speech.speechlet.*
import com.amazon.speech.ui.PlainTextOutputSpeech
import com.amazon.speech.ui.Reprompt
import com.amazon.speech.ui.SimpleCard
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils

/**
 * This sample shows how to create a simple speechlet for handling intent requests and managing
 * session interactions.
 */
@Slf4j
public class SessionSpeechlet implements Speechlet {

    private static final String MODE_KEY = "MODES";
    private static final String MODE_SLOT = "Modes";

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        return getWelcomeResponse();
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        // Get intent from the request object.
        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;
        log.info("intentName: $intentName")

        // Note: If the session is started with an intent, no welcome message will be rendered;
        // rather, the intent specific response will be returned.
        if ('EOTModeIntent'.equals(intentName)) {
            return setModeInSession(intent, session);
        } else {
            throw new SpeechletException("Invalid Intent");
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        // any cleanup logic goes here
    }

    /**
     * Creates and returns a {@code SpeechletResponse} with a welcome message.
     *
     * @return SpeechletResponse spoken and visual welcome message
     */
    private SpeechletResponse getWelcomeResponse() {
        // Create the welcome message.
        String speechText = "Starting Integrations."
        String repromptText = "Do you want to work with operations or development";

        return getSpeechletResponse(speechText, repromptText, true);
    }

    private SpeechletResponse workwithOperations(final Intent intent, final Session session) {

    }

    /**
     * Creates a {@code SpeechletResponse} for the intent and stores the extracted color in the
     * Session.
     *
     * @param intent
     *            intent for the request
     * @return SpeechletResponse spoken and visual response the given intent
     */
    private SpeechletResponse setModeInSession(final Intent intent, final Session session) {
        // Get the slots from the intent.
        Map<String, Slot> slots = intent.getSlots();

        // Get the color slot from the list of slots.
        Slot modeSlot = slots.get(MODE_SLOT);
        String speechText, repromptText;
        println intent.dump()
        println session.dump()

        // Check for favorite color and create output to user.
        if ( modeSlot != null) {
            // Store the user's favorite color in the Session and create response.
            String currentMode =  modeSlot.getValue();
            session.setAttribute(MODE_KEY, currentMode);
            if (currentMode == 'operations'){
                speechText = "Ask me about operational statistics."

            } else if (currentMode == 'development' ){
                speechText = "What do you want to do:"
                repromptText = "Create or configure?"
            }


        } else {
            // Render an error since we don't know what the users favorite color is.
            speechText = "I'm not sure what you want to work with, please try again";

        }

        return getSpeechletResponse(speechText, repromptText, true);
    }

    /**
     * Creates a {@code SpeechletResponse} for the intent and get the user's favorite color from the
     * Session.
     *
     * @param intent
     *            intent for the request
     * @return SpeechletResponse spoken and visual response for the intent
     */
    private SpeechletResponse getColorFromSession(final Intent intent, final Session session) {
        String speechText;
        boolean isAskResponse = false;

        // Get the user's favorite color from the session.
        String favoriteColor = (String) session.getAttribute(MODE_KEY);

        // Check to make sure user's favorite color is set in the session.
        if (StringUtils.isNotEmpty(favoriteColor)) {
            speechText = String.format("Your favorite color is %s. Goodbye.", favoriteColor);
        } else {
            // Since the user's favorite color is not set render an error message.
            speechText =
                    "I'm not sure what your favorite color is. You can say, my favorite color is "
            +"red";
            isAskResponse = true;
        }

        return getSpeechletResponse(speechText, speechText, isAskResponse);
    }

    /**
     * Returns a Speechlet response for a speech and reprompt text.
     */
    private SpeechletResponse getSpeechletResponse(String speechText, String repromptText,
                                                   boolean isAskResponse) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Session");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        if (isAskResponse) {
            // Create reprompt
            PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
            repromptSpeech.setText(repromptText);
            Reprompt reprompt = new Reprompt();
            reprompt.setOutputSpeech(repromptSpeech);

            return SpeechletResponse.newAskResponse(speech, reprompt, card);

        } else {
            return SpeechletResponse.newTellResponse(speech, card);
        }
    }
}
