package de.konnekting.mgnt;


import de.konnekting.deviceconfig.utils.Helper;
import de.konnekting.mgnt.DeviceManagement;
import de.konnekting.mgnt.DeviceManagementException;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import java.io.File;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author alexander
 */
public class SandboxDeviceManagementDataTest {
    
    static String toBinary(int b) {
        String s = Integer.toBinaryString(b&0xFF);
        
        while (s.length()<8) {
            s = "0"+s;
        }
        return s;
    }
    
    public static void main(String[] args) throws KnxException, DeviceManagementException {
        
//        byte DEVICEFLAG_FACTORY_BIT = (byte) 0x80;
//        
//        System.out.println("deviceflagactorybit= "+toBinary(DEVICEFLAG_FACTORY_BIT));
//        
//        byte _deviceFlags = (byte) 0b00001111;
//        System.out.println(String.format("0x%02X", _deviceFlags));
//        System.out.println("deviceflags= "+toBinary(_deviceFlags));
//        
//        boolean isFactory = _deviceFlags == 0xFF || (_deviceFlags & DEVICEFLAG_FACTORY_BIT) == DEVICEFLAG_FACTORY_BIT;
//        System.out.println("isFactory="+isFactory);
//        
//        System.out.println(toBinary((_deviceFlags & DEVICEFLAG_FACTORY_BIT)));
        
        Knx knx = new Knx("1.1.200");
        DeviceManagement dm = new DeviceManagement(knx);
        
//        dm.writeIndividualAddress(null, null, "1.1.1");
        
//        dm.startProgMode("1.1.1", 0xDEAD, (short) 0xFF, (short) 0x00);
        
//        File f = new File("test.dat");
//        dm.sendData(f, (byte)1, (byte)0);
//        
//        System.out.println("Read 1 ########################################################################");
//        
//        File f2 = new File("testrx.dat");
//        dm.readData(f2, (byte)1, (byte)0);
//        
//        System.out.println("Remove ########################################################################");
//        
//        dm.removeData((byte)1, (byte)0);
//        
//
//        System.out.println("Read 2 ########################################################################");
//        
//        File f3 = new File("testrx2.dat");
//        dm.readData(f3, (byte)1, (byte)0);
//        
//        System.out.println("Stop ########################################################################");

        //dm.unload(false, true, true, true, true);
        //System.out.println("factory reset ##################################################################");
        dm.unload(true, false, false, false, false);
        
//        dm.stopProgMode("1.1.1");
        
        //dm.unload(true, true, true, true);
        
        knx.close();
    }
    
}
