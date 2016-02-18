/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.suite.utils;

import de.konnekting.deviceconfig.utils.ReadableValue2Bytes;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author achristian
 */
public class ReadableValue2BytesTest {
    
    

    /**
     * Test of convertINT8 method, of class ReadableValue2Bytes.
     */
    @Test
    public void testConvertINT8() {
        System.out.println("convertINT8");
        ReadableValue2Bytes instance = new ReadableValue2Bytes();
        byte v;
        byte[] expResult;
        byte[] result;
        
        v = Byte.MAX_VALUE;
        expResult = new byte[]{0x7f};
        result = instance.convertINT8(v);
        assertArrayEquals("INT8 MAX failed", expResult, result);
        
        v = Byte.MIN_VALUE;
        expResult = new byte[]{(byte)0x80};
        result = instance.convertINT8(v);
        assertArrayEquals("INT8 MIN failed", expResult, result);
    }

    /**
     * Test of convertUINT8 method, of class ReadableValue2Bytes.
     */
    @Test
    public void testConvertUINT8() {
        System.out.println("convertUINT8");
        ReadableValue2Bytes instance = new ReadableValue2Bytes();
        short v;
        byte[] expResult;
        byte[] result;
        
        v = 255;
        expResult = new byte[]{(byte)0xff};
        result = instance.convertUINT8(v);
        assertArrayEquals("UINT8 MAX failed", expResult, result);
        
        v = 0;
        expResult = new byte[]{(byte)0x00};
        result = instance.convertUINT8(v);
        assertArrayEquals("UINT8 MIN failed", expResult, result);
    }

    /**
     * Test of convertINT16 method, of class ReadableValue2Bytes.
     */
    @Test
    public void testConvertINT16() {
        System.out.println("convertINT16");
        ReadableValue2Bytes instance = new ReadableValue2Bytes();
        short v;
        byte[] expResult;
        byte[] result;
        
        v = Short.MAX_VALUE;
        expResult = new byte[]{0x7f, (byte)0xff};
        result = instance.convertINT16(v);
        assertArrayEquals("INT16 MAX failed", expResult, result);
        
        v = Short.MIN_VALUE;
        expResult = new byte[]{(byte)0x80, 0x00};
        result = instance.convertINT16(v);
        assertArrayEquals("INT16 MIN failed", expResult, result);
    }

    /**
     * Test of convertUINT16 method, of class ReadableValue2Bytes.
     */
    @Test
    public void testConvertUINT16() {
        System.out.println("convertUINT16");
        ReadableValue2Bytes instance = new ReadableValue2Bytes();
        int v;
        byte[] expResult;
        byte[] result;
        
        v = 65535;
        expResult = new byte[]{(byte)0xff, (byte)0xff};
        result = instance.convertUINT16(v);
        assertArrayEquals("UINT8 MAX failed", expResult, result);
        
        v = 0;
        expResult = new byte[]{(byte)0x00, (byte)0x00};
        result = instance.convertUINT16(v);
        assertArrayEquals("UINT8 MIN failed", expResult, result);
    }

    /**
     * Test of convertINT32 method, of class ReadableValue2Bytes.
     */
    @Test
    public void testConvertINT32() {
        System.out.println("convertINT32");
        ReadableValue2Bytes instance = new ReadableValue2Bytes();
        int v;
        byte[] expResult;
        byte[] result;
        
        v = Integer.MAX_VALUE;
        expResult = new byte[]{0x7f, (byte)0xff, (byte) 0xff, (byte) 0xff};
        result = instance.convertINT32(v);
        assertArrayEquals("INT32 MAX failed", expResult, result);
        
        v = Integer.MIN_VALUE;
        expResult = new byte[]{(byte)0x80, 0x00, 0x00, 0x00};
        result = instance.convertINT32(v);
        assertArrayEquals("INT32 MIN failed", expResult, result);
    }

    /**
     * Test of convertUINT32 method, of class ReadableValue2Bytes.
     */
    @Test
    public void testConvertUINT32() {
        System.out.println("convertUINT32");
        ReadableValue2Bytes instance = new ReadableValue2Bytes();
        long v;
        byte[] expResult;
        byte[] result;
        
        v = 4294967295L;
        expResult = new byte[]{(byte)0xff, (byte)0xff, (byte) 0xff, (byte) 0xff};
        result = instance.convertUINT32(v);
        assertArrayEquals("INT32 MAX failed", expResult, result);
        
        v = 0;
        expResult = new byte[]{(byte)0x00, 0x00, 0x00, 0x00};
        result = instance.convertUINT32(v);
        assertArrayEquals("INT32 MIN failed", expResult, result);
    }
    
    /**
     * Test of convertFLOAT32 method, of class ReadableValue2Bytes.
     */
    @Test
    public void testConvertFLOAT32() {
        System.out.println("convertFLOAT32");
        ReadableValue2Bytes instance = new ReadableValue2Bytes();
        float v;
        byte[] expResult;
        byte[] result;
        
        v = Float.MAX_VALUE;
        expResult = new byte[]{(byte)0x7f, (byte)0x7f, (byte) 0xff, (byte) 0xff};
        result = instance.convertFLOAT32(v);
        assertArrayEquals("FLOAT32 MAX failed", expResult, result);
        
        v = Float.MIN_VALUE;
        expResult = new byte[]{(byte)0x00, 0x00, 0x00, 0x01};
        result = instance.convertFLOAT32(v);
        assertArrayEquals("FLOAT32 MIN failed", expResult, result);
        
        v = -11.7f;
        expResult = new byte[]{(byte)0xc1, 0x3b, 0x33, (byte)0x33};
        result = instance.convertFLOAT32(v);
        assertArrayEquals("FLOAT32 -11.7 failed", expResult, result);
    }

}
