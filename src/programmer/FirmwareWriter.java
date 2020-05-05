/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package programmer;

import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.ProgressMonitorInputStream;
import jssc.SerialPort;
import modem.XModem1K;

/**
 *
 * @author User
 */
public class FirmwareWriter {
    SerialPort serialPort;
    SerialInputStream serialInputStream;
    SerialOutputStream serialOutputStream;
    XModem1K xModem1K;
//    String resourcefile;
//    Path pathToFile;
    
    public FirmwareWriter (SerialPort port) {
        serialPort = port;
        serialInputStream  = new SerialInputStream (port);        
        serialOutputStream = new SerialOutputStream (port);
        xModem1K = new XModem1K (serialInputStream, serialOutputStream);
    }   
    
    public void write (String resourceFile) {
        try {
            
            xModem1K.send(resourceFile);            
        } catch (IOException | InterruptedException ex) {
            //Logger.getLogger(FirmwareWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void writeFile (Path pathToFile) {
        try {
            xModem1K.sendFile(pathToFile);            
        } catch (IOException | InterruptedException ex) {
            //Logger.getLogger(FirmwareWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
}
