/*
 * Copyright (C) 2020 Alexander Christian <alex(at)root1.de>. All rights reserved.
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

import de.konnekting.deviceconfig.utils.ReadableValue2Bytes;
import de.konnekting.mgnt.ChecksumIdentifier;
import static de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.MSGTYPE_CHECKSUM_SET;
import de.root1.slicknx.KnxException;

/**
 *
 * @author achristian
 */
class MsgChecksumSet extends ProgMessage {

    private final ChecksumIdentifier identifier;
    private long crc32;
    private byte[] sendData;

    public MsgChecksumSet(ChecksumIdentifier identifier, long crc32) throws KnxException {
        super(MSGTYPE_CHECKSUM_SET);
        this.identifier = identifier;
        this.crc32 = crc32;
        
        byte[] crc32bytes = ReadableValue2Bytes.convertUINT32(crc32);

        data[2] = identifier.getId();
        data[3] = crc32bytes[0];
        data[4] = crc32bytes[1];
        data[5] = crc32bytes[2];
        data[6] = crc32bytes[3];
    }

    @Override
    public String toString() {
        return "MsgChecksumSet{" + "identifier=" + identifier.name() + "#" + String.format("0x%02x", identifier.getId()) + ", " 
                + crc32+"("+String.format("0x%04x", crc32) + ")}";
    }

}
