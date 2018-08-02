/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
        System.arraycopy(data, 0, b, 5, b.length);

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
