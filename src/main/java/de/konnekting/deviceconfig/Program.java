/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.deviceconfig;

import de.konnekting.deviceconfig.utils.Helper;
import de.konnekting.xml.konnektingdevice.v0.CommObjectConfiguration;
import de.konnekting.xml.konnektingdevice.v0.KonnektingDevice;
import de.konnekting.xml.konnektingdevice.v0.ParameterConfiguration;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import de.root1.slicknx.konnekting.ComObject;
import de.root1.slicknx.konnekting.KonnektingManagement;
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

    private final List<ProgramProgressListener> listeners = new ArrayList<>();
    private final Knx knx;
    private final KonnektingManagement mgt;
    private boolean abort;

    public Program(Knx knx) {
        this.knx = knx;
        mgt = knx.createKarduinoManagement();
    }

    public void addProgressListener(ProgramProgressListener listener) {
        listeners.add(listener);
    }

    public void removeProgressListener(ProgramProgressListener listener) {
        listeners.remove(listener);
    }

    public void abort() {
        abort = true;
    }

    /**
     * This will block until all is done or exception occured
     *
     *
     * @param device
     */
    public void programAll(DeviceConfigContainer device) throws ProgramException {

        try {
            if (!device.hasConfiguration()) {
                throw new IllegalArgumentException("Device " + device + " has no programmable configuration");
            }

            KonnektingDevice c = device.getDevice();

            // prepare
            List<ComObject> list = new ArrayList<>();
            for (CommObjectConfiguration commObject : c.getConfiguration().getCommObjectConfigurations().getCommObjectConfiguration()) {
                fireProgressStatusMessage("Reading commobject: " + commObject.getId());
                list.add(new ComObject((byte) commObject.getId(), commObject.getGroupAddress()));
            }

            List<ParameterConfiguration> parameterConfiguration = c.getConfiguration().getParameterConfigurations().getParameterConfiguration();

            int i = 0;

            int maxSteps = 5;
            
            maxSteps += list.size();
            maxSteps += parameterConfiguration.size();
            
            fireProgressUpdate(++i, maxSteps);

            String individualAddress = device.getIndividualAddress();
            
            if (!abort) {
                
                log.info("About to write physical address '" + individualAddress + "'. Please press 'program' button on target device NOW ...");
                fireProgressStatusMessage("Please press 'program' button...");

                try {
                    mgt.writeIndividualAddress(individualAddress);
                    fireProgressUpdate(++i, maxSteps);
                } catch (KnxException ex) {
                    log.error("Problem writing individual address", ex);
                    throw new ProgramException("Problem writing individual address", ex);
                }
            } else {
                fireProgressStatusMessage("Aborted!");
                fireProgressUpdate(maxSteps, maxSteps);
                abort = false;
            }
            
            if (abort) {
                fireProgressStatusMessage("Aborted!");
                fireProgressUpdate(maxSteps, maxSteps);
                abort = false;
                return;
            }

            int manufacturerId = c.getDevice().getManufacturerId();
            short deviceId = c.getDevice().getDeviceId();
            short revision = c.getDevice().getRevision();

            if (!abort) {
                fireProgressStatusMessage("Starting programming...");
                mgt.startProgramming(individualAddress, manufacturerId, deviceId, revision);
                fireProgressUpdate(++i, maxSteps);
            } else {
                fireProgressStatusMessage("Aborted!");
                abort = false;
                return;
            }

            if (!abort) {
                log.info("Writing commobjects ...");
                fireProgressStatusMessage("Writing groupaddresses for commobjects...");
                mgt.writeComObject(list);
                fireProgressUpdate(i += list.size(), maxSteps);
            } else {
                fireProgressStatusMessage("Aborted!");
                abort = false;
            }

            if (!abort) {
                log.info("Writing parameter ...");
                for (ParameterConfiguration parameter : c.getConfiguration().getParameterConfigurations().getParameterConfiguration()) {
                    byte[] data = parameter.getValue();
                    log.debug("Writing " + Helper.bytesToHex(data) + " to param with id " + parameter.getId());
                    fireProgressStatusMessage("Writing parameter " + parameter.getId());
                    mgt.writeParameter(parameter.getId(), data);
                    fireProgressUpdate(++i, maxSteps);
                }
            } else {
                fireProgressStatusMessage("Aborted!");
                abort = false;
            }

            log.info("Stopping programming");
            fireProgressStatusMessage("Stopping programming...");
            mgt.stopProgramming();
            fireProgressUpdate(++i, maxSteps);
            log.info("Restart device");
            fireProgressStatusMessage("Trigger device restart...");
            mgt.restart(individualAddress);
            fireProgressUpdate(++i, maxSteps);

            log.info("All done.");
            fireProgressStatusMessage("All done.");
            fireProgressUpdate(maxSteps, maxSteps);

        } catch (KnxException ex) {
            throw new ProgramException("Programming failed", ex);
        }
    }

    public void programParams(DeviceConfigContainer device) {

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
