# Hubitat drivers

This project provides drivers to be used in [Hubtat](https://hubitat.com/)

## Drivers
General Hubitat's driver documentation can be found in the [manufacturer's Website](https://docs.hubitat.com/)

### Unbranded 8-Relay Ethernet controller HHC-N-8I8O  
[hhc-n-8180](src/hhc-n-8180.groovy)

This driver requires the board to be configured in "TCP Service" mode;

Please add the unit's ipaddress and port to the driver configuration

More information about the relay board can be found in my [board configuration repo](https://github.com/TheFabio/control-hcc-n-8180)

### Sony Received STR-DN1080
The [configured device](src/sony-receiver-str-DN1080.groovy) is capable of executing the following on a Sony Received STR-DN1080
* interrogating the receiver output zones
* performing power on/off
* set per selected zone
    * Volume level
    * Volume Up
    * Volume Down
    * mute
    * unmute

## Apps
### Time-Debounced Switch-off
App to select switch devices and switch them off automatically after a configured delay.
The automatic switch off is Time-Debounced
[Time-Debounced Switch-off](src/time-debounced-off-switch.goovy)

# Issues / Improvements

Please raise a github issue if you which to report issues or discuss enhancements.
 
