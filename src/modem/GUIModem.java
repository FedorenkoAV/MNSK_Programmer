/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modem;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 * This is core Modem class supporting XModem (and some extensions XModem-1K,
 * XModem-CRC), and YModem.<br/>
 * YModem support is limited (currently block 0 is ignored).<br/>
 * <br/>
 * Created by Anton Sirotinkin (aesirot@mail.ru), Moscow 2014 <br/>
 * I hope you will find this program useful.<br/>
 * You are free to use/modify the code for any purpose, but please leave a
 * reference to me.<br/>
 */
public class GUIModem extends javax.swing.JFrame {

    /* Protocol characters used */
    protected static final byte SOH = 0x01;
    /* Start Of Header */
    protected static final byte STX = 0x02;
    /* Start Of Text (used like SOH but means 1024 block size) */
    protected static final byte EOT = 0x04;
    /* End Of Transmission */
    protected static final byte ACK = 0x06;
    /* ACKnowlege */
    protected static final byte NAK = 0x15;
    /* Negative AcKnowlege */
    protected static final byte CAN = 0x18;
    /* CANcel character */

    protected static final byte CPMEOF = 0x1A;
    protected static final byte ST_C = 'C';

    protected static final int MAXERRORS = 10;

    protected static final int BLOCK_TIMEOUT = 1000;
    protected static final int REQUEST_TIMEOUT = 3000;
    protected static final int WAIT_FOR_RECEIVER_TIMEOUT = 60_000;
    protected static final int SEND_BLOCK_TIMEOUT = 10_000;

    private final InputStream inputStream;
    private final OutputStream outputStream;

    private final byte[] shortBlockBuffer;
    private final byte[] longBlockBuffer;

    private int numberOfBlocks;

    /**
     * Constructor
     *
     * @param inputStream stream for reading received data from other side
     * @param outputStream stream for writing data to other side
     * @param parent
     */
    public GUIModem(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        shortBlockBuffer = new byte[128];
        longBlockBuffer = new byte[1024];
        //JProgressBar progressBar = null;
        initComponents();
        this.setVisible(true);
    }

    /**
     * Wait for receiver request for transmission
     *
     * @param timer
     * @return TRUE if receiver requested CRC-16 checksum, FALSE if 8bit
     * checksum
     * @throws java.io.IOException
     */
    protected boolean waitReceiverRequest(Timer timer) throws IOException {
        int character;
        //System.out.println("Ожидаю запрос приемника на передачу");
        while (true) {
            try {
                character = readByte(timer);
                //System.out.print(character);
                if (character == NAK) {
                    //System.out.println("Получен байт NAK");
                    return false;
                }
                if (character == ST_C) {
                    //System.out.println("Получен байт ST_C");
                    return true;
                }
                //System.out.println("Ничего не получено");
            } catch (TimeoutException e) {
                throw new IOException("Timeout waiting for receiver");
            }
        }
    }

    /**
     * Send a file. <br/>
     * <p>
     * This method support correct thread interruption, when thread is
     * interrupted "cancel of transmission" will be send. So you can move long
     * transmission to other thread and interrupt it according to your
     * algorithm.
     *
     * @param pathToResourceFile
     * @param file
     * @param useBlock1K
     * @throws java.io.IOException
     */
    public void send(String pathToResourceFile, boolean useBlock1K) throws IOException {
        //open file  
//        numberOfBlocks = this.getClass().getResourceAsStream(pathToResourceFile).available();
//        if ((pathToResourceFile.length() % 1024) != 0) {
//            numberOfBlocks ++;
//        }
//        System.out.println("Будет записано " + numberOfBlocks + " блоков");
        try (DataInputStream dataStream = new DataInputStream(this.getClass().getResourceAsStream(pathToResourceFile))) {
//        try (DataInputStream dataStream = new DataInputStream(Files.newInputStream(file))) {
            Timer timer = new Timer(WAIT_FOR_RECEIVER_TIMEOUT).start();

            boolean useCRC16 = waitReceiverRequest(timer);
            CRC crc;
            if (useCRC16) {
                crc = new CRC16();
            } else {
                crc = new CRC8();
            }

            byte[] block;
            if (useBlock1K) {
                block = new byte[1024];
            } else {
                block = new byte[128];
            }

            sendDataBlocks(dataStream, 1, crc, block);

            sendEOT();
        }
    }

    public void send(Path file, boolean useBlock1K) throws IOException {
        //open file       
//        try (DataInputStream dataStream = new DataInputStream(this.getClass().getResourceAsStream(pathToResourceFile))) {
        try (DataInputStream dataStream = new DataInputStream(Files.newInputStream(file))) {
            Timer timer = new Timer(WAIT_FOR_RECEIVER_TIMEOUT).start();

            boolean useCRC16 = waitReceiverRequest(timer);
            CRC crc;
            if (useCRC16) {
                crc = new CRC16();
            } else {
                crc = new CRC8();
            }

            byte[] block;
            if (useBlock1K) {
                block = new byte[1024];
            } else {
                block = new byte[128];
            }
            sendDataBlocks(dataStream, 1, crc, block);

            sendEOT();
        }
    }

    protected void sendDataBlocks(DataInputStream dataStream, int blockNumber, CRC crc, byte[] block) throws IOException {
        //System.out.println("Отправляю блок №" + blockNumber);
        int dataLength;
        int progressBarValue = 0;
        numberOfBlocks = dataStream.available() / (block.length);
        if ((dataStream.available() % (block.length)) != 0) {
            numberOfBlocks++;
        }
        System.out.println("Будет записано " + numberOfBlocks + " блоков");

        while ((dataLength = dataStream.read(block)) != -1) {
            progressBarValue = blockNumber * 100 / numberOfBlocks;
            System.out.println("Записываю ProgressBar Value = " + progressBarValue);

            sendBlock(blockNumber++, block, dataLength, crc);
            System.out.println("blockNumber = " + blockNumber);
            jProgressBar1.setValue(progressBarValue);
            System.out.println("Считываю ProgressBar Value = " + jProgressBar1.getValue());
        }
//        System.out.println("Реально записано " + (blockNumber-1) + " блоков");
    }

    protected void sendEOT() throws IOException {
        //System.out.println("Отправляю блок: EOT");
        int errorCount = 0;
        Timer timer = new Timer(BLOCK_TIMEOUT);
        int character;
        while (errorCount < 10) {
            sendByte(EOT);
            try {
                character = readByte(timer.start());

                if (character == ACK) {
                    return;
                } else if (character == CAN) {
                    throw new IOException("Transmission terminated");
                }
            } catch (TimeoutException ignored) {
            }
            errorCount++;
        }
    }

    protected void sendBlock(int blockNumber, byte[] block, int dataLength, CRC crc) throws IOException {
        int errorCount;
        int character;
        Timer timer = new Timer(SEND_BLOCK_TIMEOUT);

        if (dataLength < block.length) {
            block[dataLength] = CPMEOF;
        }
        errorCount = 0;

        while (errorCount < MAXERRORS) {
            timer.start();

            if (block.length == 1024) {
                System.currentTimeMillis();
                //System.out.println(System.currentTimeMillis() + " :Отправляю STX");
                outputStream.write(STX);
            } else {//128
                //System.out.println(System.currentTimeMillis() + " :Отправляю SOH");
                outputStream.write(SOH);
            }
            System.out.println("Отправляю номер блока: " + blockNumber);
            outputStream.write(blockNumber);
            //System.out.println("Отправляю инвертированный номер блока: " + (~blockNumber));
            outputStream.write(~blockNumber);

            //System.out.println("Отправляю сам блок");
            outputStream.write(block);
            //System.out.println("Отправляю CRC: " + crc);
            writeCRC(block, crc);

            outputStream.flush();

            //System.out.println("Блок передан, ожидаю подтверждения");
            while (true) {
                try {
                    character = readByte(timer);
                    if (character == ACK) {
                        //System.out.println(System.currentTimeMillis() + " :Получено ACK");
                        return;
                    } else if (character == NAK) {
                        //System.out.println(System.currentTimeMillis() + " :Получено NAK");
                        errorCount++;
                        //System.out.println("Число ошибок: " + errorCount);
                        break;
                    } else if (character == CAN) {
                        //System.out.println("Передача прервана");
                        throw new IOException("Transmission terminated");
                    }
                } catch (TimeoutException e) {
                    errorCount++;
                    break;
                }
            }

        }

        throw new IOException("Too many errors caught, abandoning transfer");
    }

    private void writeCRC(byte[] block, CRC crc) throws IOException {
        byte[] crcBytes = new byte[crc.getCRCLength()];
        long crcValue = crc.calcCRC(block);
        for (int i = 0; i < crc.getCRCLength(); i++) {
            crcBytes[crc.getCRCLength() - i - 1] = (byte) ((crcValue >> (8 * i)) & 0xFF);
        }
        outputStream.write(crcBytes);
    }

    /**
     * Receive file <br/>
     * <p>
     * This method support correct thread interruption, when thread is
     * interrupted "cancel of transmission" will be send. So you can move long
     * transmission to other thread and interrupt it according to your
     * algorithm.
     *
     * @param file file path for storing
     * @throws java.io.IOException
     */
    public void receive(Path file, boolean useCRC16) throws IOException {
        try (DataOutputStream dataOutput = new DataOutputStream(Files.newOutputStream(file))) {
            int available;
            // clean input stream
            if ((available = inputStream.available()) > 0) {
                inputStream.skip(available);
            }

            int character = requestTransmissionStart(useCRC16);

            CRC crc;
            if (useCRC16) {
                crc = new CRC16();
            } else {
                crc = new CRC8();
            }

            processDataBlocks(crc, 1, character, dataOutput);
        }
    }

    protected void processDataBlocks(CRC crc, int blockNumber, int blockInitialCharacter, DataOutputStream dataOutput) throws IOException {
        // read blocks until EOT
        boolean result = false;
        boolean shortBlock;
        byte[] block;
        while (true) {
            int errorCount = 0;
            if (blockInitialCharacter == EOT) {
                // end of transmission
                sendByte(ACK);
                return;
            }

            //read and process block
            shortBlock = (blockInitialCharacter == SOH);
            try {
                block = readBlock(blockNumber, shortBlock, crc);
                dataOutput.write(block);
                blockNumber++;
                errorCount = 0;
                result = true;
                sendByte(ACK);
            } catch (TimeoutException | InvalidBlockException e) {
                errorCount++;
                if (errorCount == MAXERRORS) {
                    interruptTransmission();
                    throw new IOException("Transmission aborted, error count exceeded max");
                }
                sendByte(NAK);
                result = false;
            } catch (RepeatedBlockException e) {
                //thats ok, accept and wait for next block
                sendByte(ACK);
            } catch (SynchronizationLostException e) {
                //fatal transmission error
                interruptTransmission();
                throw new IOException("Fatal transmission error", e);
            }

            //wait for next block
            blockInitialCharacter = readNextBlockStart(result);
        }
    }

    protected void sendByte(byte b) throws IOException {
        outputStream.write(b);
        outputStream.flush();
    }

    /**
     * Request transmission start and return first byte of "first" block from
     * sender (block 1 for XModem, block 0 for YModem)
     *
     * @param useCRC16
     * @return
     * @throws java.io.IOException
     */
    protected int requestTransmissionStart(boolean useCRC16) throws IOException {
        int character;
        int errorCount = 0;
        byte requestStartByte;
        if (!useCRC16) {
            requestStartByte = NAK;
        } else {
            requestStartByte = ST_C;
        }

        // wait for first block start
        Timer timer = new Timer(REQUEST_TIMEOUT);
        while (errorCount < MAXERRORS) {
            // request transmission start (will be repeated after 10 second timeout for 10 times)
            sendByte(requestStartByte);
            timer.start();
            try {
                while (true) {
                    character = readByte(timer);

                    if (character == SOH || character == STX) {
                        return character;
                    }
                }
            } catch (TimeoutException ignored) {
                errorCount++;
            }
        }
        interruptTransmission();
        throw new RuntimeException("Timeout, no data received from transmitter");
    }

    protected int readNextBlockStart(boolean lastBlockResult) throws IOException {
        int character;
        int errorCount = 0;
        Timer timer = new Timer(BLOCK_TIMEOUT);
        while (true) {
            timer.start();
            try {
                while (true) {
                    character = readByte(timer);

                    if (character == SOH || character == STX || character == EOT) {
                        return character;
                    }
                }
            } catch (TimeoutException ignored) {
                // repeat last block result and wait for next block one more time
                if (++errorCount < MAXERRORS) {
                    sendByte(lastBlockResult ? ACK : NAK);
                } else {
                    interruptTransmission();
                    throw new RuntimeException("Timeout, no data received from transmitter");
                }
            }
        }
    }

    private void shortSleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            try {
                interruptTransmission();
            } catch (IOException ignore) {
            }
            throw new RuntimeException("Transmission was interrupted", e);
        }
    }

    /**
     * send CAN to interrupt seance
     *
     * @throws java.io.IOException
     */
    protected void interruptTransmission() throws IOException {
        sendByte(CAN);
        sendByte(CAN);
    }

    protected byte[] readBlock(int blockNumber, boolean shortBlock, CRC crc) throws IOException, TimeoutException, RepeatedBlockException, SynchronizationLostException, InvalidBlockException {
        byte[] block;
        Timer timer = new Timer(BLOCK_TIMEOUT).start();

        if (shortBlock) {
            block = shortBlockBuffer;
        } else {
            block = longBlockBuffer;
        }
        byte character;

        character = readByte(timer);

        if (character == blockNumber - 1) {
            // this is repeating of last block, possible ACK lost
            throw new RepeatedBlockException();
        }
        if (character != blockNumber) {
            // wrong block - fatal loss of synchronization
            throw new SynchronizationLostException();
        }

        character = readByte(timer);

        if (character != ~blockNumber) {
            throw new InvalidBlockException();
        }

        // data
        for (int i = 0; i < block.length; i++) {
            block[i] = readByte(timer);
        }

        while (true) {
            if (inputStream.available() >= crc.getCRCLength()) {
                if (crc.calcCRC(block) != readCRC(crc)) {
                    throw new InvalidBlockException();
                }
                break;
            }

            shortSleep();

            if (timer.isExpired()) {
                throw new TimeoutException();
            }
        }

        return block;
    }

    private long readCRC(CRC crc) throws IOException {
        long checkSumma = 0;
        for (int j = 0; j < crc.getCRCLength(); j++) {
            checkSumma = (checkSumma << 8) + inputStream.read();
        }
        return checkSumma;
    }

    private byte readByte(Timer timer) throws IOException, TimeoutException {
        while (true) {
            if (inputStream.available() > 0) {
                int b = inputStream.read();
                return (byte) b;
            }
            if (timer.isExpired()) {
                throw new TimeoutException();
            }
            shortSleep();
        }
    }

    class RepeatedBlockException extends Exception {
    }

    class SynchronizationLostException extends Exception {
    }

    class InvalidBlockException extends Exception {
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("jLabel1");

        jProgressBar1.setToolTipText("");
        jProgressBar1.setStringPainted(true);

        jButton1.setText("jButton1");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("jButton2");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 234, Short.MAX_VALUE)
                        .addComponent(jButton2)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                try {
                    send("/programmer/firmwares/8 52.dwl", true);
                } catch (IOException ex) {
                    Logger.getLogger(GUIModem.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }

        };
        worker.execute();


    }//GEN-LAST:event_jButton1ActionPerformed

//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String args[]) {
//        /* Set the Nimbus look and feel */
//        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
//        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
//         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
//         */
//        try {
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//        } catch (ClassNotFoundException ex) {
//            java.util.logging.Logger.getLogger(GUIModem.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(GUIModem.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(GUIModem.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(GUIModem.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /* Create and display the form */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                new GUIModem().setVisible(true);
//            }
//        });
//    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JProgressBar jProgressBar1;
    // End of variables declaration//GEN-END:variables
}
