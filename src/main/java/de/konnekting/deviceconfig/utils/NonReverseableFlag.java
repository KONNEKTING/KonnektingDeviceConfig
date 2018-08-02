/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.konnekting.deviceconfig.utils;

/**
 *
 * @author achristian
 */
public class NonReverseableFlag {

    private boolean flag = false;

    public void set() {
        flag = true;
    }

    public boolean isSet(){
        return flag;
    }
}
