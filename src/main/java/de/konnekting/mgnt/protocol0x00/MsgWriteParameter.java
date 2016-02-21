/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt.protocol0x00;

import de.root1.slicknx.Utils;
import static de.konnekting.mgnt.protocol0x00.ProgProtocol0x00.MSGTYPE_WRITE_PARAMETER;

/**
 *
 * @author achristian
 */
class MsgWriteParameter extends ProgMessage {

    public MsgWriteParameter(byte id, byte[] paramData) {
        super(MSGTYPE_WRITE_PARAMETER);
        data[2] = id;
        System.arraycopy(paramData, 0, data, 3, paramData.length);
    }

    @Override
    public String toString() {
        return "MsgWriteParameter{id=" +String.format("0x%02x", data[2])+", "
            + "data=["+Utils.bytesToHex(data)+"]"
            + "}";
    }
    
    
    
}
