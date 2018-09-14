/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt;

import de.konnekting.deviceconfig.utils.Helper;
import de.konnekting.mgnt.protocol0x01.InvalidMessageException;
import de.konnekting.mgnt.protocol0x01.MsgPropertyPageResponse;
import static de.konnekting.deviceconfig.utils.Bytes2ReadableValue.*;

/**
 *
 * @author achristian
 */
public class PropertyPageDeviceInfo {

    public static final int PROPERTY_PAGE_NUM = 0x00;
    
    enum SystemType {
        SYSTEM_1, SYSTEM_2, SYSTEM_3
    }

    private int manufacturerId = -1;
    private int deviceId = -1;
    private int revision = -1;
    private byte deviceFlags;
    private SystemType systemType;

    public PropertyPageDeviceInfo(byte[] data) throws IllegalArgumentException {
        manufacturerId = convertUINT16(data[2], data[3]);
        deviceId = data[4]&0xFF;
        revision = data[5]&0xFF;
        deviceFlags = data[6];

        switch (data[7]) {
            case (byte)0x00:
                systemType = SystemType.SYSTEM_1;
                break;
            case (byte)0x01:
                systemType = SystemType.SYSTEM_2;
                break;
            case (byte)0x02:
                systemType = SystemType.SYSTEM_3;
                break;
            default:
                throw new IllegalArgumentException("System type with " + String.format("0x%02x", data[7]) + " not known!");
        }
    }

    public boolean isFactorySetting() {
        return (deviceFlags & 0x80) == 0x80;
    }

    public SystemType getSystemType() throws InvalidMessageException {
        return systemType;
    }

    public int getManufacturerId() {
        return manufacturerId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public int getRevision() {
        return revision;
    }

    @Override
    public String toString() {
        return "PropertyPageDeviceInfo{" + 
                "manufacturerId=" + String.format("0x%04x",manufacturerId) + 
                ", deviceId=" + String.format("0x%02x", deviceId) + 
                ", revision=" + String.format("0x%02x", revision) + 
                ", deviceFlags=" + String.format("0x%02x", deviceFlags) + 
                ", systemType=" + String.format("0x%02x", systemType) + '}';
    }
    

    

}
