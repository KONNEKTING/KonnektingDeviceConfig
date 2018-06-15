/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt.protocol0x00;

import de.root1.slicknx.KnxException;
import de.root1.slicknx.Utils;
import static de.konnekting.mgnt.protocol0x00.ProgProtocol0x00.MSGTYPE_WRITE_PROGRAMMING_MODE;

/**
 *
 * @author achristian
 */
class MsgProgrammingModeWrite extends ProgMessage {

    public MsgProgrammingModeWrite(String individualAddress, boolean progMode) throws KnxException {
        super(MSGTYPE_WRITE_PROGRAMMING_MODE);
        System.arraycopy(Utils.getIndividualAddress(individualAddress).toByteArray(), 0, data, 2, 2);
        data[4] = (byte) (progMode ? 0x01 : 0x00);
    }

    @Override
    public String toString() {
        try {
            return "MsgWriteProgrammingMode{individualAddress=" +Utils.getIndividualAddress(data[2], data[3])
                + ", progMode="+(data[4]==0x01?"true":"false")
                + "}";
        } catch (KnxException ex) {
            return "MsgWriteProgrammingMode{individualAddress=invalid(" +String.format("0x%02x", data[2])+", "+String.format("0x%02x", data[3])+ ", exMsg="+ex.getCause().getMessage()
                + ", progMode="+(data[4]==0x01?"true":"false")+"}";
        }
    }
    
    
    
}
