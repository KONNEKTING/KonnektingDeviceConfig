/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt.protocol0x00;

import de.root1.slicknx.KnxException;
import de.root1.slicknx.Utils;
import static de.konnekting.mgnt.protocol0x00.ProgProtocol0x00.MSGTYPE_RESTART;

/**
 *
 * @author achristian
 */
public class MsgRestart extends ProgMessage {

    public MsgRestart(byte[] data) {
        super(data);
    }

    MsgRestart(String individualAddress) throws KnxException {
        super(MSGTYPE_RESTART);
        System.arraycopy(Utils.getIndividualAddress(individualAddress).toByteArray(), 0, data, 2, 2);
    }
    
    public String getAddress() throws KnxException {
        return Utils.getIndividualAddress(data[2], data[3]).toString();
    }

    @Override
    public String toString() {
        String t;
        try {
            t = "Restart{"+getAddress()+"}";
        } catch (KnxException ex) {
            t = "Restart{!!!EXCEPTION!!!}";
            log.error("Error parsing individual address ", ex);
        }
        return t;
    }
    
    
    
}
