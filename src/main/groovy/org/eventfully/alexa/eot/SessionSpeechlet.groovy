package org.eventfully.alexa.eot

import com.amazon.speech.slu.Intent
import com.amazon.speech.slu.Slot
import com.amazon.speech.speechlet.*
import com.amazon.speech.ui.PlainTextOutputSpeech
import com.amazon.speech.ui.Reprompt
import com.amazon.speech.ui.SimpleCard
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.model.ScanRequest
import com.amazonaws.services.dynamodbv2.model.ScanResult
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
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
	private static final String SESSION_KEY = "Session"

	private static final String EOTUrl = "http://echo-of-tomorrow.eu.cloudhub.io/api/"

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
			throws SpeechletException {
			//We use sessions to keep some of the states. 
			session.setAttribute("createConfig", [])
			session.setAttribute("state", "start")
            session.setAttribute("sessionData", [:])
			  
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        
		return getWelcomeResponse()
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
			// Get intent from the request object.
			Intent intent = request.getIntent()
			String intentName = intent?.getName()
			String state = session.getAttribute("state")
			String devState = session.getAttribute(DEV_KEY)
			println "INFO: intentName: $intentName, state: $state"
			println "INFO: Session id: " + session.getSessionId()

            if ('EOTModeIntent'.equals(intentName)) {
                return setModeInSession(intent, session)
            } else if ('EOTOpernationalDataIntent'.equals(intentName)) {
                return getOperationalData(intent, session)
            } else if ('EOTPageSupportIntent'.equals(intentName)) {
                return pageSupport(intent, session)
            } else if ('EOTDevelopmentIntent'.equals(intentName)) {
                if (devState) {
                    return setDevelopmentTasks(intent, session)
                } else {
                    return getDevelopmentOperations(intent, session)
                }
            } else if ("AMAZON.StopIntent".equals(intentName)) {
                    return quit()
            } else if ("AMAZON.HelpIntent".equals(intentName)) {
                    println "INFO: Helpintent"
                    return help(session)
            } else if ("AMAZON.PauseIntent".equals(intentName)) {
                println "INFO: Amazon pause intent - save state to database"
                setSessionData(session )
            } else if ("AMAZON.ResumeIntent".equals(intentName)) {
                println "INFO: Amazon resume intent - get state data from database"
                getSessionData(session)
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
       
        return getSpeechletResponse(speechText, '', false)
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
					speechText += "First choose what you want to work with, either development or operations."
					break 
				case "setModeOp":
					speechText += "Tell me what kind of data you want to know about, for example how many invoices was sent today."
					break 
				case "setModeDev":
					speechText += "Tell me if you want to create a new integration or configure a existing one."
					break 
				case "getOperationalData":
					speechText += "I have now told you the operational data, what do you want to do next? Send a message to support about what I just told you?"
					break
				case "getDevelopmentOperationsCreate":
					speechText += "Tell me what components that should be added, for example: HTTPInput, XMLTOJSON"
					break
				case "getDevelopmentOperationsConfig":
					speechText += "Tell me the integration id of the integration you want to configure."
					break
				case "setDevelopmentTasks":
					speechText += "Tell me want components you want to add to your integration."
					break
			
			}
		
		println "INFO: Help response: $speechText"
		return getSpeechletResponse(speechText, '', true)
	}

    private SpeechletResponse setModeInSession(final Intent intent, final Session session) {

        // Get the slots from the intent.
        Map<String, Slot> slots = intent.getSlots()

        // Get the color slot from the list of slots.
        Slot modeSlot = slots.get(MODE_SLOT)
        String speechText, repromptText

        // Check for favorite color and create output to user.
        if (modeSlot) {
            // Store the user's favorite color in the Session and create response.
            String currentMode =  modeSlot.getValue()
            session.setAttribute(MODE_KEY, currentMode)
            if (currentMode == 'operations'){
                session.setAttribute("state", "setModeOp")
				speechText = "Ask me about operational statistics."

            } else if (currentMode == 'development' ){
				session.setAttribute("state", "setModeDev")
                speechText = "What do you want to do:"
                repromptText = "Create or configure?"
            }


        } else {
            // Render an error since we don't know what the users favorite color is.
            speechText = "I'm not sure what you want to work with, please try again"

        }

        return getSpeechletResponse(speechText, repromptText, true)
    }
    private SpeechletResponse sendOperationalRequest(def type, def desc, def dir, final Session session) {
        session.setAttribute("state", "sendOperationalRequest")
        println "INFO: typeSlot: " + type + " descSlot: " + desc + " dirSlot: " + dir


        def queryParams = [:]

        println "INFO: type: $type desc: $desc dir: $dir"
        def backEndPath = "/operations/" + type ?: 'all'

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
            messageType = response.message.name?:''
            partner = response.partner.name?:''

            if (response.direction == 'inbound') {
                direction = "received"
            } else {
                direction = "sent"
            }
            if (response.total == 0) {
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
	private SpeechletResponse getOperationalData(final Intent intent, final Session session) {
			session.setAttribute("state", "getOperationalData")
            def slotsSession = [:]

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

            slotsSession[TYPE_SLOT]=type
            slotsSession[DESC_SLOT]=desc
            slotsSession[DIR_SLOT]=dir

            session.setAttribute('slots', slotsSession)
            return sendOperationalRequest(type, desc, dir, session)


	}
	private SpeechletResponse getDevelopmentOperations(final Intent intent, final Session session) {
            session.setAttribute("state", "getDevelopmentOperations")
			//Save the data to the session
            def slotsSession = [:]

			// Get the slots from the intent.
			String speechText, repromptText
			Map<String, Slot> slots = intent.getSlots()

			// Get the dev task from the slot
			Slot devSlot = slots.get(DEV_SLOT)
            slotsSession[DEV_SLOT]=devSlot.getValue()

			// Check if the user wants to create or configure
			if (devSlot) {	
				String currentOperation =  devSlot.getValue()
				session.setAttribute(DEV_KEY, currentOperation)

				if (currentOperation == 'create'){
					session.setAttribute("state", "getDevelopmentOperationsCreate")
					//ToDo, get next INT ID from backend (svn, github?)
					speechText = "Creating a new integration for you. Configure it by adding components"
					repromptText = "Which component?"
					session.setAttribute(DEV_KEY, "DEV")

				} else if (currentOperation == 'configure' ){
					session.setAttribute("state", "getDevelopmentOperationsConfig")
					speechText = "Which integration do you want to configure?"
					
				}


			} else {
				// Render an error since we don't know what the users favorite color is.
				speechText = "I'm not sure what you want to work with, please try again"

			}

             session.setAttribute('slots', slotsSession)


			return getSpeechletResponse(speechText, repromptText, true)
	}
    private SpeechletResponse setDevelopmentTasks(final Intent intent, final Session session) {
        session.setAttribute("state", "setDevelopmentTasks")

        //Save the data to the session
        def sessionData = session.getAttribute("sessionData")
        def slotsSession = [:]

        String speechText, repromptText
        Map<String, Slot> slots = intent.getSlots()
        boolean isAskResponse = false
        // Get the dev task from the slot
        Slot tasksSlot = slots.get(CONFIG_SLOT)

        def myConfig = session.getAttribute("createConfig")

        String createTask = tasksSlot.getValue()?:''
        slotsSession.put(CONFIG_SLOT, createTask)
        println "setDev $createTask"
        if(createTask == 'none') {
            speechText = "Integration INT001 configured with: " + myConfig.join(',') + ". Notification sent."
            isAskResponse = false
        } else if (!createTask) {
            speechText = "Sorry, I did not get that, what did you want to add?"
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
        session.setAttribute("state", "pageSupport")

        String speechText
        boolean isAskResponse = false

        // Get the information from the session
        String direction = session.getAttribute(DIR_KEY)?:''
        String type = session.getAttribute(TYPE_KEY)?:''
        String description = session.getAttribute(DESC_KEY)?:''
        String messageCount = session.getAttribute(COUNT_KEY)?:''
        // We could do this check when we have the request, and send it to diffrent functions

        if (direction && type && description) {
            speechText = "Paging support, $messageCount $type for integration $description was $direction today."
        } else if (direction && type) {
            speechText = "Paging support, $messageCount $type was $direction today."
        } else if (direction && description) {
            speechText = "Paging support, $messageCount for integration $description was $direction today."
        } else if (type && description) {
            speechText = "Paging support, $messageCount $type for integration $description in total today."
        } else if (type) {
            speechText = "Paging support, $messageCount $type in total today."
        } else if (direction) {
            speechText = "Paging support, $messageCount transaction was $direction today."
        } else if (description) {
            speechText = "Paging support, $messageCount for integration $description in total today."
        } else {
            // Missing some of the values? Adjust the text
            speechText = "I need more information to page support, sorry!"
            isAskResponse = false
        }

        return getSpeechletResponse(speechText, "", isAskResponse)
    }
    /**
     *  All functions for keeping state, when pause/resume
     *  Using sessionData from dynamodb
     */
    private SpeechletResponse getDevelopmentOperations(def sessionData, final Session session) {
        session.setAttribute("state", "getDevelopmentOperations")
        // Get the slots from the intent.
        String speechText, repromptText
        def slots = sessionData["slots"]

        // Get the dev task from the slot
        def devSlot = slots[DEV_SLOT]


        // Check if the user wants to create or configure
        if (devSlot) {
            session.setAttribute(DEV_KEY, devSlot)
            if (devSlot == 'create'){
                session.setAttribute("state", "getDevelopmentOperationsCreate")
                //ToDo, get next INT ID from backend (svn, github?)
                speechText = "Creating new integration with id INT0036. "
                repromptText = "Which component?"
                session.setAttribute(DEV_KEY, "DEV")

            } else if (devSlot == 'configure' ){
                session.setAttribute("state", "getDevelopmentOperationsConfig")
                speechText = "Which integration do you want to configure?"

            }


        } else {
            // Render an error since we don't know what the users favorite color is.
            speechText = "I'm not sure what you want to work with, please try again"

        }

        return getSpeechletResponse(speechText, repromptText, true)
    }
	private SpeechletResponse setDevelopmentTasks(def sessionData, final Session session) {
		session.setAttribute("state", "setDevelopmentTasks")
		
		String speechText, repromptText
	
		def myConfig = sessionData["createConfig"]
        session.setAttribute("createConfig", myConfig)
        session.setAttribute(DEV_KEY, "DEV")


        speechText = "The integration is configured with: " + myConfig.join(',') + ". Anything else?"

		return getSpeechletResponse(speechText, repromptText, true)
	
	}
    private SpeechletResponse getOperationalData(def sessionData, final Session session) {
        String type = sessionData[TYPE_SLOT]
        String desc = sessionData[DESC_SLOT]
        String dir = sessionData[DIR_SLOT]

        return sendOperationalRequest(type, desc, dir, session)


    }
    private SpeechletResponse setModeInSession(final Session session) {
        String speechText, repromptText

        if(session.getAttribute("state")=="setModeOp") {
            speechText = "Ask me about operational statistics."
        }else if (session.getAttribute("state")=="setModeDev"){
            speechText = "What do you want to do:"
            repromptText = "Create or configure?"
        }

        return getSpeechletResponse(speechText, repromptText, true)
    }
    /**
     *
     * @param session
     * @return dispatch to correct function from the state saved
     */
	private SpeechletResponse getSessionData(Session session) {
        def sessionData = [:]

        DynamoDB dynamoDB = new DynamoDB(new AmazonDynamoDBClient())
        Table table = dynamoDB.getTable("EOT")

        Item item = table.getItem("id", "1")
        println "INFO: The Item from dynamodb" + item
        println "INFO: The state of it: " + item.state
        if (item.createConfig) {
            session.setAttribute("createConfig", item.createConfig)

        }
        if (item.slots) {
            sessionData = item.slots
        }
        println "INFO SessionData stored: " + sessionData

        session.setAttribute("state", item.state)

        if (item.state == "setModeOp" || item.state == "setModeDev") {
            return setModeInSession(session)
        } else if (item.state == "getOperationalData") {
            return getOperationalData(sessionData, session)
        } else if (item.state=="getDevelopmentOperations" || item.state =="getDevelopmentOperationsCreate" || item.state=="getDevelopmentOperationsConfig" ) {
            return getDevelopmentOperations(sessionData, session)
        } else if (item.state=="setDevelopmentTasks") {
            return setDevelopmentTasks(sessionData, session)
        } else {

            return getSpeechletResponse("Sorry, I dont know where to resume from, please start over.", "", false)
        }




	}
    /**
     *
     * @param session
     * @return Speechelt response for the Pause intent
     */
	private SpeechletResponse setSessionData(Session session){
        //Get the session data
        def state = session.getAttribute("state")
        def stateData = session.getAttribute("sessionData")

        DynamoDB dynamoDB = new DynamoDB(new AmazonDynamoDBClient())
		Table table = dynamoDB.getTable("EOT")

        table.deleteItem("id", "1");
		Item item = new Item()
				.withPrimaryKey("id", "1")
				.withString("state", state)

        stateData.each{key, value ->
            item.withString(key, value)
        }

        def sessionConfig = session.getAttribute("createConfig")
        if (sessionConfig){
            item.withList("createConfig", sessionConfig)
        }
        def sessionSlots = session.getAttribute("slots")
        println "INFO: SessionSlots - " +  sessionSlots

        if (sessionSlots) {
            item.withMap("slots", sessionSlots)
        }


		PutItemOutcome outcome = table.putItem(item)
        return getSpeechletResponse("Ok, I will pause", "", false)
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
    /**
     *
     * @param userName
     * @param password
     * @param path
     * @param parameters
     * @return json response from backend
     */
    private def backendRequest(def userName, def password, def path, Map<String, String> parameters) {
        def requestUrl = new RESTClient(EOTUrl)
        //requestUrl.auth.basic userName, password
        def response = requestUrl.get(path: path,  query: parameters)


        return response.json
    }
}

