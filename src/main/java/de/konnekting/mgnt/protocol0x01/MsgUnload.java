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
import static de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.MSGTYPE_UNLOAD;

/**
 *
 * @author achristian
 */
public class MsgUnload extends ProgMessage {

    private static final byte TRUE = (byte) 0xFF;
    private static final byte FALSE = (byte) 0x00;
    
    private boolean ia;
    private boolean co;
    private boolean params;
    private boolean datastorage;

    public MsgUnload(byte[] data) {
        super(data);
    }

    MsgUnload(boolean ia, boolean co, boolean params, boolean datastorage) throws KnxException {
        super(MSGTYPE_UNLOAD);
        
        this.ia = ia;
        this.co = co;
        this.params = params;
        this.datastorage = datastorage;
        
        data[2] = ia ? TRUE : FALSE;
        data[3] = co ? TRUE : FALSE;
        data[4] = params ? TRUE : FALSE;
        data[5] = datastorage ? TRUE : FALSE;
    }
    
    @Override
    public String toString() {
        return "MsgUnload{ia=" + ia+" co="+co+" params="+params+" datastorage="+datastorage+"}";
    }


}
