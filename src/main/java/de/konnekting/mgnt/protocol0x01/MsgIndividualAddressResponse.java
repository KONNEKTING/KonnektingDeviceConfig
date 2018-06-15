/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of slicKnx.
 *
 *   slicKnx is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   slicKnx is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with slicKnx.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.konnekting.mgnt.protocol0x01;

import de.root1.slicknx.KnxException;
import de.root1.slicknx.Utils;

/**
 *
 * @author achristian
 */
class MsgIndividualAddressResponse extends ProgMessage {

    public MsgIndividualAddressResponse(byte[] data) throws InvalidMessageException {
        super(data);
        validateEmpty(4, 13);
    }
    
    public String getAddress() throws KnxException {
        return Utils.getIndividualAddress(data[2], data[3]).toString();
    }
    
    @Override
    public String toString() {
        String t;
        try {
            t = "MsgIndividualAddressResponse{individualAddress="+getAddress()+"}";
        } catch (KnxException ex) {
            t = "MsgIndividualAddressResponse{!!!EXCEPTION!!!}";
            log.error("Error parsing individual address ", ex);
        }
        return t;
    }
    
}
