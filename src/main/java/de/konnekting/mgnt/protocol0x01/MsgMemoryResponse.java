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

import de.konnekting.deviceconfig.utils.Helper;
import static de.konnekting.deviceconfig.utils.Bytes2ReadableValue.*;

/**
 *
 * @author achristian
 */
class MsgMemoryResponse extends ProgMessage {
    
    private int count;
    private int address;

    public MsgMemoryResponse(byte[] data) {
        super(data);
        count = data[2]&0xFF; 
        address = convertUINT16(data[3], data[4]);
    }
    
    public int getCount() {
        return count;
    }
    
    public int getAddress() {
        return address;
    }
    
    public byte[] getData() {
        byte[] b = new byte[count];
        System.arraycopy(data, 5, b, 0, count);
        return b;
    }

    @Override
    public String toString() {
        return "MsgMemoryResponse{" + "count=" + count + ", address=" + String.format("0x%04x",address) + ", data="+Helper.bytesToHex(getData())+'}';
    }
    
    
        
}
