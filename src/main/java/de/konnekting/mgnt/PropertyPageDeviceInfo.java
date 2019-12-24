/*
 * Copyright (C) 2019 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of KONNEKTING DeviceConfig.
 *
 *   KONNEKTING DeviceConfig is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   KONNEKTING DeviceConfig is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with KONNEKTING DeviceConfig.  If not, see <http://www.gnu.org/licenses/>.
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
    private SystemType systemType;

    public PropertyPageDeviceInfo(byte[] data) throws IllegalArgumentException {
        manufacturerId = convertUINT16(data[2], data[3]);
        deviceId = data[4]&0xFF;
        revision = data[5]&0xFF;

        switch (data[6]) {
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
                throw new IllegalArgumentException("System type with " + String.format("0x%02x", data[6]) + " not known!");
        }
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
                ", systemType=" + systemType + '}';
    }
    

    

}
