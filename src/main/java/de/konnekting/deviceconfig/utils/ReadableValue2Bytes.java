/*
 * Copyright (C) 2016 Alexander Christian <alex(at)root1.de>. All rights reserved.
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
package de.konnekting.deviceconfig.utils;

import java.io.UnsupportedEncodingException;

/**
 * big endian
 * @author achristian
 */
public class ReadableValue2Bytes {

    public static synchronized byte[] convertINT8(byte v) {
        return new byte[]{v};
    }

    public static synchronized byte[] convertUINT8(int v) {
        return new byte[]{(byte)((v) & 0xFF)};
    }

    public static synchronized byte[] convertINT16(int v) {
        byte b0 = (byte)((v >>> 8) & 0xFF);
        byte b1 = (byte)((v >>> 0) & 0xFF);
        return new byte[]{b0, b1};
    }

    public static synchronized byte[] convertUINT16(int v) {
        byte b0 = (byte)(v >>>  8);
        byte b1 = (byte)(v >>>  0);
        return new byte[]{b0, b1};
    }

    public static synchronized byte[] convertINT32(int v) {
        byte b0 = (byte)((v >>> 24) & 0xFF);
        byte b1 = (byte)((v >>> 16) & 0xFF);
        byte b2 = (byte)((v >>> 8) & 0xFF);
        byte b3 = (byte)((v >>> 0) & 0xFF);
        return new byte[]{b0, b1, b2, b3};
    }

    public static synchronized byte[] convertUINT32(long v) {
        byte b0 = (byte)(v >>> 24);
        byte b1 = (byte)(v >>> 16);
        byte b2 = (byte)(v >>>  8);
        byte b3 = (byte)(v >>>  0);
        return new byte[]{b0, b1, b2, b3};
    }
    
    public static synchronized byte[] convertFLOAT32(float v) {
        
        int floatToIntBits = Float.floatToIntBits(v);
        
        byte b0 = (byte)(floatToIntBits >>> 24);
        byte b1 = (byte)(floatToIntBits >>> 16);
        byte b2 = (byte)(floatToIntBits >>>  8);
        byte b3 = (byte)(floatToIntBits >>>  0);
        return new byte[]{b0, b1, b2, b3};
    }
    
    /**
     * Converts a Java String into an 11 byte ISO-8859-1 byte array.
     * @param s
     * @return
     * @throws UnsupportedEncodingException 
     */
    public static synchronized byte[] convertString11(String s) throws UnsupportedEncodingException {
        /* 
         * init resulting array with 0x00
         * so that unused tailing chars of string11 are 0x00
         */
        byte[] b = new byte[]{
            (int) 0x00, (int) 0x00, (int) 0x00, 
            (int) 0x00, (int) 0x00, (int) 0x00, 
            (int) 0x00, (int) 0x00, (int) 0x00, 
            (int) 0x00, (int) 0x00};
        byte[] strBytes = s.getBytes("ISO-8859-1");
        System.arraycopy(strBytes, 0, b, 0, strBytes.length);
        return b;
    }
    
}
