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
import de.konnekting.xml.konnektingdevice.v0.DeviceMemory;
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
import de.root1.logging.JulFormatter;
import de.root1.rooteventbus.RootEventBus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

    public static final int LIMIT_SYSTEM = 12;
    public static final int LIMIT_ADDRESSTABLEENTRIES = 127;
    public static final int LIMIT_ASSOCIATIONTABLEENTRIES = 255;
    public static final int LIMIT_COMMOBJECTTABLEENTRIES = 85;

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
        writeConfig(f, true);
    }

    public synchronized void writeConfig(File file, boolean rename) throws JAXBException, SAXException {
        if (f == null) {
            log.debug("About to write removed file for {}, skipping", this);
        }
        this.f = file;
        log.debug("About to write config: {}", f.getName());
        fillDefaults();
        updateDeviceMemory();
        boolean equal = false;

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
            if (rename) {
                renameFile();
            }
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

    public CommObjectConfiguration getCommObjectConfiguration(int id) {

        List<CommObjectConfiguration> list = device.getConfiguration().getCommObjectConfigurations().getCommObjectConfiguration();
        for (CommObjectConfiguration co : list) {
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
     * missing dependency). This will also update automatically the active-flag
     * on the config for this comobj
     *
     * @param co
     * @return
     */
    public boolean isCommObjectEnabled(CommObject co) {

        log.info("Testing co #{}", co.getId());
        Dependencies dependencies = device.getDevice().getDependencies();

        // no dependency defined, return true
        if (dependencies == null) {
            log.info("No dependencies. force to true.");
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
        hash = 71 * hash + Objects.hashCode(this.device);
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

    public List<String> getCommObjectGroupAddress(Short id) {
        List<CommObjectConfiguration> commObjectConfigurations = device.getConfiguration().getCommObjectConfigurations().getCommObjectConfiguration();
        for (CommObjectConfiguration conf : commObjectConfigurations) {
            if (conf.getId() == id) {

                List<String> groupAddresses = conf.getGroupAddress();
                for (int i = 0; i < groupAddresses.size(); i++) {
                    groupAddresses.set(i, Helper.convertNullString(groupAddresses.get(i)));
                }
                return groupAddresses;
            }
        }
        return new ArrayList<String>();
    }

    public void setCommObjectActive(Short id, boolean active) {
        boolean oldValue = getCommObjectActive(id);
        getOrCreateCommObjConf(id).setActive(active);
        if (active != oldValue) {
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

    public boolean addCommObjectGroupAddress(Short id, String address) throws InvalidAddressFormatException {
        Helper.checkValidGa(address);
        List<String> gas = getCommObjectGroupAddress(id);
        if (!gas.contains(address)) {
            gas.add(address);
            eventbus.post(new EventDeviceChanged(this));
            return true;
        }
        return false;
    }

    public boolean removeCommObjectGroupAddress(Short id, String address) throws InvalidAddressFormatException {
        Helper.checkValidGa(address);
        List<String> gas = getCommObjectGroupAddress(id);
        if (gas.contains(address)) {
            gas.remove(address);
            eventbus.post(new EventDeviceChanged(this));
            return true;
        }
        return false;
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

    public ParameterGroup getParameterGroup(String selectedGroup) {

        List<ParameterGroup> groups = device.getDevice().getParameters().getParameterGroup();
        for (ParameterGroup group : groups) {
            if (group.getName().equals(selectedGroup)) {
                return group;
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
                testresult = (shortValue != shortTestValue);
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
            if (dep.getParamId() == param.getId()) {
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
            if (dep.getParamGroupId() == group.getId()) {
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

    public void updateDeviceMemory() {

        DeviceMemory deviceMemory = device.getConfiguration().getDeviceMemory();

        if (deviceMemory == null) {
            deviceMemory = new DeviceMemory();
            device.getConfiguration().setDeviceMemory(deviceMemory);
        }

        /**
         * System --> fixed size array
         */
        byte[] systemBytes = new byte[LIMIT_SYSTEM];
        clearBytes(systemBytes);
        byte[] iaBytes = Helper.convertIaToBytes(device.getConfiguration().getIndividualAddress().getAddress());
        if (iaBytes != null) {
            // insert IA
            systemBytes[0] = iaBytes[0];
            systemBytes[1] = iaBytes[1];
        }
        deviceMemory.setSystem(systemBytes);

        /**
         * AddressTable --> fixed size table
         */
        byte[] addressTable = new byte[1 + (2 * LIMIT_ADDRESSTABLEENTRIES)];
        clearBytes(addressTable);

        // use hashset to easily add GAs without duplicates
        Set<String> addrSet = new HashSet<>();
        List<CommObjectConfiguration> commObjectConfigurations = device.getConfiguration().getCommObjectConfigurations().getCommObjectConfiguration();
        for (CommObjectConfiguration commObjConfig : commObjectConfigurations) {
            List<String> groupAddress = commObjConfig.getGroupAddress();
            addrSet.addAll(groupAddress);
        }

        // convert hashset to arraylist and sort alphanumeric
        List<String> addressTableList = new ArrayList<>();
        addressTableList.addAll(addrSet);
        Collections.sort(addressTableList);

        // insert size
        addressTable[0] = (byte) addressTableList.size();

        // insert all GAs
        int addrTableIndex = 1;
        int i=0;
        for (String addr : addressTableList) {
            log.info("AddressTable index={} addrID={} GA={}", addrTableIndex, i, addr);
            byte[] ga = Helper.convertGaToBytes(addr);
            System.arraycopy(ga, 0, addressTable, addrTableIndex, 2);
            addrTableIndex += 2;
            i++;
        }
        deviceMemory.setAddressTable(addressTable);

        /**
         * AssociationTable --> fixed size table
         */
        byte[] associationTable = new byte[1 + (2 * LIMIT_ASSOCIATIONTABLEENTRIES)];
        clearBytes(associationTable);

        // sort by CommObj ID
        Collections.sort(commObjectConfigurations, new ReflectionIdComparator());

        int associationCount = 0;
        int assoTableIndex = 1;
        for (CommObjectConfiguration commObjectConfiguration : commObjectConfigurations) {

            List<String> associatedAddresses = commObjectConfiguration.getGroupAddress();

            if (!associatedAddresses.isEmpty()) {
                associationCount += associatedAddresses.size();
                for (String addr : associatedAddresses) {
                    int addrID = addressTableList.indexOf(addr);
                    if (addrID == -1) {
                        throw new RuntimeException("ComObj " + commObjectConfiguration.getId() + " has GA " + addr + " assigned, but GA is not in AddressTable!");
                    }

                    associationTable[assoTableIndex] = (byte) addrID;
                    associationTable[assoTableIndex + 1] = (byte) commObjectConfiguration.getId();
                    log.info("AssociationTable index={} addrID={} comObjID={}", assoTableIndex, addrID, commObjectConfiguration.getId());
                    assoTableIndex += 2;
                }
            }
        }

        // insert size
        associationTable[0] = (byte) associationCount;
        log.info("AssociationTable size={}", associationCount);
        deviceMemory.setAssociationTable(associationTable);

        /**
         * CommObjectTable --> fixed size table
         */
        byte[] commObjTable = new byte[1 + LIMIT_COMMOBJECTTABLEENTRIES];
        clearBytes(commObjTable);

        int comobjCount = getAllCommObjects().size();
        if (comobjCount > LIMIT_COMMOBJECTTABLEENTRIES) {
            throw new RuntimeException("CommObj limit exceeded: " + comobjCount + " of max. " + LIMIT_COMMOBJECTTABLEENTRIES);
        } else {

            // set size
            commObjTable[0] = (byte) comobjCount;

            int commObjTableIndex = 1;

            List<CommObject> commObjList = device.getDevice().getCommObjects().getCommObject();
            Collections.sort(commObjList, new ReflectionIdComparator());

            for (CommObject co : commObjList) {
                byte flags = co.getFlags();
                // start with default flags
                byte configbyte = (byte) 0xbf; // 10111111 --> active = off
                configbyte = (byte) (flags & (byte) 0x3f); // take B6..B0 of flags

                CommObjectConfiguration conf = getCommObjectConfiguration(co.getId());
                if (conf != null) {
                    flags = conf.getFlags();
                    configbyte = (byte) (flags & (byte) 0x3f); // apply flags

                    int active = conf.isActive() ? 1 : 0;
                    configbyte = (byte) (configbyte & (active << 7));
                }
                commObjTable[commObjTableIndex] = configbyte;

                log.info("CommObjectTable index={} coID={} configbyte={}, userflags={}", new Object[]{commObjTableIndex, co.getId(), Helper.bytesToHex(new byte[]{configbyte}), conf != null});
                commObjTableIndex++;
            }

        }
        deviceMemory.setCommObjectTable(commObjTable);

        /**
         * ParameterTable --> dynamic size table (fixed on xml setup)
         */
        List<Parameter> params = getAllParameters();
        // get size of param table
        int paramTableSize = 0;
        for (Parameter parameter : params) {
            ParamType type = parameter.getValue().getType();
            paramTableSize += Helper.getParameterSize(type);
        }
        byte[] paramTable = new byte[paramTableSize];

        if (paramTableSize > 0) {
            List<ParameterConfiguration> parameterConfigurations = device.getConfiguration().getParameterConfigurations().getParameterConfiguration();
            if (parameterConfigurations.size() != params.size()) {
                throw new RuntimeException("The number of parameter configs do not match the number of available params!");
            }
            int paramTableIndex = 0;
            for (ParameterConfiguration parameterConfiguration : parameterConfigurations) {
                byte[] value = parameterConfiguration.getValue();
                System.arraycopy(value, 0, paramTable, paramTableIndex, value.length);

                log.info("ParameterTable index={}, length={}, value={}", new Object[]{paramTableIndex, value.length, Helper.bytesToHex(value)});

                paramTableIndex += value.length; // increment index for next param/value
            }
        }
        deviceMemory.setParameterTable(paramTable);

    }

    /**
     * fill array with 0xff
     *
     * @param bytearray
     */
    private void clearBytes(byte[] bytearray) {
        for (int i = 0; i < bytearray.length; i++) {
            bytearray[i] = (byte) 0xff;
        }
    }

    public static void main(String[] args) throws JAXBException, SAXException {

        JulFormatter.set();
        File fin = new File("test.kconfig.xml");
        File fout = new File("testout.kconfig.xml");
        DeviceConfigContainer dcc = new DeviceConfigContainer(fin);
        dcc.updateDeviceMemory();
        dcc.writeConfig(fout, false);

        dcc = new DeviceConfigContainer(fout);
        DeviceMemory deviceMemory = dcc.getDevice().getConfiguration().getDeviceMemory();

        System.out.println("System           = " + Helper.bytesToHex(deviceMemory.getSystem(), true));
        System.out.println("AddressTable     = " + Helper.bytesToHex(deviceMemory.getAddressTable(), true));
        System.out.println("AssociationTable = " + Helper.bytesToHex(deviceMemory.getAssociationTable(), true));
        System.out.println("CommObjectTable  = " + Helper.bytesToHex(deviceMemory.getCommObjectTable(), true));
        System.out.println("ParameterTable   = " + Helper.bytesToHex(deviceMemory.getParameterTable(), true));
    }

}
