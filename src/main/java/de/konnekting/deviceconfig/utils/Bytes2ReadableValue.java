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
 *
 * @author achristian
 */
public class Bytes2ReadableValue {

    public static synchronized byte convertINT8(byte[] b) {
        return b[0];
    }

    public static synchronized short convertUINT8(byte[] b) {
        int ch = b[0] & 0xff;
        return (short) ch;

    }

    public static synchronized short convertINT16(byte[] b) {
        int ch1 = b[0];
        int ch2 = b[1];
        return (short) ((ch1 << 8) + ((ch2 << 0) & 0xFF));
    }

    public static synchronized int convertUINT16(byte[] b) {
        int ch1 = b[0];
        int ch2 = b[1];
        return ((ch1 << 8) & 0xff00) + ((ch2 << 0) & 0xFF);
    }
    
    public static synchronized int convertUINT16(byte b0, byte b1) {
        return convertUINT16(new byte[]{b0, b1});
    }

    public static synchronized int convertINT32(byte[] b) {
        int ch1 = b[0];
        int ch2 = b[1];
        int ch3 = b[2];
        int ch4 = b[3];
        return (((ch1 << 24) & 0xFF000000)
            + ((ch2 << 16) & 0x00FF0000)
            + ((ch3 << 8) & 0x0000FF00)
            + ((ch4 << 0) & 0x000000FF));
    }

    public static synchronized long convertUINT32(byte[] b) {
        return (((long) (b[0] & 0XFF) << 24)
            + ((b[1] & 0XFF) << 16)
            + ((b[2] & 0XFF) << 8)
            + ((b[3] & 0XFF) << 0));
    }

    public static synchronized float convertFLOAT32(byte[] b) {

        int i = (((int) (b[0] & 0XFF) << 24)
            + ((b[1] & 0XFF) << 16)
            + ((b[2] & 0XFF) << 8)
            + ((b[3] & 0XFF) << 0));

        return Float.intBitsToFloat(i);
    }

    public static synchronized String convertString11(byte[] b) throws UnsupportedEncodingException {
        // search for terminating 0x00 and cut string off
        int indexOf = -1;
        for (int i = 0; i < b.length; i++) {
            if (b[i] == (int) 0x00) {
                indexOf = i;
                break;
            }
        }
        if (indexOf != -1) {
            byte[] s = new byte[indexOf /* = length of string */];
            System.arraycopy(b, 0, s, 0, indexOf);
            return new String(s, "ISO-8859-1");
        } else {
            return new String(b, "ISO-8859-1");
        }
    }

}
