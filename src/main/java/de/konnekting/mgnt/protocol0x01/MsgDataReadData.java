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
import java.util.Arrays;

/**
 *
 * @author achristian
 */
class MsgDataReadData extends ProgMessage {

    private int count;
    private byte[] data;

    public MsgDataReadData(byte[] data) {
        super(data);
        count = data[2];
        data = Arrays.copyOfRange(data, 3, 3 + count - 1);
    }

    @Override
    public String toString() {
        return "MsgDataReadData{" + "count=" + count + ", data=" + Helper.bytesToHex(data, true) + '}';
    }

}
