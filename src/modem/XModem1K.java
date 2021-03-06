package modem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * XModem with 1K block<br/>
 *
 * Created by Anton Sirotinkin (aesirot@mail.ru), Moscow 2014 <br/>
 * I hope you will find this program useful.<br/>
 * You are free to use/modify the code for any purpose, but please leave a reference to me.<br/>
 */
public class XModem1K {
    private Modem modem;
    
    /**
     * Constructor
     *
     * @param inputStream  stream for reading received data from other side
     * @param outputStream stream for writing data to other side
     */
    public XModem1K(InputStream inputStream, OutputStream outputStream) {                    
        this.modem = new Modem(inputStream, outputStream);
//        modem.pack();
//        modem.setVisible(true);
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
    public void send(String resourceFile) throws IOException, InterruptedException {
        modem.send(resourceFile, true);        
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
        modem.send(pathFile, true);
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
        modem.receive(file, false);
    }

}
