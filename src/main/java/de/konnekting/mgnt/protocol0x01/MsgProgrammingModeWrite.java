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

import de.konnekting.deviceconfig.utils.Bytes2ReadableValue;
import de.konnekting.deviceconfig.utils.Helper;
import de.root1.slicknx.KnxException;
import de.root1.slicknx.Utils;
import static de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.MSGTYPE_PROGRAMMING_MODE_WRITE;

/**
 *
 * @author achristian
 */
class MsgProgrammingModeWrite extends ProgMessage {

    public MsgProgrammingModeWrite(String individualAddress, boolean progMode) throws KnxException {
        super(MSGTYPE_PROGRAMMING_MODE_WRITE);
        
        byte[] ia = Helper.convertIaToBytes(individualAddress);

        log.error("ia={}", String.format("0x%04x",Bytes2ReadableValue.convertUINT16(ia)));
        data[2] = ia[0];
        data[3] = ia[1];
        data[4] = (byte) (progMode ? 0x01 : 0x00);
    }

    @Override
    public String toString() {
        try {
            return "MsgWriteProgrammingMode{individualAddress=" +Utils.getIndividualAddress(data[2], data[3])
                + ", progMode="+(data[4]==0x01?"true":"false")
                + "}";
        } catch (KnxException ex) {
            return "MsgWriteProgrammingMode{individualAddress=invalid(" +String.format("0x%02x", data[2])+", "+String.format("0x%02x", data[3])+ ", exMsg="+ex.getCause().getMessage()
                + ", progMode="+(data[4]==0x01?"true":"false")+"}";
        }
    }
    
    
    
}
