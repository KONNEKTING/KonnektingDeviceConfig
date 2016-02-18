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

import de.konnekting.xml.konnektingdevice.v0.ParameterType;
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

    private static final Logger log = LoggerFactory.getLogger(Helper.class);
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, false);
    }

    public static String bytesToHex(byte[] bytes, boolean withWhitespace) {
        char[] hexChars = new char[bytes.length * 2 + (withWhitespace ? bytes.length : 0)];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            if (withWhitespace) {
                hexChars[j * 2 + j] = hexArray[v >>> 4];
                hexChars[j * 2 + j + 1] = hexArray[v & 0x0F];
                hexChars[j * 2 + j + 2] = " ".charAt(0);
            } else {
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static final Pattern paPattern = Pattern.compile("\\A\\d{1,2}\\.\\d{1,2}\\.\\d{1,3}\\z");
    private static final Pattern paParkedPattern = Pattern.compile("\\A\\d{1,2}\\.\\d{1,2}\\.\\z");
    private static final Pattern gaPattern = Pattern.compile("\\A\\d{1,2}/\\d{1,2}/\\d{1,3}\\z");

    public static byte[] addrToBytes(int a, int b, int c) {
        byte[] buffer = new byte[2];

        buffer[0] = (byte) ((byte) (a << 4) | b);
        buffer[1] = (byte) c;

        return buffer;
    }

    public static boolean checkValidPa(String pa) {
        Matcher matcher = paPattern.matcher(pa);
        boolean found = false;
        while (matcher.find()) {
            found = true;
        }
        if (!found) {
            log.error("Given pa '" + pa + "' is not valid.");
            return false;
        }
        String[] split = pa.split("\\.");
        int area = Integer.parseInt(split[0]);
        int line = Integer.parseInt(split[1]);
        int member = Integer.parseInt(split[2]);

        if (area < 0 || area > 15) {
            log.error("Area of given pa '" + pa + "' is not in range 1..15");
            return false;
        }
        if (line < 0 || line > 15) {
            log.error("Line of given pa '" + pa + "' is not in range 1..15");
            return false;
        }
        if (member < 0 || member > 255) {
            log.error("Member of given pa '" + pa + "' is not in range 0..255");
            return false;
        }
        return true;
    }

    public static boolean checkValidGa(String ga) {
        Matcher matcher = gaPattern.matcher(ga);
        boolean found = false;
        while (matcher.find()) {
            found = true;
        }
        if (!found) {
            log.error("Given ga '" + ga + "' is not valid.");
            return false;
        }
        String[] split = ga.split("/");
        int main = Integer.parseInt(split[0]); // 0..15
        int middle = Integer.parseInt(split[1]); //0..7
        int sub = Integer.parseInt(split[2]); //0..255

        if (main < 0 || main > 15) {
            log.error("Main of given ga '" + ga + "' is not in range 0..15");
            return false;
        }
        if (middle < 0 || middle > 7) {
            log.error("Middle of given ga '" + ga + "' is not in range 0..7");
            return false;
        }
        if (sub < 0 || sub > 255) {
            log.error("Sub of given ga '" + ga + "' is not in range 0..255");
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
        Matcher matcher = paParkedPattern.matcher(individualAddress);
        boolean found = false;
        while (matcher.find()) {
            return true;
        }
        return false;
    }

    public static boolean isNumberType(ParameterType paramType) {
        switch (paramType) {
            case INT8:
            case UINT8:
            case INT16:
            case UINT16:
            case INT32:
            case UINT32:
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

    public static String getTempFilename() throws IOException {
        File createTempFile = File.createTempFile("KONNEKTING", "Temp");
        createTempFile.delete();
        createTempFile.deleteOnExit();
        return createTempFile.getName();
    }

}
