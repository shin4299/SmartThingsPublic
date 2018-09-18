/**
 *  Xiaomi Door(v.0.0.1)
 *
 * MIT License
 *
 * Copyright (c) 2018 fison67@nate.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
 
import groovy.json.JsonSlurper

metadata {
	definition ("Tyco Door/Window Sensor", namespace: "smartthings", author: "SmartThings", runLocally: true, minHubCoreVersion: '000.017.0012', executeCommandsLocally: false, mnmn: "SmartThings", vid: "generic-contact") {
        capability "Sensor"
        capability "Contact Sensor"
        capability "Battery"
	capability "Refresh"
               
        attribute "lastCheckin", "Date"
        attribute "lastOpen", "Date"
        attribute "lastClosed", "Date"
        
	}


	simulator {
	}

	tiles {
		multiAttributeTile(name:"contact", type: "generic", width: 6, height: 4){
			tileAttribute ("device.contact", key: "PRIMARY_CONTROL") {
               	attributeState "open", label:'${name}', icon:"http://postfiles12.naver.net/MjAxODA0MDNfNTMg/MDAxNTIyNzI0MjgzMjk1.H97Au-OWeJ5aUpwpYbhu3P_H_cA0tMHz-EWpgDPmmTcg.Jcbknv16shFQ86cBtY-Z1n4Jx9P1WGWGpj4voOLxzV8g.PNG.shin4299/door_on1.png?type=w3", backgroundColor:"#e86d13"
            	attributeState "closed", label:'${name}', icon:"https://postfiles.pstatic.net/MjAxODA0MDJfMTI3/MDAxNTIyNjcwOTc2NDgy.WVcwn0G7-BnyFTkk4pUxZ44j-810YDbVb81-A-52D1gg.X_0ijEFzbyu8IeYXU_fr0mVtS4v_4JbZncfmoFCPH5cg.PNG.shin4299/door_off.png?type=w3", backgroundColor:"#00a0dc"
			}
            tileAttribute("device.battery", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'Battery: ${currentValue}%\n')
            }
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'\nLast Update: ${currentValue}')
            }
		}
        
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
        }
        valueTile("lastOpen_label", "", decoration: "flat") {
            state "default", label:'Last\nOpen'
        }
        valueTile("lastOpen", "device.lastOpen", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }
        valueTile("lastClosed_label", "", decoration: "flat") {
            state "default", label:'Last\nClosed'
        }
        valueTile("lastClosed", "device.lastClosed", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }

	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setInfo(String app_url, String id) {
	log.debug "${app_url}, ${id}"
	state.app_url = app_url
    state.id = id
}

def setStatus(params){
	log.debug "${params.key} : ${params.data}"
	def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
 	switch(params.key){
    case "contact":
	if(params.data == "true"){
    		sendEvent(name:"contact", value: "closed" )
    		sendEvent(name:"lastClosed", value: now )
	} else {
    		sendEvent(name:"contact", value: "open" )
    		sendEvent(name:"lastOpen", value: now )
	}		
    	break;
    case "batteryLevel":
    	sendEvent(name:"battery", value: params.data)
    	break;
    }
    
    updateLastTime()
}

def callback(physicalgraph.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
        sendEvent(name:"contact", value: (jsonObj.properties.contact == true ? "closed" : "open"))
        sendEvent(name:"battery", value: jsonObj.properties.batteryLevel)
    
        updateLastTime()
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

def updated() {
}

def updateLastTime(){
	def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    sendEvent(name: "lastCheckin", value: now)
}

def refresh(){
	log.debug "Refresh"
    def options = [
     	"method": "GET",
        "path": "/devices/get/${state.id}",
        "headers": [
        	"HOST": state.app_url,
            "Content-Type": "application/json"
        ]
    ]
    sendCommand(options, callback)
}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}

def makeCommand(body){
	def options = [
     	"method": "POST",
        "path": "/control",
        "headers": [
        	"HOST": state.app_url,
            "Content-Type": "application/json"
        ],
        "body":body
    ]
    return options
}


