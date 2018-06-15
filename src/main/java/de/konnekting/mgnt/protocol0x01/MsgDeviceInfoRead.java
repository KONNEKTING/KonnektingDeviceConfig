/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt.protocol0x01;

import de.root1.slicknx.KnxException;
import de.root1.slicknx.Utils;
import static de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.MSGTYPE_DEVICE_INFO_READ;

/**
 *
 * @author achristian
 */
class MsgDeviceInfoRead extends ProgMessage {

    public MsgDeviceInfoRead(String individualAddress) throws KnxException {
        super(MSGTYPE_DEVICE_INFO_READ);
        System.arraycopy(Utils.getIndividualAddress(individualAddress).toByteArray(), 0, data, 2, 2);
    }

    @Override
    public String toString() {
        try {
            return "MsgReadDeviceInfo{individualAddress=" +Utils.getIndividualAddress(data[2], data[3])+ "}";
        } catch (KnxException ex) {
            return "MsgReadDeviceInfo{individualAddress=invalid(" +String.format("0x%02x", data[2])+", "+String.format("0x%02x", data[3])+ ", exMsg="+ex.getCause().getMessage()+"}";
        }
    }
    
}
