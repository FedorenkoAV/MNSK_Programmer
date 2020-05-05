package modem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import programmer.MyThread;

/**
 * XModem with 1K block<br/>
 *
 * Created by Anton Sirotinkin (aesirot@mail.ru), Moscow 2014 <br/>
 * I hope you will find this program useful.<br/>
 * You are free to use/modify the code for any purpose, but please leave a reference to me.<br/>
 */
public class GUIXModem1K {
    //private Modem modem;
    GUIModem guiModem;

    /**
     * Constructor
     *
     * @param inputStream  stream for reading received data from other side
     * @param outputStream stream for writing data to other side
     */
    public GUIXModem1K(InputStream inputStream, OutputStream outputStream) {
        
                /* Create and display the form */
        MyThread mt = new MyThread (inputStream, outputStream);
        java.awt.EventQueue.invokeLater(mt);
        guiModem = mt.getGUIModem();
        
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                guiModem = new GUIModem(inputStream, outputStream);
//                guiModem.setVisible(true);
//            }
//        });
    
//        this.modem = new Modem(inputStream, outputStream);
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
