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
import static de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.MSGTYPE_MEMORY_READ;
import static de.konnekting.deviceconfig.utils.ReadableValue2Bytes.*;

/**
 *
 * @author achristian
 */
class MsgMemoryRead extends ProgMessage {

    
    private int count;
    private short address;
    
    public MsgMemoryRead(int memoryAddress, int count) {
        super(MSGTYPE_MEMORY_READ);

        if (count<=0) {
            throw new IllegalArgumentException("you must read at least one byte");
        }
        
        this.count = count;
        this.address = (short) (memoryAddress & 0xFFFF);

        data[2] = (byte) count;
        data[3] = convertUINT16(address)[0];
        data[4] = convertUINT16(address)[1];
    }

    @Override
    public String toString() {
        return "MsgMemoryRead{" + "count=" + count + ", address=" + String.format("0x%04x", address) + '}';
    }

}
