/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt.protocol0x01;

import de.root1.slicknx.KnxException;
import de.root1.slicknx.Utils;
import static de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.MSGTYPE_INDIVIDUAL_ADDRESS_WRITE;
import tuwien.auto.calimero.IndividualAddress;

/**
 *
 * @author achristian
 */
class MsgIndividualAddressWrite extends ProgMessage {

    public MsgIndividualAddressWrite(String address) throws KnxException {
        super(MSGTYPE_INDIVIDUAL_ADDRESS_WRITE);
        IndividualAddress ia = Utils.getIndividualAddress(address);
        System.arraycopy(ia.toByteArray(), 0, data, 2, 2);
    }

    @Override
    public String toString() {
        try {
            return "MsgIndividualAddressWrite{individualAddress=" +Utils.getIndividualAddress(data[2], data[3])+ "}";
        } catch (KnxException ex) {
            return "MsgIndividualAddressWrite{individualAddress=invalid(" +String.format("0x%02x", data[2])+", "+String.format("0x%02x", data[3])+ ", exMsg="+ex.getCause().getMessage()+"}";
        }
    }
    
    
    
}
