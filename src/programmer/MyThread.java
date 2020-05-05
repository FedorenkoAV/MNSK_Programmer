/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package programmer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import modem.GUIModem;

/**
 *
 * @author Федоренко Александр
 */
public class MyThread implements Runnable {

    GUIModem guiModem;
    InputStream inputStream;
    OutputStream outputStream;
    
    public MyThread (InputStream inpStr, OutputStream outStr) {
        inputStream = inpStr;
        outputStream = outStr;        
    }

    // Entry point of thread.
    public void run() {
        GUIModem guiMod = new GUIModem(inputStream, outputStream);
        guiMod.setVisible(true);
        setGUIModem (guiMod);
    }
    
    public GUIModem getGUIModem () {
        return guiModem;
    }
    
    public void setGUIModem (GUIModem guiMod) {
        guiModem = guiMod;
    }
    /**
     * Send a file. <br/>
     *
     * This method support correct thread interruption, when thread is interrupted "cancel of transmission" will be send.
     * So you can move long transmission to other thread and interrupt it according to your algorithm.
     *
     * @param resourceFile
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    public void send(String resourceFile) throws IOException, InterruptedException {
        guiModem.send(resourceFile, true);        
    }
    
    /**
     * Send a file. <br/>
     *
     * This method support correct thread interruption, when thread is interrupted "cancel of transmission" will be send.
     * So you can move long transmission to other thread and interrupt it according to your algorithm.
     *
     * @param String
     * @throws java.io.IOException
     */
    public void sendFile(Path pathFile) throws IOException, InterruptedException {
        guiModem.send(pathFile, true);
    }

    /**
     * Receive file <br/>
     *
     * This method support correct thread interruption, when thread is interrupted "cancel of transmission" will be send.
     * So you can move long transmission to other thread and interrupt it according to your algorithm.
     *
     * @param file file path for storing
     * @throws java.io.IOException
     */
    public void receive(Path file) throws IOException {
        guiModem.receive(file, false);
    }
}
