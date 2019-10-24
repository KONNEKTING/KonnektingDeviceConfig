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
import de.root1.slicknx.KnxException;
import static de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.MSGTYPE_MEMORY_WRITE;
import static de.konnekting.deviceconfig.utils.ReadableValue2Bytes.*;
import static de.konnekting.deviceconfig.utils.Bytes2ReadableValue.*;

/**
 *
 * @author achristian
 */
class MsgMemoryWrite extends ProgMessage {

    public MsgMemoryWrite(int address, byte[] b) throws KnxException {
        super(MSGTYPE_MEMORY_WRITE);

        if (b.length > 9) {
            throw new IllegalArgumentException("max. 1..9 bytes of data!");
        }

        data[2] = (byte) b.length;
        data[3] = convertUINT16(address)[0];
        data[4] = convertUINT16(address)[1];
        System.arraycopy(b, 0, data, 5, b.length);

    }

    @Override
    public String toString() {
        return "MsgMemoryWrite{"
                + "count=" + (int) (data[2] & 0xff) + ", "
                + "address=" + String.format("0x%04x", convertUINT16(data[3], data[4])) + ", "
                + "data=" + Helper.bytesToHex(data, 5, (int) (data[2] & 0xff), true)
                + "}";
    }

}
