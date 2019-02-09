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
import static de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.MSGTYPE_DATA_WRITE;
import static de.konnekting.deviceconfig.utils.ReadableValue2Bytes.*;
import de.root1.slicknx.KnxException;

/**
 *
 * @author achristian
 */
class MsgDataWrite extends ProgMessage {

    private int count;
    private byte[] data;
    
    public MsgDataWrite(int count, byte[] data) throws KnxException {
        super(MSGTYPE_DATA_WRITE);
        this.count = count;
        this.data = data;

        if (count <= 0 || count > 11) {
            throw new IllegalArgumentException("count must be within 1..11");
        }
        
        if (data.length > 11) {
            throw new IllegalArgumentException("canot handle more than 11 bytes");
        }

        data[2] = (byte) count;
        System.arraycopy(data, 0, data, 3, data.length);
    }

    @Override
    public String toString() {
        return "MsgDataWrite{" + "count=" + count + ", data=" + Helper.bytesToHex(data, true) + '}';
    }

    

    
        
}
