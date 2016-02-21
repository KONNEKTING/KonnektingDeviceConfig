/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt.protocol0x00;

/**
 *
 * @author achristian
 */
class MsgReadIndividualAddress extends ProgMessage {

    public MsgReadIndividualAddress(byte[] data) {
        super(data);
    }

    public MsgReadIndividualAddress() {
        super(ProgProtocol0x00.MSGTYPE_READ_INDIVIDUAL_ADDRESS);
    }

    @Override
    public String toString() {
        return "MsgReadIndividualAddress{}";
    }
    
}
