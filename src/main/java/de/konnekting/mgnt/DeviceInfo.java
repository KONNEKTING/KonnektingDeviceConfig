/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of slicKnx.
 *
 *   slicKnx is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   slicKnx is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with slicKnx.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.konnekting.mgnt;


/**
 *
 * @author achristian
 */
public class DeviceInfo {

    private final int manufacturerId;
    private final short deviceId;
    private final short revisionId;
    private final byte deviceFlags;
    private final String individualAddress;
    
    public DeviceInfo(int manufacturerId, short deviceId, short revisionId, byte deviceFlags, String individualAddress) {
        this.manufacturerId = manufacturerId;
        this.deviceId = deviceId;
        this.revisionId = revisionId;
        this.deviceFlags = deviceFlags;
        this.individualAddress = individualAddress;
    }
    
    public int getManufacturerId() {
        return manufacturerId;
    }
    
    public short getRevisionId() {
        return revisionId;
    }
    
    public short getDeviceId() {
        return deviceId;
    }
    
    public byte getDeviceFlags(){
        return deviceFlags;
    }

    public String getIndividualAddress() {
        return individualAddress;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" + "manufacturerId=" + manufacturerId + ", deviceId=" + deviceId + ", revisionId=" + revisionId + ", deviceFlags=" + Integer.toBinaryString(deviceFlags) + ", individualAddress=" + individualAddress + '}';
    }
    
    
    
}
