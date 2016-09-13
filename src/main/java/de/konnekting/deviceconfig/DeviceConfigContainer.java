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
package de.konnekting.deviceconfig;

import com.rits.cloning.Cloner;
import de.konnekting.deviceconfig.exception.InvalidAddressFormatException;
import de.konnekting.deviceconfig.utils.Bytes2ReadableValue;
import de.konnekting.deviceconfig.utils.Helper;
import de.konnekting.deviceconfig.utils.ReflectionIdComparator;
import de.konnekting.xml.konnektingdevice.v0.CommObject;
import de.konnekting.xml.konnektingdevice.v0.CommObjectConfiguration;
import de.konnekting.xml.konnektingdevice.v0.CommObjectConfigurations;
import de.konnekting.xml.konnektingdevice.v0.CommObjectDependency;
import de.konnekting.xml.konnektingdevice.v0.Configuration;
import de.konnekting.xml.konnektingdevice.v0.Dependencies;
import de.konnekting.xml.konnektingdevice.v0.IndividualAddress;
import de.konnekting.xml.konnektingdevice.v0.KonnektingDevice;
import de.konnekting.xml.konnektingdevice.v0.Parameter;
import de.konnekting.xml.konnektingdevice.v0.ParameterConfiguration;
import de.konnekting.xml.konnektingdevice.v0.ParameterConfigurations;
import de.konnekting.xml.konnektingdevice.v0.ParameterGroup;
import de.konnekting.xml.konnektingdevice.v0.KonnektingDeviceXmlService;
import de.konnekting.xml.konnektingdevice.v0.ParamType;
import de.konnekting.xml.konnektingdevice.v0.ParameterDependency;
import de.konnekting.xml.konnektingdevice.v0.ParameterGroupDependency;
import de.konnekting.xml.konnektingdevice.v0.Parameters;
import de.konnekting.xml.konnektingdevice.v0.TestType;
import de.root1.rooteventbus.RootEventBus;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unitils.reflectionassert.ReflectionComparator;
import static org.unitils.reflectionassert.ReflectionComparatorFactory.createRefectionComparator;
import org.unitils.reflectionassert.difference.Difference;
import org.xml.sax.SAXException;

/**
 *
 * @author achristian
 */
public class DeviceConfigContainer {

    private Logger log = LoggerFactory.getLogger(getClass());

    private static final RootEventBus eventbus = RootEventBus.getDefault();
    private final KonnektingDevice device;
    private KonnektingDevice deviceLastSave;
    private File f;
    private boolean defaultsFilled = false;

    public DeviceConfigContainer(File f) throws JAXBException, SAXException {
        this.f = f;
        device = KonnektingDeviceXmlService.readConfiguration(f);
        deviceLastSave = deepCloneDevice();
        fillDefaults();
    }

    /**
     * Fills in default values for comobjects and params, if not already set and
     * as long as this file is NOT a .kdevice.xml
     *
     * @throws SAXException
     * @throws JAXBException
     */
    private void fillDefaults() throws SAXException, JAXBException {
        if (!f.getName().endsWith(".kdevice.xml")) {

            boolean dirty = false;
            if (!defaultsFilled) {

                log.info("Filling defaults in file {}", f);
                defaultsFilled = true;

                Configuration c = getOrCreateConfiguration();

                if (c.getIndividualAddress() == null) {
                    log.info("Setting default individual address");
                    IndividualAddress individualAddress = new IndividualAddress();
                    individualAddress.setAddress("1.1.");
                    c.setIndividualAddress(individualAddress);
                }

                CommObjectConfigurations comObjectConfigurations = c.getCommObjectConfigurations();

                if (comObjectConfigurations == null) {
                    comObjectConfigurations = new CommObjectConfigurations();
                    c.setCommObjectConfigurations(comObjectConfigurations);
                    dirty = true;
                }

                if (comObjectConfigurations.getCommObjectConfiguration().isEmpty()) {
                    log.info("Setting defaults for com objects");
                    // set default values for comobjects
                    for (CommObject comObj : device.getDevice().getCommObjects().getCommObject()) {
                        CommObjectConfiguration comObjConf = new CommObjectConfiguration();
                        comObjConf.setId(comObj.getId());
                        comObjectConfigurations.getCommObjectConfiguration().add(comObjConf);
                        dirty = true;
                    }
                }

                ParameterConfigurations parameterConfigurations = c.getParameterConfigurations();

                if (parameterConfigurations == null) {
                    parameterConfigurations = new ParameterConfigurations();
                    c.setParameterConfigurations(parameterConfigurations);
                    dirty = true;
                }

                if (parameterConfigurations.getParameterConfiguration().isEmpty()) {
                    log.info("Setting defaults for parameters");
                    // set default param values
                    Parameters parameters = device.getDevice().getParameters();
                    if (parameters != null) {
                        List<ParameterGroup> paramGroups = parameters.getParameterGroup();
                        for (ParameterGroup paramGroup : paramGroups) {
                            List<Parameter> params = paramGroup.getParameter();
                            for (Parameter param : params) {
                                ParameterConfiguration paramConf = new ParameterConfiguration();
                                paramConf.setId(param.getId());
                                paramConf.setValue(param.getValue().getDefault());
                                parameterConfigurations.getParameterConfiguration().add(paramConf);
                                dirty = true;
                            }
                        }
                    }
                }
            }

            // update active/inactive dependencie check
            log.info("Setting active/inactive flags for com objects");
            for (CommObjectConfiguration comObjConf : device.getConfiguration().getCommObjectConfigurations().getCommObjectConfiguration()) {
                boolean oldValue = comObjConf.isActive();
                
                comObjConf.setActive(isCommObjectEnabled(comObjConf.getId()));
                boolean newValue = comObjConf.isActive();

                if (oldValue != newValue) {
                    log.info("changed active/inactive of comobj #{} from {} to {}", comObjConf.getId(), oldValue, newValue);
                    dirty = true;
                }
            }
            if (dirty) {
                log.info("Writing configuration due to dirty flag");
                writeConfig();
            }
        }
    }

    public KonnektingDevice getDevice() {
        return device;
    }

    public void writeConfig() throws JAXBException, SAXException {
        writeConfig(f);
    }

    public synchronized void writeConfig(File file) throws JAXBException, SAXException {
        this.f = file;
        log.debug("About to write config: " + f.getName());
        fillDefaults();
        boolean equal;

        ReflectionComparator reflectionComparator = createRefectionComparator();
        Difference difference = reflectionComparator.getDifference(device, deviceLastSave);
        if (difference != null) {
            equal = false;
        } else {
            equal = true;
        }

        if (!equal) {
            log.info("Saved changes for " + f.getName());
            KonnektingDeviceXmlService.validateWrite(device);
            KonnektingDeviceXmlService.writeConfiguration(file, device);
            renameFile();
            deviceLastSave = deepCloneDevice();
        } else {
            log.debug("No change detected for " + f.getName());
        }

    }

    private KonnektingDevice deepCloneDevice() {
        Cloner cloner = new Cloner();
        KonnektingDevice clone = cloner.deepClone(device);
        return clone;
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

            for (CommObject co : getAllCommObjects()) {
                CommObjectConfiguration coc = new CommObjectConfiguration();
                coc.setId(co.getId());
            }
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
            eventbus.post(new EventDeviceChanged(this));
        }
    }

    public List<? extends CommObject> getAllCommObjects() {
        return device.getDevice().getCommObjects().getCommObject();
    }

    public CommObject getCommObject(int id) {
        for (CommObject co : getAllCommObjects()) {
            if (co.getId() == id) {
                return co;
            }
        }
        return null;
    }

    public boolean isCommObjectEnabled(int id) {
        CommObject co = getCommObject(id);
        return isCommObjectEnabled(co);
    }

    /**
     * Returns true if CommObjects is "enabled" through param-dependency (or
     * missing dependency). This will also update automatically the active-flag on the config for this comobj
     *
     * @param co
     * @return
     */
    public boolean isCommObjectEnabled(CommObject co) {

        log.info("Testing co #{}", co.getId());
        Dependencies dependencies = device.getDevice().getDependencies();

        // no dependency defined, return true
        if (dependencies == null) {
            return true;
        }

        List<CommObjectDependency> commObjectDependency = dependencies.getCommObjectDependency();

        // no co dependency set, return true
        if (commObjectDependency.isEmpty()) {
            return true;
        }

        List<Parameter> allParameters = getAllParameters();
        allParameters.sort(new ReflectionIdComparator());

        for (CommObjectDependency dep : commObjectDependency) {
            if (dep.getCommObjId() == co.getId()) {
                // matching dependency found, do test
                TestType test = dep.getTest();
                Short testParamId = dep.getTestParamId();
                byte[] testValue = dep.getTestValue();

                ParameterConfiguration parameterConfig = getParameterConfig(testParamId);
                byte[] value = parameterConfig.getValue();

                ParamType type = allParameters.get(testParamId).getValue().getType();

                boolean enabled = testValue(test, testValue, value, type);
                setCommObjectActive(co.getId(), enabled);
                log.info("Dependency test result: {}", enabled);
                return enabled;

            }
        }
        
        log.info("No dependency found. Forcing to true");
        
        setCommObjectActive(co.getId(), true);
        return true;
    }

    public List<ParameterGroup> getParameterGroups() {
        return device.getDevice().getParameters().getParameterGroup();
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
    
    public boolean getCommObjectActive(Short id) {
        List<CommObjectConfiguration> commObjectConfigurations = getOrCreateCommObjectConfigurations().getCommObjectConfiguration();
        for (CommObjectConfiguration conf : commObjectConfigurations) {
            if (conf.getId() == id) {
                return conf.isActive();
            }
        }
        return false;
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
    
    public void setCommObjectActive(Short id, boolean active) {
        boolean oldValue = getCommObjectActive(id);
        getOrCreateCommObjConf(id).setActive(active);
        if (active!=oldValue) {
            eventbus.post(new EventDeviceChanged(this));
        }
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
        List<ParameterGroup> paramGroups = device.getDevice().getParameters().getParameterGroup();
        for (ParameterGroup paramGroup : paramGroups) {
            params.addAll(paramGroup.getParameter());
        }
        return params;
    }

    @Override
    public String toString() {
        return getIndividualAddress() + " " + getDescription() + (f == null ? "" : "@" + f.getAbsolutePath());
    }

    public Parameter getParameter(short id) {
        List<ParameterGroup> groups = device.getDevice().getParameters().getParameterGroup();
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

        conf.setValue(value);
        if (!Arrays.equals(oldValue, value)) {
            eventbus.post(new EventDeviceChanged(this));
            log.info("New param value: id=" + id + " value=" + Arrays.toString(value));
        }
    }

    public List<Parameter> getParameterGroup(String selectedGroup) {

        List<ParameterGroup> groups = device.getDevice().getParameters().getParameterGroup();
        for (ParameterGroup group : groups) {
            if (group.getName().equals(selectedGroup)) {
                return group.getParameter();
            }
        }
        throw new IllegalArgumentException("Group '" + selectedGroup + "' not known. XML faulty?");
    }

    public boolean hasConfiguration() {
        Configuration configuration = device.getConfiguration();
        return configuration != null && configuration.getIndividualAddress() != null && configuration.getIndividualAddress().getDescription() != null;
    }

    public void removeConfig() throws JAXBException, SAXException {
        device.setConfiguration(null);
        writeConfig();
    }

    public void remove() {
        f.delete();
        f = null;
    }

    /**
     * Rename file matching to device name etc. if required, erlse just return
     *
     * @throws JAXBException
     * @throws SAXException
     */
    private void renameFile() throws JAXBException, SAXException {
        if (!hasConfiguration() || f.getName().endsWith(".kdevice.xml")) {
            return;
        }
        String name = getDescription();

        if (name == null || name.isEmpty()) {
            name = "notdefined";
        }

        name = name.replace(" ", "_");
        name = name.replace("/", "_");
        name = name.replace("\\", "_");

        File parentFolder = f.getParentFile();

        File newFile = new File(parentFolder, name + ".kconfig.xml");

        if (newFile.equals(f)) {
            // nothing to rename
            log.trace("File {} needs no renaming", f.getName());
        } else {

            int i = 0;
            while (newFile.exists()) {
                i++;
                newFile = new File(parentFolder, name + "_" + i + ".kconfig.xml");
            }

            f.renameTo(newFile);
            log.trace("File {} renamed to {}", f.getName(), newFile.getName());
            f = newFile;
        }
    }

    /**
     * Clone the underlying XML file
     *
     * @param projectDir
     * @return resulting clone file
     * @throws IOException
     */
    public DeviceConfigContainer makeConfigFile(File projectDir) throws IOException {
        try {
            writeConfig();
            File newFile = new File(projectDir, Helper.getTempFilename(".kconfig.xml"));
            Files.copy(f.toPath(), newFile.toPath(), REPLACE_EXISTING);
            return new DeviceConfigContainer(newFile);
        } catch (SAXException | JAXBException ex) {
            throw new IOException("Error writing data before cloning.", ex);
        }
    }

    private boolean testValue(TestType test, byte[] testValue, byte[] value, ParamType valueType) {

        if (valueType != ParamType.UINT_8) {
            log.warn("INVALID DEPENDENCY: only uint8-typed params are supported as dependant param. Test will fail.");
            return false;
        }

        Bytes2ReadableValue b2r = new Bytes2ReadableValue();
        short shortValue = b2r.convertUINT8(value);
        short shortTestValue = b2r.convertUINT8(testValue);

        log.debug("Testing value {} against testvalue {} with test {}", shortValue, shortTestValue, test.toString());

        boolean testresult = false;
        switch (test) {
            case EQ:
                testresult = (shortValue == shortTestValue);
                break;

            case NE:
                testresult = (shortValue == shortTestValue);
                break;

            case GT:
                testresult = (shortValue > shortTestValue);
                break;

            case LT:
                testresult = (shortValue < shortTestValue);
                break;

            case GE:
                testresult = (shortValue >= shortTestValue);
                break;

            case LE:
                testresult = (shortValue <= shortTestValue);
                break;
        }
        return testresult;
    }

    public boolean isParameterEnabled(Parameter param) {
        log.info("Testing param #{}", param.getId());
        Dependencies dependencies = device.getDevice().getDependencies();

        // no dependency defined, return true
        if (dependencies == null) {
            return true;
        }

        List<ParameterDependency> paramDependencies = dependencies.getParameterDependency();

        // no co dependency set, return true
        if (paramDependencies.isEmpty()) {
            return true;
        }

        List<Parameter> allParameters = getAllParameters();
        allParameters.sort(new ReflectionIdComparator());

        for (ParameterDependency dep : paramDependencies) {
            if (dep.getParamId()== param.getId()) {
                // matching dependency found, do test
                TestType test = dep.getTest();
                Short testParamId = dep.getTestParamId();
                byte[] testValue = dep.getTestValue();

                ParameterConfiguration parameterConfig = getParameterConfig(testParamId);
                byte[] value = parameterConfig.getValue();

                ParamType type = allParameters.get(testParamId).getValue().getType();

                boolean enabled = testValue(test, testValue, value, type);
                log.info("Dependency test result: {}", enabled);
                return enabled;

            }
        }
        
        log.info("No dependency found. Forcing to true");
        
        return true;
    }
    
    public boolean isParameterGroupEnabled(ParameterGroup group) {
        log.info("Testing paramgroup #{}", group.getId());
        Dependencies dependencies = device.getDevice().getDependencies();

        // no dependency defined, return true
        if (dependencies == null) {
            return true;
        }

        List<ParameterGroupDependency> paramGroupDependencies = dependencies.getParameterGroupDependency();

        // no co dependency set, return true
        if (paramGroupDependencies.isEmpty()) {
            return true;
        }

        List<Parameter> allParameters = getAllParameters();
        allParameters.sort(new ReflectionIdComparator());

        for (ParameterGroupDependency dep : paramGroupDependencies) {
            if (dep.getParamGroupId()== group.getId()) {
                // matching dependency found, do test
                TestType test = dep.getTest();
                Short testParamId = dep.getTestParamId();
                byte[] testValue = dep.getTestValue();

                ParameterConfiguration parameterConfig = getParameterConfig(testParamId);
                byte[] value = parameterConfig.getValue();

                ParamType type = allParameters.get(testParamId).getValue().getType();

                boolean enabled = testValue(test, testValue, value, type);
                log.info("Dependency test result: {}", enabled);
                return enabled;

            }
        }
        
        log.info("No dependency found. Forcing to true");
        
        return true;
    }

}
