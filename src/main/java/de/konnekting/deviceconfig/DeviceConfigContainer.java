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
import de.konnekting.xml.konnektingdevice.v0.KonnektingDeviceXmlService;
import de.konnekting.xml.konnektingdevice.v0.ParameterType;
import de.root1.rooteventbus.RootEventBus;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.JAXBException;
import org.xml.sax.SAXException;

/**
 *
 * @author achristian
 */
public class DeviceConfigContainer {

    private static final RootEventBus eventbus = RootEventBus.getDefault();
    private final KonnektingDevice device;
    private File f;

    public DeviceConfigContainer(File f) throws JAXBException, SAXException {
        this.f = f;
        device = KonnektingDeviceXmlService.readConfiguration(f);
    }

    public void writeConfig() throws JAXBException, SAXException {
        writeConfig(f);
    }

    public void writeConfig(File file) throws JAXBException, SAXException {
        this.f = file;
        KonnektingDeviceXmlService.validateWrite(device);
        KonnektingDeviceXmlService.writeConfiguration(file, device);
    }

    private Configuration getOrCreateConfiguration() {
        Configuration configuration = device.getConfiguration();
        if (configuration == null) {
            configuration = new Configuration();
            configuration.setDeviceId(getDeviceId());
            configuration.setManufacturerId(getManufacturerId());
            configuration.setRevision(getRevision());
            device.setConfiguration(configuration);
        }
        return configuration;
    }

    private IndividualAddress getOrCreateIndividualAddress() {
        Configuration configuration = getOrCreateConfiguration();
        IndividualAddress individualAddress = configuration.getIndividualAddress();
        if (individualAddress == null) {
            individualAddress = new IndividualAddress();
            device.getConfiguration().setIndividualAddress(individualAddress);
        }
        return individualAddress;
    }

    private ParameterConfiguration getOrCreateParameterConf(short id) {

        // check if ID is valid
        List<Parameter> params = getAllParameters();

        boolean idValid = false;
        for (Parameter param : params) {
            if (param.getId() == id) {
                idValid = true;
                break;
            }
        }
        if (!idValid) {
            throw new IllegalArgumentException("Parameter ID " + id + " not known/valid");
        }

        List<ParameterConfiguration> paramConfigs = device.getConfiguration().getParameterConfigurations().getParameterConfiguration();
        for (ParameterConfiguration conf : paramConfigs) {
            if (conf.getId() == id) {
                return conf;
            }
        }
        ParameterConfiguration conf = new ParameterConfiguration();
        conf.setId(id);
        device.getConfiguration().getParameterConfigurations().getParameterConfiguration().add(conf);
        return conf;
    }

    private CommObjectConfiguration getOrCreateCommObjConf(short id) {

        // check if ID is valid
        List<CommObject> commObjects = device.getDevice().getCommObjects().getCommObject();

        boolean idValid = false;
        for (CommObject co : commObjects) {
            if (co.getId() == id) {
                idValid = true;
                break;
            }
        }
        if (!idValid) {
            throw new IllegalArgumentException("CommObject ID " + id + " not known/valid");
        }

        CommObjectConfigurations commObjectConfigurations = getOrCreateCommObjectConfigurations();

        List<CommObjectConfiguration> commObjectConfigurationList = commObjectConfigurations.getCommObjectConfiguration();

        for (CommObjectConfiguration conf : commObjectConfigurationList) {
            if (conf.getId() == id) {
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
        if (parameterConfigurations == null) {
            parameterConfigurations = new ParameterConfigurations();
            configuration.setParameterConfigurations(parameterConfigurations);
        }
        return parameterConfigurations;
    }

    private CommObjectConfigurations getOrCreateCommObjectConfigurations() {
        Configuration configuration = getOrCreateConfiguration();
        CommObjectConfigurations commObjectConfigurations = configuration.getCommObjectConfigurations();
        if (commObjectConfigurations == null) {
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
            if (!Helper.checkValidPa(address)) {
                throw new InvalidAddressFormatException("given individual address is not valid.");
            };
        }
        String oldIndividualAddress = getIndividualAddress();
        getOrCreateIndividualAddress().setAddress(address);
        if (!address.equals(oldIndividualAddress)) {
            eventbus.post(new EventDeviceChanged(this));
        }
    }

    public String getDescription() {
        return getOrCreateIndividualAddress().getDescription();
    }

    public void setDescription(String description) {
        if (description == null) {
            description = "";
        }
        String oldDescription = getDescription();
        getOrCreateIndividualAddress().setDescription(description);
        if (!description.equals(oldDescription)) {
//            renameFile();
            eventbus.post(new EventDeviceChanged(this));
        }
    }

    public List<? extends CommObject> getCommObjects() {
        return device.getDevice().getCommObjects().getCommObject();
    }

    public List<ParameterGroup> getParameterGroups() {
        return device.getDevice().getParameters().getGroup();
    }

    public String getDeviceName() {
        String deviceName = device.getDevice().getDeviceName();
        if (deviceName == null) {
            deviceName = "Device(" + String.format("0x%02x", getDeviceId()) + ")";
        }
        return deviceName;

    }

    public String getManufacturerName() {
        String manufacturerName = device.getDevice().getManufacturerName();
        if (manufacturerName == null) {
            manufacturerName = "Manufacturer(" + String.format("0x%04x", getManufacturerId()) + ")";
        }
        return manufacturerName;
    }

    public int getManufacturerId() {
        return device.getDevice().getManufacturerId();
    }

    public short getDeviceId() {
        return device.getDevice().getDeviceId();
    }

    public short getRevision() {
        return device.getDevice().getRevision();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.device);
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
        if (!Objects.equals(this.device, other.device)) {
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
            if (conf.getId() == id) {
                return Helper.convertNullString(conf.getDescription());
            }
        }
        return "";
    }

    public String getCommObjectGroupAddress(Short id) {
        List<CommObjectConfiguration> commObjectConfigurations = device.getConfiguration().getCommObjectConfigurations().getCommObjectConfiguration();
        for (CommObjectConfiguration conf : commObjectConfigurations) {
            if (conf.getId() == id) {
                return Helper.convertNullString(conf.getGroupAddress());
            }
        }
        return "";
    }

    public void setCommObjectDescription(Short id, String description) {
        if (description == null) {
            description = "";
        }
        String oldDescription = getCommObjectDescription(id);
        getOrCreateCommObjConf(id).setDescription(description);
        if (!description.equals(oldDescription)) {
            eventbus.post(new EventDeviceChanged(this));
        }
    }

    public void setCommObjectGroupAddress(Short id, String address) throws InvalidAddressFormatException {
        Helper.checkValidGa(address);
        String oldCommObjectGroupAddress = getCommObjectGroupAddress(id);
        getOrCreateCommObjConf(id).setGroupAddress(address);
        if (!address.equals(oldCommObjectGroupAddress)) {
            eventbus.post(new EventDeviceChanged(this));
        }
    }

    private List<Parameter> getAllParameters() {
        List<Parameter> params = new ArrayList<>();
        List<ParameterGroup> paramGroups = device.getDevice().getParameters().getGroup();
        for (ParameterGroup paramGroup : paramGroups) {
            params.addAll(paramGroup.getParameter());
        }
        return params;
    }

    @Override
    public String toString() {
        return getIndividualAddress() + " " + getDescription() + "@" + f.getAbsolutePath();
    }

    public Parameter getParameter(short id) {
        List<ParameterGroup> groups = device.getDevice().getParameters().getGroup();
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
            if (conf.getId() == id) {
                return conf;
            }
        }

        ParameterConfiguration conf = new ParameterConfiguration();
        conf.setId(id);
        device.getConfiguration().getParameterConfigurations().getParameterConfiguration().add(conf);
        return conf;
    }
    

    public void setParameterValue(short id, byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("parameter value must not be null");
        }
        byte[] oldValue = getParameterConfig(id).getValue();
        ParameterConfiguration conf = getOrCreateParameterConf(id);
        Parameter.Value paramValue = getParameter(id).getValue();
        
        byte[] minRaw = paramValue.getMin();
        byte[] maxRaw = paramValue.getMax();
        String typeRaw = paramValue.getType();

//        ParameterType type = ParameterType.valueOf(typeRaw);
//        
//        switch(type) {
//            case UINT8:
//                Short minShort = Short.valueOf(Helper.bytesToHex(minRaw), 16);
//                Short maxShort = Short.valueOf(Helper.bytesToHex(maxRaw), 16);
//                Short newValue = Short.valueOf(Helper.bytesToHex(value), 16);
//                
//                if (newValue<minShort || newValue>maxShort){
//                    throw new IllegalArgumentException("Value is not in range");
//                }
//                break;
//            default:
//                break;
//        }
        
        
        conf.setValue(value);
        if (!Arrays.equals(oldValue, value)) {
            eventbus.post(new EventDeviceChanged(this));
        }
    }

    public List<Parameter> getParameterGroup(String selectedGroup) {

        List<ParameterGroup> groups = device.getDevice().getParameters().getGroup();
        for (ParameterGroup group : groups) {
            if (group.getName().equals(selectedGroup)) {
                return group.getParameter();
            }
        }
        throw new IllegalArgumentException("Group '" + selectedGroup + "' not known. XML faulty?");
    }

    public boolean hasConfiguration() {
        Configuration configuration = device.getConfiguration();
        return configuration != null && configuration.getIndividualAddress() != null;
    }

    public void removeConfig() throws JAXBException, SAXException {
        device.setConfiguration(null);
        writeConfig();
    }

    public void remove() {
        f.delete();
        f = null;
    }

    private void renameFile() throws JAXBException, SAXException {
        writeConfig();
        String name = getDescription();
        
        if (name==null || name.isEmpty()) {
            name = "notdefined";
        }
        
        name = name.replace(" ", "_");
        name = name.replace("/", "_");
        name = name.replace("\\", "_");
        
        File parentFolder = f.getParentFile();
        
        File newFile = new File(parentFolder, name + ".kdevice.xml");
        int i = 0;
        while (newFile.exists()) {
            i++;
            newFile = new File(parentFolder, name + "_"+i+".kdevice.xml");
        }
        
        f.renameTo(newFile);
        f = newFile;
    }

}
