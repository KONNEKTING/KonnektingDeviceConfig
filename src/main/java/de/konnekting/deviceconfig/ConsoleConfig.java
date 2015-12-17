/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
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

import de.konnekting.deviceconfig.exception.ConfigurationException;
import de.konnekting.deviceconfig.exception.XMLFormatException;
import de.konnekting.deviceconfig.utils.Helper;
import de.konnekting.xml.schema.konnekting.CommObjectConfiguration;
import de.konnekting.xml.schema.konnekting.KONNEKTING;
import de.konnekting.xml.schema.konnekting.ParameterConfiguration;
import de.konnekting.xmlschema.KonnektingXmlService;
import de.root1.logging.JulFormatter;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import de.root1.slicknx.karduino.ComObject;
import de.root1.slicknx.karduino.KarduinoManagement;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.xml.sax.SAXException;
import tuwien.auto.calimero.exception.KNXException;

/**
 *
 * @author achristian
 */
public class ConsoleConfig {

    private static final boolean TEST = false;

    public static void main(String[] args) throws UnknownHostException, KNXException, IOException, XMLFormatException, ConfigurationException, InterruptedException, KnxException, JAXBException, SAXException {
        JulFormatter.set();
        printHeader();

        if (args.length != 4) {
            System.out.println("Incorrect number of arguments provided!");
            printHelpAndExit();
        }

        String host = null;
        int port = -1;
        String individualAddress = null;
        File f = null;

        host = args[0];
        port = Integer.parseInt(args[1]);
        individualAddress = args[2];
        f = new File(args[3]);

        System.out.println("Reading config file '" + f.getName() + "' >>>");
        KONNEKTING c = KonnektingXmlService.readConfiguration(f);
        System.out.println("<<<");

        System.out.println("Connecting to KNX via " + host + ":" + port + " >>>");
        Knx knx = new Knx();
        KarduinoManagement karduino = knx.createKarduinoManagement();
        System.out.println("<<<");

        System.out.println("About to write physical address '" + individualAddress + "'. Please press 'program' button on target device >>>");
        if (!TEST) {
            boolean b = karduino.writeIndividualAddress(individualAddress);
            if (!b) {
                System.out.println("Addressconflict with existing device");
                System.exit(1);
            }
            
            int manufacturerId = c.getDevice().getManufacturerId();
            short deviceId = c.getDevice().getDeviceId();
            short revision = c.getDevice().getRevision();
            
            karduino.startProgramming(individualAddress, manufacturerId, deviceId, revision);
        }
        System.out.println("<<<");

        System.out.println("Writing commobject >>>");
        List<ComObject> list = new ArrayList<>();
        for (CommObjectConfiguration commObject : c.getConfiguration().getCommObjectConfigurations().getCommObjectConfiguration()) {
            System.out.println("Processing commobject: " + commObject);

            list.add( new ComObject((byte) commObject.getId(), commObject.getGroupAddress()));
        }
        System.out.println("Writing GAs ... ");
        if (!TEST) {
            karduino.writeComObject(list);
        }
        
        System.out.println("<<<");

        System.out.println("Writing parameter memory >>>");
        for (ParameterConfiguration parameter : c.getConfiguration().getParameterConfigurations().getParameterConfiguration()) {
            System.out.println("Processing parameter: " + parameter);
            byte[] data = parameter.getValue();

            System.out.println("Writing " + Helper.bytesToHex(data) + " to param with id " + parameter.getId());
            if (!TEST) {
                karduino.writeParameter((byte) parameter.getId(), data);
            }
        }
        System.out.println("<<<");
        
        if (!TEST) {
            karduino.stopProgramming();
            karduino.restart(individualAddress);
        }

        System.out.println("Disconnecting from KNX >>>");

        System.out.println("<<<");
        System.out.println("Successfully terminated.");

    }

    private static void printHeader() {
        System.out.println(">> KONNEKTING/DeviceConfig Console by knx@root1.de <<\n");
    }

    private static void printHelpAndExit() {
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("   java -cp KonnektingDeviceConfig.jar de.konnekting.deviceconfig.ConsoleConfig <multicast ip of router f.i. 224.0.23.12> <port f.i. 3671> <physical address f.i. 1.1.10> <configfile>");
        System.out.println("");
        System.exit(1);
    }

}
