/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt.protocol0x00;

import static de.konnekting.mgnt.protocol0x00.ProgProtocol0x00.MSGTYPE_READ_PARAMETER;

/**
 *
 * @author achristian
 */
class MsgReadParameter extends ProgMessage {

    public MsgReadParameter(byte id) {
        super(MSGTYPE_READ_PARAMETER);
        data[2] = id;
    }

    @Override
    public String toString() {
        return "MsgReadParameter{id=" +String.format("0x%02x", data[2])+ "}";
    }
    
    
    
}
