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

import de.konnekting.deviceconfig.utils.Helper;
import de.konnekting.xml.konnektingdevice.v0.CommObjectConfiguration;
import de.konnekting.xml.konnektingdevice.v0.KonnektingDevice;
import de.konnekting.xml.konnektingdevice.v0.ParameterConfiguration;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import de.konnekting.mgnt.ComObject;
import de.konnekting.mgnt.KonnektingManagement;
import de.konnekting.mgnt.SystemReadOnlyTable;
import de.konnekting.xml.konnektingdevice.v0.DeviceMemory;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class Program {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("de/konnekting/deviceconfig/i18n/language"); // NOI18N
    private final List<ProgramProgressListener> listeners = new ArrayList<>();
    private final Knx knx;
    private final KonnektingManagement mgt;
    private boolean abort;

    public Program(Knx knx) {
        this.knx = knx;
        mgt = new KonnektingManagement(knx);
    }

    public void addProgressListener(ProgramProgressListener listener) {
        listeners.add(listener);
    }

    public void removeProgressListener(ProgramProgressListener listener) {
        listeners.remove(listener);
    }

    public void abort() {
        abort = true;
        log.info("Abort triggered!");
    }

    /**
     * This will block until all is done or exception occured 
     *
     * @param device
     * @param doIndividualAddress
     * @param doComObjects
     * @param doParams
     * @throws de.konnekting.deviceconfig.ProgramException
     */
    public void program(DeviceConfigContainer device, boolean doIndividualAddress, boolean doComObjects, boolean doParams) throws ProgramException {

        try {
            fireProgressStatusMessage(getLangString("initialize")); // "Initialize..."
            if (!device.hasConfiguration()) {
                throw new IllegalArgumentException("Device " + device + " has no programmable configuration");
            }

            KonnektingDevice c = device.getDevice();
            String individualAddress = device.getIndividualAddress();

            int i = 0;

            int maxSteps = 4;

            if (doIndividualAddress) {
                maxSteps++;
            }

            DeviceMemory deviceMemory = c.getConfiguration().getDeviceMemory();

            fireProgressStatusMessage(getLangString("readingSystemTable"));
            byte[] system = deviceMemory.getSystem();
            fireProgressUpdate(++i, maxSteps);

            fireProgressStatusMessage(getLangString("readingAddressTable"));
            byte[] addressTable = deviceMemory.getAddressTable();
            fireProgressUpdate(++i, maxSteps);

            fireProgressStatusMessage(getLangString("readingAssociationTable"));
            byte[] associationTable = deviceMemory.getAssociationTable();
            fireProgressUpdate(++i, maxSteps);

            fireProgressStatusMessage(getLangString("readingCommObjectTable"));
            byte[] commObjectTable = deviceMemory.getCommObjectTable();
            fireProgressUpdate(++i, maxSteps);

            fireProgressStatusMessage(getLangString("readingParameterTable"));
            byte[] parameterTable = deviceMemory.getParameterTable();
            fireProgressUpdate(++i, maxSteps);
            

            fireProgressStatusMessage(getLangString("readingSystemTable"));
            SystemReadOnlyTable systemReadOnly = mgt.getSystemReadOnlyTable();


            if (doIndividualAddress) {
                if (!abort) {

                    log.info("About to write physical address '" + individualAddress + "'. Please press 'program' button on target device NOW ...");
                    fireProgressStatusMessage(getLangString("pleasePressProgButton"));//Please press 'program' button...

                    try {
                        mgt.writeIndividualAddress(individualAddress);
                        fireProgressUpdate(++i, maxSteps);
                    } catch (KnxException ex) {
                        log.error("Problem writing individual address", ex);
                        throw new ProgramException("Problem writing individual address", ex);
                    }
                } else {
                    fireProgressStatusMessage(getLangString("cancelled"));
                    fireProgressUpdate(maxSteps, maxSteps);
                    abort = false;
                    return;
                }
            }

            if (abort) {
                fireProgressStatusMessage(getLangString("cancelled"));
                fireProgressUpdate(maxSteps, maxSteps);
                abort = false;
                return;
            }

            int manufacturerId = c.getDevice().getManufacturerId();
            short deviceId = c.getDevice().getDeviceId();
            short revision = c.getDevice().getRevision();

            if (!abort) {
                fireProgressStatusMessage(getLangString("startProgramming"));//Starting programming...
                mgt.startProgramming(individualAddress, manufacturerId, deviceId, revision);
                fireProgressUpdate(++i, maxSteps);
            } else {
                fireProgressStatusMessage(getLangString("cancelled"));
                abort = false;
                return;
            }

            if (doComObjects) {
                if (!abort) {
                    log.info("Writing AddressTable");
                    mgt.memoryWrite(systemReadOnly.getAddressTableAddress(), addressTable);
                    fireProgressUpdate(++i, maxSteps);
                    
                    log.info("Writing AssociationTable");
                    mgt.memoryWrite(systemReadOnly.getAssociationTableAddress(), associationTable);
                    fireProgressUpdate(++i, maxSteps);

                    
                    log.info("Writing CommObjectTable");
                    mgt.memoryWrite(systemReadOnly.getCommobjectTableAddress(), commObjectTable);
                    fireProgressUpdate(++i, maxSteps);

                } else {
                    fireProgressStatusMessage(getLangString("cancelled"));
                    abort = false;
                    return;
                }
            }

            if (doParams) {
                if (!abort) {
                    log.info("Writing parameter ...");
                    
                    log.info("Writing ParameterTable");
                    mgt.memoryWrite(systemReadOnly.getParameterTableAddress(), parameterTable);
                    
                    fireProgressUpdate(++i, maxSteps);

                } else {
                    fireProgressStatusMessage(getLangString("cancelled"));
                    abort = false;
                    return;
                }
            }

            log.info("Stopping programming");
            fireProgressStatusMessage(getLangString("stoppingProgramming"));//Stopping programming...");
            mgt.stopProgramming();
            fireProgressUpdate(++i, maxSteps);
            log.info("Restart device");
            fireProgressStatusMessage(getLangString("triggerDeviceRestart"));//Trigger device restart...");
            mgt.restart(individualAddress);
            fireProgressUpdate(++i, maxSteps);

            log.info("All done.");
            fireProgressStatusMessage(getLangString("done"));//All done.");
            fireProgressUpdate(maxSteps, maxSteps);

        } catch (KnxException ex) {
            throw new ProgramException("Programming failed", ex);
        }
    }

    private String getLangString(String key, Object... values) {
        String completeKey = getClass().getSimpleName()+"."+key;
        try {
            String s = bundle.getString(completeKey);
            return String.format(s, values);
        } catch (Exception ex) {
            log.error("Problem reading/using key '"+completeKey+"'", ex);
            return "<"+completeKey+">";
        }
    }
    
    private String getLangString(String key) {
        String completeKey = getClass().getSimpleName()+"."+key;
        try {
        return bundle.getString(completeKey);
        } catch (Exception ex) {
            log.error("Problem reading/using key '"+completeKey+"'", ex);
            return "<"+completeKey+">";
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

}
