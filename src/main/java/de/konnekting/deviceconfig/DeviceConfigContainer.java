/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.deviceconfig;

import de.konnekting.xml.schema.konnekting.CommObject;
import de.konnekting.xml.schema.konnekting.KONNEKTING;
import de.konnekting.xml.schema.konnekting.Parameter;
import de.konnekting.xml.schema.konnekting.ParameterGroup;
import de.konnekting.xmlschema.KonnektingXmlService;
import java.io.File;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.xml.sax.SAXException;

/**
 *
 * @author achristian
 */
public class DeviceConfigContainer {

    private KONNEKTING konnekt;
    private final File f;
    
    public DeviceConfigContainer(File f) throws JAXBException, SAXException {
        this.f = f;
        konnekt = KonnektingXmlService.readConfiguration(f);
    }
    
    public void writeConfig() throws JAXBException, SAXException {
        KonnektingXmlService.writeConfiguration(f, konnekt);
    }
    
    
    public void writeConfig(File file) throws JAXBException, SAXException {
        KonnektingXmlService.writeConfiguration(file, konnekt);
    }

    public String getIndividualAddress() {
        return konnekt.getConfiguration().getIndividualAddress().getAddress();
    }
    
    public String getDescription() {
        return konnekt.getConfiguration().getIndividualAddress().getDescription();
    }
    
    public void setDescription(String description) {
        konnekt.getConfiguration().getIndividualAddress().setDescription(description);
    }

    public List<? extends CommObject> getCommObjects() {
        return konnekt.getDevice().getCommObjects().getCommObject();
    }

    public List<ParameterGroup> getGroupedParameters() {
        return konnekt.getDevice().getParameters().getGroup();
    }
    
    public List<Parameter> getUngroupedParameters() {
        return konnekt.getDevice().getParameters().getParameter();
    }

    public String getDeviceName() {
        return konnekt.getDevice().getDeviceName();
    }

    public String getManufacturerName() {
        return konnekt.getDevice().getManufacturerName();
    }


}
