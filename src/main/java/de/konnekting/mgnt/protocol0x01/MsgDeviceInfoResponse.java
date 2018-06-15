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
package de.konnekting.mgnt.protocol0x01;

import de.root1.slicknx.KnxException;
import de.root1.slicknx.Utils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author achristian
 */
class MsgDeviceInfoResponse extends ProgMessage {

    
    public MsgDeviceInfoResponse(byte[] data) throws InvalidMessageException {
        super(data);
        validateEmpty(10, 13);
    }

    /**
     * Manufacturer-ID, 2 bytes value, unsigned
     * @return 
     */
    public int getManufacturerId() {
        byte hi = data[2];
        byte lo = data[3];
        
        return ((hi << 8)&0xffff) + ((lo << 0)&0xff);
    }

    /**
     * Device ID, 1 byte value
     * @return 
     */
    public short getDeviceId() {
        return (short) (data[4]&0xff);
    }

    /**
     * Device revision, 1 byte value
     * @return 
     */
    public short getRevisionId() {
        return (short) (data[5]&0xff);
    }

    public byte getDeviceFlags() {
        return data[6];
    }

    public String getIndividualAddress() throws KnxException {
        return Utils.getIndividualAddress(data[7], data[8]).toString();
    }
    
    public byte getSystemType() {
        return data[9];
    }

    @Override
    public String toString() {
        String individualAddress = "n/a";
        try {
            individualAddress = getIndividualAddress();
        } catch (KnxException ex) {
            log.warn("cannot read IA from data", ex);
        }
        return "MsgDeviceInfoResponse{manufacturerId="+String.format("0x%04x", getManufacturerId())+", "
            + "deviceId="+String.format("0x%02x", getDeviceId())+", "
            + "revisionId="+String.format("0x%02x", getRevisionId())+", "
            + "ia="+individualAddress+", "
            + "systemType="+String.format("0x%02x", getSystemType())
            +"}";
    }
    
    
    
    
}
