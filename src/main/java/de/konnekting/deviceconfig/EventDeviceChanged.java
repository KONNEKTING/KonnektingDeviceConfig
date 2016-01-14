/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.deviceconfig;


/**
 *
 * @author achristian
 */
public class EventDeviceChanged {

    private final DeviceConfigContainer deviceconfig;

    EventDeviceChanged(DeviceConfigContainer deviceconfig) {
        this.deviceconfig = deviceconfig;
    }

    public DeviceConfigContainer getDeviceconfig() {
        return deviceconfig;
    }
    
}
