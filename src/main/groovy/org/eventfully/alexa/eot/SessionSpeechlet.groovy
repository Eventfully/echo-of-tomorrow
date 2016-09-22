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
 * This is the echo of tomorrow's integration 
 * 
 */
@Slf4j
public class SessionSpeechlet implements Speechlet {

    private static final String MODE_KEY = "MODE"
    private static final String MODE_SLOT = "Mode"
	private static final String TYPE_KEY = "TYPE"
    private static final String TYPE_SLOT = "Type"
	private static final String DIR_KEY = "DIRECTION"
    private static final String DIR_SLOT = "Direction"
	private static final String DESC_KEY = "DESCRIPTION"
    private static final String DESC_SLOT = "Description"
	private static final String PAGE_SLOT = "Page" //No need for this
	private static final String DEV_SLOT = "Dev"
	private static final String DEV_KEY = "DEV"
	private static final String CONFIG_KEY = "CONFIG"
	private static final String CONFIG_SLOT = "Config"

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
			
			session.setAttribute("createConfig", [])
			log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),session.getSessionId())
        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
			log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),session.getSessionId())
        
		return getWelcomeResponse()
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        
			log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),session.getSessionId())

			// Get intent from the request object.
			Intent intent = request.getIntent();
			String intentName = (intent != null) ? intent.getName() : null;
			Slot answer = intent.getSlot("Answer")
			String state = session.getAttribute(DEV_KEY)
			
			log.info("intentName: $intentName")
			log.info("state: $state")
			log.info("answer :  $answer")
        // Note: If the session is started with an intent, no welcome message will be rendered;
        // rather, the intent specific response will be returned.
        
	
		
		if ('EOTModeIntent'.equals(intentName)) {
            return setModeInSession(intent, session)
		} else if ('EOTOpernationalDataIntent'.equals(intentName)) {
			return getOperationalData(intent, session)
		} else if ('EOTPageSupportIntent'.equals(intentName)) {
			return pageSupport(intent, session)
		} else if ('EOTDevelopmentIntent'.equals(intentName)) {
			if (state) {
				return setDeveloptmentTasks(intent, session)
			} else {	
				return getDevelopmentOperations(intent, session)
			}
		} else if ("AMAZON.StopIntent".equals(intentName)) {
				return quit()
        } else {
            throw new SpeechletException("Invalid Intent")
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
			log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(), session.getSessionId())
        // any cleanup logic goes here
    }

    /**
     * Creates and returns a {@code SpeechletResponse} with a welcome message.
     *
     * @return SpeechletResponse spoken and visual welcome message
     */
    private SpeechletResponse getWelcomeResponse() {
        // Create the welcome message.
        String speechText = "Starting Integrations. Do you want to work with operations or development"
        String repromptText = "You need to choose either operations or development";

        return getSpeechletResponse(speechText, repromptText, true)
    }
	
	private SpeechletResponse quit() {
        // Create the quit message.
        String speechText = "Ending Integrations."
        String repromptText = "Thank you";

        return getSpeechletResponse(speechText, repromptText, false)
    }

    /**
     * Creates a {@code SpeechletResponse} for the intent
     *
     * @param intent
     *            intent for the request
     * @return SpeechletResponse spoken and visual response the given intent
     */
    private SpeechletResponse setModeInSession(final Intent intent, final Session session) {
        // Get the slots from the intent.
        Map<String, Slot> slots = intent.getSlots()

        // Get the color slot from the list of slots.
        Slot modeSlot = slots.get(MODE_SLOT)
        String speechText, repromptText
        println slots.dump()

        // Check for favorite color and create output to user.
        if ( modeSlot != null) {
            // Store the user's favorite color in the Session and create response.
            String currentMode =  modeSlot.getValue()
            session.setAttribute(MODE_KEY, currentMode)
            if (currentMode == 'operations'){
                speechText = "Ask me about operational statistics."

            } else if (currentMode == 'development' ){
                speechText = "What do you want to do:"
                repromptText = "Create or configure?"
            }


        } else {
            // Render an error since we don't know what the users favorite color is.
            speechText = "I'm not sure what you want to work with, please try again"

        }

        return getSpeechletResponse(speechText, repromptText, true)
    }
	private SpeechletResponse getOperationalData(final Intent intent, final Session session) {
	
			Slot typeSlot = intent.getSlot(TYPE_SLOT)
            Slot descSlot = intent.getSlot(DESC_SLOT)
			Slot dirSlot = intent.getSlot(DIR_SLOT)
			
			println "typeSlot: " + typeSlot.dump()
			println "descSlot: " + descSlot.dump()
			println "dirSlot: " + dirSlot.dump()
			
			//Handle what we have
			//ToDo, dynamic handling of what we have.. 
			String speechText, repromptText;
			if (dirSlot != null || dirSlot.getValue() != null) {
				speechText = "Today no " + typeSlot.getValue() + " was " + dirSlot.getValue() + " from " + descSlot.getValue()
				//Set the session values for later
				session.setAttribute(TYPE_KEY, typeSlot.getValue())
				session.setAttribute(DESC_KEY, descSlot.getValue())
				session.setAttribute(DIR_KEY, dirSlot.getValue())
				
				//ToDo, save the number of INVOICES etc from the backend to the session
				
			} else {
				speechText = "Today no " + typeSlot.getValue() + " was " + dirSlot.getValue() + " from " + descSlot.getValue()
			}
			
			return getSpeechletResponse(speechText, repromptText, true)
	}
	
	private SpeechletResponse getDevelopmentOperations(final Intent intent, final Session session) {
	
			// Get the slots from the intent.
			String speechText, repromptText
			Map<String, Slot> slots = intent.getSlots()

			// Get the dev task from the slot
			Slot devSlot = slots.get(DEV_SLOT)
			println slots.dump()

			// Check if the user wants to create or configure
			if (devSlot) {	
				String currentOperation =  devSlot.getValue()
				session.setAttribute(DEV_KEY, currentOperation)
				if (currentOperation == 'create'){
					//ToDo, get next INT ID from backend (svn, github?)
					speechText = "Creating new integration with id INT0036. "
					repromptText = "Which component?"
					session.setAttribute(DEV_KEY, "DEV")

				} else if (currentOperation == 'configure' ){
					speechText = "Which integration do you want to configure?"
					
				}


			} else {
				// Render an error since we don't know what the users favorite color is.
				speechText = "I'm not sure what you want to work with, please try again"

			}

			return getSpeechletResponse(speechText, repromptText, true)
	}
	
	private SpeechletResponse setDeveloptmentTasks(final Intent intent, final Session session) {
		String speechText, repromptText
		Map<String, Slot> slots = intent.getSlots()
		boolean isAskResponse = false
		// Get the dev task from the slot
		Slot tasksSlot = slots.get(CONFIG_SLOT)
		println slots.dump()
		
	
		def myConfig = session.getAttribute("createConfig")
		
		String createTask = tasksSlot.getValue()
		
		println "setDev $createTask"
		if(createTask == 'none') {
			speechText = "Integration INT001 configured with: " + myConfig.join(',') + ". Notification sent."
			isAskResponse = false
		} else {
			session.setAttribute(DEV_KEY, "DEV")
			myConfig.add(createTask)
			session.setAttribute("createConfig", myConfig)
			isAskResponse = true
			speechText = "Added $createTask, Which component?"
			repromptText = "Do you want to add anything else?"
		}
		
		return getSpeechletResponse(speechText, repromptText, isAskResponse)
	
	}
	
	private SpeechletResponse pageSupport(final Intent intent, final Session session) {
	
			String speechText
			boolean isAskResponse = false

			// Get the information from the session
			String direction = (String) session.getAttribute(DIR_KEY)
			String type = (String) session.getAttribute(TYPE_KEY)
			String description = (String) session.getAttribute(DESC_KEY)

			// We could do this check when we have the request, and send it to diffrent functions
			if (direction && type && description) {
				speechText = "Paging support, no $type for integration INT001 $description was $direction today."
			} else {
				// Missing some of the values? Adjust the text
				speechText = "I need more information to page support, what do you want me to send?"

				isAskResponse = true
			}

			return getSpeechletResponse(speechText, speechText, isAskResponse)
	}


    /**
     * Returns a Speechlet response for a speech and reprompt text.
     */
    private SpeechletResponse getSpeechletResponse(String speechText, String repromptText, boolean isAskResponse) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard()
        card.setTitle("Session")
        card.setContent(speechText)

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
        speech.setText(speechText)

        if (isAskResponse) {
            // Create reprompt
            PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech()
            repromptSpeech.setText(repromptText)
            Reprompt reprompt = new Reprompt()
            reprompt.setOutputSpeech(repromptSpeech)

            return SpeechletResponse.newAskResponse(speech, reprompt, card)

        } else {
            return SpeechletResponse.newTellResponse(speech, card)
        }
    }
}
