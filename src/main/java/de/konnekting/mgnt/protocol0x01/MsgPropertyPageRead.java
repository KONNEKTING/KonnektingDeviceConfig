/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt.protocol0x01;

import de.root1.slicknx.KnxException;
import de.root1.slicknx.Utils;
import static de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.MSGTYPE_PROPERTY_PAGE_READ;

/**
 *
 * @author achristian
 */
class MsgPropertyPageRead extends ProgMessage {

    /**
     * 
     * @param individualAddress address, in case you want to read specific device by its adress, null if you only want the device whch is in prog mode. Ensure that only one device is n prog mode!
     * @param pagenum
     * @throws KnxException 
     */
    public MsgPropertyPageRead(String individualAddress, int pagenum) throws KnxException {
        super(MSGTYPE_PROPERTY_PAGE_READ);
        if (individualAddress==null) {
            data[2] = (byte) 0x00;
            data[3] = (byte) 0xff;
            data[4] = (byte) 0xff;
        } else {
            data[2] = (byte) 0xFF;
            // IA is at 3+4
            System.arraycopy(Utils.getIndividualAddress(individualAddress).toByteArray(), 0, data, 3, 2);
        }
        if (pagenum<0 || pagenum>255) {
            throw new IllegalArgumentException("pagenum must be in range 0..255");
        }
        data[5] = (byte) ((pagenum >>>  0) & 0xFF);
        fillUnused(6);
    }

    @Override
    public String toString() {
        try {
            return "MsgPropertyPageRead{individualAddress=" +Utils.getIndividualAddress(data[2], data[3])+ "pagenum="+String.format("0x%02x", data[4])+"}";
        } catch (KnxException ex) {
            return "MsgPropertyPageRead{individualAddress=invalid(" +String.format("0x%02x", data[2])+", "+String.format("0x%02x", data[3])+ "pagenum="+String.format("0x%02x", data[4])+", exMsg="+ex.getCause().getMessage()+"}";
        }
    }
    
}
