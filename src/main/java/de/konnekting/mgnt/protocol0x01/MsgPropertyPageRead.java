/*
 * Copyright (C) 2019 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of KONNEKTING DeviceConfig.
 *
 *   KONNEKTING DeviceConfig is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   KONNEKTING DeviceConfig is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with KONNEKTING DeviceConfig.  If not, see <http://www.gnu.org/licenses/>.
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
