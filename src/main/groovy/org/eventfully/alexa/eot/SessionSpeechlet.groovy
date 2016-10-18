package org.eventfully.alexa.eot

import com.amazon.speech.slu.Intent
import com.amazon.speech.slu.Slot
import com.amazon.speech.speechlet.*
import com.amazon.speech.ui.PlainTextOutputSpeech
import com.amazon.speech.ui.Reprompt
import com.amazon.speech.ui.SimpleCard
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import wslite.rest.*
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
	private static final String COUNT_KEY = "COUNT"
	private static final String CONFIG_SLOT = "Config"
	
	private static final String EOTUrl = "http://echo-of-tomorrow.eu.cloudhub.io/api/"

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
			throws SpeechletException {
			
			session.setAttribute("createConfig", [])
			session.setAttribute("state", "start")
			log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),session.getSessionId())
			  
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
			String intentName = intent?.getName()
			
			Slot answer = intent.getSlot("Answer")
			String state = session.getAttribute(DEV_KEY)
			
			log.info("intentName: $intentName")
			log.info("state: $state")
			log.info("answer :  $answer")
			
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
        } else if ("AMAZON.HelpIntent".equals(intentName)) {
				return help(session)
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
	 /**
     * Creates and returns a {@code SpeechletResponse} with a quit message.
     *
     * @return SpeechletResponse spoken and visual quit message
     */
	private SpeechletResponse quit() {
        // Create the quit message.
        String speechText = "Ending Integrations. Thank you"
       
        return getSpeechletResponse(speechText, repromptText, false)
    }
	 /**
     * Creates and returns a {@code SpeechletResponse} with a help message.
     *
     * @return SpeechletResponse spoken and visual help message
     */
	private SpeechletResponse help(final Session session) {
		String speechText = "You can say stop or cancel to end the session at any time.  I will guide you through the every step. "
		
		def state = session.getAttribute("state")
			switch(state){
				case "start": 
					speechText += "First choose what you want to work with, either development or operations.If you need a question repeated, say repeat question."
					break 
				case "setMode":
					speechText += "First choose what you want to work with, either development or operations.If you need a question repeated, say repeat question."
					break 
				case "getOperationalData":
					speechText += "Tell me what integration you want to get operational data from"
					break
				case "getDevelopmentOperations":
					speechText += "Tell me if you want to create or configure an integration"
					break
				case "setDeveloptmentTasks":
					speechText += "Tell me want components you want to add to your integration"
					break
			
			}
		
		
		return getSpeechletResponse(speechText, repromptText, true)
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
		session.setAttribute("state", "setMode")
        // Check for favorite color and create output to user.
        if (modeSlot) {
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
			session.setAttribute("state", "getOperationalData")
			
			Slot typeSlot = intent.getSlot(TYPE_SLOT)
            Slot descSlot = intent.getSlot(DESC_SLOT)
			Slot dirSlot = intent.getSlot(DIR_SLOT)
			
			println "INFO: typeSlot: " + typeSlot.dump() + " descSlot: " + descSlot.dump() + " dirSlot: " + dirSlot.dump()
			
			//Handle what we have
			//If we have all of them for the backendRequest
			String backEndPath, type, desc, dir = ""
			
			type = typeSlot?.getValue()
			desc = descSlot?.getValue()
			dir = dirSlot?.getValue()
			def queryParams = [:]
			
			println "INFO: type: $type desc: $desc dir: $dir" 
			backEndPath = "/operations/" + type ?: 'all' 
			
			if (dir) {
				queryParams['direction'] = dir
			} 
			if (desc) {
				queryParams['partner'] = desc
			}
			queryParams['interval'] = 'today'
			
			def response = backendRequest(null, null, backEndPath, queryParams)
			println "INFO: response from backend: $response"
			//ToDo, dynamic handling of what we have.. 
			String speechText, repromptText  = ""
			def direction, messageType,messageCount, partner =""
			//INFO: response from backend: [total:0, partner:[name:Acme], message:[name:invoices], errors:1, direction:sent]
			if (response) {
			println "INFO: We have a response"
				messageType = response.message.name
				partner = response.partner.name
				
				if (response.direction == 'inbound') {
					direction = "received"
				} else {
					direction = "sent"
				}
				if (response.total == '0') {
					messageCount = "no"
				} else {
					messageCount= response.total 
				}
				println "INFO: We have direction: $direction, messageType: $messageType ,messageCount: $messageCount, partner: $partner"
				//handle how the response should be
				if (direction && messageType && partner) {
					speechText = "Today, $messageCount $messageType was $direction from $partner"
				} else if (direction && messageType) {
					speechText = "Today, $messageCount $messageType was $direction"
				} else if (direction && partner) {
					speechText = "Today, $messageCount message was $direction from $partner"
				} else if (partner) {
					speechText = "Today, there was a total of $messageCount from $partner"
				} else if (messageType) {
					speechText = "Today, there was a total of $messageCount of $messageType"
				}
				
			//ToDo we should handle this more generic.. 
			} else {
				if (direction && partner && messageType) {
					speechText = "Sorry, I could not find any information about $messageType that was $direction with the key of $partner"
				} else if (direction && partner) {
					speechText = "Sorry, I could not find any information about transactions that was $direction with the key of $partner"
				} else if (direction && messageType) {
					speechText = "Sorry, I could not find any information about $messageType that was $direction"
				} else if (partner && messageType) {
					speechText = "Sorry, I could not find any information about $messageType with the key of $partner"
				} else if (partner) {
					speechText = "Sorry, I could not find any information about transactions with the key of $partner"
				} else if (messageType) {
					speechText = "Sorry, I could not find any information about $messageType"
				} else if(direction){
					speechText = "Sorry, I could not find any information about transactions $direction"
				}
				
				
			}
			//Set the session values for later
			session.setAttribute(TYPE_KEY, messageType)
			session.setAttribute(DESC_KEY, partner)
			session.setAttribute(DIR_KEY, direction)
			session.setAttribute(COUNT_KEY, messageCount)
			
			println "INFO: Sending text: $speechText" 
			return getSpeechletResponse(speechText, repromptText, true)
	}
	
	private SpeechletResponse getDevelopmentOperations(final Intent intent, final Session session) {
			session.setAttribute("state", "getDevelopmentOperations")
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
		session.setAttribute("state", "setDeveloptmentTasks")
		
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
			String direction = session.getAttribute(DIR_KEY)
			String type = session.getAttribute(TYPE_KEY)
			String description = session.getAttribute(DESC_KEY)
			String messageCount = session.getAttribute(COUNT_KEY)
			// We could do this check when we have the request, and send it to diffrent functions
			
			if (direction && type && description) {
				speechText = "Paging support, $messageCount $type for integration $description was $direction today."
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
	private def backendRequest(def userName, def password, def path, Map<String, String> parameters) {
		def requestUrl = new RESTClient(EOTUrl)
		//requestUrl.auth.basic userName, password
		def response = requestUrl.get(path: path,  query: parameters)
		
		
		return response.json
	}
}
