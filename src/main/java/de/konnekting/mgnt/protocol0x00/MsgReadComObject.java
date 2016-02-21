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
class MsgReadComObject extends ProgMessage {

    public MsgReadComObject(Byte id1, Byte id2, Byte id3) {
        super(MSGTYPE_READ_COM_OBJECT);
        
        if (id1 == null) {
            throw new IllegalArgumentException("you must read at least one com object!");
        }
        
        int num = 1;

        if (id2 != null) {
            num++;
            if (id3 != null) {
                num++;
            }
        }
        
        // len
        data[2] = (byte) (num & 0xFF);

        // #1
        data[3] = id1;

        // #2
        if (id2 != null) {
            data[4] = id2;
        }

        // #3
        if (id3 != null) {
            data[5] = id1;
        }
    }

    @Override
    public String toString() {
        return "MsgReadComObject{"
            + "len="+data[2]+" "
            + "id1="+String.format("0x%02x", data[3])+ " " 
            + "id2="+String.format("0x%02x", data[4])+ " " 
            + "id3="+String.format("0x%02x", data[5])
            + "}";
    }

    
    
    

}
