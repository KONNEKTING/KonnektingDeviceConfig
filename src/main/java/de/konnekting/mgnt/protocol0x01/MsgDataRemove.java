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

import static de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.MSGTYPE_DATA_READ;
import de.root1.slicknx.KnxException;

/**
 *
 * @author achristian
 */
class MsgDataRemove extends ProgMessage {

    private byte dataType;
    private byte dataId;
    
    public MsgDataRemove(byte dataType, byte dataId) throws KnxException {
        super(MSGTYPE_DATA_READ);
        this.dataType = dataType;
        this.dataId= dataId;
        
        if (dataType==ProgProtocol0x01.UPDATE_DATATYPE) {
            throw new IllegalArgumentException("Cannot read data type update.");
        }

        data[2] = dataType;
        data[3] = dataId;
        
    }

    @Override
    public String toString() {
        return "MsgDataRead{" + "dataType=" + dataType + ", dataId=" + dataId + '}';
    }
        
}
