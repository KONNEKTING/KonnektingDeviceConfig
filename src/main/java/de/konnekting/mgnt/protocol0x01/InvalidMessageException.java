/*
 * Copyright (C) 2018 Alexander Christian <info(at)konnekting.de>. All rights reserved.
 * 
 * This file is part of KONNEKTING Device Library.
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

/**
 *
 * @author achristian
 */
public class InvalidMessageException extends Exception {

    public InvalidMessageException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public InvalidMessageException(String msg) {
        super(msg);
    }
    
    
    
}
