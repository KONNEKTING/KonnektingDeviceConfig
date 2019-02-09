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
import de.konnekting.deviceconfig.utils.ReadableValue2Bytes;
import java.util.zip.CRC32;

/**
 *
 * @author achristian
 */
public class TestCRC32 {
    
    public static void main(String[] args) {
        CRC32 crc32 = new CRC32();
        
        byte[] data = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,0x07,0x08,0x09, 0x0A, 0x0B};
        crc32.update(data);
        System.out.println("crc32="+crc32.getValue()+ " / "+Helper.bytesToHex(ReadableValue2Bytes.convertUINT32(crc32.getValue()), true));
    }
    
}
