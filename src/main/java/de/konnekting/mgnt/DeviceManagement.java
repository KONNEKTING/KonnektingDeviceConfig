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
package de.konnekting.mgnt;

import de.konnekting.deviceconfig.DeviceConfigContainer;
import de.konnekting.deviceconfig.utils.Helper;
import de.konnekting.xml.konnektingdevice.v0.KonnektingDevice;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import de.konnekting.mgnt.PropertyPageDeviceInfo;
import de.konnekting.mgnt.SystemTable;
import de.konnekting.mgnt.protocol0x01.ProgProtocol0x01;
import de.konnekting.xml.konnektingdevice.v0.Device;
import de.konnekting.xml.konnektingdevice.v0.DeviceMemory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class DeviceManagement {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("de/konnekting/deviceconfig/i18n/language"); // NOI18N
    private final List<ProgramProgressListener> listeners = new ArrayList<>();
    private final Knx knx;

    /**
     * Number of bytes memoryread/memorywrite can handle at once
     */
    private static final int MEMORY_READWRITE_BYTES = 9;

    private boolean abort;
    private final ProgProtocol0x01 protocol;
    private boolean isProgramming = false;
    private int progressMaxSteps = 0;
    private int progressCurrent = 0;

    public DeviceManagement(Knx knx) {
        this.knx = knx;
        protocol = ProgProtocol0x01.getInstance(knx);
    }

    /**
     * trigger programming abort
     */
    public void abortOperation() {
        abort = true;
        log.info("Abort triggered!");
        fireProgressStatusMessage(getLangString("cancelled"));
    }

    /**
     * Program the device. This method will block until all is done or exception
     * occured
     *
     * @param deviceConfigContainer
     * @param doIndividualAddress
     * @param doComObjects
     * @param doParams
     * @throws de.konnekting.deviceconfig.ProgramException
     */
    public void program(DeviceConfigContainer deviceConfigContainer, boolean doIndividualAddress, boolean doComObjects, boolean doParams) throws DeviceManagementException {

        try {
            fireProgressStatusMessage(getLangString("initialize")); // "Initialize..."

            if (!deviceConfigContainer.hasConfiguration()) {
                throw new IllegalArgumentException("Device " + deviceConfigContainer + " has no programmable configuration");
            }

            KonnektingDevice konnektingDevice = deviceConfigContainer.getDevice();
            Device device = konnektingDevice.getDevice();

            // use helper method of DCC
            String individualAddress = deviceConfigContainer.getIndividualAddress();
            DeviceMemory deviceMemory = konnektingDevice.getConfiguration().getDeviceMemory();

            // --------------
            fireIncreaseMaxSteps(5);
            fireProgressStatusMessage(getLangString("readingSystemTable"));
            byte[] systemTableBytes = deviceMemory.getSystemTable();
            fireDone();

            fireProgressStatusMessage(getLangString("readingAddressTable"));
            byte[] addressTableBytes = deviceMemory.getAddressTable();
            fireDone();

            fireProgressStatusMessage(getLangString("readingAssociationTable"));
            byte[] associationTableBytes = deviceMemory.getAssociationTable();
            fireDone();

            fireProgressStatusMessage(getLangString("readingCommObjectTable"));
            byte[] commObjectTableBytes = deviceMemory.getCommObjectTable();
            fireDone();

            fireProgressStatusMessage(getLangString("readingParameterTable"));
            byte[] parameterTableBytes = deviceMemory.getParameterTable();
            fireDone();
            // --------------

            checkAbort();

            try {
                if (doIndividualAddress) {
                    fireIncreaseMaxSteps(2);
                    startProgMode(null, device.getManufacturerId(), device.getDeviceId(), device.getRevision());
                } else {
                    log.info("About to program with individual address '" + individualAddress + "'. Please press 'program' button on target device NOW ...");
                    fireProgressStatusMessage(getLangString("pleasePressProgButton"));//Please press 'program' button...
                    fireIncreaseMaxSteps(4);
                    startProgMode(individualAddress, device.getManufacturerId(), device.getDeviceId(), device.getRevision());
                }
            } catch (KnxException ex) {
                // TODO fail!
                throw new DeviceManagementException("Error getting into programing mode", ex);
            }

            checkAbort();

            fireIncreaseMaxSteps(1);
            byte[] memoryRead = memoryRead(SystemTable.SYSTEMTABLE_ADDRESS, SystemTable.SIZE);
            fireDone();
            SystemTable systemTable = new SystemTable(memoryRead);
            systemTable.setIndividualAddress(individualAddress);

            checkAbort();

            if (systemTable.hasChanged()) {
                fireProgressStatusMessage(getLangString("writingSystemTable"));//Writing system table ...
                memoryWrite(SystemTable.SYSTEMTABLE_WRITE_ADDRESS, systemTable.getWriteData());
            }

            checkAbort();

            // FIXME How do I know that table has changed? extra flag in XML that is set when successfully programmed? Keep track of last table to get real diff?
            if (doComObjects) {
                fireProgressStatusMessage(getLangString("writingAddressTable"));
                memoryWrite(systemTable.getAddressTableAddress(), addressTableBytes);
                fireProgressStatusMessage(getLangString("writingAssociationTable"));
                memoryWrite(systemTable.getAssociationTableAddress(), associationTableBytes);
                fireProgressStatusMessage(getLangString("writingCommObjectTable"));
                memoryWrite(systemTable.getCommobjectTableAddress(), commObjectTableBytes);
            }

            checkAbort();

            if (doParams) {
                fireProgressStatusMessage(getLangString("writingParameterTable"));
                memoryWrite(systemTable.getParameterTableAddress(), parameterTableBytes);
            }

            checkAbort();

            fireIncreaseMaxSteps(2);
            log.info("Stopping programming");
            fireProgressStatusMessage(getLangString("stoppingProgramming"));//Stopping programming...");
            stopProgramming(individualAddress);
            fireDone();

            log.info("Restart device");
            fireProgressStatusMessage(getLangString("triggerDeviceRestart"));//Trigger device restart...");
            protocol.restart(individualAddress);
            fireDone();

            log.info("All done.");
            fireProgressStatusMessage(getLangString("done"));//All done.");

        } catch (KnxException | IllegalArgumentException ex) {
            throw new DeviceManagementException("Programming failed", ex);
        }

    }

    /**
     * Starts programming existing device with given address
     *
     * @param individualAddress
     * @param manufacturerId
     * @param deviceId
     * @param revision
     * @throws de.root1.slicknx.KnxException
     */
    private void startProgMode(String individualAddress, int manufacturerId, short deviceId, short revision) throws KnxException, DeviceManagementException {
        if (isProgramming) {
            throw new IllegalStateException("Already in programming mode. Please call stopProgramming() first.");
        }

        if (individualAddress != null && !individualAddress.isEmpty() && Helper.checkValidPa(individualAddress)) {

            log.debug("Program with existing IA");
            List<String> devices = protocol.programmingModeRead();
            if (!devices.isEmpty()) {
                throw new KnxException("Programming via IA. There are devices in prog-mode. Aborting.");
            }
            fireDone();

            protocol.programmingModeWrite(individualAddress, true);
            fireDone();

            devices = protocol.programmingModeRead();
            fireDone();

            if (devices.size() != 1) {
                throw new KnxException("Programming via IA. Not able to enable just one single device. Found " + devices + " in prog mode. Aborting.");
            }
            log.debug("Enabled prog-mode on device {}", individualAddress);

        } else {

            log.debug("Program with help of ProgButton");
            List<String> devices = protocol.programmingModeRead();
            fireDone();

            if (devices.isEmpty()) {
                throw new KnxException("Programming with Button. No device found in prog mode. Aborting.");
            } else if (devices.size() > 1) {
                throw new KnxException("Programming with Button. More than one device in prog-mode. aborting.");
            } else {
                log.debug("One device with prog button found.");
            }

        }

        log.debug("Reading device info ...");
        PropertyPageDeviceInfo ppdi = readDeviceInfo(individualAddress);
        fireDone();

        // check for correct device
        if (ppdi.getManufacturerId() != manufacturerId || ppdi.getDeviceId() != deviceId || ppdi.getRevision() != revision) {
            throw new DeviceManagementException("Device does not match to configuration.\n"
                    + " KONNEKTING reported: \n"
                    + "  manufacturer: " + ppdi.getManufacturerId() + "\n"
                    + "  device: " + ppdi.getDeviceId() + "\n"
                    + "  revision: " + ppdi.getRevision() + "\n"
                    + " Configuration requires:\n"
                    + "  manufacturer: " + manufacturerId + "\n"
                    + "  device: " + deviceId + "\n"
                    + "  revision: " + revision);
        }
        log.debug("Got device info: {}", ppdi);
        isProgramming = true;
    }

    private void stopProgramming(String individualAddress) throws KnxException {
        if (!isProgramming) {
            throw new IllegalStateException("Not in programming-state. Call startProgramming() first.");
        }
        fireIncreaseMaxSteps(1);
        protocol.programmingModeWrite(individualAddress, false);
        fireDone();
        isProgramming = false;
    }

    private void memoryWrite(int index, byte[] data) throws KnxException {
        if (!isProgramming) {
            throw new IllegalStateException("Not in programming-state- Call startProgramming() first.");
        }
        log.debug("Writing {} bytes of data to index {}. data: {}", index, Helper.bytesToHex(data, true));

        int written = 0;

        fireIncreaseMaxSteps((int) Math.ceil((double) data.length / (double) MEMORY_READWRITE_BYTES));

        while (written != data.length) {

            int writeStep = (data.length - written > MEMORY_READWRITE_BYTES ? MEMORY_READWRITE_BYTES : data.length - written);

            log.debug("\twrite {} bytes to index {}", writeStep, index);
            protocol.memoryWrite(index, Arrays.copyOfRange(data, index, index + writeStep - 1));

            index += writeStep; // increment index for reading next block of 1..9 bytes
            written += writeStep;
            fireDone();
        }
        log.debug("Done writing.");
    }

    private byte[] memoryRead(int index, int lenght) throws KnxException {
        if (!isProgramming) {
            throw new IllegalStateException("Not in programming-state- Call startProgramming() first.");
        }
        log.debug("Reading {} bytes beginning from index {}", lenght, index);

        byte[] result = new byte[lenght];
        int read = 0;
        fireIncreaseMaxSteps((int) Math.ceil((double) lenght / (double) MEMORY_READWRITE_BYTES));

        while (lenght != 0) {
            int readStep = (lenght > MEMORY_READWRITE_BYTES ? MEMORY_READWRITE_BYTES : lenght);
            lenght -= readStep;
            log.debug("\tRead {} bytes from index {}", readStep, index);
            byte[] data = protocol.memoryRead(index, readStep);

            System.arraycopy(data, 0, result, read, readStep);

            index += readStep; // increment index for reading next block of 1..9 bytes
            read += readStep;
            fireDone();
        }
        log.debug("Done reading. data={}", Helper.bytesToHex(result, true));
        return result;
    }

    private PropertyPageDeviceInfo readDeviceInfo(String individualAddress) throws DeviceManagementException {
        try {
            byte[] propertyPageRead = protocol.propertyPageRead(individualAddress, PropertyPageDeviceInfo.PROPERTY_PAGE_NUM);
            PropertyPageDeviceInfo ppdi = new PropertyPageDeviceInfo(propertyPageRead);
            return ppdi;
        } catch (KnxException ex) {
            log.error("Error reading device info property page", ex);
            throw new DeviceManagementException("Error reading device info property page", ex);
        }
    }

    /* ************************************
     * Non-Prog-Stuff
     * ************************************/
    private void fireIncreaseMaxSteps(int i) {
        progressMaxSteps += i;
        fireProgressUpdate(progressCurrent, progressMaxSteps);
    }

    private void fireDone() {
        fireDone(1);
    }

    private void fireDone(int i) {
        progressCurrent += i;
        fireProgressUpdate(progressCurrent, progressMaxSteps);
    }

    private String getLangString(String key, Object... values) {
        String completeKey = getClass().getSimpleName() + "." + key;

        try {
            String s = bundle.getString(completeKey);
            return String.format(s, values);
        } catch (Exception ex) {
            log.error("Problem reading/using key '" + completeKey + "'", ex);
            return "<" + completeKey + ">";
        }
    }

    private String getLangString(String key) {
        String completeKey = getClass().getSimpleName() + "." + key;

        try {
            return bundle.getString(completeKey);
        } catch (Exception ex) {
            log.error("Problem reading/using key '" + completeKey + "'", ex);
            return "<" + completeKey + ">";
        }
    }

    private void fireProgressStatusMessage(String statusMsg) {
        for (ProgramProgressListener listener : listeners) {
            listener.onStatusMessage(statusMsg);
        }
    }

    private void fireProgressUpdate(int currentStep, int steps) {
        for (ProgramProgressListener listener : listeners) {
            listener.onProgressUpdate(currentStep, steps);
        }
    }

    public void addProgressListener(ProgramProgressListener listener) {
        listeners.add(listener);
    }

    public void removeProgressListener(ProgramProgressListener listener) {
        listeners.remove(listener);
    }

    private void checkAbort() throws DeviceManagementException {
        if (abort) {
            abort=false;
            throw new DeviceManagementException("Programming aborted");
        }
    }

//    public static void main(String[] args) {
//        double x = Math.ceil(1000d / 9d);
//        System.out.println("x="+x);
//    }
}
