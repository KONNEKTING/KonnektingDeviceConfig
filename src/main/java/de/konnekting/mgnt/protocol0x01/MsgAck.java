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
package de.konnekting.mgnt.protocol0x01;

/**
 *
 * @author achristian
 */
class MsgAck extends ProgMessage {
    
    public static final byte ACK = 0x00;

    public MsgAck(byte[] data) throws InvalidMessageException {
        super(data);
        validateEmpty(4, 13);
    }
    
    MsgAck() {
        super(ACK);
    }

    @Override
    public String toString() {
        return "MsgAck{"
            + "type="+(isAcknowledged()?"ACK":"NACK")+" "
            + "errorCode="+String.format("0x%02x", getErrorCode())+" "
            + "}";
    }

    boolean isAcknowledged() {
        return data[2]==ACK;
    }
    
    public boolean isError() {
        return data[3]!=0x00;
    }
    
    public byte getErrorCode() {
        return data[3];
    }
    
    
}

