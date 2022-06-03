metadata {
  // implementation of a few functions from the api described at:
  // https://developer.sony.com/develop/audio-control-api/documentation/api-reference

  definition (
    author: "The Fabio",
    description: "Device to handle calls to the iterations with the Sony STR-DN1080 receiver API",
    importUrl: "https://raw.githubusercontent.com/TheFabio/hubitat/master/src/sony-receiver-str-DN1080.groovy",
    name: "Sony Receiver STR-DN1080",
    namespace: "thefabio"
  ) {
     capability "AudioVolume"
     capability "Switch"
     capability "SwitchLevel" // necessary for the vertical level
     capability "PushableButton"

     command "getZoneURIs"

     attribute "connectionStatus", "enum", ['connected', 'disconnected']
  }
  preferences {
        input title: "IP Address",
            name: "ipAddress",
            type: "text",
            description: "The ip address (or dns) where the Sony receiver STR-DN1080 is configured",
            required: true,
            defaultValue: "192.168.0.3"

        input title: "Zone URI",
            name: "zoneUri",
            type: "text",
            description: "The zone URI this instance controls (use the command getZoneURIs to view at the log the available zones)"

        input title: "Debug Mode",
            name: "debugMode",
            type: "bool",
            description: "Write debug mode log entries",
            required: true,
            defaultValue: false
  }
}

// default methods - begin
// This method is called when the device is first created and can be used to initialize any device specific configuration and setup
def installed() {
  setupDevice()
}

// This method is called when the device is removed to allow for any necessary cleanup.
def uninstalled() {
  unschedule()
}

// This method is called when the preferences of a device are updated.
def updated() {}

//This method is called in response to a message received by the device driver
def parse(String description) {
}
// default methods - end

// capability AudioVolume - Begin
def mute() {
  setAudioMute("on")
}
def unmute() {
  setAudioMute("off")
}
def setVolume(level) {
  writeLog "setVolume ${level}%"
  setAudioVolumeFromLevel(level)
}

def volumeDown() {
  writeLog "volumeDown"
  if (stateValue(device, "switch") != 'on') return;

  setAudioVolume("-5")
}

def volumeUp() {
  writeLog "volumeUp"
  if (stateValue(device, "switch") != 'on') return;

  setAudioVolume("+5")
}
// capability AudioVolume - End

//capability "SwitchLevel" - begin
def setLevel(level, duration) {
  writeLog "SwitchLevel setLevel - level: ${level}%"
  setAudioVolumeFromLevel(level)
}
//capability "SwitchLevel" - end


//capability "Switch" - begin
def on() {
  powerOn()
  runInMillis(500, 'getPowerStatus');
}

def off() {
  powerOff()
  runInMillis(500, 'getPowerStatus');
}
//capability "Switch" - end

//requirement of Vertical Level template - Begin
def setLevel(level) {
  writeLog "Vertical Level setLevel - level: ${level}%"
  setAudioVolumeFromLevel(level)
}
//requirement of Vertical Level template - End

//capability "PushableButton" - begin
def push(buttonNumber){
  // although the docs enumerate the buttonNumber var as number, it was observed as string
  writeLog "push, buttonNumber ${buttonNumber}"
  switch (buttonNumber){
    case "volumeUp":
      setAudioVolume("+3")
      break;
    case "volumeDown":
      setAudioVolume("-3")
      break;
    case "mute":
      setAudioMute("on")
      break;
    case "unmute":
      setAudioMute("off")
      break;
  }
}
//capability "PushableButton" - end

def setupDevice() {
  unschedule()
  schedule("0/15 * * ? * *", "refreshStatus") // every 15 seconds
}

def refreshStatus() {
  getPowerStatus()
  if (stateValue(device, "switch") != 'on') return

  runInMillis(200, 'getVolumeInformation')
}

def getPowerStatus() {
  writeLog "getPowerStatus"
  asynchttpPost("getPowerStatusHandler", [
            uri: "http://${ipAddress}:10000/sony/system",
            requestContentType: 'application/json',
            body : [
              "method":"getPowerStatus",
              "id":65,
              "params":[],
              "version":"1.1"
              ]
        ])
}

def getPowerStatusHandler(response, data) {
    genericResponseHandler(response, null)
    if (response.status != 200) {
        return
    }

    payload = parseJson(response.data)
    if (payload.error) return;

    if (payload.result[0].status == "active") {
      updateStateValue(device, "switch", 'on')
    } else {
      updateStateValue(device, "switch", 'off')
    }
}

def getVolumeInformation() {
  writeLog "getVolumeInformation"
  if (zoneUri == null || zoneUri == ""){
    log.error "'zoneUri' configuration is missing"
    return
  }
  asynchttpPost("getVolumeInformationHandler", [
            uri: "http://${ipAddress}:10000/sony/audio",
            requestContentType: 'application/json',
            body : [
              "method":"getVolumeInformation",
              "id":50,
              "params":[["output":zoneUri]],
              "version":"1.1"
              ]
        ])
}

def getVolumeInformationHandler(response, date) {
  genericResponseHandler(response, null)
  if (response.status != 200) {
      return
  }

  payload = parseJson(response.data)
  if (payload.error) return;

  for (item in payload.result[0]) {
    int level = (int) Math.round(item.volume / 0.55);
    if (level > 100){
      level = 100
    }
    if (level < 0){
      level = 0
    }
    writeLog "getVolumeInformationHandler item.volume: ${item.volume}, level: ${level}"

    updateStateValue(device, "volume", level)
    updateStateValue(device, "level", level)
  }
}

def genericResponseHandler(response, date) {
    writeLog "http status: ${response.status}"

    if (response.status != 200) {
        updateStateValue(device, 'connectionStatus', 'disconnected')
        writeLog response.properties.collect{it}.join(',')
        return
    }
    updateStateValue(device, 'connectionStatus', 'connected')

    payload = parseJson(response.data)
    if (payload.error){
      log.error "payload.error: ${payload.error}"
    }
}

def setAudioMute(muteValue) {
  writeLog "setAudioMute muteValue: ${muteValue}"
  asynchttpPost("genericResponseHandler", [
              uri: "http://${ipAddress}:10000/sony/audio",
              requestContentType: 'application/json',
              body : [
                "method":"setAudioMute",
                "id":64,
                "params":[["mute": muteValue,  "output":zoneUri]], // muteValue ~ "off" , "on" , "toggle"
                "version":"1.1"
                ]
          ])
}

def setAudioVolumeFromLevel(level){
    if (stateValue(device, "switch") != 'on') {
            return
    }

    int volume = (int) Math.round(0.55 * level);
    if (volume > 55) volume = 55
    else if (volume < 0) volume = 0

    setAudioVolume(volume)
}

def setAudioVolume(volume) {
  writeLog "setAudioVolume ${volume} (0-55)"
  if (zoneUri == null || zoneUri == ""){
    log.error "'zoneUri' configuration is missing"
    return
  }
  if (stateValue(device, "switch") != 'on') return;

  asynchttpPost("genericResponseHandler", [
              uri: "http://${ipAddress}:10000/sony/audio",
              requestContentType: 'application/json',
              body : [
                "method":"setAudioVolume",
                "id":63,
                "params":[["volume":"${volume}", "output":zoneUri]],
                "version":"1.1"
                ]
          ])
  runInMillis(500, 'getVolumeInformation');
}

def setPowerStatus(status) {
  writeLog "setPowerStatus ${status}"
  asynchttpPost("genericResponseHandler", [
                  uri: "http://${ipAddress}:10000/sony/system",
                  requestContentType: 'application/json',
                  body : [
                    "method": "setPowerStatus",
                    "id":62,
                    "params": [["status": status]],
                    "version":"1.1"
                    ]
              ])
}

def powerOn() {
  writeLog "powerOn"
  setPowerStatus("active")
}

def powerOff() {
  writeLog "powerOff"
  setPowerStatus("off")
}

def getZoneURIs() {
  // prettier function name
  writeLog "getZoneURIs"
  getCurrentExternalTerminalsStatus()
}

def getCurrentExternalTerminalsStatus() {
  writeLog "getZonegetCurrentExternalTerminalsStatusURIs"
  asynchttpPost("getCurrentExternalTerminalsStatusHandler", [
            uri: "http://${ipAddress}:10000/sony/avContent",
            requestContentType: 'application/json',
            body : [
              "method":"getCurrentExternalTerminalsStatus",
              "id":65,
              "params":[],
              "version":"1.0"
              ]
        ])
}

def getCurrentExternalTerminalsStatusHandler(response, data) {
    genericResponseHandler(response, null)
    if (response.status != 200) {
        return
    }

    payload = parseJson(response.data)
    if (payload.error) return;

    log.info "*** Available Zone URIs - Begin ***"
    for (item in payload.result[0]) {
      log.info "${item.active} - ${item.title} - URI: ${item.uri}"
    }
    log.info "*** Available Zone URIs - End ***"

    for (item in payload.result[0]) {
      writeLog item
    }
}

def writeLog(logLine){
    if (!debugMode){
        return
    }

    log.debug logLine
}

def stateValue(target, name) {
    target.currentState(name) ? target.currentState(name).value : ''
}

def updateStateValue(target, name, newValue) {
  if (stateValue(target, name) == "${newValue}") return;

  target.sendEvent(name: name, value: newValue)
}
