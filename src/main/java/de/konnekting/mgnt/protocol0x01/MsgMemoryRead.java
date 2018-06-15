/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt.protocol0x00;

import static de.konnekting.mgnt.protocol0x00.ProgProtocol0x00.MSGTYPE_READ_COM_OBJECT;

/**
 *
 * @author achristian
 */
class MsgMemoryRead extends ProgMessage {

    public MsgMemoryRead(Byte id) {
        super(MSGTYPE_READ_COM_OBJECT);

        if (id == null) {
            throw new IllegalArgumentException("you must read at least one com object!");
        }

        data[2] = id;
    }

    @Override
    public String toString() {
        return "MsgReadComObject{"
                + "id=" + String.format("0x%02x", data[2])
                + "}";
    }

}
