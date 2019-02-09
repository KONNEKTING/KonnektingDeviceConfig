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
import static de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.MSGTYPE_DATA_WRITE_PREPARE;
import static de.konnekting.deviceconfig.utils.ReadableValue2Bytes.*;
import de.root1.slicknx.KnxException;

/**
 *
 * @author achristian
 */
class MsgDataWritePrepare extends ProgMessage {
    
    private DataType dt;
    private byte dataId;
    private long size;
    
    public MsgDataWritePrepare(DataType dt, byte dataId, long size) throws KnxException {
        super(MSGTYPE_DATA_WRITE_PREPARE);
        this.dt = dt;
        this.dataId = dataId;
        this.size=size;

        if (size > UINT32_MAX) {
            throw new IllegalArgumentException("max. "+UINT32_MAX+" bytes of data!");
        }
        
        if (dt == DataType.UPDATE && dataId != 0x00) {
            throw new IllegalArgumentException("for UPDATE, dataId must be 0x00, always!");
        }

        data[2] = (byte) dt.getValue();
        data[3] = dataId;
        data[4] = convertUINT32(size)[0];
        data[5] = convertUINT32(size)[1];
        data[6] = convertUINT32(size)[2];
        data[7] = convertUINT32(size)[3];
        fillUnused(8);
    }

    @Override
    public String toString() {
        return "MsgDataWritePrepare{" + "dt=" + dt + ", dataId=" + dataId + ", size=" + size + "}";
    }

    
        
}
