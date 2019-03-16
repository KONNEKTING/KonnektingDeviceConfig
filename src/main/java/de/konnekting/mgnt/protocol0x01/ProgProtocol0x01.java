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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
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

    /**
     * Number of bytes memoryread/memorywrite can handle at once
     */
    public static final int MEMORY_READWRITE_BYTES_MAX = 9;
    /**
     * Number of bytes datawrite can handle at once
     */
    public static final int DATA_WRITE_BYTES_MAX = 11;

    /**
     * Number of bytes in one read package
     */
    public static final int DATA_READ_BYTES_MAX = 11;
    
    public static ProgProtocol0x01 getInstance(Knx knx) {
//        boolean debug = Boolean.getBoolean("de.root1.slicknx.konnekting.debug");
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
    
    public static final byte MSGTYPE_UNLOAD = 0x08;
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
                        case MSGTYPE_DATA_READ_RESPONSE:
                            msg = new MsgDataReadResponse(data);
                            break;
                        case MSGTYPE_DATA_READ_DATA:
                            msg = new MsgDataReadData(data);
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
                        log.debug("Waiting {}ms", timeout / 10);
                        receivedMessages.wait(timeout / 10);
                    } else {
                        log.debug("messages available, continue");
                    }
                    
                    if (!receivedMessages.isEmpty()) {
                        
                        if (returnOnFirstMsg) {
                            list.add(receivedMessages.remove(0));
                            log.debug("got one, return 1st. duration={} ms", (System.currentTimeMillis() - start));
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
        log.debug("done. duration={} ms", (System.currentTimeMillis() - start));
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
                    String.format("%02X", msgData[0]),
                    String.format("%02X", msgData[1]),
                    String.format("%02X", msgData[2]),
                    String.format("%02X", msgData[3]),
                    String.format("%02X", msgData[4]),
                    String.format("%02X", msgData[5]),
                    String.format("%02X", msgData[6]),
                    String.format("%02X", msgData[7]),
                    String.format("%02X", msgData[8]),
                    String.format("%02X", msgData[9]),
                    String.format("%02X", msgData[10]),
                    String.format("%02X", msgData[11]),
                    String.format("%02X", msgData[12]),
                    String.format("%02X", msgData[13]),});
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
    
    public byte[] propertyPageRead(String individualAddress, int pagenum) throws KnxException {
        sendMessage(new MsgPropertyPageRead(individualAddress, pagenum));
        MsgPropertyPageResponse msg = expectSingleMessage(MsgPropertyPageResponse.class);
        return msg.getData();
    }
    
    public void memoryWrite(int memoryAddress, byte[] data) throws KnxException {
        sendMessage(new MsgMemoryWrite(memoryAddress, data));
        expectAck(2 * WAIT_TIMEOUT); // writing data to memory may take some time
    }
    
    public void dataWritePrepare(byte dataType, byte dataId, long size) throws KnxException {
        sendMessage(new MsgDataWritePrepare(dataType, dataId, size));
        expectAck(2 * WAIT_TIMEOUT);
    }
    
    public void dataWrite(int count, byte[] data) throws KnxException {
        sendMessage(new MsgDataWrite(count, data));
        expectAck(5 * WAIT_TIMEOUT);
    }
    
    public void dataWriteFinish(CRC32 crc32) throws KnxException {
        sendMessage(new MsgDataWriteFinish(crc32));
        expectAck(5 * WAIT_TIMEOUT);
    }

    /**
     *
     * @param f file to store received data
     * @param dataType data type to request
     * @param dataId data d to request
     * @return true, if file received error free, false if crc mismatches
     * @throws KnxException
     * @throws FileNotFoundException if given file is not useable
     * @throws IOException if IO error happens
     */
    public DataReadResponse startDataRead(byte dataType, byte dataId) throws KnxException, FileNotFoundException, IOException {
        log.debug("initiate reading data...");
        sendMessage(new MsgDataRead(dataType, dataId));
        MsgDataReadResponse drr = expectSingleMessage(MsgDataReadResponse.class, 5 * WAIT_TIMEOUT);
        sendAck();
        return new DataReadResponse(drr.getSize(), 0);
    }

    /**
     * waits for data to receive. this call is expected to block until data is
     * received, although the blocking time is veeeeery short.
     *
     * @return received data
     * @throws KnxException
     */
    public byte[] dataRead() throws KnxException, FileNotFoundException, IOException {
        MsgDataReadData drd = expectSingleMessage(MsgDataReadData.class, 5 * WAIT_TIMEOUT);
        sendAck();
        return drd.getReceivedData();
    }

    /**
     * waits for data to receive. this call is expected to block until data is
     * received, although the blocking time is veeeeery short.
     *
     * @return received data
     * @throws KnxException
     */
    public DataReadResponse dataReadFinalize() throws KnxException, FileNotFoundException, IOException {
        MsgDataReadResponse drr = expectSingleMessage(MsgDataReadResponse.class, 5 * WAIT_TIMEOUT);
        sendAck();
        return new DataReadResponse(drr.getSize(), drr.getCrc32());
    }
    
    public byte[] memoryRead(int memoryAddress, int length) throws KnxException {
        sendMessage(new MsgMemoryRead(memoryAddress, length));
        MsgMemoryResponse msg = expectSingleMessage(MsgMemoryResponse.class, WAIT_TIMEOUT * 2); // reading from memory may take some time
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
            log.warn("Exception during waiting for progmoderesponse messages", ex);
        }
        return addresses;
    }
    
    public void unload(boolean ia, boolean co, boolean params, boolean datastorage) throws KnxException {
        sendMessage(new MsgUnload(ia, co, params, datastorage));
        expectAck(5 * WAIT_TIMEOUT);
    }
    
    public void restart(String individualAddress) throws KnxException {
        sendMessage(new MsgRestart(individualAddress));
//        expectAck();
    }
    
    public void sendAck() throws KnxException {
        sendMessage(new MsgAck());
    }
    
    public class DataReadResponse {
        
        private long size;
        private long crc32;
        
        public DataReadResponse(long size, long crc32) {
            this.size = size;
            this.crc32 = crc32;
        }
        
        public long getSize() {
            return size;
        }
        
        public long getCrc32() {
            return crc32;
        }
        
    }
    
}
