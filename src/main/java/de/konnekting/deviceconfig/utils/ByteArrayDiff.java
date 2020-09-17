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
package de.konnekting.deviceconfig.utils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author achristian
 */
public class ByteArrayDiff {
    
    public static class Block {
        public static final int UNDEFINED = -1;
        int index=UNDEFINED;
        int length=UNDEFINED;

        public Block() {
        }
        
        public Block(int index, int length) {
            this.index = index;
            this.length = length;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public void setLength(int length) {
            this.length = length;
        }
        
        public int getIndex() {
            return index;
        }

        public int getLength() {
            return length;
        }
        
        public boolean isInvalid() {
            return (index==UNDEFINED) && (length==UNDEFINED);
        }
        
        public boolean isPartialValid() {
            return (index!=UNDEFINED) || (length!=UNDEFINED);
        }

        @Override
        public String toString() {
            return "Block{" + "index=" + index + ", length=" + length + '}';
        }
        
    }
    
    public static class DataBlock {
        public static final int UNDEFINED = -1;
        int index=UNDEFINED;
        byte[] data;

        public DataBlock() {
        }
        
        public DataBlock(int index, byte[] data) {
            this.index = index;
            this.data = data;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public void setData(byte[] data) {
            this.data = data;
        }
        
        public int getIndex() {
            return index;
        }

        public byte[] getData() {
            return data;
        }
        
        public boolean isInvalid() {
            return (index==UNDEFINED) && (data==null);
        }
        
        public boolean isPartialValid() {
            return (index!=UNDEFINED) || (data!=null);
        }

        @Override
        public String toString() {
            return "DataBlock{" + "index=" + index + ", data.length=" + data.length + ", data(hex)="+Helper.bytesToHex(data, true)+'}';
        }
        
    }
    
    /**
     * Returns the data blocks which are different in newData, compared to oldData
     * @param oldData
     * @param newData
     * @return the diferent datablocks from newData, compared to oldData
     */
    public static List<DataBlock> getDiffData(byte[] oldData, byte[] newData) {
        List<Block> diff = getDiff(oldData, newData);
        List<DataBlock> dataBlocks = new ArrayList<>();
        diff.forEach(blk -> {
            byte[] d = new byte[blk.length];
            System.arraycopy(newData, blk.index, d, 0, blk.length);
            dataBlocks.add(new DataBlock(blk.index, d));
        });
        return dataBlocks;
    }
        
    
    public static List<Block> getDiff(byte[] data1, byte[] data2) {
        if (data1==null || data2==null) {
            throw new IllegalArgumentException("You must not provide null arguments.");
        }
        if (data1.length!=data2.length) {
            throw new IllegalArgumentException("both arrays must have same size. data1="+data1.length+" data2="+data2.length);
        }
        
        List<Block> diff = new ArrayList<>();

        Block block = new Block();
        
        for (int i=0;i<data1.length; i++) {
            
            // find difference
            if (block.isInvalid() && data1[i]!=data2[i]) {
                block.setIndex(i);
            } else if (block.isPartialValid() && data1[i] == data2[i]) {
                block.setLength(i-block.getIndex());
                diff.add(block);
                block = new Block();
            }
        }
        if (block.isPartialValid()) {
            block.setLength(data1.length-block.getIndex());
            diff.add(block);
        }
        
        return diff;
    }
    
    public static void main(String[] args) {
        byte[] oldData = {0, 1, 0, 0, 0, 5, 6, 7, 0, 9, 00, 00, 00, 12, 00, 15, 16, 17, 18, 00, 00};
        byte[] newData = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        
        System.out.println(Helper.bytesToHex(oldData, true));
        System.out.println(Helper.bytesToHex(newData, true));
        
        List<Block> diff = getDiff(oldData, newData);
        
        for (Block block : diff) {
            System.out.println(block);
        }
        
        
        List<DataBlock> diffData = getDiffData(oldData, newData);
        
        for (DataBlock block : diffData) {
            System.out.println(block);
        }
    }
    
}
