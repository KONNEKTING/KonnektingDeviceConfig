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
import de.root1.slicknx.KnxException;
import de.root1.slicknx.Utils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
    private static Map<ParamType, Integer> PARAM_SIZE_MAP = new HashMap<>();

    static {
        PARAM_SIZE_MAP.put(ParamType.INT_8, 1);
        PARAM_SIZE_MAP.put(ParamType.UINT_8, 1);

        PARAM_SIZE_MAP.put(ParamType.INT_16, 2);
        PARAM_SIZE_MAP.put(ParamType.UINT_16, 2);

        PARAM_SIZE_MAP.put(ParamType.INT_32, 4);
        PARAM_SIZE_MAP.put(ParamType.UINT_32, 4);

        PARAM_SIZE_MAP.put(ParamType.RAW_1, 1);
        PARAM_SIZE_MAP.put(ParamType.RAW_2, 2);
        PARAM_SIZE_MAP.put(ParamType.RAW_3, 3);
        PARAM_SIZE_MAP.put(ParamType.RAW_4, 4);
        PARAM_SIZE_MAP.put(ParamType.RAW_5, 5);
        PARAM_SIZE_MAP.put(ParamType.RAW_6, 6);
        PARAM_SIZE_MAP.put(ParamType.RAW_7, 7);
        PARAM_SIZE_MAP.put(ParamType.RAW_8, 8);
        PARAM_SIZE_MAP.put(ParamType.RAW_9, 9);
        PARAM_SIZE_MAP.put(ParamType.RAW_10, 10);
        PARAM_SIZE_MAP.put(ParamType.RAW_11, 11);

        PARAM_SIZE_MAP.put(ParamType.STRING_11, 11);
    }

    /**
     * return upper case hex!
     *
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, false);
    }

    public static String bytesToHex(byte[] bytes, int index, int length, boolean whitespace) {
        return bytesToHex(Arrays.copyOfRange(bytes, index, index + length - 1), whitespace);
    }

    /**
     * returns upper case hex
     *
     * @param bytearray
     * @param whitespace
     * @return
     */
    public static String bytesToHex(byte[] bytearray, boolean whitespace) {
        StringBuilder sb = new StringBuilder(bytearray.length * 2);
        for (int i = 0; i < bytearray.length; i++) {
            sb.append(String.format(whitespace ? "%02x " : "%02x", bytearray[i] & 0xff));
        }
        return sb.toString().trim().toUpperCase();
    }

    public static byte[] hexToBytes(String s) {

        if (!s.matches("^[0-9a-fA-F]+$") || s.length() % 2 != 0) {
            throw new NumberFormatException("Given value '" + s + "' is not a valid hex string");
        }

        int sLen = s.length();
        byte[] bytearray = new byte[sLen / 2];
        for (int i = 0; i < sLen; i += 2) {
            bytearray[i / 2] = (byte) ( /* left hex-char: shift 4 bits the the left*/(Character.digit(s.charAt(i), 16) << 4)
                    + /* right hex-char: no need to shift, as value is already "low" enough */ Character.digit(s.charAt(i + 1), 16));
        }
        return bytearray;
    }

    private static final Pattern PA_PATTERB = Pattern.compile("\\A\\d{1,2}\\.\\d{1,2}\\.\\d{1,3}\\z");
    private static final Pattern PA_PARKED_PATTERN = Pattern.compile("\\A\\d{1,2}\\.\\d{1,2}\\.\\z");
    private static final Pattern GA_PATTERN = Pattern.compile("\\A\\d{1,2}/\\d{1,2}/\\d{1,3}\\z");

    /**
     * uses big endian
     *
     * @param ga
     * @return
     */
    public static byte[] convertGaToBytes(String ga) {
        try {
            return Utils.getGroupAddress(ga).toByteArray();
        } catch (KnxException ex) {
            LOG.error("Error converting GA to bytes", ex);
            return null;
        }
    }

    /**
     * uses big endian
     *
     * @param b0
     * @param b1
     * @return
     */
    public static String convertBytesToIA(byte b0, byte b1) {
        try {
            // TODO check corect endianess
            return Utils.getIndividualAddress(b1, b0).toString();
        } catch (KnxException ex) {
            // will never happen, cause we fix the byte-array
            return "input.not.valid";
        }
    }

    /**
     * uses big endian
     *
     * @param ia
     * @return
     */
    public static byte[] convertIaToBytes(String ia) {
        if (isParkedAddress(ia)) {
            LOG.error("Individual address is parked. Cannot convert");
            return null;
        }
        try {
            return Utils.getIndividualAddress(ia).toByteArray();
        } catch (KnxException ex) {
            LOG.error("Error converting IA to bytes", ex);
            return null;
        }
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
        if (ga.isEmpty()) {
            return true;
        }

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

    public static int getParameterSize(ParamType paramType) {
        if (!PARAM_SIZE_MAP.containsKey(paramType)) {
            throw new RuntimeException("Someone forgot to add the size of " + paramType.name() + " to PARAM_SIZE_MAP!");
        }
        return PARAM_SIZE_MAP.get(paramType);
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

    public static String getTempFilename(String suffix) throws IOException {
        File createTempFile = File.createTempFile("KONNEKTING_", "_Temp" + suffix);
        createTempFile.delete();
        createTempFile.deleteOnExit();
        return createTempFile.getName();
    }

    public static boolean setByte(byte[] dst, int dstIndex, byte in) {
        boolean dirty = false;
        if (dst[dstIndex] != in) {
            dst[dstIndex] = in;
            dirty = true;
        }
        return dirty;
    }

//    public static byte getHI(int v) {
//        return (byte)((v >>> 8) & 0xFF);
//    }
//    
//    public static byte getLO(int v) {
//        return (byte)((v >>> 0) & 0xFF);
//    }
//    
//    public static int getFrom16bit(byte lo, byte hi) {
//        return (int) ((hi << 8) + ((lo << 0) & 0xFF));
//    }
    public static int convertGaToInt(String ga) {
        byte[] bytes = convertGaToBytes(ga);
        int convertUINT16 = Bytes2ReadableValue.convertUINT16(bytes);
        return convertUINT16;
    }

    public static long roundUp(long num, long divisor) {
        return (num + divisor - 1) / divisor;
    }
}
