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

    public MsgWriteComObject(ComObject co1, ComObject co2, ComObject co3) throws KnxException {
        super(MSGTYPE_WRITE_COM_OBJECT);

        if (co1 == null) {
            throw new IllegalArgumentException("you must write at least one com object!");
        }
        int num = 1;

        if (co2 != null) {
            num++;
            if (co3 != null) {
                num++;
            }
        }

        // len
        data[2] = (byte) (num & 0xFF);

        data[3] = co1.getId();
        System.arraycopy(Utils.getGroupAddress(co1.getGroupAddress()).toByteArray(), 0, data, 4, 2);

        if (co2 != null) {
            data[6] = co2.getId();
            System.arraycopy(Utils.getGroupAddress(co2.getGroupAddress()).toByteArray(), 0, data, 7, 2);
        }

        if (co3 != null) {
            data[9] = co3.getId();
            System.arraycopy(Utils.getGroupAddress(co3.getGroupAddress()).toByteArray(), 0, data, 10, 2);
        }

    }

    @Override
    public String toString() {
        try {
            int len = data[2];
            return "MsgWriteComObject{"
                    + "len=" + data[2] + ", "
                    + "id1=" + String.format("0x%02x", data[3]) + ", "
                    + "ga1=" + Utils.getGroupAddress(data[4], data[5]) + ", "
                    + "ga1(hex)=0x" + Utils.bytesToHex(new byte[]{data[4], data[5]}) + ", "
                            + (len >= 2
                                    ? "id2=" + String.format("0x%02x", data[6]) + ", "
                                    + "ga2=" + Utils.getGroupAddress(data[7], data[8]) + ", "
                                    + "ga2(hex)=0x" + Utils.bytesToHex(new byte[]{data[7], data[8]}) + ", "
                                    + (len == 3
                                            ? "id3=" + String.format("0x%02x", data[9]) + ", "
                                            + "ga3=" + Utils.getGroupAddress(data[10], data[11]) + ", "
                                            + "ga3(hex)=0x" + Utils.bytesToHex(new byte[]{data[10], data[11]}) + ", "
                                            : "")
                                    : "")
                            + "}";
        } catch (KnxException ex) {
            ex.printStackTrace();
            return "MsgWriteComObject{toStringFailed! -> " + super.toString() + "}";
        }
    }

}
