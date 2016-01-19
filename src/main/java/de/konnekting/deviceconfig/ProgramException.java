/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.deviceconfig;

/**
 *
 * @author achristian
 */
public class ProgramException extends Exception {

    public ProgramException(String message) {
        super(message);
    }

    public ProgramException(Throwable cause) {
        super(cause);
    }

    public ProgramException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
