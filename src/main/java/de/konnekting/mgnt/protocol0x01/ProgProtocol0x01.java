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
package de.konnekting.mgnt.protocol0x01;

import de.konnekting.deviceconfig.utils.Helper;
import de.root1.slicknx.GroupAddressEvent;
import de.root1.slicknx.GroupAddressListener;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class ProgProtocol0x01 {

    private static final Logger log = LoggerFactory.getLogger(ProgProtocol0x01.class);
    private static final Logger plog = LoggerFactory.getLogger("ProgrammingLogger");
    
    public static final byte UPDATE_DATATYPE = (byte) 0x00;
    public static final byte UPDATE_DATAID = (byte) 0x00;

    public static ProgProtocol0x01 getInstance(Knx knx) {
        boolean debug = Boolean.getBoolean("de.root1.slicknx.konnekting.debug");
//        if (debug) {
//            WAIT_TIMEOUT = 5000;
//            log.info("###### RUNNING DEBUG MODE #######");
//        }
        return new ProgProtocol0x01(knx);
    }

    private final Knx knx;

    private static int WAIT_TIMEOUT = 1500; // produktiv: 500ms, debug: 5000ms

    public static final String PROG_GA = "15/7/255";
    public static final byte PROTOCOL_VERSION = 0x01;

    public static final byte MSGTYPE_ACK = 0x00;

    public static final byte MSGTYPE_PROPERTY_PAGE_READ = 0x01;
    public static final byte MSGTYPE_PROPERTY_PAGE_RESPONSE = 0x02;
    public static final byte MSGTYPE_RESTART = 0x09;

    public static final byte MSGTYPE_PROGRAMMING_MODE_WRITE = 0x0A;
    public static final byte MSGTYPE_PROGRAMMING_MODE_READ = 0x0B;
    public static final byte MSGTYPE_PROGRAMMING_MODE_RESPONSE = 0x0C;

    public static final byte MSGTYPE_MEMORY_WRITE = 0x1E;
    public static final byte MSGTYPE_MEMORY_READ = 0x1F;
    public static final byte MSGTYPE_MEMORY_RESPONSE = 0x20;

    public static final byte MSGTYPE_DATA_WRITE_PREPARE = 0x28;
    public static final byte MSGTYPE_DATA_WRITE = 0x29;
    public static final byte MSGTYPE_DATA_WRITE_FINISH = 0x2A;
    
    public static final byte MSGTYPE_DATA_READ = 0x2B;
    public static final byte MSGTYPE_DATA_READ_RESPONSE = 0x2C;
    public static final byte MSGTYPE_DATA_READ_DATA = 0x2D;  
    
    public static final byte MSGTYPE_DATA_REMOVE = 0x2E;

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
                        throw new InvalidMessageException(String.format("Protocol version in telegram does not match. expected: 0x%02x, got: 0x%02x. %s", PROTOCOL_VERSION, version, Helper.bytesToHex(data, true)));
                    }
                    byte type = data[1];

                    switch (type) {

                        // handle answer messages
                        case MSGTYPE_ACK:
                            msg = new MsgAck(data);
                            break;
                        case MSGTYPE_PROPERTY_PAGE_RESPONSE:
                            msg = new MsgPropertyPageResponse(data);
                            break;
                        case MSGTYPE_PROGRAMMING_MODE_RESPONSE:
                            msg = new MsgProgrammingModeResponse(data);
                            break;
                        case MSGTYPE_MEMORY_RESPONSE:
                            msg = new MsgMemoryResponse(data);
                            break;

                        // log everything else     
                        default:
                            throw new InvalidMessageException("Received unknown/invalid message: " + new ProgMessage(data) {
                            });

                    }
                    if (msg != null) {
                        synchronized (receivedMessages) {
                            receivedMessages.add(msg);
                            plog.info("Received message {} from {}. receivedMessages={}", msg, event.getSource(), receivedMessages.size());
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

                    if (receivedMessages.isEmpty()) {
                        log.debug("Waiting {}ms", timeout/10);
                        receivedMessages.wait(timeout / 10);
                    } else {
                        log.debug("messages available, continue");
                    }

                    if (!receivedMessages.isEmpty()) {

                        if (returnOnFirstMsg) {
                            list.add(receivedMessages.remove(0));
                            log.debug("got one, return 1st. duration={} ms", (System.currentTimeMillis()-start));
                            return list;
                        } else {
                            log.debug("got {}, clear and return", receivedMessages.size());
                            list.addAll(receivedMessages);
                            receivedMessages.clear();
                        }

                    }
                } catch (InterruptedException ex) {
                }
            }
        }
        log.debug("done. duration={} ms", (System.currentTimeMillis()-start));
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

    private <T extends ProgMessage> List<T> expectMessages(Class<T> msgClass, int timeout) throws KnxException {

        log.debug("Waiting for messages of typ [{}]", msgClass.getName());
        List<ProgMessage> list = waitForMessage(timeout, true);

        if (list.isEmpty()) {

            throw new KnxException("Waiting for answer of type " + msgClass.getName() + " timed out.");

        } else {

            // search for wrong message in list
            for (ProgMessage progMessage : list) {
                if (!msgClass.isAssignableFrom(progMessage.getClass())) {
                    // more then one message received
                    StringBuilder sb = new StringBuilder();
                    list.forEach((msg) -> {
                        sb.append("\n");
                        sb.append(msg);
                    });
                    throw new KnxException("Received " + list.size() + " messages. Expected only one type, but got at least one extra: " + progMessage.getClass() + ". List: " + sb.toString());
                }

            }

            return (List<T>) list;
        }
    }

    private <T extends ProgMessage> T expectSingleMessage(Class<T> msgClass) throws KnxException {
        return expectSingleMessage(msgClass, WAIT_TIMEOUT);
    }

    private void expectAck(int timeout) throws KnxException {
        MsgAck ack = expectSingleMessage(MsgAck.class, timeout);
        if (!ack.isAcknowledged()) {
            String exMsg = "Not acknowledged. " + ack.toString();
            throw new KnxException(exMsg);
        }
    }

    private void expectAck() throws KnxException {
        expectAck(WAIT_TIMEOUT);
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
    public List<String> findDevicesInProgMode() throws KnxException {
        sendMessage(new MsgProgrammingModeRead());
        List<ProgMessage> waitForMessage = waitForMessage(WAIT_TIMEOUT, false);
        List<String> devicesFound = new ArrayList<>();

        for (ProgMessage msg : waitForMessage) {
            if (msg.getType() == MSGTYPE_PROGRAMMING_MODE_RESPONSE) {
                MsgProgrammingModeResponse mpr = (MsgProgrammingModeResponse) msg;
                devicesFound.add(mpr.getAddress());
            }
        }
        return devicesFound;
    }

//    /**
//     * Reads the individual address of a konnekting device
//     * <p>
//     * The konnekting device is a device in programming mode. In situations
//     * necessary to know whether more than one device is in programming mode,
//     * <code>oneAddressOnly</code> is set to <code>false</code> and the device
//     * addresses are listed in the returned address array. In this case, the
//     * whole response timeout is waited for read responses. If
//     * <code>oneAddressOnly</code> is <code>true</code>, the method returns
//     * after receiving the first read response.
//     *
//     * @param oneAddressOnly
//     * @return lis of found addresseskk
//     * @throws KnxException
//     */
//    public List<String> readIndividualAddress(boolean oneAddressOnly) throws KnxException {
//        List<String> list = new ArrayList<>();
//        sendMessage(new MsgIndividualAddressRead());
//        if (oneAddressOnly) {
//            MsgIndividualAddressResponse expectSingleMessage = expectSingleMessage(MsgIndividualAddressResponse.class);
//            list.add(expectSingleMessage.getAddress());
//        } else {
//            List<ProgMessage> msgList = waitForMessage(WAIT_TIMEOUT, false);
//            for (ProgMessage msg : msgList) {
//                if (msg instanceof MsgIndividualAddressResponse) {
//                    MsgIndividualAddressResponse ia = (MsgIndividualAddressResponse) msg;
//                    list.add(ia.getAddress());
//                }
//            }
//        }
//        return list;
//    }
//    /**
//     * Writes address to device which is in programming mode
//     *
//     * @param address address to write to device
//     * @throws KnxException if f.i. a timeout occurs or more than one device is
//     * in programming mode
//     */
//    public void writeIndividualAddress(String address) throws KnxException {
//        boolean exists = false;
//
//        try {
//            readDeviceInfo(address);
//            exists = true;
//            log.debug("Device with {} exists", address);
//        } catch (KnxException ex) {
//
//        }
//
//        boolean setAddr = false;
//        int attempts = 20;
//        int count = 0;
//
//        String msg = "";
//
//        // ensure only one is in prog mode
//        while (count != 1 && attempts-- > 0) {
//            try {
//                // gibt nur Antwort von Geräten im ProgMode
//                List<String> list = findDevicesInProgMode();
//                count = list.size();
//
//                log.info("responsed: {}", count);
//
//                if (count == 0) {
//                    log.info("No device responded.");
//                    msg = "no device in prog mode";
//                } else if (count == 1 && !list.get(0).equals(address)) {
//                    msg = "one device with different address (" + list.get(0) + ") responded.";
//                    setAddr = true;
//                } else if (count == 1 && list.get(0).equals(address)) {
//                    log.debug("One device responded, but already has {}.", address);
//                    msg = "One device responded, but already has " + address + ".";
//                    setAddr = true;
//                } else {
//                    msg = "more than one device in prog mode: " + Arrays.toString(list.toArray());
//                }
//            } catch (KnxException ex) {
//                ex.printStackTrace();
//                if (exists) {
//                    log.warn("device exists but is not in programming mode, cancel writing address");
//                    throw new KnxException("device exists but is not in programming mode, cancel writing address");
//                }
//            }
//            log.debug("KONNEKTINGs in programming mode: {}", count);
//        }
//        if (!setAddr) {
//            log.warn("Can not set address. " + msg);
//            throw new KnxException("Can not set address. " + msg);
//        }
//        log.debug("Writing address ...");
//        
//        int systemTableAddress = 0;
//        int iaOffset = 0;
//        MsgMemoryWrite iaWriteMsg = new MsgMemoryWrite(systemTableAddress+iaOffset, Helper.convertIaToBytes(address));
//        
//        sendMessage(iaWriteMsg);
//        expectAck();
//    }
    public byte[] propertyPageRead(String individualAddress, int pagenum) throws KnxException {
        sendMessage(new MsgPropertyPageRead(individualAddress, pagenum));
        MsgPropertyPageResponse msg = expectSingleMessage(MsgPropertyPageResponse.class);
        return msg.getData();
    }

    public void memoryWrite(int memoryAddress, byte[] data) throws KnxException {
        sendMessage(new MsgMemoryWrite(memoryAddress, data));
        expectAck(WAIT_TIMEOUT*2); // writing data to memory may take some time
    }
    
    public void dataWritePrepare(byte dataType, byte dataId, long size) throws KnxException {
        sendMessage(new MsgDataWritePrepare(dataType, dataId, size));
        expectAck(WAIT_TIMEOUT);
    }
    
    public void dataWrite(int count, byte[] data) throws KnxException {
        sendMessage(new MsgDataWrite(count, data));
        expectAck(WAIT_TIMEOUT);
    }
    
    public void dataWriteFinish(CRC32 crc32) throws KnxException {
        sendMessage(new MsgDataWriteFinish(crc32));
        expectAck(WAIT_TIMEOUT);
    }

    public byte[] memoryRead(int memoryAddress, int length) throws KnxException {
        sendMessage(new MsgMemoryRead(memoryAddress, length));
        MsgMemoryResponse msg = expectSingleMessage(MsgMemoryResponse.class, WAIT_TIMEOUT*2); // reading from memory may take some time
        return msg.getData();
    }

    public void programmingModeWrite(String individualAddress, boolean progMode) throws KnxException {
        sendMessage(new MsgProgrammingModeWrite(individualAddress, progMode));
        expectAck(WAIT_TIMEOUT); // give the sketch enough time to respond and set prog-mode (which should pause the device-logic)
    }

    public List<String> programmingModeRead() throws KnxException {
        sendMessage(new MsgProgrammingModeRead());
        List<String> addresses = new ArrayList<>();
        try {
            // there may be responses, but maybe not. who knows. it's okay when nothing is responding.
            List<MsgProgrammingModeResponse> messages = expectMessages(MsgProgrammingModeResponse.class, WAIT_TIMEOUT);
            for (MsgProgrammingModeResponse msg : messages) {
                addresses.add(msg.getAddress());
            }
        } catch (KnxException ex) {
            log.info("",ex);
        }
        return addresses;
    }

    public void restart(String individualAddress) throws KnxException {
        sendMessage(new MsgRestart(individualAddress));
//        expectAck();
    }

}
