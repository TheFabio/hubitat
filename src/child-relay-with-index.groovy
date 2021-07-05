metadata {
  definition (
    author: "The Fabio",
    description: "Device to raise on-off clicked events on parent devices",
    importUrl: "https://raw.githubusercontent.com/TheFabio/hubitat/master/src/child-relay-with-index.groovy",
    name: "child-relay-switch-with-index",
    namespace: "thefabio"
  ) {
     capability "RelaySwitch"

     attribute "relayIndex", "number"
  }
  preferences {
  }
}

// default methods - begin

// This method is called when the device is first created and can be used to initialize any device specific configuration and setup
def installed() {
}

// This method is called when the device is removed to allow for any necessary cleanup.
def uninstalled() {
}

// This method is called when the preferences of a device are updated.
def updated() {
}

//This method is called in response to a message received by the device driver
def parse(String description) {
}
// default methods - end

//capability "RelaySwitch" - begin
def on() {
    getParent().switchRelay(relayIndex, true)
}

def off() {
    getParent().switchRelay(relayIndex, false)
}
//capability "RelaySwitch" - end
