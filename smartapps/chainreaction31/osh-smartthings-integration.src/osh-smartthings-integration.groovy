/**
 *  OSH Interface
 *
 *  Copyright 2017 Ian Patterson
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

definition(
        name: "OSH SmartThings Integration",
        namespace: "ChainReaction31",
        author: "Ian Patterson",
        description: "Integrate SmartThings into OpenSensorHub.",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
        
        //OSH Related settings


preferences {

    // Might not need all available pages
    // Code excerpts from https://github.com/erocm123/SmartThingsPublic/blob/master/smartapps/erocm123/device-monitor.src/device-monitor.groovy
    page name: "pageConfigure"
}

//***************************
//Show Configure Page
//***************************
def pageConfigure() {

    def inputMotionDevices = [name: "motiondevices", type: "capability.motionSensor", title: "Which motion sensors?", multiple: true, required: false]
    def inputHumidityDevices = [name: "humiditydevices", type: "capability.relativeHumidityMeasurement", title: "Which humidity sensors?", multiple: true, required: false]
    def inputLeakDevices = [name: "leakdevices", type: "capability.waterSensor", title: "Which leak sensors?", multiple: true, required: false]
    def inputThermoDevices = [name: "thermodevices", type: "capability.thermostat", title: "Which thermostats?", multiple: true, required: false]
    def inputTemperature = [name: "tempdevices", type: "capability.temperatureMeasurement", title: "Which temperature sensors?", multiple: true, required: false]
    def inputContactDevices = [name: "contactdevices", type: "capability.contactSensor", title: "Which open/close contact sensors?", multiple: true, required: false]
    def inputLockDevices = [name: "lockdevices", type: "capability.lock", title: "Which locks?", multiple: true, required: false]
    def inputAlarmDevices = [name: "alarmdevices", type: "capability.alarm", title: "Which alarms/sirens?", multiple: true, required: false]
    def inputSwitchDevices = [name: "switchdevices", type: "capability.switch", title: "Which switches?", multiple: true, required: false]
    def inputPresenceDevices = [name: "presencedevices", type: "capability.presenceSensor", title: "Which presence sensors?", multiple: true, required: false]
    def inputSmokeDevices = [name: "smokedevices", type: "capability.smokeDetector", title: "Which Smoke/CO2 detectors?", multiple: true, required: false]
    def inputButtonDevices = [name: "buttondevices", type: "capability.button", title: "Which Button Devices?", multiple: true, required: false]
    def inputHubDevices = [name: "hubdevices", type: "hub", title: "Which SmartThings Hubs?", multiple: true, required: false]
    
    def endpoint = [name: "endpoint", type: "text", title: "OSH SOS-T endpoint URL", multiple: false, required: true]

    def pageProperties = [name: "pageConfigure",
                          title: "Device Monitor - Configure Devices",
                          nextPage: null,
                          params: [refresh: false],
                          uninstall: true,
                          install: true
    ]

    return dynamicPage(pageProperties) {

        section("Devices To Check") {
            input inputMotionDevices
            input inputContactDevices
            input inputLockDevices
            input inputSwitchDevices
            input inputHumidityDevices
            input inputLeakDevices
            input inputThermoDevices
            input inputTemperature
            input inputAlarmDevices
            input inputPresenceDevices
            input inputSmokeDevices
            input inputButtonDevices
            //input inputHubDevices
            
            // Input OSH SOS-T endpoint
            input endpoint
        }

    }
}

//***************************
//Show Status page
//***************************
def pageStatus(params) {

    def pageProperties = [
            name: "pageStatus",
            title: "Device Monitor - Status",
            nextPage: null,
            install: false,
            uninstall: false
    ]

    if (settings.motiondevices == null &&
            settings.humiditydevices == null &&
            settings.leakdevices == null &&
            settings.thermodevices == null &&
            settings.tempdevices == null &&
            settings.contactdevices == null &&
            settings.lockdevices == null &&
            settings.alarmdevices == null &&
            settings.switchdevices == null &&
            settings.smokedevices == null &&
            settings.buttondevices == null &&
            settings.hubdevices == null &&
            settings.presencedevices == null) {
        return pageConfigure()
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"


    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    // map of capability:uri pairs
    atomicState.URIs = [:]
    def metaSensor = atomicState.URIs
    atomicState.sensorCaps = [:]

    //doCheck(metaSensor)
    generateSML()

    atomicState.URIs = metaSensor

    // Check SensorMap
    log.trace "Printing sensor map" + atomicState.URIs

    // Subscribe to events
    log.info settings.motiondevices
    subscribe(motiondevices, "motion", scheduleHandler)
    log.info contactdevices
    subscribe(contactdevices, "contact", scheduleHandler)
    subscribe(switchdevices,"switch", scheduleHandler)
    subscribe(lockdevices,"lock", scheduleHandler)
    subscribe(presencedevices,"presence", scheduleHandler)
    subscribe(temperaturedevices, "temperature", scheduleHandler)
    /*subscribe(inputHumidityDevices, "", humidityHandler)
    subscribe(inputLeakDevices, "", leakHandler)
    subscribe(inputThermoDevices, "", thermoHandler)
    subscribe(inputAlarmDevices,"", alarmHandler)
    subscribe(inputSmokeDevices,"",smokeHandler)
    subscribe(inputButtonDevices,"",buttonHandler)*/

    // Schedule minute data polling
    runEvery1Minute(scheduleHandler)

}

// TODO: add parameters for server uri
def generateInsertSML(String sensorName){
    def beginningXML = '''<?xml version="1.0" encoding="UTF-8"?>
<swes:InsertSensor service="SOS" version="2.0"
    xmlns:gml="http://www.opengis.net/gml/3.2"
    xmlns:sml="http://www.opengis.net/sensorml/2.0"
    xmlns:sos="http://www.opengis.net/sos/2.0"
    xmlns:swe="http://www.opengis.net/swe/2.0"
    xmlns:swes="http://www.opengis.net/swes/2.0" xmlns:xlink="http://www.w3.org/1999/xlink">
    <swes:procedureDescriptionFormat>http://www.opengis.net/sensorml/2.0</swes:procedureDescriptionFormat>
    <swes:procedureDescription>
        <sml:PhysicalSystem gml:id="SENSORNET">
            <gml:identifier codeSpace="uid">urn:osh:client:'''

    def endingXML = '''</gml:identifier>
            <gml:name>''' + sensorName + '''</gml:name>
        </sml:PhysicalSystem>
    </swes:procedureDescription>
    <swes:metadata>
        <sos:SosInsertionMetadata>
            <sos:observationType>http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation</sos:observationType>
            <sos:featureOfInterestType>gml:Feature</sos:featureOfInterestType>
        </sos:SosInsertionMetadata>
    </swes:metadata>
</swes:InsertSensor>'''

    def fullXMLBody = beginningXML + sensorName + endingXML
    //log.info fullXMLBody
    return fullXMLBody
}

// TODO: Parse sensor description to generate result structure
def generateDescriptionSML(theSensor, currCapability) {
    String sensorName = theSensor
    log.trace "[ln:201]Capability genDescription: " + currCapability
    def xmlDescBeginning = '''<?xml version="1.0" encoding="UTF-8"?>
<sos:InsertResultTemplate service="SOS" version="2.0"
    xmlns:gml="http://www.opengis.net/gml/3.2"
    xmlns:om="http://www.opengis.net/om/2.0"
    xmlns:sos="http://www.opengis.net/sos/2.0"
    xmlns:swe="http://www.opengis.net/swe/2.0"
    xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <sos:proposedTemplate>
        <sos:ResultTemplate>
            <sos:offering>urn:osh:client:''' + sensorName + '''-sos</sos:offering>
            <sos:observationTemplate>
                <om:OM_Observation gml:id="OBS_001">
                    <om:phenomenonTime>
                        <gml:TimeInstant gml:id="T1">
                            <gml:timePosition indeterminatePosition="unknown"/>
                        </gml:TimeInstant>
                    </om:phenomenonTime>
                    <om:resultTime>
                        <gml:TimeInstant gml:id="T2">
                            <gml:timePosition indeterminatePosition="unknown"/>
                        </gml:TimeInstant>
                    </om:resultTime>
                    <om:procedure xsi:nil="true"/>
                    <om:observedProperty xsi:nil="true"/>
                    <om:featureOfInterest xsi:nil="true"/>
                    <om:result/>
                </om:OM_Observation>
            </sos:observationTemplate>
            <sos:resultStructure>
                <swe:DataRecord>
                    <swe:field name="time">
                        <swe:Time
                            definition="http://www.opengis.net/def/property/OGC/0/SamplingTime" referenceFrame="http://www.opengis.net/def/trs/BIPM/0/UTC">
                            <swe:label>Sampling Time</swe:label>
                            <swe:uom xlink:href="http://www.opengis.net/def/uom/ISO-8601/0/Gregorian"/>
                        </swe:Time>
                    </swe:field>'''

    def xmlDescEnd = '''                
				</swe:DataRecord>
            </sos:resultStructure>
            <sos:resultEncoding>
                <swe:TextEncoding blockSeparator="&#xa;"
                    collapseWhiteSpaces="true" decimalSeparator="." tokenSeparator=","/>
            </sos:resultEncoding>
        </sos:ResultTemplate>
    </sos:proposedTemplate>
</sos:InsertResultTemplate>'''


    //def xmlDesc = ""
    def xmlDesc = generateResultTemplate(theSensor, capability) // take a capability instead
    if (xmlDesc != null){
    	def requestSML = xmlDescBeginning + xmlDesc + xmlDescEnd
    	return requestSML
    }
    else {return}

    //log.debug "[ln:256]Generated SML Description: " + requestSML
    
}

// Return the SML snippet needed by insertResultTemplate to
// allow the sensor to send data to SOS
// generate SML desc based on capability
def generateResultTemplate(sensor){
    // TODO: add name to each output based on capability
    log.debug "[ln:268]Capabilities: " + sensor.getCapabilities()
    def description = ""
    def capMap = atomicState.sensorCaps
    def capabilitiesList = capMap.getAt(removeSpaces(sensor.getLabel()))

    log.info "[ln:274]Capability Name: " + capability
    switch(capability){
        // TODO: Above <swe:field name="{sensorName}"> add  <sml:output name="{capabilityName}">
        case "Contact Sensor":
        description += '''<swe:field name="contact">
                <swe:Category definition="http://sensorml.com/ont/swe/property/ContactSensor">
                <swe:constraint>
                <swe:AllowedTokens>
                <swe:value>closed</swe:value>
                <swe:value>open</swe:value>                                    
                </swe:AllowedTokens>
                </swe:constraint>
                </swe:Category>
                </swe:field>'''
        capabilitiesList.add("contact")
        	break
        case "Motion Sensor":
        description += '''<swe:field name="motion">
                <swe:Category definition="http://sensorml.com/ont/swe/property/MotionSensor">
                <swe:constraint>
                <swe:AllowedTokens>
                <swe:value>active</swe:value>
                <swe:value>inactive</swe:value>                                    
                </swe:AllowedTokens>
                </swe:constraint>
                </swe:Category>
                </swe:field>'''
        capabilitiesList.add("motion")
        	break
        case "Lock":
        description += '''<swe:field name="lock">
                <swe:Category definition="http://sensorml.com/ont/swe/property/Lock">
                <swe:constraint>
                <swe:AllowedTokens>
                <swe:value>locked</swe:value>
                <swe:value>unlocked</swe:value>
                <swe:value>unknown</swe:value>
                <swe:value>unlocked with timeout</swe:value>                                    
                </swe:AllowedTokens>
                </swe:constraint>
                </swe:Category>
                </swe:field>'''
        capabilitiesList.add("lock")
        	break
        case "Switch":
        description += '''<swe:field name="switch">
                <swe:Category definition="http://sensorml.com/ont/swe/property/Switch">
                <swe:constraint>
                <swe:AllowedTokens>
                <swe:value>on</swe:value>
                <swe:value>off</swe:value>                                    
                </swe:AllowedTokens>
                </swe:constraint>
                </swe:Category>
                </swe:field>'''
        capabilitiesList.add("switch")
        	break
        case "Temperature Measurement":
        description += '''<swe:field name="temperature">
                <swe:Quantity definition="http://sensorml.com/ont/swe/property/AirTemperature">
                <swe:label>Air Temperature</swe:label>
                <swe:uom code="F"/>
                </swe:Quantity>
                </swe:field>'''
        capabilitiesList.add("temperature")
        	break
        case "Presence Sensor":
        description += '''<swe:field name="presence">
                <swe:Category definition="http://sensorml.com/ont/swe/property/PresenceSensor">
                <swe:constraint>
                <swe:AllowedTokens>
                <swe:value>not present</swe:value>
                <swe:value>present</swe:value>                                    
                </swe:AllowedTokens>
                </swe:constraint>
                </swe:Category>
                </swe:field>'''
        capabilitiesList.add("presence")
        	break
        default:
        description = null
            break        
    }
    log.debug "[ln:356] Generated Field Tags: " + description
    log.debug "[ln:357] Current Sensor Capabilites: " + capabilitieslist
    capMap.put(removeSpaces(sensor.getLabel()), capabilitiesList)
    log.debug "[ln:359] Capabilties List: " + capMap
    atomicState.sensorCaps = capMap
    return description
}

def getOSHDate(date){
    def dateSplit = date.split(" ", 6)
    def month, day, year, time
    //month = dateSplit[1]
    month = monthConvert(dateSplit[1])
    day = dateSplit[2]
    year = dateSplit[5]
    time = dateSplit[3]
    def newDate = year + '-' + month + '-' + day + 'T' + time + 'Z'
    log.info "Formatted Date String: " + newDate
    return newDate
}

def monthConvert(month){
    def newMonth
    switch(month){
        case 'Jan':
            newMonth = '01'
            break
        case 'Feb':
            newMonth = '02'
            break
        case 'Mar':
            newMonth = '03'
            break
        case 'Apr':
            newMonth = '04'
            break
        case 'May':
            newMonth = '05'
            break
        case 'Jun':
            newMonth = '06'
            break
        case 'Jul':
            newMonth = '07'
            break
        case 'Aug':
            newMonth = '08'
            break
        case 'Sep':
            newMonth = '09'
            break
        case 'Oct':
            newMonth = '10'
            break
        case 'Nov':
            newMonth = '11'
            break
        case 'Dec':
            newMonth = '12'
            break
    }
    return newMonth
}


def generateSML(){

    //def allDevices = []

    [motiondevices, humiditydevices, leakdevices, thermodevices, tempdevices, contactdevices,
     lockdevices, alarmdevices, switchdevices, presencedevices, smokedevices, buttondevices].each { n ->
        if (n != null){

            for (x in n){
                log.debug "[ln:504]Logging capability: " + x.getCapabilities()
                //def count = 0
                def params = [
                        //uri: 'http://146.148.39.135:8181/sensorhub/sos',
                        uri: endpoint,
                        body: generateInsertSML(x.getLabel()),
                        requestContentType: 'application/xml'
                ]
                insertSensor(params)
                for (currCapability in x.getCapabilities()){
                	insertResultTemplate(x)
                }

            }
            //allDevices += n
        }
    }
}

// Handle inserting sensor into OSH via SOS-T
def insertSensor(params) {

    //log.trace "Params: " + params

    try {
        httpPost(params) { resp ->
            resp.headers.each {
                //log.debug "${it.name} : ${it.value}"
            }
        }
    } catch (e) {
        log.error "Adding sensor failed: $e"
    }
    //log.trace "Sensor description: " + metaSensor
}



// Handle inserting a sensor's result template via SOS-T
def insertResultTemplate(sensorName) {

    // TODO: change sensor uri map back to previous method if needed
    def URIs = atomicState.URIs
    def sensorURIs = []
    log.trace "[ln:541]sensorCapability (insertResultTemplate): " + sensorName.getCapabilities()
    
    for(capability in sensorName.getCapabilities()){
        try {
            def paramsRequest = [
                    //uri: 'http://146.148.39.135:8181/sensorhub/sos',
                    uri: endpoint,
                    body: generateDescriptionSML(sensorName, capability),
                    requestContentType: 'application/xml'
            ]
            httpPost(paramsRequest) { resp2 ->
                resp2.headers.each {
                    //log.info "${it.name} : ${it.value}"
                }

                log.debug "response data: ${resp2.data}"

                String data = resp2.data
                sensorURIs.add(data)
                URIs.put(capability.getName(), sensorURIs)
            }
        } catch (e) {
            log.error "Inserting Request Template failed: $e"
        }
    }
    log.trace 'MetaSensor data: ' +  URIs
    URIs.put(sensorName, sensorURIs)
    atomicState.URIs = URIs
    log.debug "Current URI map: " + atomicState.URIs
}


// schedule data polling (is executed once per minute)
def scheduleHandler(){
    def sensors = atomicState.URIs
    def capMap = atomicState.sensorCaps
    // TODO: examine responses to insertResultTemplate requests to isolate how to structure the posts in order to provide proper sensor data
    [motiondevices, humiditydevices, leakdevices, thermodevices, tempdevices, contactdevices,
     lockdevices, alarmdevices, switchdevices, presencedevices, smokedevices, buttondevices].each { n ->
        if (n != null){

            for (x in n){
            
                // create cs string to feed 
                def dataString = ""
                def count = 0
                for ( capability in capMap.getAt(x.getLabel())){
                    count++
                    log.debug "Size of caps: " + capMap.getAt(x.getLabel()).size()
                    def size = capMap.getAt(x.getLabel()).size()
                    log.trace "[ln:604] Capability: " + capability
                    log.debug "Current Value of capability: " + x.currentValue(capability).toString()
                    dataString += x.currentValue(capability)
                    if (count < size){
                        dataString += ","
                    }
                }
                
                //get current time
                log.debug "Current time: " + now()
                def currDate = new Date(now())
                log.debug "Current date: " + currDate
                String stringDate = currDate
                def time = getOSHDate(stringDate)
                log.debug time

                try {
        			def request = [
                        //uri: 'http://146.148.39.135:8181/sensorhub/sos',
                        uri: endpoint,
                		body: '''<sos:InsertResult xmlns:sos="http://www.opengis.net/sos/2.0" service="SOS" version="2.0.0">
    						<sos:template>''' + atomicState.URIs.getAt(x.getLabel()) + '''</sos:template>
   							<sos:resultValues>''' + time + ',' + dataString  + '''</sos:resultValues>
    						</sos:InsertResult>''',
                		requestContentType: 'application/xml'
                    ]

        			log.debug "Result Request: " + request.body

        			httpPost(request) { resp2 ->
            			resp2.headers.each {
                		//log.info "${it.name} : ${it.value}"
           				}
            			//log.debug "response contentType: ${resp2.contentType}"
            			log.debug "response data: ${resp2.data}"
        			}
    			} catch (e) {
        		log.error "Sending Data failed: $e"
    			}
            }
    	} 
	}
}

// TODO: determine if there's a way to get the capability that triggered the even without resorting to versions for each type of capability
// TODO: could possibly have the capability doing the trigger passed in as a string (may be able to parse event.getData() or getName() for the info)
// schedule data polling (is executed on event)
def scheduleHandler(evt){
    log.debug "Event name: ${evt.name}"
    log.debug "Event data: ${evt.data}"
    def sensors = atomicState.URIs
    def capMap = atomicState.sensorCaps

    [motiondevices, humiditydevices, leakdevices, thermodevices, tempdevices, contactdevices,
     lockdevices, alarmdevices, switchdevices, presencedevices, smokedevices, buttondevices].each { n ->
        if (n != null){

            /*for (x in n){
            
                // create cs string to feed 
                def dataString = ""
                def count = 0
                for ( capability in capMap.getAt(x.getLabel())){
                    count++
                    //log.debug "Size of caps: " + capMap.getAt(x.getLabel()).size()
                    def size = capMap.getAt(x.getLabel()).size()
                    log.trace "[ln:604] Capability: " + capability
                    log.debug "Current Value of capability: " + x.currentValue(capability).toString()
                    dataString += x.currentValue(capability)
                    if (count < size){
                        dataString += ","
                    }
                }*/
                
            def dataString = evt.value

            //get current time
            log.debug "Current time: " + now()
            def currDate = new Date(now())
            log.debug "Current date: " + currDate
            String stringDate = currDate
            def time = getOSHDate(stringDate)
            log.debug time

            try {
                def request = [
                    //uri: 'http://146.148.39.135:8181/sensorhub/sos',
                    uri: endpoint,
                    body: '''<sos:InsertResult xmlns:sos="http://www.opengis.net/sos/2.0" service="SOS" version="2.0.0">
						<sos:template>''' + atomicState.URIs.getAt(x.getLabel()) + '''</sos:template>
						<sos:resultValues>''' + time + ',' + dataString  + '''</sos:resultValues>
						</sos:InsertResult>''',
                    requestContentType: 'application/xml'
                ]

                log.debug "Result Request: " + request.body

                httpPost(request) { resp2 ->
                    resp2.headers.each {
                        //log.info "${it.name} : ${it.value}"
                    }
                    //log.debug "response contentType: ${resp2.contentType}"
                    log.debug "response data: ${resp2.data}"
                }
            } catch (e) {
                log.error "Sending Data failed: $e"
            }
        }
    } 
}

// remove spaces in sensor labels
def removeSpaces(label){
	/*def label2 = label.split(" ")
	log.debug "Label: " + label2
    def replacement = ""
    log.debug "Init replacement: " + replacement
    
    for(x in label2){
    	replacement += x
        log.debug replacement
    }
    log.debug "Label replacement: " + replacement
    return replacement*/
    return label
}