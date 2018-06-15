/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt;

import de.konnekting.deviceconfig.utils.Helper;

/**
 *
 * @author achristian
 */
public class SystemReadOnlyTable {

    private int version;
    private byte deviceFlags;
    private int addressTableAddress;
    private int associationTableAddress;
    private int commobjectTableAddress;
    private int parameterTableAddress;

    public SystemReadOnlyTable(byte[] system) {
        version = Helper.getFromHILO(system[0], system[1]);
        deviceFlags = system[2];
        addressTableAddress = Helper.getFromHILO(system[3], system[4]);
        associationTableAddress = Helper.getFromHILO(system[5], system[6]);
        commobjectTableAddress = Helper.getFromHILO(system[7], system[8]);
        parameterTableAddress = Helper.getFromHILO(system[9], system[10]);
    }

    public int getAddressTableAddress() {
        return addressTableAddress;
    }

    public int getAssociationTableAddress() {
        return associationTableAddress;
    }

    public int getCommobjectTableAddress() {
        return commobjectTableAddress;
    }

    public int getParameterTableAddress() {
        return parameterTableAddress;
    }

    public int getVersion() {
        return version;
    }

    public byte getDeviceFlags() {
        return deviceFlags;
    }
    
}
