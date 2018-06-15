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
package de.konnekting.mgnt.protocol0x01;

import de.root1.slicknx.GroupAddressEvent;
import de.root1.slicknx.GroupAddressListener;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import de.konnekting.mgnt.ComObject;
import de.konnekting.mgnt.DeviceInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class ProgProtocol0x01 {

    private static final Logger log = LoggerFactory.getLogger(ProgProtocol0x01.class);
    private static final Logger plog = LoggerFactory.getLogger("ProgrammingLogger");

    public static ProgProtocol0x01 getInstance(Knx knx) {
        boolean debug = Boolean.getBoolean("de.root1.slicknx.konnekting.debug");
        if (debug) {
            WAIT_TIMEOUT = 5000;
            log.info("###### RUNNING DEBUG MODE #######");
        }
        return new ProgProtocol0x01(knx);
    }

    private final Knx knx;

    private static int WAIT_TIMEOUT = 500; // produktiv: 500ms, debug: 5000ms

    public static final String PROG_GA = "15/7/255";
    public static final byte PROTOCOL_VERSION = 0x01;

    public static final byte MSGTYPE_ACK = 0x00;
    public static final byte MSGTYPE_DEVICE_INFO_READ = 0x01;
    public static final byte MSGTYPE_DEVICE_INFO_RESPONSE = 0x02;
    public static final byte MSGTYPE_RESTART = 0x09;

    public static final byte MSGTYPE_PROGRAMMING_MODE_WRITE = 0x0A;
    public static final byte MSGTYPE_PROGRAMMING_MODE_READ = 0x0B;
    public static final byte MSGTYPE_PROGRAMMING_MODE_RESPONSE = 0x0C;

    public static final byte MSGTYPE_INDIVIDUAL_ADDRESS_WRITE = 0x14;
    public static final byte MSGTYPE_INDIVIDUAL_ADDRESS_READ = 0x15;
    public static final byte MSGTYPE_INDIVIDUAL_ADDRESS_RESPONSE = 0x16;

    public static final byte MSGTYPE_MEMORY_WRITE = 0x1E;
    public static final byte MSGTYPE_MEMORY_READ = 0x1F;
    public static final byte MSGTYPE_MEMORY_RESPONSE = 0x20;

    public static final byte MSGTYPE_DATA_PREPARE = 0x28;
    public static final byte MSGTYPE_DATA_WRITE = 0x29;
    public static final byte MSGTYPE_DATA_FINISH = 0x2A;
    public static final byte MSGTYPE_DATA_REMOVE = 0x2B;

    private final List<ProgMessage> receivedMessages = new ArrayList<>();

    private final GroupAddressListener gal = new GroupAddressListener() {

        @Override
        public void readRequest(GroupAddressEvent event) {
            // not handled
        }

        @Override
        public void readResponse(GroupAddressEvent event) {
            // not handled
        }

        @Override
        public void write(GroupAddressEvent event) {

            if (event.getDestination().equals(PROG_GA)) {
                try {
                    // seems to be relevant
                    byte[] data = event.getData();
                    ProgMessage msg = null;
                    byte version = data[0];

                    if (data.length != 14) {
                        throw new InvalidMessageException(String.format("Telegram size does not match. expected: %i, got: %i", 14, data.length));
                    }

                    if (version != PROTOCOL_VERSION) {
                        throw new InvalidMessageException(String.format("Protocol version in telegram does not match. expected: 0x%02x, got: 0x%02x", PROTOCOL_VERSION, version));
                    }
                    byte type = data[1];

                    switch (type) {

                        // handle answer messages
                        case MSGTYPE_ACK:
                            msg = new MsgAck(data);
                            break;
                        case MSGTYPE_DEVICE_INFO_RESPONSE:
                            msg = new MsgDeviceInfoResponse(data);
                            break;
                        case MSGTYPE_PROGRAMMING_MODE_RESPONSE:
                            msg = new MsgProgrammingModeResponse(data);
                            break;
                        case MSGTYPE_INDIVIDUAL_ADDRESS_RESPONSE:
                            msg = new MsgIndividualAddressResponse(data);
                            break;
                        case MSGTYPE_MEMORY_RESPONSE:
                            msg = new MsgMemoryResponse(data);
                            break;

                        // log everything else     
                        default:
                            throw new InvalidMessageException("Received unknown/invalid message: "+ new ProgMessage(data) {
                            });
                            
                    }
                    if (msg != null) {
                        synchronized (receivedMessages) {
                            plog.info("Received message: {}", msg);
                            receivedMessages.add(msg);
                            receivedMessages.notifyAll();
                        }
                    }
                } catch (InvalidMessageException ex) {
                    plog.warn("Invalid message during programming detected", ex);
                }

            }
        }
    };

    private ProgProtocol0x01(Knx knx) {
        this.knx = knx;
        knx.addGroupAddressListener(PROG_GA, gal);
    }

    /**
     * Wait for messages.
     *
     * @param timeout milliseconds to wait for messages
     * @param returnOnFirstMsg if method should return on first received message
     * @return
     */
    private List<ProgMessage> waitForMessage(int timeout, boolean returnOnFirstMsg) {
        log.debug("Waiting for message. timeout={} returnOnFirst={}", timeout, returnOnFirstMsg);
        long start = System.currentTimeMillis();
        List<ProgMessage> list = new ArrayList<>();
        while ((System.currentTimeMillis() - start) < timeout) {
            synchronized (receivedMessages) {
                try {

                    receivedMessages.wait(timeout / 10);

                    if (!receivedMessages.isEmpty()) {

                        if (returnOnFirstMsg) {
                            list.add(receivedMessages.remove(0));
                            return list;
                        } else {
                            list.addAll(receivedMessages);
                            receivedMessages.clear();
                        }

                    }
                } catch (InterruptedException ex) {
                }
            }
        }
        return list;
    }

    private <T extends ProgMessage> T expectSingleMessage(Class<T> msgClass, int timeout) throws KnxException {

        log.debug("Waiting for single message [{}]", msgClass.getName());
        List<ProgMessage> list = waitForMessage(timeout, true);

        if (list.isEmpty()) {

            throw new KnxException("Waiting for answer of type " + msgClass.getName() + " timed out.");

        } else if (list.size() == 1) {

            if (!(list.get(0).getClass().isAssignableFrom(msgClass))) {
                throw new KnxException("Wrong message type received. Expected:" + msgClass + ". Got: " + list.get(0));
            } else {
                // all ok, return message
                return (T) list.get(0);
            }

        } else {

            // more then one message received
            StringBuilder sb = new StringBuilder();
            for (ProgMessage msg : list) {
                sb.append("\n" + msg.toString());
            }

            throw new KnxException("Received " + list.size() + " messages. Expected 1 of type " + msgClass.getName() + ". List: " + sb.toString());

        }
    }

    private <T extends ProgMessage> T expectSingleMessage(Class<T> msgClass) throws KnxException {
        return expectSingleMessage(msgClass, WAIT_TIMEOUT);
    }

    private void expectAck(int timeout) throws KnxException {
        MsgAck ack = expectSingleMessage(MsgAck.class);
        if (!ack.isAcknowledged()) {
            String exMsg = "Not acknowledged. " + ack.toString();
            throw new KnxException(exMsg);
        }
    }

    private void expectAck() throws KnxException {
        MsgAck ack = expectSingleMessage(MsgAck.class);
        if (!ack.isAcknowledged()) {
            String exMsg = "Not acknowledged. " + ack.toString();
            throw new KnxException(exMsg);
        }
    }

    private void sendMessage(ProgMessage msg) throws KnxException {
        plog.info("Sending: {}", msg);
        byte[] msgData = msg.data;

        log.trace("Sending message \n"
                + "ProtocolVersion: {}\n"
                + "MsgTypeId      : {}\n"
                + "data[2..13]    : {} {} {} {} {} {} {} {} {} {} {} {}", new Object[]{
                    String.format("%02x", msgData[0]),
                    String.format("%02x", msgData[1]),
                    String.format("%02x", msgData[2]),
                    String.format("%02x", msgData[3]),
                    String.format("%02x", msgData[4]),
                    String.format("%02x", msgData[5]),
                    String.format("%02x", msgData[6]),
                    String.format("%02x", msgData[7]),
                    String.format("%02x", msgData[8]),
                    String.format("%02x", msgData[9]),
                    String.format("%02x", msgData[10]),
                    String.format("%02x", msgData[11]),
                    String.format("%02x", msgData[12]),
                    String.format("%02x", msgData[13]),});
        knx.writeRaw(false, PROG_GA, msgData);
    }

    /**
     *
     * @return true, if exactly one device responds to read-device-info -->
     * @throws KnxException
     */
    public boolean onlyOneDeviceInProgMode() throws KnxException {
        sendMessage(new MsgProgrammingModeRead());
        List<ProgMessage> waitForMessage = waitForMessage(WAIT_TIMEOUT, false);
        int count = 0;

        for (ProgMessage msg : waitForMessage) {// FIXME check also for IA matching
            if (msg.getType() == MSGTYPE_PROGRAMMING_MODE_RESPONSE) {
                count++;
            }
        }
        return count == 1;
    }

    /**
     * Reads the individual address of a konnekting device
     * <p>
     * The konnekting device is a device in programming mode. In situations
     * necessary to know whether more than one device is in programming mode,
     * <code>oneAddressOnly</code> is set to <code>false</code> and the device
     * addresses are listed in the returned address array. In this case, the
     * whole response timeout is waited for read responses. If
     * <code>oneAddressOnly</code> is <code>true</code>, the method returns
     * after receiving the first read response.
     *
     * @param oneAddressOnly
     * @return lis of found addresseskk
     * @throws KnxException
     */
    public List<String> readIndividualAddress(boolean oneAddressOnly) throws KnxException {
        List<String> list = new ArrayList<>();
        sendMessage(new MsgIndividualAddressRead());
        if (oneAddressOnly) {
            MsgIndividualAddressResponse expectSingleMessage = expectSingleMessage(MsgIndividualAddressResponse.class);
            list.add(expectSingleMessage.getAddress());
        } else {
            List<ProgMessage> msgList = waitForMessage(WAIT_TIMEOUT, false);
            for (ProgMessage msg : msgList) {
                if (msg instanceof MsgIndividualAddressResponse) {
                    MsgIndividualAddressResponse ia = (MsgIndividualAddressResponse) msg;
                    list.add(ia.getAddress());
                }
            }
        }
        return list;
    }

    public DeviceInfo readDeviceInfo(String individualAddress) throws KnxException {
        sendMessage(new MsgDeviceInfoRead(individualAddress));
        MsgDeviceInfoResponse msg = expectSingleMessage(MsgDeviceInfoResponse.class);

        return new DeviceInfo(msg.getManufacturerId(), msg.getDeviceId(), msg.getRevisionId(), msg.getDeviceFlags(), msg.getIndividualAddress());
    }

    /**
     * Writes address to device which is in programming mode
     *
     * @param address address to write to device
     * @throws KnxException if f.i. a timeout occurs or more than one device is
     * in programming mode
     */
    public void writeIndividualAddress(String address) throws KnxException {
        boolean exists = false;

        try {
            readDeviceInfo(address);
            exists = true;
            log.debug("Device with {} exists", address);
        } catch (KnxException ex) {

        }

        boolean setAddr = false;
        int attempts = 20;
        int count = 0;

        String msg = "";

        while (count != 1 && attempts-- > 0) {
            try {
                // gibt nur Antwort von Geräten im ProgMode
                List<String> list = readIndividualAddress(false);
                count = list.size();

                log.info("responsed: {}", count);

                if (count == 0) {
                    log.info("No device responded.");
                    msg = "no device in prog mode";
                } else if (count == 1 && !list.get(0).equals(address)) {
                    msg = "one device with different address (" + list.get(0) + ") responded.";
                    setAddr = true;
                } else if (count == 1 && list.get(0).equals(address)) {
                    log.debug("One device responded, but already has {}.", address);
                    msg = "One device responded, but already has " + address + ".";
                    setAddr = true;
                } else {
                    msg = "more than one device in prog mode: " + Arrays.toString(list.toArray());
                }
            } catch (KnxException ex) {
                ex.printStackTrace();
                if (exists) {
                    log.warn("device exists but is not in programming mode, cancel writing address");
                    throw new KnxException("device exists but is not in programming mode, cancel writing address");
                }
            }
            log.debug("KONNEKTINGs in programming mode: {}", count);
        }
        if (!setAddr) {
            log.warn("Can not set address. " + msg);
            throw new KnxException("Can not set address. " + msg);
        }
        log.debug("Writing address ...");
        sendMessage(new MsgIndividualAddressWrite(address));
        expectAck();
    }

    public void memoryWrite(int index, byte[] data) throws KnxException {
        // FIXME
        //sendMessage(new MsgWriteComObject(comObject));
        expectAck();
    }

    public byte[] memoryRead(int index, int length) throws KnxException {
        byte[] data = null;
        // FIXME
//        sendMessage(new MsgReadComObject(id));
//        MsgAnswerComObject comObj = expectSingleMessage(MsgAnswerComObject.class);
        return data;
    }

    public void programmingModeWrite(String individualAddress, boolean progMode) throws KnxException {
        sendMessage(new MsgProgrammingModeWrite(individualAddress, progMode));
        expectAck(2 * WAIT_TIMEOUT); // give the sketch enough time to respond and set prog-mode (which should pause the device-logic)
    }

    public void restart(String individualAddress) throws KnxException {
        sendMessage(new MsgRestart(individualAddress));
//        expectAck();
    }

}
