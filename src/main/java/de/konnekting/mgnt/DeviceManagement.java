/*
 * Copyright (C) 2019 Alexander Christian <alex(at)root1.de>. All rights reserved.
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
import de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.DataReadResponse;
import de.konnekting.xml.konnektingdevice.v0.Device;
import de.konnekting.xml.konnektingdevice.v0.DeviceMemory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;
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

            deviceConfigContainer.updateDeviceMemory();

            KonnektingDevice konnektingDevice = deviceConfigContainer.getDevice();
            Device device = konnektingDevice.getDevice();

            // use helper method of DCC
            String individualAddress = deviceConfigContainer.getIndividualAddress();
            DeviceMemory deviceMemory = konnektingDevice.getConfiguration().getDeviceMemory();

            // --------------
            fireIncreaseMaxSteps(5);
            fireProgressStatusMessage(getLangString("readingSystemTable"));
            byte[] systemTableBytes = deviceMemory.getSystemTable();
            fireSingleStepDone();

            fireProgressStatusMessage(getLangString("readingAddressTable"));
            byte[] addressTableBytes = deviceMemory.getAddressTable();
            fireSingleStepDone();

            fireProgressStatusMessage(getLangString("readingAssociationTable"));
            byte[] associationTableBytes = deviceMemory.getAssociationTable();
            fireSingleStepDone();

            fireProgressStatusMessage(getLangString("readingCommObjectTable"));
            byte[] commObjectTableBytes = deviceMemory.getCommObjectTable();
            fireSingleStepDone();

            fireProgressStatusMessage(getLangString("readingParameterTable"));
            byte[] parameterTableBytes = deviceMemory.getParameterTable();
            fireSingleStepDone();
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
            fireSingleStepDone();
            SystemTable systemTable = new SystemTable(memoryRead);
            systemTable.setIndividualAddress(individualAddress);
            log.debug("read system table: {}", systemTable);

            deviceMemory.setSystemTable(systemTable.getData());
            deviceConfigContainer.updateDeviceMemory();

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
            fireSingleStepDone();

            log.info("Restart device");
            fireProgressStatusMessage(getLangString("triggerDeviceRestart"));//Trigger device restart...");
            protocol.restart(individualAddress);
            fireSingleStepDone();

            log.info("All done.");
            fireProgressStatusMessage(getLangString("done"));//All done.");

        } catch (KnxException | IllegalArgumentException ex) {
            throw new DeviceManagementException("Programming failed", ex);
        }

    }

    /**
     * Send firmware over the bus, requires started prog-mode
     *
     * @param f firmwarefile to send to the device
     */
    public void sendFOTB(File f) throws DeviceManagementException {
        sendData(f, ProgProtocol0x01.UPDATE_DATATYPE, ProgProtocol0x01.UPDATE_DATAID);
    }

    /**
     * Send data over the bus, requires started prog-mode
     *
     * @param f file to send to the device
     */
    public void sendData(File f, byte dataType, byte dataId) throws DeviceManagementException {
        if (isProgramming) {
            fireIncreaseMaxSteps(2);
            try {
                CRC32 crc32 = new CRC32();
                protocol.dataWritePrepare(dataType, dataId, f.length());
                fireSingleStepDone();

                FileInputStream fis = new FileInputStream(f);
                BufferedInputStream bis = new BufferedInputStream(fis);

                byte[] buffer = new byte[ProgProtocol0x01.DATA_WRITE_BYTES_MAX];

                long length = f.length();

                //----
                int written = 0;

                fireIncreaseMaxSteps((int) Math.ceil((double) length / (double) ProgProtocol0x01.DATA_WRITE_BYTES_MAX));

                while (written != length) {

                    int writeStep = (int) (length - written > ProgProtocol0x01.DATA_WRITE_BYTES_MAX ? ProgProtocol0x01.DATA_WRITE_BYTES_MAX : length - written);

                    log.debug("\twriting {} bytes. {} of {} bytes done", writeStep, written, length);

                    buffer = bis.readNBytes(writeStep);
                    protocol.dataWrite(progressCurrent, buffer);

                    written += writeStep;
                    fireSingleStepDone();
                }
                // ----

                fis.close();

                protocol.dataWriteFinish(crc32);
                fireSingleStepDone();
            } catch (KnxException | IOException ex) {
                throw new DeviceManagementException("writing data failed", ex);
            }
        } else {
            throw new IllegalStateException("Device is not set to prog mode via API");
        }
    }

    /**
     * read data over the bus, requires started prog-mode
     *
     * @param f file to write received data to
     */
    public void readData(File f, byte dataType, byte dataId) throws DeviceManagementException {
        if (isProgramming) {
            fireIncreaseMaxSteps(2);
            try {
                DataReadResponse dataReadResponse = protocol.startDataRead(dataType, dataId);

                log.debug("got response: {}", dataReadResponse.toString());
                
                FileOutputStream fos = new FileOutputStream(f);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                
                long size = dataReadResponse.getSize();
                CRC32 crc32 = new CRC32();
                
                int dataMsgCount = Math.round(size / ProgProtocol0x01.DATA_READ_BYTES_MAX);
                log.debug("will receive {} read data messages based on {} bytes of data", dataMsgCount, size);
                for (int i = 0; i < dataMsgCount; i++) {
                    byte[] data = protocol.dataRead();
                    log.debug("Got data #{}: {}", Helper.bytesToHex(data, true));
                    crc32.update(data);
                    bos.write(data);
                    protocol.sendAck();
                }
                bos.close();
                log.debug("data read done");
                if (crc32.getValue() == dataReadResponse.getCrc32()) {
                    log.debug("CRC match!");
                } else {
                    log.error("CRC mismatch!");
                }

            } catch (KnxException | IOException ex) {
                throw new DeviceManagementException("reading data failed", ex);
            }
        } else {
            throw new IllegalStateException("Device is not set to prog mode via API");
        }
    }

    /**
     * Starts programming existing device with given address
     *
     * @param individualAddress address of device you want to program, or null
     * if you program via progbutton
     * @param manufacturerId
     * @param deviceId
     * @param revision
     * @throws de.root1.slicknx.KnxException
     */
    private void startProgMode(String individualAddress, int manufacturerId, short deviceId, short revision) throws KnxException, DeviceManagementException {

        progressMaxSteps = 0;
        progressCurrent = 0;

        if (isProgramming) {
            throw new IllegalStateException("Already in programming mode. Please call stopProgramming() first.");
        }

        if (individualAddress != null && !individualAddress.isEmpty() && Helper.checkValidPa(individualAddress)) {

            log.debug("Program with existing IA");
            List<String> devices = protocol.programmingModeRead();
            if (!devices.isEmpty()) {
                throw new KnxException("Programming via IA. There are devices in prog-mode. Aborting.");
            }
            fireSingleStepDone();

            protocol.programmingModeWrite(individualAddress, true);
            fireSingleStepDone();

            devices = protocol.programmingModeRead();
            fireSingleStepDone();

            if (devices.size() != 1) {
                throw new KnxException("Programming via IA. Not able to enable just one single device. Found " + devices + " in prog mode. Aborting.");
            }
            log.debug("Enabled prog-mode on device {}", individualAddress);

        } else {

            log.debug("Program with help of ProgButton");
            List<String> devices = protocol.programmingModeRead();
            fireSingleStepDone();

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
        fireSingleStepDone();

        // check for correct device
        if (ppdi.getManufacturerId() != manufacturerId || ppdi.getDeviceId() != deviceId || ppdi.getRevision() != revision) {
            throw new DeviceManagementException("Device does not match to configuration.\n"
                    + " KONNEKTING reported: \n"
                    + "  manufacturer: " + String.format("0x%04x", ppdi.getManufacturerId()) + "\n"
                    + "  device: " + String.format("0x%02x", ppdi.getDeviceId()) + "\n"
                    + "  revision: " + String.format("0x%02x", ppdi.getRevision()) + "\n"
                    + " Configuration requires:\n"
                    + "  manufacturer: " + String.format("0x%04x", manufacturerId) + "\n"
                    + "  device: " + String.format("0x%02x", deviceId) + "\n"
                    + "  revision: " + String.format("0x%02x", revision));
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
        fireSingleStepDone();
        isProgramming = false;
    }

    private void memoryWrite(int addr, byte[] data) throws KnxException {
        if (!isProgramming) {
            throw new IllegalStateException("Not in programming-state- Call startProgramming() first.");
        }
        log.debug("Writing {} bytes of data to addr {}. data: {}", data.length, String.format("0x%02x", addr), Helper.bytesToHex(data, true));

        int written = 0;

        fireIncreaseMaxSteps((int) Math.ceil((double) data.length / (double) ProgProtocol0x01.MEMORY_READWRITE_BYTES_MAX));
        while (written != data.length) {

            int writeStep = (data.length - written > ProgProtocol0x01.MEMORY_READWRITE_BYTES_MAX ? ProgProtocol0x01.MEMORY_READWRITE_BYTES_MAX : data.length - written);

            log.debug("\twrite {} bytes to index {}", writeStep, String.format("0x%02x", addr));
            protocol.memoryWrite(addr, Arrays.copyOfRange(data, written, written + writeStep));

            addr += writeStep; // increment index for reading next block of 1..9 bytes
            written += writeStep;
            fireSingleStepDone();
        }
        log.debug("Done writing.");
    }

    private byte[] memoryRead(int addr, int lenght) throws KnxException {
        if (!isProgramming) {
            throw new IllegalStateException("Not in programming-state- Call startProgramming() first.");
        }
        log.debug("Reading {} bytes beginning from addr {}", lenght, String.format("0x%02x", addr));

        byte[] result = new byte[lenght];
        int read = 0;
        fireIncreaseMaxSteps((int) Math.ceil((double) lenght / (double) ProgProtocol0x01.MEMORY_READWRITE_BYTES_MAX));

        while (lenght != 0) {
            int readStep = (lenght > ProgProtocol0x01.MEMORY_READWRITE_BYTES_MAX ? ProgProtocol0x01.MEMORY_READWRITE_BYTES_MAX : lenght);
            lenght -= readStep;
            log.debug("\tRead {} bytes from addr {}", readStep, String.format("0x%02x", addr));
            byte[] data = protocol.memoryRead(addr, readStep);

            System.arraycopy(data, 0, result, read, readStep);

            addr += readStep; // increment index for reading next block of 1..9 bytes
            read += readStep;
            fireSingleStepDone();
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

    private void fireSingleStepDone() {
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
            log.error("Problem reading/using key '" + completeKey + "': " + ex.getMessage());
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
            abort = false;
            throw new DeviceManagementException("Programming aborted");
        }
    }

    public static void main(String[] args) {
        byte b0 = (byte) 0x00;
        byte b1 = (byte) 0xff;
        String ia = "15/7/0";
        byte[] convertIaToBytes = Helper.convertIaToBytes(ia);
        System.out.println("b0=" + String.format("0x%02x", convertIaToBytes[0]));
        System.out.println("b1=" + String.format("0x%02x", convertIaToBytes[1]));
    }
}
