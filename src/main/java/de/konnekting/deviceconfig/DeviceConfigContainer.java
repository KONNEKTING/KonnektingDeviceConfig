/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.deviceconfig;

import de.konnekting.deviceconfig.exception.InvalidAddressFormatException;
import de.konnekting.deviceconfig.utils.Helper;
import de.konnekting.xml.konnektingdevice.v0.CommObject;
import de.konnekting.xml.konnektingdevice.v0.CommObjectConfiguration;
import de.konnekting.xml.konnektingdevice.v0.CommObjectConfigurations;
import de.konnekting.xml.konnektingdevice.v0.Configuration;
import de.konnekting.xml.konnektingdevice.v0.IndividualAddress;
import de.konnekting.xml.konnektingdevice.v0.KonnektingDevice;
import de.konnekting.xml.konnektingdevice.v0.Parameter;
import de.konnekting.xml.konnektingdevice.v0.ParameterConfiguration;
import de.konnekting.xml.konnektingdevice.v0.ParameterConfigurations;
import de.konnekting.xml.konnektingdevice.v0.ParameterGroup;
import de.konnekting.xml.KonnektingXmlService;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.JAXBException;
import org.xml.sax.SAXException;

/**
 *
 * @author achristian
 */
public class DeviceConfigContainer {

    private final KonnektingDevice konnekt;
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
    
    private Configuration getOrCreateConfiguration(){
        Configuration configuration = konnekt.getConfiguration();
        if (configuration==null) {
            configuration = new Configuration();
            configuration.setDeviceId(getDeviceId());
            configuration.setManufacturerId(getManufacturerId());
            configuration.setRevision(getRevision());
            konnekt.setConfiguration(configuration);
        }
        return configuration;
    }
    
    private IndividualAddress getOrCreateIndividualAddress() {
        Configuration configuration = getOrCreateConfiguration();
        IndividualAddress individualAddress = configuration.getIndividualAddress();
        if (individualAddress==null) {
            individualAddress = new IndividualAddress();
            konnekt.getConfiguration().setIndividualAddress(individualAddress);
        }
        return individualAddress;
    }
    
    private ParameterConfiguration getOrCreateParameterConf(short id) {
        
        // check if ID is valid
        List<Parameter> params = getAllParameters();
        
        boolean idValid = false;
        for (Parameter param : params) {
            if (param.getId()==id)  {
                idValid=true;
                break;
            }
        }
        if (!idValid) throw new IllegalArgumentException("Parameter ID "+id+" not known/valid");
        
        List<ParameterConfiguration> paramConfigs = konnekt.getConfiguration().getParameterConfigurations().getParameterConfiguration();
        for (ParameterConfiguration conf : paramConfigs) {
            if (conf.getId()==id){
                return conf;
            }
        }
        ParameterConfiguration conf = new ParameterConfiguration();
        conf.setId(id);
        konnekt.getConfiguration().getParameterConfigurations().getParameterConfiguration().add(conf);
        return conf;
    }
    
    private CommObjectConfiguration getOrCreateCommObjConf(short id) {
        
        // check if ID is valid
        List<CommObject> commObjects = konnekt.getDevice().getCommObjects().getCommObject();
        
        boolean idValid = false;
        for (CommObject co : commObjects) {
            if (co.getId()==id)  {
                idValid=true;
                break;
            }
        }
        if (!idValid) throw new IllegalArgumentException("CommObject ID "+id+" not known/valid");
        
        
        CommObjectConfigurations commObjectConfigurations = getOrCreateCommObjectConfigurations();
        
        List<CommObjectConfiguration> commObjectConfigurationList = commObjectConfigurations.getCommObjectConfiguration();
        
        for (CommObjectConfiguration conf : commObjectConfigurationList) {
            if (conf.getId()==id){
                return conf;
            }
        }
        CommObjectConfiguration conf = new CommObjectConfiguration();
        conf.setId(id);
        commObjectConfigurations.getCommObjectConfiguration().add(conf);
        return conf;
    }
    
    private ParameterConfigurations getOrCreateParameterConfigurations() {
        Configuration configuration = getOrCreateConfiguration();
        ParameterConfigurations parameterConfigurations = configuration.getParameterConfigurations();
        if (parameterConfigurations==null) {
            parameterConfigurations = new ParameterConfigurations();
            configuration.setParameterConfigurations(parameterConfigurations);
        }
        return parameterConfigurations;
    }
    
    private CommObjectConfigurations getOrCreateCommObjectConfigurations() {
        Configuration configuration = getOrCreateConfiguration();
        CommObjectConfigurations commObjectConfigurations = configuration.getCommObjectConfigurations();
        if (commObjectConfigurations==null) {
            commObjectConfigurations = new CommObjectConfigurations();
            configuration.setCommObjectConfigurations(commObjectConfigurations);
        }
        return commObjectConfigurations;
    }

    public String getIndividualAddress() {
        return getOrCreateIndividualAddress().getAddress();
    }
    
    public void setIndividualAddress(String address) throws InvalidAddressFormatException {
        if (!Helper.isParkedAddress(address)) {
            Helper.checkValidPa(address);
        }
        getOrCreateIndividualAddress().setAddress(address);
    }
    
    public String getDescription() {
        return getOrCreateIndividualAddress().getDescription();
    }
    
    public void setDescription(String description) {
        getOrCreateIndividualAddress().setDescription(description);
    }

    public List<? extends CommObject> getCommObjects() {
        return konnekt.getDevice().getCommObjects().getCommObject();
    }

    public List<ParameterGroup> getParameterGroups() {
        return konnekt.getDevice().getParameters().getGroup();
    }
    
    public String getDeviceName() {
        String deviceName = konnekt.getDevice().getDeviceName();
        if (deviceName==null) {
            deviceName = "Device("+String.format("0x%02x",getDeviceId())+")";
        }
        return deviceName;
        
    }

    public String getManufacturerName() {
        String manufacturerName = konnekt.getDevice().getManufacturerName();
        if (manufacturerName==null) {
            manufacturerName = "Manufacturer("+String.format("0x%04x",getManufacturerId())+")";
        }
        return manufacturerName;
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

    public String getCommObjectDescription(Short id) {
        
        List<CommObjectConfiguration> commObjectConfigurations = getOrCreateCommObjectConfigurations().getCommObjectConfiguration();
        for (CommObjectConfiguration conf : commObjectConfigurations) {
            if (conf.getId()==id){
                return Helper.convertNullString(conf.getDescription());
            }
        }
        return "";
    }

    public String getCommObjectGroupAddress(Short id) {
        List<CommObjectConfiguration> commObjectConfigurations = konnekt.getConfiguration().getCommObjectConfigurations().getCommObjectConfiguration();
        for (CommObjectConfiguration conf : commObjectConfigurations) {
            if (conf.getId()==id){
                return Helper.convertNullString(conf.getGroupAddress());
            }
        }
        return "";
    }

    public void setCommObjectDescription(Short id, String description) {
        getOrCreateCommObjConf(id).setDescription(description);
    }
    
    public void setCommObjectGroupAddress(Short id, String address) throws InvalidAddressFormatException {
        Helper.checkValidGa(address);
        getOrCreateCommObjConf(id).setGroupAddress(address);
    }
    
    private List<Parameter> getAllParameters() {
        List<Parameter> params = new ArrayList<>();
        List<ParameterGroup> paramGroups = konnekt.getDevice().getParameters().getGroup();
        for (ParameterGroup paramGroup : paramGroups) {
            params.addAll(paramGroup.getParameter());
        }
        return params;
    }

    @Override
    public String toString() {
        return getIndividualAddress()+" "+getDescription()+"@"+f.getAbsolutePath();
    }

    public Parameter getParameter(short id) {
        List<ParameterGroup> groups = konnekt.getDevice().getParameters().getGroup();
        for (ParameterGroup group : groups) {
            List<Parameter> params = group.getParameter();
            for (Parameter param : params) {
                if (param.getId() == id) {
                    return param;
                }
            }
            
        }
        return null;
    }
    
    public ParameterConfiguration getParameterConfig(short id) {
        ParameterConfigurations parameterConfigurations = getOrCreateParameterConfigurations();
        List<ParameterConfiguration> parameterConfigurationList = parameterConfigurations.getParameterConfiguration();
        
        for (ParameterConfiguration conf : parameterConfigurationList) {
            if (conf.getId()==id) {
                return conf;
            }
        }
        
        ParameterConfiguration conf = new ParameterConfiguration();
        conf.setId(id);
        konnekt.getConfiguration().getParameterConfigurations().getParameterConfiguration().add(conf);
        return conf;
    }

    public void setParameterValue(short id, byte[] value) {
        if (value==null) {
            throw new IllegalArgumentException("parameter value must not be null");
        }
        ParameterConfiguration conf = getOrCreateParameterConf(id);
        conf.setValue(value);
    }

    public List<Parameter> getParameterGroup(String selectedGroup) {
        
        List<ParameterGroup> groups = konnekt.getDevice().getParameters().getGroup();
        for (ParameterGroup group : groups) {
            if (group.getName().equals(selectedGroup)) {
                return group.getParameter();
            }
        }
        throw new IllegalArgumentException("Group '"+selectedGroup+"' not known. XML faulty?");
    }

    public boolean hasConfiguration() {
        Configuration configuration = konnekt.getConfiguration();
        return configuration!=null && configuration.getIndividualAddress()!=null;
    }
    
    

}
