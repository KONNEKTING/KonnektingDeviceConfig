/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt.protocol0x01;

import de.konnekting.deviceconfig.utils.Helper;
import static de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.MSGTYPE_MEMORY_READ;
import static de.konnekting.deviceconfig.utils.ReadableValue2Bytes.*;

/**
 *
 * @author achristian
 */
class MsgMemoryRead extends ProgMessage {

    
    private int count;
    private short address;
    
    public MsgMemoryRead(int memoryAddress, int count) {
        super(MSGTYPE_MEMORY_READ);

        if (count<=0) {
            throw new IllegalArgumentException("you must read at least one byte");
        }
        
        this.count = count;
        this.address = (short) (memoryAddress & 0xFFFF);

        data[2] = (byte) count;
        data[3] = convertUINT16(address)[0];
        data[4] = convertUINT16(address)[1];
    }

    @Override
    public String toString() {
        return "MsgMemoryRead{" + "count=" + count + ", address=" + String.format("0x%04x", address) + '}';
    }

}
