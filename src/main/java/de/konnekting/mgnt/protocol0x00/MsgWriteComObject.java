/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt.protocol0x00;

import de.root1.slicknx.KnxException;
import de.root1.slicknx.Utils;
import de.konnekting.mgnt.ComObject;
import static de.konnekting.mgnt.protocol0x00.ProgProtocol0x00.MSGTYPE_WRITE_COM_OBJECT;

/**
 *
 * @author achristian
 */
class MsgWriteComObject extends ProgMessage {

    public MsgWriteComObject(ComObject co1) throws KnxException {
        super(MSGTYPE_WRITE_COM_OBJECT);

        if (co1 == null) {
            throw new IllegalArgumentException("you must write at least one com object!");
        }

        data[2] = co1.getId();
        System.arraycopy(Utils.getGroupAddress(co1.getGroupAddress()).toByteArray(), 0, data, 3 /* index */, 2 /* num bytes */);
        data[5] = 0x00; // settings
    }

    @Override
    public String toString() {
        try {
            int len = data[2];
            return "MsgWriteComObject{"
                        + "id=" + String.format("0x%02x", data[3]) + ", "
                        + "ga=" + Utils.getGroupAddress(data[3], data[4]) + ", "
                        + "ga(hex)=0x" + Utils.bytesToHex(new byte[]{data[3], data[4]}) + ", "
                        + "settings="+ String.format("0x%02x", data[5])
                    + "}";
        } catch (KnxException ex) {
            ex.printStackTrace();
            return "MsgWriteComObject{toStringFailed! -> " + super.toString() + "}";
        }
    }

}
