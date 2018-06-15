/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of slicKnx.
 *
 *   slicKnx is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   slicKnx is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with slicKnx.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.konnekting.mgnt;

import de.konnekting.deviceconfig.utils.Helper;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import de.konnekting.mgnt.protocol0x01.ProgProtocol0x01;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to manage an "KNX-on-Arduino" (Karduino) device.
 *
 * @author achristian
 */
public class KonnektingManagement {

    private static final Logger log = LoggerFactory.getLogger(KonnektingManagement.class);

    /**
     * factory
     *
     * @param knx
     * @return
     */
    public static KonnektingManagement createInstance(Knx knx) {
        return new KonnektingManagement(knx);
    }

    private final Knx knx;
    private ProgProtocol0x01 protocol;
    private boolean isProgramming = false;
    private String individualAddress;

    /**
     * Dont' use this constructor directly. Use {@link Knx#createKarduinoManagement()
     * } instead.
     *
     * @param knx
     */
    public KonnektingManagement(Knx knx) {
        this.knx = knx;
        protocol = ProgProtocol0x01.getInstance(knx);
    }

    /**
     * Write individual address to device. Requires prog-button to be pressed.
     * Returns false if failed.
     *
     * @param individualAddress
     * @throws de.root1.slicknx.KnxException
     */
    public void writeIndividualAddress(String individualAddress) throws KnxException {
        protocol.writeIndividualAddress(individualAddress);
    }

    public List<String> readIndividualAddress(boolean oneAddressOnly) throws KnxException {
        return protocol.readIndividualAddress(oneAddressOnly);
    }

    /**
     * Starts programming existing device with given address
     *
     * @param individualAddress
     * @param manufacturerId
     * @param deviceId
     * @param revisionId
     * @throws de.root1.slicknx.KnxException
     */
    public void startProgramming(String individualAddress, int manufacturerId, short deviceId, short revisionId) throws KnxException {
        if (isProgramming) {
            throw new IllegalStateException("Already in programming mode. Please call stopProgramming() first.");
        }

        // set prog mode based on pa
        log.debug("Set programming mode = true");
        try {
            protocol.programmingModeWrite(individualAddress, true);
        } catch (KnxException ex) {
            throw new KnxException("No device responded for enabling prog-mode on address " + individualAddress, ex);
        }

        log.debug("Checking for devices in prog mode");
        // check for single device in prog mode (uses ReadProgMode)
        boolean cont = protocol.onlyOneDeviceInProgMode();

        if (!cont) {
            throw new KnxException("It seems that no or more than one device is in programming-mode.");
        }

        log.debug("Reading device info ...");

        DeviceInfo di = protocol.readDeviceInfo(individualAddress);

        // check for correct device
        if (di.getManufacturerId() != manufacturerId || di.getDeviceId() != deviceId || di.getRevisionId() != revisionId) {
            throw new KnxException("Device does not match.\n"
                    + " KONNEKTING reported: \n"
                    + "  manufacturer: " + di.getManufacturerId() + "\n"
                    + "  device: " + di.getDeviceId() + "\n"
                    + "  revision: " + di.getRevisionId() + "\n"
                    + " Configuration requires:\n"
                    + "  manufacturer: " + manufacturerId + "\n"
                    + "  device: " + deviceId + "\n"
                    + "  revision: " + revisionId);
        }
        log.debug("Got device info: {}", di);
        this.individualAddress = individualAddress;
        isProgramming = true;
    }

    public void stopProgramming() throws KnxException {
        if (!isProgramming) {
            throw new IllegalStateException("Not in programming-state- Call startProgramming() first.");
        }
        protocol.programmingModeWrite(individualAddress, false);
        isProgramming = false;
    }

    public void memoryWrite(int index, byte[] data) throws KnxException {
        if (!isProgramming) {
            throw new IllegalStateException("Not in programming-state- Call startProgramming() first.");
        }
        log.debug("Writing {} bytes of data to index {}. data: {}", index, Helper.bytesToHex(data, true));

        int written = 0;

        while (written != data.length) {

            int writeStep = (data.length - written > 9 ? 9 : data.length - written);

            log.debug("\twrite {} bytes to index {}", writeStep, index);
            protocol.memoryWrite(index, Arrays.copyOfRange(data, index, index + writeStep - 1));

            index += writeStep; // increment index for reading next block of 1..9 bytes
            written += writeStep;
        }
        log.debug("Done writing.");
    }

    public byte[] memoryRead(int index, int lenght) throws KnxException {
        if (!isProgramming) {
            throw new IllegalStateException("Not in programming-state- Call startProgramming() first.");
        }
        log.debug("Reading {} bytes beginning from index {}", lenght, index);

        byte[] result = new byte[lenght];
        int read = 0;

        while (lenght != 0) {
            int readStep = (lenght > 9 ? 9 : lenght);
            lenght -= readStep;
            log.debug("\tRead {} bytes from index {}", readStep, index);
            byte[] data = protocol.memoryRead(index, readStep);

            System.arraycopy(data, 0, result, read, readStep);

            index += readStep; // increment index for reading next block of 1..9 bytes
            read += readStep;
        }
        log.debug("Done reading. data={}", Helper.bytesToHex(result, true));
        return result;
    }

    /**
     * Reads read-only part of system table
     * @return
     * @throws KnxException 
     */
    public SystemReadOnlyTable getSystemReadOnlyTable() throws KnxException {
        // TODO read from memory
//        byte[] system = memoryRead(0, 32);

        byte[] system = new byte[32];
        system[0] = Helper.getHI(0);
        system[1] = Helper.getHI(0);
        
        system[2] = 0x00;
        
        system[3] = Helper.getHI(32);
        system[4] = Helper.getLO(32);
        
        system[5] = Helper.getHI(545);
        system[6] = Helper.getLO(545);
        
        system[7] = Helper.getHI(1058);
        system[8] = Helper.getLO(1058);
        
        system[9] = Helper.getHI(1315);
        system[10] = Helper.getLO(1315);
        
        return new SystemReadOnlyTable(system);
    }

    public void restart(String address) throws KnxException {
        protocol.restart(address);
    }

}
