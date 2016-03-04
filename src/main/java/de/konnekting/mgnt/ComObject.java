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
package de.konnekting.mgnt;

/**
 *
 * @author achristian
 */
public class ComObject {
    private byte id;
    private String groupAddress;
    private boolean active = false;

    public ComObject(byte id, String groupAddress) {
        this.id = id;
        this.groupAddress = groupAddress;
        
        if (groupAddress==null || (groupAddress!=null && groupAddress.isEmpty())) {
            active = false;
        } else {
            active = true;
        }
    }

    public byte getId() {
        return id;
    }

    public String getGroupAddress() {
        return groupAddress;
    }

    public void setId(byte id) {
        this.id = id;
    }

    public void setGroupAddress(String groupAddress) {
        this.groupAddress = groupAddress;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
    
}
