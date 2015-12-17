/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.deviceconfig;

import de.konnekting.xml.schema.konnekting.KONNEKTING;
import de.konnekting.xmlschema.KonnektingXmlService;
import java.io.File;
import javax.xml.bind.JAXBException;
import org.xml.sax.SAXException;

/**
 *
 * @author achristian
 */
public class Test {
    
    public static void main(String[] args) throws JAXBException, SAXException {
        KONNEKTING readConfiguration = KonnektingXmlService.readConfiguration(new File("Test.kdevice.xml"));
        System.out.println("->"+readConfiguration.getVersion()+"<-");
        
        int mid = readConfiguration.getDevice().getManufacturerId();
        short shortmid = (short) mid;
        
        System.out.println(String.format("0x%02x", shortmid));
    }
    
}
