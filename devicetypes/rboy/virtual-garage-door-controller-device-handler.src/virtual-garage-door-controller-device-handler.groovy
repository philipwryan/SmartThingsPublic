/*
 * -----------------------
 * --- DEVICE HANDLER ----
 * -----------------------
 *
 * STOP:  Do NOT PUBLISH the code to GitHub, it is a VIOLATION of the license terms.
 * You are NOT allowed share, distribute, reuse or publicly host (e.g. GITHUB) the code. Refer to the license details on our website.
 *
 */

/* **DISCLAIMER**
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * Without limitation of the foregoing, Contributors/Regents expressly does not warrant that:
 * 1. the software will meet your requirements or expectations;
 * 2. the software or the software content will be free of bugs, errors, viruses or other defects;
 * 3. any results, output, or data provided through or generated by the software will be accurate, up-to-date, complete or reliable;
 * 4. the software will be compatible with third party software;
 * 5. any errors in the software will be corrected.
 * The user assumes all responsibility for selecting the software and for the results obtained from the use of the software. The user shall bear the entire risk as to the quality and the performance of the software.
 */ 
 
def clientVersion() {
    return "01.02.04"
}

/*
 * Copyright RBoy Apps, redistribution or reuse of code is not allowed without permission
 *
 * Change Log
 * 2018-12-04 - (v01.02.04) Allow the user to pick their own icon for the device
 * 2018-10-16 - (v01.02.03) Support for new ST app
 * 2018-08-05 - (v01.02.02) Added health check and basic support for the new ST app
 * 2018-04-17 - (v01.02.01) Added command to definition
 * 2018-04-16 - (v01.02.00) Added vacation mode
 * 2018-04-12 - (v01.01.00) Workaround to reduce logs in recent activity
 * 2018-02-20 - (v01.00.00) Initial release
 *
 */
 
metadata {
	definition (name: "Virtual Garage Door Controller Device Handler", namespace: "rboy", author: "RBoy Apps", ocfDeviceType: "oic.d.garagedoor", mnmn: "SmartThings", vid:"generic-contact-4") {
		capability "Actuator"
		capability "Door Control"
		capability "Garage Door Control"
		capability "Contact Sensor"
		capability "Refresh"
		capability "Sensor"
        capability "Polling"
        capability "Switch"
        capability "Momentary"
        capability "Health Check"
        
        attribute "codeVersion", "string"
        attribute "dhName", "string"
        attribute "slot", "number"
        attribute "vacationMode", "string"
        
        command "enableVacation"
        command "disableVacation"
        command "reportEvent", ["JSON_OBJECT"]
	}

    preferences {
        input title: "", description: "Virtual Garage Door Controller Device Handler v${clientVersion()}", displayDuringSetup: false, type: "paragraph", element: "paragraph"
    }

	tiles(scale: 2) {
		multiAttributeTile(name:"summary", type: "generic", width: 6, height: 4){
			tileAttribute ("device.door", key: "PRIMARY_CONTROL") {
                attributeState("unknown", label:'${name}', action:"refresh.refresh", icon:"st.doors.garage.garage-open", backgroundColor:"#e86d13")
                attributeState("closed", label:'${name}', action:"door control.open", icon:"st.doors.garage.garage-closed", backgroundColor:"#00a0dc", nextState:"opening")
                attributeState("open", label:'${name}', action:"door control.close", icon:"st.doors.garage.garage-open", backgroundColor:"#e86d13", nextState:"closing")
                attributeState("opening", label:'${name}', icon:"st.doors.garage.garage-opening", backgroundColor:"#e86d13")
                attributeState("closing", label:'${name}', icon:"st.doors.garage.garage-closing", backgroundColor:"#00a0dc")
            }
            //tileAttribute ("device.lowBattery", key: "SECONDARY_CONTROL") {
	        //    attributeState "battery", label:'${currentValue}', backgroundColor:"#ffffff"
            //}
        }
		standardTile("toggle", "device.door", width: 4, height: 4, canChangeIcon: true) {
			state("unknown", label:'${name}', action:"refresh.refresh", icon:"st.doors.garage.garage-open", backgroundColor:"#e86d13")
			state("closed", label:'${name}', action:"door control.open", icon:"st.doors.garage.garage-closed", backgroundColor:"#00a0dc", nextState:"opening")
			state("open", label:'${name}', action:"door control.close", icon:"st.doors.garage.garage-open", backgroundColor:"#e86d13", nextState:"closing")
			state("opening", label:'${name}', icon:"st.doors.garage.garage-opening", backgroundColor:"#e86d13")
			state("closing", label:'${name}', icon:"st.doors.garage.garage-closing", backgroundColor:"#00a0dc")
		}
		standardTile("open", "device.door", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'open', action:"door control.open", icon:"st.doors.garage.garage-opening"
		}
		standardTile("close", "device.door", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'close', action:"door control.close", icon:"st.doors.garage.garage-closing"
		}
		standardTile("refresh", "device.door", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("vacation", "device.vacationMode", width: 2, height: 2, decoration: "flat") {
			state "disabled", label:'', action:"enableVacation", icon:"http://www.rboyapps.com/images/Vacation.png", backgroundColor:"#ffffff", defaultState: true
			state "enabled", label:'Vacation', action:"disableVacation", icon:"http://www.rboyapps.com/images/Vacation.png", backgroundColor:"#00a0dc"
		}
		standardTile("blank2x", "device.door", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"", icon:""
		}
        

		main "toggle"
		details(["summary", "open", "close", "vacation", "blank2x", "refresh", "blank2x"])
	}
}

def installed() {
	log.trace "Installed called settings: $settings"

    // Device-Watch simply pings if no device events received for 32min(checkInterval)
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

    try {
        refresh() // Configure and get updated
	} catch (e) {
		log.warn "updated() threw $e"
	}
}

def updated() {
	log.trace "Update called settings: $settings"

    // Device-Watch simply pings if no device events received for 32min(checkInterval)
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

    try {
        refresh() // Configure and get updated
	} catch (e) {
		log.warn "updated() threw $e"
	}
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	refresh()
}

def parse(event) {
	log.trace "Parse: $event"
    sendEvent(event)
}

def open() {
    log.trace "Garage door Open called"
    
    if (device.currentValue("vacationMode") == "enabled") {
        log.warn "Device in vacation mode"
        return
    }
    
    parent.open(this)
}

def close() {
    log.trace "Garage door Close called"
    
    if (device.currentValue("vacationMode") == "enabled") {
        log.warn "Device in vacation mode"
        return
    }
    
    parent.close(this)
}

def enableVacation() {
    sendEvent(name: "vacationMode", value: "enabled")
}

def disableVacation() {
    sendEvent(name: "vacationMode", value: "disabled")
}

def refresh() {
	log.trace "Refresh called"
    poll()
}

def poll() {
	log.trace "Poll called"

    sendEvent([name: "codeVersion", value: clientVersion()]) // Save client version for parent app
    sendEvent([name: "dhName", value: "Virtual Garage Door Controller Device Handler"]) // Save DH Name for parent app
    
    parent.refresh(this)
}

def on() {
    log.trace "Garage door Turning on"
    
    if (device.currentValue("vacationMode") == "enabled") {
        log.warn "Device in vacation mode"
        return
    }
    
    parent.open(this)
}

def off() {
    log.trace "Garage door Turning off"
    
    if (device.currentValue("vacationMode") == "enabled") {
        log.warn "Device in vacation mode"
        return
    }
    
    parent.close(this)
}

def push() {
    def latest = device.latestValue("door");
	log.trace "Garage door push button, current state $latest"
    
    if (device.currentValue("vacationMode") == "enabled") {
        log.warn "Device in vacation mode"
        return
    }

	switch (latest) {
    	case "open":
    	case "opening":
        	log.debug "Closing garage door"
        	parent.close(this)
            break
            
        case "closed":
    	case "closing":
        	log.debug "Opening garage door"
        	parent.open(this)
            break
            
        default:
        	log.warn "Can't change state of door, unknown state $latest"
            break
    }
}

def setSlot(Integer num) {
    sendEvent(name: "slot", value: num)
    log.trace "Set slot to ${num}"
}

def reportEvent(event) {
    log.trace "Sending event: $event"
    sendEvent(event)
}

// THIS IS THE END OF THE FILE

