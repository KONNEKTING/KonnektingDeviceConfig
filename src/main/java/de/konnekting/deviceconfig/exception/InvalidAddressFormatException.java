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
package de.konnekting.deviceconfig.exception;

/**
 *
 * @author achristian
 */
public class InvalidAddressFormatException extends Exception {

    public InvalidAddressFormatException() {
    }

    public InvalidAddressFormatException(String message) {
        super(message);
    }

    public InvalidAddressFormatException(Throwable cause) {
        super(cause);
    }

    public InvalidAddressFormatException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
