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
package de.konnekting.mgnt.protocol0x00;

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
public class ProgProtocol0x00 {

    private static final Logger log = LoggerFactory.getLogger(ProgProtocol0x00.class);
    private static final Logger plog = LoggerFactory.getLogger("ProgrammingLogger");

    public static ProgProtocol0x00 getInstance(Knx knx) {
        boolean debug = Boolean.getBoolean("de.root1.slicknx.konnekting.debug");
        if (debug) {
            WAIT_TIMEOUT = 5000;
            log.info("###### RUNNING DEBUG MODE #######");
        }
        return new ProgProtocol0x00(knx);
    }

    private final Knx knx;

    private static int WAIT_TIMEOUT = 500; // produktiv: 500ms, debug: 5000ms

    public static final String PROG_GA = "15/7/255";
    public static final byte PROTOCOL_VERSION = 0x00;

    public static final byte MSGTYPE_ACK = 0;
    public static final byte MSGTYPE_READ_DEVICE_INFO = 1;
    public static final byte MSGTYPE_ANSWER_DEVICE_INFO = 2;
    public static final byte MSGTYPE_RESTART = 9;

    public static final byte MSGTYPE_WRITE_PROGRAMMING_MODE = 10;
    public static final byte MSGTYPE_READ_PROGRAMMING_MODE = 11;
    public static final byte MSGTYPE_ANSWER_PROGRAMMING_MODE = 12;

    public static final byte MSGTYPE_WRITE_INDIVIDUAL_ADDRESS = 20;
    public static final byte MSGTYPE_READ_INDIVIDUAL_ADDRESS = 21;
    public static final byte MSGTYPE_ANSWER_INDIVIDUAL_ADDRESS = 22;

    public static final byte MSGTYPE_WRITE_PARAMETER = 30;
    public static final byte MSGTYPE_READ_PARAMETER = 31;
    public static final byte MSGTYPE_ANSWER_PARAMETER = 32;

    public static final byte MSGTYPE_WRITE_COM_OBJECT = 40;
    public static final byte MSGTYPE_READ_COM_OBJECT = 41;
    public static final byte MSGTYPE_ANSWER_COM_OBJECT = 42;

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

            if (event.getDestination().equals(PROG_GA) && event.getData().length == 14 && event.getData()[0] == PROTOCOL_VERSION) {
                // seems to be relevant
                byte[] data = event.getData();
                ProgMessage msg = null;
                byte type = data[1];

                switch (type) {

                    // handle answer messages
                    case MSGTYPE_ACK:
                        msg = new MsgAck(data);
                        break;
                    case MSGTYPE_ANSWER_COM_OBJECT:
                        msg = new MsgAnswerComObject(data);
                        break;
                    case MSGTYPE_ANSWER_DEVICE_INFO:
                        msg = new MsgAnswerDeviceInfo(data);
                        break;
                    case MSGTYPE_ANSWER_INDIVIDUAL_ADDRESS:
                        msg = new MsgAnswerIndividualAddress(data);
                        break;
                    case MSGTYPE_ANSWER_PROGRAMMING_MODE:
                        msg = new MsgAnswerProgrammingMode(data);
                        break;
                    case MSGTYPE_ANSWER_PARAMETER:
                        msg = new MsgAnswerParameter(data);
                        break;

                    // do nothing, we sent those messages...
                    case MSGTYPE_READ_COM_OBJECT:
                    case MSGTYPE_READ_DEVICE_INFO:
                    case MSGTYPE_READ_INDIVIDUAL_ADDRESS:
                    case MSGTYPE_READ_PARAMETER:
                    case MSGTYPE_READ_PROGRAMMING_MODE:
                    case MSGTYPE_RESTART:
                    case MSGTYPE_WRITE_COM_OBJECT:
                    case MSGTYPE_WRITE_INDIVIDUAL_ADDRESS:
                    case MSGTYPE_WRITE_PARAMETER:
                    case MSGTYPE_WRITE_PROGRAMMING_MODE:
                        break;

                    // log everything else     
                    default:
                        plog.warn("Received unknown/invalid message: {}", new ProgMessage(data) {
                        });
                }
                if (msg != null) {
                    synchronized (receivedMessages) {
                        plog.info("Received message: {}", msg);
                        receivedMessages.add(msg);
                        receivedMessages.notifyAll();
                    }
                }

            }
        }
    };

    private ProgProtocol0x00(Knx knx) {
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
        sendMessage(new MsgReadProgrammingMode());
        List<ProgMessage> waitForMessage = waitForMessage(WAIT_TIMEOUT, false);
        int count = 0;

        for (ProgMessage msg : waitForMessage) {// FIXME check also for IA matching
            if (msg.getType() == MSGTYPE_ANSWER_PROGRAMMING_MODE) {
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
        sendMessage(new MsgReadIndividualAddress());
        if (oneAddressOnly) {
            MsgAnswerIndividualAddress expectSingleMessage = expectSingleMessage(MsgAnswerIndividualAddress.class);
            list.add(expectSingleMessage.getAddress());
        } else {
            List<ProgMessage> msgList = waitForMessage(WAIT_TIMEOUT, false);
            for (ProgMessage msg : msgList) {
                if (msg instanceof MsgAnswerIndividualAddress) {
                    MsgAnswerIndividualAddress ia = (MsgAnswerIndividualAddress) msg;
                    list.add(ia.getAddress());
                }
            }
        }
        return list;
    }

    public DeviceInfo readDeviceInfo(String individualAddress) throws KnxException {
        sendMessage(new MsgReadDeviceInfo(individualAddress));
        MsgAnswerDeviceInfo msg = expectSingleMessage(MsgAnswerDeviceInfo.class);

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
        sendMessage(new MsgWriteIndividualAddress(address));
        expectAck();
    }

    public void writeParameter(byte id, byte[] paramData) throws KnxException {
        if (paramData.length > 11) {
            throw new IllegalArgumentException("Data must not exceed 11 bytes.");
        }
        sendMessage(new MsgWriteParameter(id, paramData));
        expectAck();
    }

    public byte[] readParameter(byte id) throws KnxException {
        sendMessage(new MsgReadParameter(id));
        MsgAnswerParameter parameter = expectSingleMessage(MsgAnswerParameter.class);
        return parameter.getParamValue();
    }

    public void writeComObject(ComObject comObject) throws KnxException {
        sendMessage(new MsgWriteComObject(comObject));
        expectAck();
    }

    public ComObject readComObject(byte id) throws KnxException {
        sendMessage(new MsgReadComObject(id));
        MsgAnswerComObject comObj = expectSingleMessage(MsgAnswerComObject.class);
        return comObj.getComObject();
    }

    public void writeProgrammingMode(String individualAddress, boolean progMode) throws KnxException {
        sendMessage(new MsgWriteProgrammingMode(individualAddress, progMode));
        expectAck(2 * WAIT_TIMEOUT); // give the sketch enough time to respond and set prog-mode (which should pause the device-logic)
    }

    public void restart(String individualAddress) throws KnxException {
        sendMessage(new MsgRestart(individualAddress));
//        expectAck();
    }

}
