/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.suite.utils;

import de.konnekting.deviceconfig.utils.Bytes2ReadableValue;
import de.konnekting.deviceconfig.utils.Helper;
import java.io.UnsupportedEncodingException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author achristian
 */
public class Bytes2ReadableValueTest {
    
    /*
    
    Byte INT8 MAX 0x7F
    Byte INT8 MIN 0x80
    
    Short INT16 MAX 0x7F FF
    Short INT16 MIN 0x80 00
    
    Integer INT32 MAX 0x7F FF FF FF
    Integer INT32 MIN 0x80 00 00 00
    */
    
    /**
     * Test of convertINT8 method, of class Bytes2ReadableValue.
     */
    @Test
    public void testConvertINT8() {
        System.out.println("convertINT8");
        Bytes2ReadableValue instance = new Bytes2ReadableValue();
        byte expResult;
        byte result;
        
        expResult = 0x7F;
        result = instance.convertINT8(new byte[]{Byte.MAX_VALUE});
        assertEquals("INT8 MAX failed", expResult, result);
        
        expResult = 0x00;
        result = instance.convertINT8(new byte[]{0x00});
        assertEquals("INT8 0 failed", expResult, result);
        
        expResult = (byte)0x80;
        result = instance.convertINT8(new byte[]{Byte.MIN_VALUE});
        assertEquals("INT8 MIN failed", expResult, result);
    }

    /**
     * Test of convertUINT8 method, of class Bytes2ReadableValue.
     */
    @Test
    public void testConvertUINT8() {
        System.out.println("convertUINT8");
        Bytes2ReadableValue instance = new Bytes2ReadableValue();
        short expResult;
        short result;
        
        expResult = 255;
        result = instance.convertUINT8(new byte[]{(byte)0xFF});
        assertEquals("UINT8 MAX failed", expResult, result);
        
        expResult = 0;
        result = instance.convertUINT8(new byte[]{0x00});
        assertEquals("UINT8 MIN failed", expResult, result);
    }

    /**
     * Test of convertINT16 method, of class Bytes2ReadableValue.
     */
    @Test
    public void testConvertINT16() {
        System.out.println("convertINT16");
        Bytes2ReadableValue instance = new Bytes2ReadableValue();
        short expResult;
        short result;
        
        /*
        Short INT16 MAX 0x7F FF
        Short INT16 MIN 0x80 00
        */
        
        expResult = Short.MAX_VALUE;
        result = instance.convertINT16(new byte[]{(byte)0x7f, (byte)0xff});
        assertEquals("INT16 MAX failed", expResult, result);
        
        expResult = 0;
        result = instance.convertINT16(new byte[]{(byte)0x00, (byte)0x00});
        assertEquals("INT16 0 failed", expResult, result);
        
        expResult = Short.MIN_VALUE;
        result = instance.convertINT16(new byte[]{(byte)0x80, (byte)0x00});
        assertEquals("INT16 MIN failed", expResult, result);
    }

    /**
     * Test of convertUINT16 method, of class Bytes2ReadableValue.
     */
    @Test
    public void testConvertUINT16() {
        System.out.println("convertUINT16");
        Bytes2ReadableValue instance = new Bytes2ReadableValue();
        int expResult;
        int result;
        
        /*
        MAX 65535
        MIN 0
        */
        
        expResult = 65535;
        result = instance.convertUINT16(new byte[]{(byte)0xFF, (byte)0xFF});
        assertEquals("UINT16 MAX failed", expResult, result);
        
        expResult = 0;
        result = instance.convertUINT16(new byte[]{0x00, 0x00});
        assertEquals("UINT16 MIN failed", expResult, result);
    }

    /**
     * Test of convertINT32 method, of class Bytes2ReadableValue.
     */
    @Test
    public void testConvertINT32() {
        System.out.println("convertINT32");
        Bytes2ReadableValue instance = new Bytes2ReadableValue();
        int expResult;
        int result;
        
        /*
        Short INT32 MAX 0x7F FF FF FF
        Short INT32 MIN 0x80 00 00 00
        */
        
        expResult = Integer.MAX_VALUE;
        result = instance.convertINT32(new byte[]{(byte)0x7f, (byte)0xff, (byte)0xff, (byte)0xff});
        assertEquals("INT32 MAX failed", expResult, result);
        
        expResult = 0;
        result = instance.convertINT32(new byte[]{(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00});
        assertEquals("INT32 0 failed", expResult, result);
        
        expResult = Integer.MIN_VALUE;
        result = instance.convertINT32(new byte[]{(byte)0x80, (byte)0x00, (byte)0x00, (byte)0x00});
        assertEquals("INT32 MIN failed", expResult, result);
    }

    /**
     * Test of convertUINT32 method, of class Bytes2ReadableValue.
     */
    @Test
    public void testConvertUINT32() {
        System.out.println("convertUINT32");
        Bytes2ReadableValue instance = new Bytes2ReadableValue();
        long expResult;
        long result;
        
        /*
        MAX 4294967295
        MIN 0
        */
        
        expResult = 4294967295L;
        result = instance.convertUINT32(new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF});
        assertEquals("UINT32 MAX failed", expResult, result);
        
        expResult = 0;
        result = instance.convertUINT32(new byte[]{0x00, 0x00, 0x00, 0x00});
        assertEquals("UINT32 MIN failed", expResult, result);
    }
    
    /**
     * Test of convertFLOAT32 method, of class Bytes2ReadableValue.
     */
    @Test
    public void testConvertFLOAT32() {
        System.out.println("convertFLOAT32");
        Bytes2ReadableValue instance = new Bytes2ReadableValue();
        float expResult;
        float result;
        
        /*
        MAX 3.4028234663852886E38f
        MIN 1.401298464324817E-45f
        */
        
        expResult = Float.MAX_VALUE;
        result = instance.convertFLOAT32(new byte[]{(byte)0x7F, (byte)0x7F, (byte)0xFF, (byte)0xFF});
        assertEquals("FLOAT32 MAX failed", expResult, result, 0.0001);
        
        expResult = Float.MIN_VALUE;
        result = instance.convertFLOAT32(new byte[]{0x00, 0x00, 0x00, 0x01});
        assertEquals("FLOAT32 MIN failed", expResult, result, 0.0001);
        
        expResult = -11.7f;
        result = instance.convertFLOAT32(new byte[]{(byte)0xc1, 0x3b, 0x33, (byte)0x33});
        assertEquals("FLOAT32 -11.7 failed", expResult, result, 0.0001);
    }
    
    /**
     * Test of convertSTRING11 method, of class Bytes2ReadableValue.
     */
    @Test
    public void testConvertSTRING11() throws UnsupportedEncodingException {
        System.out.println("convertSTRING11");
        Bytes2ReadableValue instance = new Bytes2ReadableValue();
        
        String expResult = "Hello World";
        String result = instance.convertString11(Helper.hexToBytes("48656c6c6f20576f726c64"));
        assertEquals("STRING11 '"+expResult+"' failed", expResult, result);
        
        expResult = "foo bar";
        result = instance.convertString11(Helper.hexToBytes("666f6f2062617200000000"));
        assertEquals("STRING11 '"+expResult+"' failed", expResult, result);
    }
    
}
