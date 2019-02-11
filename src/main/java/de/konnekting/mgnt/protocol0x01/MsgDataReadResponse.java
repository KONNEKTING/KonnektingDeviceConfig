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
import de.konnekting.deviceconfig.utils.ReadableValue2Bytes;

/**
 *
 * @author achristian
 */
class MsgDataReadResponse extends ProgMessage {
    
    private byte dataType;
    private byte dataId;
    private long size;
    private long crc32;

    public MsgDataReadResponse(byte[] data) {
        super(data);
        dataType = data[2];
        dataId = data[3];
        size = convertUINT32(data[4], data[5], data[6], data[7]);
        crc32 = convertUINT32(data[8], data[9], data[10], data[11]);
    }

    @Override
    public String toString() {
        return "MsgDataReadResponse{dataType=" + dataType + ", dataId=" + dataId + ", size=" + size + ", crc32=" + Helper.bytesToHex(ReadableValue2Bytes.convertUINT32(crc32), true) + '}';
    }
    
}
