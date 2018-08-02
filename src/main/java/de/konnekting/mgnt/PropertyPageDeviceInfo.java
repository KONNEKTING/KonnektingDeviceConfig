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
        deviceId = data[4];
        revision = data[5];
        deviceFlags = data[6];

        switch (data[7]) {
            case 0:
                systemType = SystemType.SYSTEM_1;
            case 1:
                systemType = SystemType.SYSTEM_2;
            case 2:
                systemType = SystemType.SYSTEM_3;
            default:
                throw new IllegalArgumentException("System type with " + String.format("0x%02x", systemType) + " not known!");
        }
    }

    public boolean isFactorySetting() {
        return (deviceFlags & 0x80) == 1;
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
    
    

}
