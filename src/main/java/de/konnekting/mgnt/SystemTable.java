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
package de.konnekting.mgnt;

import de.konnekting.deviceconfig.utils.Helper;
import de.root1.slicknx.KnxException;
import static de.konnekting.deviceconfig.utils.Bytes2ReadableValue.*;
import de.konnekting.deviceconfig.utils.NonReverseableFlag;

/**
 *
 * @author achristian
 */
public class SystemTable {

    
    private NonReverseableFlag dirty = new NonReverseableFlag();

    /**
     * Size of system table
     */
    public static final int SIZE = 32;
    
    /**
     * Memory address of system table
     */
    public static final int SYSTEMTABLE_ADDRESS = 0;
    /**
     * Memory address of writeable system table space
     */
    public static final int SYSTEMTABLE_WRITE_ADDRESS = 16;

    private byte[] data;
    
    public SystemTable(byte[] data) {
        if (data.length!=SystemTable.SIZE) {
            throw new IllegalArgumentException("Given data array has wrong size: "+data.length+". Needs to be "+SystemTable.SIZE);
        }
        this.data = data;
    }

    public int getAddressTableAddress() {
        return convertUINT16(data[3], data[4]);
    }

    public int getAssociationTableAddress() {
        return convertUINT16(data[5], data[6]);
    }

    public int getCommobjectTableAddress() {
        return convertUINT16(data[7], data[8]);
    }

    public int getParameterTableAddress() {
        return convertUINT16(data[9], data[10]);
    }

    public int getVersion() {
        return convertUINT16(data[0], data[1]);
    }

    public byte getDeviceFlags() {
        return data[2];
    }
    
    public String getIndividualAddress() {
        return Helper.convertBytesToIA(data[16], data[17]);
    }
    
    public void setIndividualAddress(String address) {
        if (!getIndividualAddress().equals(address)) {
            dirty.set();
        }
        byte[] ia = Helper.convertIaToBytes(address);
        data[16] = ia[0];
        data[17] = ia[1];
    }
    
    public byte[] getData() {
        return data;
    }
    
    public byte[] getWriteData() throws KnxException {
        byte[] memData = new byte[16];
        System.arraycopy(data, 16, memData, 0, 16);
        return memData;
    }
    
    public boolean hasChanged() {
        return dirty.isSet();
    }

    @Override
    public String toString() {
        return "SystemTable{" + "dirty=" + dirty + 
                " deviceFlags="+String.format("0x%02x",getDeviceFlags())+
                " groupAddressTable="+String.format("0x%04x",getAddressTableAddress())+
                " assocationTable="+String.format("0x%04x",getAssociationTableAddress())+
                " commObjectTable="+String.format("0x%04x", getCommobjectTableAddress())+
                " parameterTable="+String.format("0x%04x", getParameterTableAddress())+
                " ia="+getIndividualAddress()+"}";
    }

    
    
    
}
