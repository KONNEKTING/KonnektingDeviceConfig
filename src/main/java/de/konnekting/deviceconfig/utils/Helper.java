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

import de.konnekting.xml.konnektingdevice.v0.ParamType;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class Helper {

    private static final Logger LOG = LoggerFactory.getLogger(Helper.class);
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, false);
    }

    public static String bytesToHex(byte[] bytes, boolean withWhitespace) {
        char[] hexChars = new char[bytes.length * 2 + (withWhitespace ? bytes.length : 0)];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            if (withWhitespace) {
                hexChars[j * 2 + j] = HEX_ARRAY[v >>> 4];
                hexChars[j * 2 + j + 1] = HEX_ARRAY[v & 0x0F];
                hexChars[j * 2 + j + 2] = " ".charAt(0);
            } else {
                hexChars[j * 2] = HEX_ARRAY[v >>> 4];
                hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
            }
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String s) {
        
        if (!s.matches("^[0-9a-fA-F]+$") || s.length()%2!=0) {
            throw new NumberFormatException("Given value '"+s+"' is not a valid hex string");
        };
        
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static final Pattern PA_PATTERB = Pattern.compile("\\A\\d{1,2}\\.\\d{1,2}\\.\\d{1,3}\\z");
    private static final Pattern PA_PARKED_PATTERN = Pattern.compile("\\A\\d{1,2}\\.\\d{1,2}\\.\\z");
    private static final Pattern GA_PATTERN = Pattern.compile("\\A\\d{1,2}/\\d{1,2}/\\d{1,3}\\z");

    public static byte[] addrToBytes(int a, int b, int c) {
        byte[] buffer = new byte[2];

        buffer[0] = (byte) ((byte) (a << 4) | b);
        buffer[1] = (byte) c;

        return buffer;
    }

    public static boolean checkValidPa(String pa) {
        Matcher matcher = PA_PATTERB.matcher(pa);
        boolean found = false;
        while (matcher.find()) {
            found = true;
        }
        if (!found) {
            LOG.error("Given pa '" + pa + "' is not valid.");
            return false;
        }
        String[] split = pa.split("\\.");
        int area = Integer.parseInt(split[0]);
        int line = Integer.parseInt(split[1]);
        int member = Integer.parseInt(split[2]);

        if (area < 0 || area > 15) {
            LOG.error("Area of given pa '" + pa + "' is not in range 1..15");
            return false;
        }
        if (line < 0 || line > 15) {
            LOG.error("Line of given pa '" + pa + "' is not in range 1..15");
            return false;
        }
        if (member < 0 || member > 255) {
            LOG.error("Member of given pa '" + pa + "' is not in range 0..255");
            return false;
        }
        return true;
    }

    public static boolean checkValidGa(String ga) {
        
        // allow empty GA --> unused ComObject
        if (ga.isEmpty()) return true;
        
        Matcher matcher = GA_PATTERN.matcher(ga);
        boolean found = false;
        while (matcher.find()) {
            found = true;
        }
        if (!found) {
            LOG.error("Given ga '" + ga + "' is not valid.");
            return false;
        }
        String[] split = ga.split("/");
        int main = Integer.parseInt(split[0]); // 0..15
        int middle = Integer.parseInt(split[1]); //0..7
        int sub = Integer.parseInt(split[2]); //0..255

        if (main < 0 || main > 15) {
            LOG.error("Main of given ga '" + ga + "' is not in range 0..15");
            return false;
        }
        if (middle < 0 || middle > 7) {
            LOG.error("Middle of given ga '" + ga + "' is not in range 0..7");
            return false;
        }
        if (sub < 0 || sub > 255) {
            LOG.error("Sub of given ga '" + ga + "' is not in range 0..255");
            return false;
        }
        return true;
    }

    /**
     * Converts a string to not-null-string. Means <code>null</code> is
     * connverted to "". Otherwise same string is returned
     *
     * @param string
     * @return converted string
     */
    public static String convertNullString(String string) {
        if (string == null) {
            return "";
        }
        return string;
    }

    public static boolean isParkedAddress(String individualAddress) {
        Matcher matcher = PA_PARKED_PATTERN.matcher(individualAddress);
        boolean found = false;
        while (matcher.find()) {
            return true;
        }
        return false;
    }

    public static boolean isNumberType(ParamType paramType) {
        switch (paramType) {
            case INT_8:
            case UINT_8:
            case INT_16:
            case UINT_16:
            case INT_32:
            case UINT_32:
                return true;
            default:
                return false;
        }
    }
    
    public static boolean isRawType(ParamType paramType) {
        switch (paramType) {
            case RAW_1:
            case RAW_2:
            case RAW_3:
            case RAW_4:
            case RAW_5:
            case RAW_6:
            case RAW_7:
            case RAW_8:
            case RAW_9:
            case RAW_10:
            case RAW_11:
                return true;
            default:
                return false;
        }
    }
    
//    public static boolean isFloatType(ParameterType paramType) {
//        switch (paramType) {
//            case FLOAT32:
//                return true;
//            default:
//                return false;
//        }
//    }

    public static String getTempFilename(String suffix) throws IOException {
        File createTempFile = File.createTempFile("KONNEKTING", "Temp"+suffix);
        createTempFile.delete();
        createTempFile.deleteOnExit();
        return createTempFile.getName();
    }
    
}
