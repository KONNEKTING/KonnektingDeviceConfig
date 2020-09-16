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
import de.konnekting.deviceconfig.exception.InvalidAddressFormatException;
import de.konnekting.deviceconfig.exception.XMLFormatException;
import de.konnekting.deviceconfig.utils.ByteArrayDiff;
import de.konnekting.deviceconfig.utils.Helper;
import de.konnekting.xml.konnektingdevice.v0.KonnektingDevice;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import de.konnekting.mgnt.protocol0x01.ProgProtocol0x01;
import de.konnekting.mgnt.protocol0x01.ProgProtocol0x01.DataReadResponse;
import de.konnekting.xml.konnektingdevice.v0.Device;
import de.konnekting.xml.konnektingdevice.v0.DeviceMemory;
import de.konnekting.xml.konnektingdevice.v0.IndividualAddress;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.CRC32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class DeviceManagement {

    /**
     * Describes the available progamming tasks that can be choosen from
     */
    public enum ProgrammingTask {

        /**
         * Programs all: IA, CommObj, Params
         */
        ALL,
        /**
         * Programms delta since last programming
         */
        PARTIAL,
        /**
         * Programs only CommObj and Params
         */
        APPDATA

    }
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("de/konnekting/deviceconfig/i18n/language"); // NOI18N
    private final List<ProgramProgressListener> listeners = new ArrayList<>();

    private boolean abort;
    private final ProgProtocol0x01 protocol;
    private boolean isProgramming = false;
    private int progressMaxSteps = 0;
    private int progressCurrent = 0;

    public DeviceManagement(Knx knx) {
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
     * @param programmingTask the task to run for programming
     * @throws de.konnekting.deviceconfig.ProgramException
     */
    public void program(DeviceConfigContainer deviceConfigContainer, ProgrammingTask programmingTask) throws DeviceManagementException {

        boolean doIndividualAddress = false;
        boolean doCommObjects = false;
        boolean doParams = false;

        boolean partial = false;

        switch (programmingTask) {
            default:
            case ALL:
                doIndividualAddress = true;
                doCommObjects = true;
                doParams = true;
                partial = false;
                break;
            case PARTIAL:
                doIndividualAddress = true;
                doCommObjects = true;
                doParams = true;
                partial = true;
                break;
            case APPDATA:
                doIndividualAddress = false;
                doCommObjects = true;
                doParams = true;
                partial = false;
                break;
        }
        try {
            fireProgressStatusMessage(getLangString("initialize")); // "Initialize..."

            if (!deviceConfigContainer.hasConfiguration()) {
                throw new IllegalArgumentException("Device " + deviceConfigContainer + " has no programmable configuration");
            }

            KonnektingDevice konnektingDevice = deviceConfigContainer.getDevice();
            Device device = konnektingDevice.getDevice();

            String individualAddress = deviceConfigContainer.getIndividualAddress();
            DeviceMemory lastDeviceMemory = konnektingDevice.getConfiguration().getDeviceMemory();
            DeviceMemory newDeviceMemory = deviceConfigContainer.createWorkingCopyDeviceMemory();

            // --------------
            fireIncreaseMaxSteps(4);

            fireProgressStatusMessage(getLangString("readingAddressTable"));
            byte[] lastAddressTableBytes = lastDeviceMemory == null ? new byte[]{} : lastDeviceMemory.getAddressTable();
            byte[] newAddressTableBytes = newDeviceMemory.getAddressTable();
            fireSingleStepDone();

            fireProgressStatusMessage(getLangString("readingAssociationTable"));
            byte[] lastAssociationTableBytes = lastDeviceMemory == null ? new byte[]{} : lastDeviceMemory.getAssociationTable();
            byte[] newAssociationTableBytes = newDeviceMemory.getAssociationTable();
            fireSingleStepDone();

            fireProgressStatusMessage(getLangString("readingCommObjectTable"));
            byte[] lastCommObjectTableBytes = lastDeviceMemory == null ? new byte[]{} : lastDeviceMemory.getCommObjectTable();
            byte[] newCommObjectTableBytes = newDeviceMemory.getCommObjectTable();
            fireSingleStepDone();

            fireProgressStatusMessage(getLangString("readingParameterTable"));
            byte[] lastParameterTableBytes = lastDeviceMemory == null ? new byte[]{} : lastDeviceMemory.getParameterTable();
            byte[] newParameterTableBytes = newDeviceMemory.getParameterTable();
            fireSingleStepDone();

            if (lastDeviceMemory == null && partial) {
                log.warn("Need to force partial=false due to missing device memory section in configuration. ");
                partial = false;
            }
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
            // read system table bytes from arduino... create local system table object to make table "human readable"
            fireProgressStatusMessage(getLangString("readingSystemTable"));
            SystemTable systemTable = new SystemTable(memoryRead(SystemTable.SYSTEMTABLE_ADDRESS, SystemTable.SIZE));
            fireSingleStepDone();
            // set IA in system table            
            systemTable.setIndividualAddress(individualAddress);
            log.debug("read system table: {}", systemTable);

            checkAbort();

            if (systemTable.hasChanged()) {
                //Writing system table ... if changed. It's not required to write table partially, it's anyhow only 16 bytes
                fireProgressStatusMessage(getLangString("writingSystemTable"));
                memoryWrite(SystemTable.SYSTEMTABLE_WRITE_ADDRESS, systemTable.getWriteData());
            }

            checkAbort();

            if (doCommObjects) {
                if (partial) {

                    fireProgressStatusMessage(getLangString("writingAddressTable"));
                    List<ByteArrayDiff.DataBlock> addressTableDiff = ByteArrayDiff.getDiffData(lastAddressTableBytes, newAddressTableBytes);
                    writeDataBlocks(systemTable.getAddressTableAddress(), addressTableDiff);

                    fireProgressStatusMessage(getLangString("writingAssociationTable"));
                    List<ByteArrayDiff.DataBlock> assocTableDiff = ByteArrayDiff.getDiffData(lastAssociationTableBytes, newAssociationTableBytes);
                    writeDataBlocks(systemTable.getAssociationTableAddress(), assocTableDiff);

                    fireProgressStatusMessage(getLangString("writingCommObjectTable"));
                    List<ByteArrayDiff.DataBlock> comObjTableDiff = ByteArrayDiff.getDiffData(lastCommObjectTableBytes, newCommObjectTableBytes);
                    writeDataBlocks(systemTable.getAddressTableAddress(), comObjTableDiff);

                } else {
                    // do the whole memory
                    fireProgressStatusMessage(getLangString("writingAddressTable"));
                    memoryWrite(systemTable.getAddressTableAddress(), newAddressTableBytes);

                    fireProgressStatusMessage(getLangString("writingAssociationTable"));
                    memoryWrite(systemTable.getAssociationTableAddress(), newAssociationTableBytes);

                    fireProgressStatusMessage(getLangString("writingCommObjectTable"));
                    memoryWrite(systemTable.getCommobjectTableAddress(), newCommObjectTableBytes);
                }
            }

            checkAbort();

            if (doParams) {
                fireProgressStatusMessage(getLangString("writingParameterTable"));
                if (partial) {
                    List<ByteArrayDiff.DataBlock> paramTableDiff = ByteArrayDiff.getDiffData(lastParameterTableBytes, newParameterTableBytes);
                    writeDataBlocks(systemTable.getParameterTableAddress(), paramTableDiff);
                } else {
                    memoryWrite(systemTable.getParameterTableAddress(), newParameterTableBytes);
                }
            }

            checkAbort();

            fireIncreaseMaxSteps(2);
            log.info("Stopping programming");
            fireProgressStatusMessage(getLangString("stoppingProgramming"));//Stopping programming...");
            stopProgMode(individualAddress);
            fireSingleStepDone();

            // update the device memory in config section, as we now have successfully programmed the device
            deviceConfigContainer.updateConfigDeviceMemory(systemTable);

            log.info("Restart device");
            fireProgressStatusMessage(getLangString("triggerDeviceRestart"));//Trigger device restart...");
            protocol.restart(individualAddress);
            fireSingleStepDone();

            log.info("All done.");
            fireProgressStatusMessage(getLangString("done"));//All done.");
            deviceConfigContainer.writeConfig();

        } catch (KnxException | IllegalArgumentException | XMLFormatException ex) {
            throw new DeviceManagementException("Programming failed", ex);
        }

    }

    /**
     * Writes diff data blocks to memory. This is used when doing partial
     * programming to speed up the programming
     *
     * @param offset offset or index at which to start
     * @param blocks blocks to write to memory at given offset
     * @throws KnxException
     */
    private void writeDataBlocks(int offset, List<ByteArrayDiff.DataBlock> blocks) throws KnxException {

        for (ByteArrayDiff.DataBlock block : blocks) {
            log.debug("Writing partial data: final offset={} length={}", offset + block.getIndex(), block.getData().length);
            memoryWrite(offset + block.getIndex(), block.getData());
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
     * @param dataType
     * @param dataId
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

                    log.info("\twriting {} bytes. {} of {} bytes done so far", writeStep, written, length);

                    buffer = bis.readNBytes(writeStep);
                    crc32.update(buffer, 0, writeStep);
                    protocol.dataWrite(writeStep, buffer);

                    written += writeStep;
                    fireSingleStepDone();
                }
                // ----

                fis.close();
                log.info("\tfinishing with crc32: {}", crc32.getValue());
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
     * read data from device over the bus, requires started prog-mode
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

                int dataMsgCount = (int) Helper.roundUp(size, ProgProtocol0x01.DATA_READ_BYTES_MAX);
                log.info("will receive {} read data messages based on {} bytes of data", dataMsgCount, size);
                for (int i = 0; i < dataMsgCount; i++) {
                    byte[] data = protocol.dataRead();
                    log.debug("Got data #{}: {}", Helper.bytesToHex(data, true));
                    crc32.update(data);
                    bos.write(data);
                }
                bos.close();
                log.debug("data read done");
                dataReadResponse = protocol.dataReadFinalize();
                if (crc32.getValue() == dataReadResponse.getCrc32()) {
                    log.debug("CRC match!");
                } else {
                    log.error("CRC mismatch! via protocol: {} self-calculated: {}", dataReadResponse.getCrc32(), crc32.getValue());
                }

            } catch (KnxException | IOException ex) {
                throw new DeviceManagementException("reading data failed", ex);
            }
        } else {
            throw new IllegalStateException("Device is not set to prog mode via API");
        }
    }

    public void removeData(byte dataType, byte dataId) throws DeviceManagementException {
        if (isProgramming) {
            try {
                protocol.dataRemove(dataType, dataId);
            } catch (KnxException ex) {
                throw new DeviceManagementException("removing data failed", ex);
            }
        } else {
            throw new IllegalStateException("Device is not set to prog mode via API");
        }
    }

    /**
     * Starts prog mode without verifying correctness of manufacturer and
     * deviceid and revision. This is only useful if only IA is programmed.
     * Therefore, this method is private access.
     *
     * @param individualAddress
     * @throws KnxException
     * @throws DeviceManagementException
     */
    private void startProgMode(String individualAddress) throws KnxException, DeviceManagementException {
        startProgMode(individualAddress, -1, (short) -1, (short) -1);
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
    void startProgMode(String individualAddress, int manufacturerId, short deviceId, short revision) throws KnxException, DeviceManagementException {

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

            ensureProgButtonOneDevice();

        }
        if (manufacturerId != -1 && deviceId != -1 && revision != -1) {

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
        }
        isProgramming = true;
    }

    private void ensureProgButtonOneDevice() throws KnxException {
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

    void stopProgMode(String individualAddress) throws KnxException {
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

    public void unload(boolean factoryreset, boolean ia, boolean co, boolean params, boolean datastorage) throws KnxException {
        ensureProgButtonOneDevice();
        protocol.unload(factoryreset, ia, co, params, datastorage);
    }

    public void restart(String individualAddress) throws KnxException {
        protocol.restart(individualAddress);
    }

    /**
     * Just write a new IA to a device identified by its current IA. The current
     * IA may be null, but requires pressed prog-button on device
     *
     * @param deviceConfigContainer
     * @param individualAddress IA to adress the device; may be null if
     * progbutton is pressed
     * @param newIndividualAddress new IA for device
     * @throws KnxException
     * @throws DeviceManagementException
     */
    public void writeIndividualAddress(DeviceConfigContainer deviceConfigContainer, String individualAddress, String newIndividualAddress) throws KnxException, DeviceManagementException {
        if (deviceConfigContainer != null) {
            try {
                deviceConfigContainer.setIndividualAddress(newIndividualAddress);
            } catch (InvalidAddressFormatException ex) {
                throw new DeviceManagementException("Given new individual address has invalid format", ex);
            }
        } else {
            throw new DeviceManagementException("no device config container given");
        }

        if (individualAddress != null) {
            startProgMode(individualAddress);
        }
        ensureProgButtonOneDevice();
        isProgramming = true;

        // read current system table from device
        SystemTable systemTable = new SystemTable(memoryRead(SystemTable.SYSTEMTABLE_ADDRESS, SystemTable.SIZE));
        log.debug("read system table: {}", systemTable);
        // set new IA on working copy of system table
        systemTable.setIndividualAddress(newIndividualAddress);
        log.debug("updated system table: {}", systemTable);

        // if table has changed, write it to device
        if (systemTable.hasChanged()) {
            log.debug("Writing table");
            memoryWrite(SystemTable.SYSTEMTABLE_WRITE_ADDRESS, systemTable.getWriteData());
        }

        stopProgMode(newIndividualAddress);
        restart(newIndividualAddress);
    }

}
