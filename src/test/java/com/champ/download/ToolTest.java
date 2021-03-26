package com.champ.download;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author : championjing
 * @version V1.0
 * @Description:
 * @date Date : 2021年03月17日 16:04
 */
public class ToolTest {

    @Test
    void testSplit(){
//        String
    }

    @Test
    void testRandomAccessFile() throws FileNotFoundException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile("D:\\迅雷下载\\2.mkv", "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        byte[] bs = new byte[1024*1024];
        FileOutputStream fos = new FileOutputStream("D:\\迅雷下载\\2.bak.mkv");
        try {
            while ( raf.read( bs ) != -1 ) {
                fos.write( bs );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
