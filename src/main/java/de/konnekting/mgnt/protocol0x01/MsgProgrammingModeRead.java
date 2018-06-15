/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.mgnt.protocol0x01;

/**
 *
 * @author achristian
 */
class MsgProgrammingModeRead extends ProgMessage {

    public MsgProgrammingModeRead() {
        super(ProgProtocol0x01.MSGTYPE_PROGRAMMING_MODE_READ);
    }

    @Override
    public String toString() {
        return "MsgProgrammingModeRead{}";
    }
    
    
    
}
