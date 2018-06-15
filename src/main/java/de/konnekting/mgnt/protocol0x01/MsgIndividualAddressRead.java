/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt.protocol0x01;

/**
 *
 * @author achristian
 */
class MsgIndividualAddressRead extends ProgMessage {

    public MsgIndividualAddressRead(byte[] data) {
        super(data);
    }

    public MsgIndividualAddressRead() {
        super(ProgProtocol0x01.MSGTYPE_INDIVIDUAL_ADDRESS_READ);
    }

    @Override
    public String toString() {
        return "MsgIndividualAddressRead{}";
    }
    
}
