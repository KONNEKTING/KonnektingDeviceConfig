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
import java.util.Objects;
import javax.xml.bind.JAXBException;
import org.xml.sax.SAXException;

/**
 *
 * @author achristian
 */
public class DeviceConfigContainer {

    private final KONNEKTING konnekt;
    private File f;
    
    public DeviceConfigContainer(File f) throws JAXBException, SAXException {
        this.f = f;
        konnekt = KonnektingXmlService.readConfiguration(f);
    }
    
    public void writeConfig() throws JAXBException, SAXException {
        KonnektingXmlService.writeConfiguration(f, konnekt);
    }
    
    
    public void writeConfig(File file) throws JAXBException, SAXException {
        this.f = file;
        KonnektingXmlService.writeConfiguration(file, konnekt);
    }

    public String getIndividualAddress() {
        return konnekt.getConfiguration().getIndividualAddress().getAddress();
    }
    
    public void setIndividualAddress(String address) {
        // FIXME validate!
        konnekt.getConfiguration().getIndividualAddress().setAddress(address);
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

    public int getManufacturerId() {
        return konnekt.getDevice().getManufacturerId();
    }

    public short getDeviceId() {
        return konnekt.getDevice().getDeviceId();
    }

    public short getRevision() {
        return konnekt.getDevice().getRevision();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.konnekt);
        hash = 59 * hash + Objects.hashCode(this.f);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DeviceConfigContainer other = (DeviceConfigContainer) obj;
        if (!Objects.equals(this.konnekt, other.konnekt)) {
            return false;
        }
        if (!Objects.equals(this.f, other.f)) {
            return false;
        }
        return true;
    }


    


}
