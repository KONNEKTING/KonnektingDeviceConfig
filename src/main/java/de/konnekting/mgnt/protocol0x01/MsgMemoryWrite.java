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
class MsgMemoryWrite extends ProgMessage {

    public MsgMemoryWrite(ComObject co) throws KnxException {
        super(MSGTYPE_WRITE_COM_OBJECT);

        if (co == null) {
            throw new IllegalArgumentException("ComObject must not be null!");
        }

        data[2] = co.getId();
        
        
        if (co.isActive()) {
            System.arraycopy(Utils.getGroupAddress(co.getGroupAddress()).toByteArray(), 0, data, 3 /* index */, 2 /* num bytes */);
        } else {
            // CO is not active, use 0/0/0 as GA. This GA will not be used, as the CO is marked as inactive. It's just to have ANY value.
            data[3] = 0x00;
            data[4] = 0x00;
        }
        
        // left most bit is set to 1 (--> 0x80) if CO is active. Otherwise bit is 0 (--> 0x00)
        data[5] = co.isActive()?(byte)0x80:(byte)0x00; 
    }

    @Override
    public String toString() {
        try {
            return "MsgWriteComObject{"
                        + "id=" + String.format("0x%02x", data[2]) + ", "
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
