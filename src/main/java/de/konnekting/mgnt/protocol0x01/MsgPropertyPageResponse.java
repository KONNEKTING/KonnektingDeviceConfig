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

import de.konnekting.deviceconfig.utils.Helper;
import de.root1.slicknx.KnxException;
import de.root1.slicknx.Utils;
import java.util.logging.Logger;

/**
 *
 * @author achristian
 */
public class MsgPropertyPageResponse extends ProgMessage {

    
    public MsgPropertyPageResponse(byte[] data) throws InvalidMessageException {
        super(data);
    }

    @Override
    public String toString() {
        return "MsgPropertyPageResponse{data="+Helper.bytesToHex(data, true)+"}";
    }
    
}
