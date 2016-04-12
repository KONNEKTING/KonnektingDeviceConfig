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

import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import de.konnekting.mgnt.protocol0x00.ProgProtocol0x00;
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
    private ProgProtocol0x00 protocol;
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
        protocol = ProgProtocol0x00.getInstance(knx);
    }

    /**
     * Write individual address to device. Requires prog-button to be pressed.
     * Returns false if failed.
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
            protocol.writeProgrammingMode(individualAddress, true);
        } catch (KnxException ex) {
            throw new KnxException("No device responded for enabling prog-mode on address "+individualAddress, ex);
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
        protocol.writeProgrammingMode(individualAddress, false);
        isProgramming = false;
    }

    public void writeParameter(short id, byte[] data) throws KnxException {
        if (!isProgramming) {
            throw new IllegalStateException("Not in programming-state- Call startProgramming() first.");
        }
        log.debug("Writing parameter #{}", id);
        protocol.writeParameter((byte) id, data);
    }

    public void writeComObject(ComObject comObject) throws KnxException {
        if (!isProgramming) {
            throw new IllegalStateException("Not in programming-state- Call startProgramming() first.");
        }
        log.debug("Writing ComObject ", comObject);
        protocol.writeComObject(comObject);
    }

    public void restart(String address) throws KnxException {
        protocol.restart(address);
    }

}
