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
import de.konnekting.mgnt.ComObject;
import de.konnekting.mgnt.KonnektingManagement;
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
     * This will block until all is done or exception occured Programs: - IA -
     * KO - Params
     *
     *
     * @param device
     */
    public void program(DeviceConfigContainer device, boolean doIndividualAddress, boolean doComObjects, boolean doParams) throws ProgramException {

        try {
            fireProgressStatusMessage("Initialize...");
            if (!device.hasConfiguration()) {
                throw new IllegalArgumentException("Device " + device + " has no programmable configuration");
            }

            KonnektingDevice c = device.getDevice();
            String individualAddress = device.getIndividualAddress();

            // prepare
            List<CommObjectConfiguration> comObjectConfiguration = null;
            List<ParameterConfiguration> parameterConfiguration;

            int i = 0;

            int maxSteps = 4;

            if (doIndividualAddress) {
                maxSteps++;
            }

            if (doComObjects) {
                fireProgressStatusMessage("Reading commobjects...");
                comObjectConfiguration = c.getConfiguration().getCommObjectConfigurations().getCommObjectConfiguration();
                maxSteps += comObjectConfiguration.size();
            }

            if (doParams) {
                fireProgressStatusMessage("Reading parameters...");
                parameterConfiguration = c.getConfiguration().getParameterConfigurations().getParameterConfiguration();
                maxSteps += parameterConfiguration.size();
            }

            fireProgressUpdate(++i, maxSteps);

            if (doIndividualAddress) {
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
                    return;
                }
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

            if (doComObjects) {
                if (!abort) {
                    log.info("Writing commobjects ...");

                    for (CommObjectConfiguration comObj : comObjectConfiguration) {
                        if (!abort) {
                            ComObject comObjectToWrite = new ComObject((byte) comObj.getId(), comObj.getGroupAddress());
                            log.debug("Writing ComObject: id={} ga={} active={}", new Object[]{comObjectToWrite.getId(), comObjectToWrite.getGroupAddress(), comObjectToWrite.isActive()});
                            fireProgressStatusMessage("Writing comobject " + comObjectToWrite.getId() + " / active=" + comObjectToWrite.isActive());
                            mgt.writeComObject(comObjectToWrite);
                            fireProgressUpdate(++i, maxSteps);
                        } else {
                            fireProgressStatusMessage("Aborted!");
                            abort = false;
                            return;
                        }
                    }

                } else {
                    fireProgressStatusMessage("Aborted!");
                    abort = false;
                    return;
                }
            }

            if (doParams) {
                if (!abort) {
                    log.info("Writing parameter ...");
                    for (ParameterConfiguration parameter : c.getConfiguration().getParameterConfigurations().getParameterConfiguration()) {
                        if (!abort) {
                            byte[] data = parameter.getValue();
                            log.debug("Writing " + Helper.bytesToHex(data) + " to param with id " + parameter.getId());
                            fireProgressStatusMessage("Writing parameter " + parameter.getId());
                            mgt.writeParameter(parameter.getId(), data);
                            fireProgressUpdate(++i, maxSteps);
                        } else {
                            fireProgressStatusMessage("Aborted!");
                            abort = false;
                            return;
                        }
                    }
                } else {
                    fireProgressStatusMessage("Aborted!");
                    abort = false;
                    return;
                }
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
