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

import static de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.PROTOCOL_VERSION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
abstract class ProgMessage {

    protected Logger log = LoggerFactory.getLogger(ProgMessage.class);
    protected final byte[] data;
    private static final byte UNUSED = (byte) 0xFF;

    public ProgMessage(byte[] message) {
        this.data = message;
    }

    /**
     * Create prog message. All message bytes are initialized with UNUSED bytes.
     * @param type 
     */
    public ProgMessage(byte type) {
        data = new byte[14];
        data[0] = PROTOCOL_VERSION;
        data[1] = type;
        for (int i = 2; i < data.length; i++) {
            data[i] = UNUSED;
        }
    }

    public byte getType() {
        return data[1];
    }

    public byte getProtocolversion() {
        return data[0];
    }

    /**
     * Empty bytes have 0xff value
     *
     * @param from
     * @param to
     * @throws InvalidMessageException
     */
    void validateEmpty(int from, int to) throws InvalidMessageException {
        for (int i = from; i <= to; i++) {
            if (data[i] != UNUSED) {
                throw new InvalidMessageException("Message not valid!");
            }
        }
    }

    /**
     * fill with "unused" beginning at given index
     *
     * @param startIndex start to fill "unused" here
     */
    void fillUnused(int startIndex) {
        for (int i = startIndex; i < data.length; i++) {
            data[i] = UNUSED;
        }
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return String.format("ProgMessage[%02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x]",
                data[0],
                data[1],
                data[2],
                data[3],
                data[4],
                data[5],
                data[6],
                data[7],
                data[8],
                data[9],
                data[10],
                data[11],
                data[12],
                data[13]);
    }

}
