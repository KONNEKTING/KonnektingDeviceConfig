package de.konnekting.mgnt;


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
    
    public static void main(String[] args) throws KnxException, DeviceManagementException {
        Knx knx = new Knx("1.1.200");
        DeviceManagement dm = new DeviceManagement(knx);
        
        dm.startProgMode("1.1.1", 0xDEAD, (short) 0xFF, (short) 0x00);
        
        File f = new File("test.jpg");
        dm.sendData(f, (byte)1, (byte)0);
        
        System.out.println("########################################################################");
        
        File f2 = new File("testrx.dat");
        dm.readData(f2, (byte)1, (byte)0);
        
        dm.stopProgMode("1.1.1");
        
        //dm.unload(true, true, true, true);
    }
    
}
