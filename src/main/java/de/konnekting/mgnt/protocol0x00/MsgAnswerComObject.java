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
package de.konnekting.mgnt.protocol0x00;

import de.root1.slicknx.KnxException;
import de.root1.slicknx.Utils;
import de.konnekting.mgnt.ComObject;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author achristian
 */
class MsgAnswerComObject extends ProgMessage {

    public MsgAnswerComObject(byte[] data) {
        super(data);
    }
    
    public List<ComObject> getComObjects() throws KnxException {
        List<ComObject> list = new ArrayList<>();
        
        byte number = data[2];
        switch(number) {
            case 3:
                list.add(new ComObject(data[9], Utils.getGroupAddress(data[10], data[11]).toString()));
            case 2:
                list.add(new ComObject(data[6], Utils.getGroupAddress(data[7], data[8]).toString()));
            case 1:
                list.add(new ComObject(data[3], Utils.getGroupAddress(data[4], data[5]).toString()));
                break;
        }
        
        return list;
    }
    
}
