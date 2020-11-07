/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt;

/**
 *
 * @author alexander
 */
public enum ChecksumIdentifier {
        SYSTEM_TABLE((byte) 0x00),
        ADDRESS_TABLE((byte) 0x01),
        ASSOCIATION_TABLE((byte) 0x02),
        COMMOBJECT_TABLE((byte) 0x03),
        PARAMETER_TABLE((byte) 0x04);

        public final byte id;

        private ChecksumIdentifier(byte id) {
            this.id = id;
        }

        public byte getId() {
            return id;
        }

    }