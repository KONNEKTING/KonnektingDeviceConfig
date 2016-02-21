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
package de.konnekting.mgnt.protocol0x00;

/**
 *
 * @author achristian
 */
class MsgAck extends ProgMessage {
    
    public static final byte ACK = 0x00;
    public static final byte NO_INDEX = (byte) 0xFF;

    public MsgAck(byte[] data) {
        super(data);
    }

    @Override
    public String toString() {
        return "ACK{"
            + "type="+(isAcknowledged()?"ACK":"NACK")+" "
            + "errorCode="+String.format("0x%02x", getErrorCode())+" "
            + "indexInformation="+(hasIndexInformation()?String.format("0x%02x", getIndexInformation()):"false")
            + "}";
    }

    boolean isAcknowledged() {
        return data[2]==ACK;
    }
    
    public byte getErrorCode() {
        return data[3];
    }
    
    public byte getIndexInformation() {
        return data[4];
    }
    
    public boolean hasIndexInformation(){
        return data[4]!=NO_INDEX;
    }
    
    
    
}

