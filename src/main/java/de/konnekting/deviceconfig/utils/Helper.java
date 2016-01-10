/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
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

import de.konnekting.deviceconfig.exception.InvalidAddressFormatException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author achristian
 */
public class Helper {
    
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    
    private static final Pattern paPattern = Pattern.compile("\\A\\d{1,2}\\.\\d{1,2}\\.\\d{1,3}\\z");
    private static final Pattern gaPattern = Pattern.compile("\\A\\d{1,2}/\\d{1,2}/\\d{1,3}\\z");
    
    public static byte[] addrToBytes(int a, int b, int c) {
        byte[] buffer = new byte[2];
        
        buffer[0] = (byte)((byte)(a << 4) | b);
        buffer[1] = (byte)c; 
        
        return buffer;
    }
    
    public static void checkValidPa(String pa) throws InvalidAddressFormatException {
        Matcher matcher = paPattern.matcher(pa);
        boolean found = false;
        while (matcher.find()) {
            found=true;
        }
        if (!found) throw new InvalidAddressFormatException("Given pa '"+pa+"' is not valid.");
        String[] split = pa.split("\\.");
        int area = Integer.parseInt(split[0]);
        int line = Integer.parseInt(split[1]);
        int member = Integer.parseInt(split[2]);
        
        if (area < 1 || area >15) throw new InvalidAddressFormatException("Area of given pa '"+pa+"' is not in range 1..15");
        if (line < 1 || line >15) throw new InvalidAddressFormatException("Line of given pa '"+pa+"' is not in range 1..15");
        if (member < 0 || member >255) throw new InvalidAddressFormatException("Member of given pa '"+pa+"' is not in range 0..255");
    }
    
    public static void checkValidGa(String ga) throws InvalidAddressFormatException {
        Matcher matcher = gaPattern.matcher(ga);
        boolean found = false;
        while (matcher.find()) {
            found=true;
        }
        if (!found) throw new InvalidAddressFormatException("Given ga '"+ga+"' is not valid.");
        String[] split = ga.split("/");
        int main = Integer.parseInt(split[0]); // 0..15
        int middle = Integer.parseInt(split[1]); //0..7
        int sub = Integer.parseInt(split[2]); //0..255
        
        if (main < 0 || main >15) throw new InvalidAddressFormatException("Main of given ga '"+ga+"' is not in range 0..15");
        if (middle < 0 || middle >7) throw new InvalidAddressFormatException("Middle of given ga '"+ga+"' is not in range 0..7");
        if (sub < 0 || sub >255) throw new InvalidAddressFormatException("Sub of given ga '"+ga+"' is not in range 0..255");
    }
    
    
    
    
    
    /**
     * Converts a string to not-null-string. Means <code>null</code> is connverted to "".
     * Otherwise same string is returned
     * @param string
     * @return converted string
     */
    public static String convertNullString(String string) {
        if (string==null) {
            return "";
        } 
        return string;
    }
    
}
