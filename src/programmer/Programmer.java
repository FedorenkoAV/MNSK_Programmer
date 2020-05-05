/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package programmer;

import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;
import modem.GUIModem;

;

/**
 *
 * @author User
 */
public class Programmer extends javax.swing.JFrame {

    public final int NOP = 0;
    public final int TSET = 1;
    public final int TSERVER = 2;
    public final int TID = 3;

    SerialPort serialPort;
    String portName;
    int baudRate;
    int dataBits;
    int stopBits;
    int parity;
    Path pathFile = null;
    String answerString;
    int command = NOP;

    /**
     * Creates new form MainJFrame
     */
    public Programmer() {
        initComponents();
        String[] ports = SerialPortList.getPortNames();
        for (String port : ports) {
            jComboBoxPortName.addItem(port);
        }

    }

    void sendCtrlZ() {
        try {
            byte b = 26;
            serialPort.writeByte(b);
        } catch (SerialPortException ex) {
            //DialogMessage dialogMessage = new DialogMessage(this, DialogMessage.TYPE_ERROR, "Writing data", "Error occurred while writing data.");
        }
        waitSomeTime(1);
    }

    void openFile() {
        int returnVal = jFileChooserOpen.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) { //Если файл был выбран, то
            File file = jFileChooserOpen.getSelectedFile(); //Берем выбраный файл (файл еще не открыт)
            pathFile = file.toPath();

            jLabelFirmware.setText(pathFile.toString());
//            try {
//              // What to do with the file, e.g. display it in a TextArea
//              jTextAreaIn.read( new FileReader( file.getAbsolutePath() ), null );              
//            } catch (IOException ex) {
//              System.out.println("problem accessing file"+file.getAbsolutePath());
//            }
        } else {
            if (returnVal == JFileChooser.CANCEL_OPTION) {
                System.out.println("Выбор файла отменен");
            } else {
                System.out.println("В процессы выбора файла произошла ошибка.");
            }
        }
    }

    void writeOneFile(Path pathToFile) {
        String str = "AT+WDWL\r\n";
        sendString(str);
        try {
            serialPort.removeEventListener();
        } catch (SerialPortException ex) {
            //Logger.getLogger(Programmer.class.getName()).log(Level.SEVERE, null, ex);
        }
        FirmwareWriter fw = new FirmwareWriter(serialPort);
        System.out.println("Начинаю передачу файла " + pathToFile.getFileName() + " Ждите.");
        fw.writeFile(pathToFile);
        System.out.println("Передача файла завершена удачно. Ура!");
        try {
            enableEventListener();
        } catch (SerialPortException ex) {
            Logger.getLogger(Programmer.class.getName()).log(Level.SEVERE, null, ex);
        }
        sendString("AT+CFUN=1\r\n");
        waitSomeTime(10);
    }

    void writeOneFirmwareFile(String fileName) {
        String str = "AT+WDWL\r\n";
        sendString(str);
        try {
            serialPort.removeEventListener();
        } catch (SerialPortException ex) {
            //Logger.getLogger(Programmer.class.getName()).log(Level.SEVERE, null, ex);
        }
        FirmwareWriter fw = new FirmwareWriter(serialPort);
        System.out.println("Начинаю передачу файла " + fileName.substring(1 + fileName.lastIndexOf("/")) + " Ждите.");
        fw.write(fileName);
        System.out.println("Передача файла завершена удачно. Ура!");
        try {
            enableEventListener();
        } catch (SerialPortException ex) {
            Logger.getLogger(Programmer.class.getName()).log(Level.SEVERE, null, ex);
        }
        sendString("AT+CFUN=1\r\n");
        waitSomeTime(10);
    }

    void waitSomeTime(int waitTime) {
        System.out.println("Жду " + waitTime + " секунд(ы)");
        try {
            Thread.sleep(waitTime * 1000);
        } catch (InterruptedException ex) {
            //Logger.getLogger(Programmer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void enableEventListener() throws SerialPortException {
        /*
        Добавляем прослушиватель событий.
        В качестве параметров в методе задаются:
            1) объект типа "SerialPortEventListener" 
        Этот объект должен быть должным образом описан, как он 
        будет отвечать за обработку произошедших событий.
            2) маска событий. чтобы сделать ее нужно 
        использовать переменные с префиксом "MASK_" например 
        "MASK_RXCHAR"
         */
        serialPort.addEventListener(new Reader(), SerialPort.MASK_RXCHAR
                | SerialPort.MASK_RXFLAG
                | SerialPort.MASK_CTS
                | SerialPort.MASK_DSR
                | SerialPort.MASK_RLSD);
    }

    void setPortSettings() {
        portName = jComboBoxPortName.getSelectedItem().toString();
        baudRate = SerialPort.BAUDRATE_115200;
        dataBits = SerialPort.DATABITS_8;
        stopBits = SerialPort.STOPBITS_1;
        parity = SerialPort.PARITY_NONE;
        serialPort = new SerialPort(portName); // в переменную serialPort заносим выбраный COM-порт

        try {
            if (serialPort.openPort()) { // Пытаемся открыть порт, если он открывается, то
                System.out.println("Порт " + portName + " открыт");
                if (serialPort.setParams(baudRate, dataBits, stopBits, parity)) { // пытаемся установить параметры порта, если они устанавливаются, то 
                    System.out.println("Параметры порта установлены");
                    enableEventListener();
                    //jButtonOpenPort.setText("Close port"); // меняем надпись на кнопке на "Close port"
                    /*
                        Добавляем прослушиватель событий.
                        В качестве параметров в методе задаются:
                            1) объект типа "SerialPortEventListener" 
                        Этот объект должен быть должным образом описан, как он 
                        будет отвечать за обработку произошедших событий.
                            2) маска событий. чтобы сделать ее нужно 
                        использовать переменные с префиксом "MASK_" например 
                        "MASK_RXCHAR"
                     */
//                        serialPort.addEventListener(new Reader(), SerialPort.MASK_RXCHAR |
//                                                                  SerialPort.MASK_RXFLAG |
//                                                                  SerialPort.MASK_CTS |
//                                                                  SerialPort.MASK_DSR |
//                                                                  SerialPort.MASK_RLSD);

                    //enableControls(true);
//                        if(serialPort.isCTS()){
//                            jLabelCTS.setBorder(NimbusGui.borderStatusOn);
//                            jLabelCTS.setBackground(NimbusGui.colorStatusOnBG);
//                        }
//                        else {
//                            jLabelCTS.setBorder(NimbusGui.borderStatusOff);
//                            jLabelCTS.setBackground(NimbusGui.colorStatusOffBG);
//                        }
//                        if(serialPort.isDSR()){
//                            jLabelDSR.setBorder(NimbusGui.borderStatusOn);
//                            jLabelDSR.setBackground(NimbusGui.colorStatusOnBG);
//                        }
//                        else {
//                            jLabelDSR.setBorder(NimbusGui.borderStatusOff);
//                            jLabelDSR.setBackground(NimbusGui.colorStatusOffBG);
//                        }
//                        if(serialPort.isRLSD()){
//                            jLabelRLSD.setBorder(NimbusGui.borderStatusOn);
//                            jLabelRLSD.setBackground(NimbusGui.colorStatusOnBG);
//                        }
//                        else {
//                            jLabelRLSD.setBorder(NimbusGui.borderStatusOff);
//                            jLabelRLSD.setBackground(NimbusGui.colorStatusOffBG);
//                        }
//                        if(serialPort.setRTS(true)){
//                            jToggleButtonRTS.setSelected(true);
//                        }
//                        if(serialPort.setDTR(true)){
//                            jToggleButtonDTR.setSelected(true);
//                        }
                } else {
                    //DialogMessage dialogMessage = new DialogMessage(this, DialogMessage.TYPE_ERROR, "Setting parameters", "Can't set selected parameters.");
                    serialPort.closePort();
                    System.out.println("Порт " + portName + " закрыт");
                }
            }
        } catch (SerialPortException ex) {
            //DialogMessage dialogMessage = new DialogMessage(this, DialogMessage.TYPE_ERROR, ex.getPortName(), ex.getExceptionType());
            //Do nothing
        }
    }

    private void sendString(String str) {
        //String str = jTextFieldOut.getText();

        System.out.print("Отправлена команда: \n" + str);
        if (str.length() > 0) {
            try {
                serialPort.writeBytes(str.getBytes());
                str = "";
            } catch (Exception ex) {
                //DialogMessage dialogMessage = new DialogMessage(this, DialogMessage.TYPE_ERROR, "Writing data", "Error occurred while writing data.");
            }
        }
    }

    void closePort() {
        try {
            serialPort.closePort();
        } catch (Exception ex) {
            //Do nothing
        }
        System.out.println("Порт " + portName + " закрыт");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooserOpen = new javax.swing.JFileChooser();
        jFileChooserSave = new javax.swing.JFileChooser();
        jDialog1 = new javax.swing.JDialog();
        jPanel1 = new javax.swing.JPanel();
        jLabelPortName = new javax.swing.JLabel();
        jComboBoxPortName = new javax.swing.JComboBox<>();
        jButtonReadTSET = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldPIN = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldAPN = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldLogin = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldPassword = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jButtonSendTSet = new javax.swing.JButton();
        jButtonReadTSERVER = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        jTextFieldServerIP = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jTextFieldPort = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jButtonSendTSERVER = new javax.swing.JButton();
        jButtonReadTID = new javax.swing.JButton();
        jLabel19 = new javax.swing.JLabel();
        jTextFieldTID = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        jButtonSendTID = new javax.swing.JButton();
        jCheckBoxTSET = new javax.swing.JCheckBox();
        jCheckBoxTSERVER = new javax.swing.JCheckBox();
        jCheckBoxTID = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaIn = new javax.swing.JTextArea();
        jButtonGroupRead = new javax.swing.JButton();
        jButtonGroupSend = new javax.swing.JButton();
        jButtonWriteFirmware = new javax.swing.JButton();
        jLabelFirmware = new javax.swing.JLabel();
        jButtonFile = new javax.swing.JButton();
        jButtonCtrlZ = new javax.swing.JButton();
        jButtonCGMR = new javax.swing.JButton();
        jProgressBar1 = new javax.swing.JProgressBar();
        jButton1 = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItemOpen = new javax.swing.JMenuItem();
        jMenuItemSave = new javax.swing.JMenuItem();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();

        jFileChooserOpen.setDialogTitle("Открыть файл");

        jFileChooserSave.setApproveButtonToolTipText("");
        jFileChooserSave.setDialogTitle("Сохранить файл");

        jDialog1.setTitle("Перезаписать существующий файл?");

        javax.swing.GroupLayout jDialog1Layout = new javax.swing.GroupLayout(jDialog1.getContentPane());
        jDialog1.getContentPane().setLayout(jDialog1Layout);
        jDialog1Layout.setHorizontalGroup(
            jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jDialog1Layout.setVerticalGroup(
            jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("-=FM=- Programmer");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jLabelPortName.setText("COM порт:");

        jComboBoxPortName.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "" }));
        jComboBoxPortName.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBoxPortNameItemStateChanged(evt);
            }
        });
        jComboBoxPortName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxPortNameActionPerformed(evt);
            }
        });

        jButtonReadTSET.setText("Считать");
        jButtonReadTSET.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadTSETActionPerformed(evt);
            }
        });

        jLabel1.setText("AT+TSET=\"");

        jTextFieldPIN.setText("0000");

        jLabel2.setText("PIN:");

        jLabel3.setText("\",\"");

        jTextFieldAPN.setText("internet.mts.ru");

        jLabel4.setText("\",\"");

        jTextFieldLogin.setText("mts");

        jLabel5.setText("\",\"");

        jTextFieldPassword.setText("mts");

        jLabel6.setText("\"");

        jLabel7.setText("APN:");

        jLabel8.setText("Login:");

        jLabel9.setText("Password:");

        jButtonSendTSet.setText("Записать");
        jButtonSendTSet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSendTSetActionPerformed(evt);
            }
        });

        jButtonReadTSERVER.setText("Считать");
        jButtonReadTSERVER.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadTSERVERActionPerformed(evt);
            }
        });

        jLabel10.setText("AT+TSERVER=\"");

        jTextFieldServerIP.setText("195.210.184.11");

        jLabel11.setText("Server IP:");

        jLabel12.setText("\",\"");

        jTextFieldPort.setText("7676");

        jLabel15.setText("\"");

        jLabel16.setText("Port:");

        jButtonSendTSERVER.setText("Записать");
        jButtonSendTSERVER.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSendTSERVERActionPerformed(evt);
            }
        });

        jButtonReadTID.setText("Считать");
        jButtonReadTID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadTIDActionPerformed(evt);
            }
        });

        jLabel19.setText("AT+TID=1,");

        jTextFieldTID.setText("1");

        jLabel20.setText("Radio ID:");

        jButtonSendTID.setText("Записать");
        jButtonSendTID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSendTIDActionPerformed(evt);
            }
        });

        jTextAreaIn.setColumns(20);
        jTextAreaIn.setFont(new java.awt.Font("Monospaced", 0, 14)); // NOI18N
        jTextAreaIn.setRows(5);
        jScrollPane1.setViewportView(jTextAreaIn);

        jButtonGroupRead.setText("Считать группу");
        jButtonGroupRead.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGroupReadActionPerformed(evt);
            }
        });

        jButtonGroupSend.setText("Записать группу");
        jButtonGroupSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGroupSendActionPerformed(evt);
            }
        });

        jButtonWriteFirmware.setText("Записать прошивку");
        jButtonWriteFirmware.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonWriteFirmwareActionPerformed(evt);
            }
        });

        jLabelFirmware.setText("Файл не выбран");

        jButtonFile.setText("Прошить файл");
        jButtonFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonFileActionPerformed(evt);
            }
        });

        jButtonCtrlZ.setText("Отправить Ctrl-Z");
        jButtonCtrlZ.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCtrlZActionPerformed(evt);
            }
        });

        jButtonCGMR.setText("AT+CGMR");
        jButtonCGMR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCGMRActionPerformed(evt);
            }
        });

        jProgressBar1.setStringPainted(true);

        jButton1.setText("jButton1");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelPortName)
                            .addComponent(jComboBoxPortName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jCheckBoxTSET)
                                    .addComponent(jCheckBoxTSERVER)
                                    .addComponent(jCheckBoxTID))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jButtonReadTSERVER)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel10)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jTextFieldServerIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel12))
                                            .addComponent(jLabel11))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel15)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButtonSendTSERVER))
                                            .addComponent(jLabel16)))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jButtonReadTID)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel19)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(jLabel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(jTextFieldTID, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButtonSendTID))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jButtonReadTSET)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel1)
                                        .addGap(12, 12, 12)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel2)
                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jTextFieldPIN, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel3)))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jTextFieldAPN, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel4))
                                            .addComponent(jLabel7))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jTextFieldLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel5))
                                            .addComponent(jLabel8))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel9)
                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jTextFieldPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel6)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButtonSendTSet))))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jButtonGroupRead)
                                        .addGap(18, 18, 18)
                                        .addComponent(jButtonGroupSend)
                                        .addGap(18, 18, 18)
                                        .addComponent(jButtonCtrlZ)
                                        .addGap(18, 18, 18)
                                        .addComponent(jButtonCGMR))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jButtonWriteFirmware)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabelFirmware)
                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jButtonFile)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton1)))))))
                        .addGap(0, 35, Short.MAX_VALUE))
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelPortName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBoxPortName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(29, 29, 29)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jCheckBoxTSET)
                                .addGap(49, 49, 49))
                            .addComponent(jCheckBoxTSERVER))
                        .addGap(28, 28, 28)
                        .addComponent(jCheckBoxTID))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel1)
                                    .addComponent(jTextFieldPIN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel3)))
                            .addComponent(jButtonReadTSET)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jTextFieldAPN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel4))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel11)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel10)
                                    .addComponent(jTextFieldServerIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel12)))
                            .addComponent(jButtonReadTSERVER)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel16)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel15)
                                    .addComponent(jButtonSendTSERVER))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel20)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel19)
                                    .addComponent(jTextFieldTID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jButtonSendTID)))
                            .addComponent(jButtonReadTID)))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(jButtonSendTSet))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel8)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextFieldLogin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel5)))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel9)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jTextFieldPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonGroupRead)
                    .addComponent(jButtonGroupSend)
                    .addComponent(jButtonCtrlZ)
                    .addComponent(jButtonCGMR))
                .addGap(18, 18, 18)
                .addComponent(jLabelFirmware)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonWriteFirmware)
                    .addComponent(jButtonFile)
                    .addComponent(jButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(41, Short.MAX_VALUE))
        );

        jMenu1.setText("Файл");

        jMenuItemOpen.setText("Открыть");
        jMenuItemOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemOpen);

        jMenuItemSave.setText("Сохранить");
        jMenuItemSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemSave);

        jMenuItemExit.setText("Выход");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemExit);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Настройки");
        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
        // TODO add your handling code here:
        closePort();
        System.exit(0);
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        closePort();
    }//GEN-LAST:event_formWindowClosing

    private void jMenuItemOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOpenActionPerformed
        // TODO add your handling code here:        
        int returnVal = jFileChooserOpen.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) { //Если файл был выбран, то
            File file = jFileChooserOpen.getSelectedFile(); //Берем выбраный файл (файл еще не открыт)
            pathFile = file.toPath();

            jLabelFirmware.setText(pathFile.toString());
//            try {
//              // What to do with the file, e.g. display it in a TextArea
//              jTextAreaIn.read( new FileReader( file.getAbsolutePath() ), null );              
//            } catch (IOException ex) {
//              System.out.println("problem accessing file"+file.getAbsolutePath());
//            }
        } else {
            if (returnVal == JFileChooser.CANCEL_OPTION) {
                System.out.println("Выбор файла отменен");
            } else {
                System.out.println("В процессы выбора файла произошла ошибка.");
            }
        }
    }//GEN-LAST:event_jMenuItemOpenActionPerformed

    private void jMenuItemSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveActionPerformed
        // TODO add your handling code here:        
        int returnVal = jFileChooserSave.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) { //Если файл был выбран, то
            File file = jFileChooserSave.getSelectedFile(); //Берем выбраный файл (файл еще не открыт)            
            if (!file.exists()) {
                String mes = "Файл " + file.getName() + " не существует, хотите его создать?";
                int ans = JOptionPane.showConfirmDialog(this, mes, "Создание файла", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (ans == JOptionPane.OK_OPTION) {
                    try {
                        file.createNewFile();
                    } catch (IOException ex) {
                        Logger.getLogger(Programmer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {

            }
            try {
                // What to do with the file, e.g. display it in a TextArea              
                jTextAreaIn.write(new FileWriter(file.getAbsolutePath()));
            } catch (IOException ex) {
                System.out.println("problem saving file" + file.getAbsolutePath());
            }
        } else {
            System.out.println("File saves cancelled by user.");
        }
    }//GEN-LAST:event_jMenuItemSaveActionPerformed

    private void jButtonWriteFirmwareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonWriteFirmwareActionPerformed
        // TODO add your handling code here:
        sendCtrlZ();
//        writeOneFirmwareFile ("/programmer/firmwares/1 dwl.dwl");
//        writeOneFirmwareFile ("/programmer/firmwares/2 q2687RD.dwl");
//        writeOneFirmwareFile ("/programmer/firmwares/3 R7.51.0_q2687RD.dwl");
//        writeOneFirmwareFile ("/programmer/firmwares/4 52_20131127.dwl");
//        String str = "AT+WOPEN=0\r\n";
//        sendString (str);
//        str = "AT+WOPEN=3\r\n";
//        sendString (str);
//        str = "AT+WOPEN=1\r\n";
//        sendString (str);
//        waitSomeTime (10);
//        writeOneFirmwareFile ("/programmer/firmwares/5 w.dwl");
//        writeOneFirmwareFile ("/programmer/firmwares/6 dwl.dwl");
//        writeOneFirmwareFile ("/programmer/firmwares/7 R7.52_q2687RD.dwl");
        jButtonWriteFirmware.setEnabled(false);
        SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                writeOneFirmwareFile("/programmer/firmwares/8 52.dwl");
                return null;
            }

            @Override
            protected void done() {
                jButtonWriteFirmware.setEnabled(true);
            }

        };

        worker.execute();
        while (!worker.isDone()) {

        }

        String str = "AT+WOPEN=0\r\n";
        sendString(str);
        str = "AT+WOPEN=3\r\n";
        sendString(str);
        str = "AT+WOPEN=1\r\n";
        sendString(str);
        waitSomeTime(10);
        str = "";
        str += "AT+TSET=\"" + jTextFieldPIN.getText() + "\",\"" + jTextFieldAPN.getText() + "\",\"" + jTextFieldLogin.getText() + "\",\"" + jTextFieldPassword.getText() + "\"\r\n";
        str += "AT+TSERVER=\"" + jTextFieldServerIP.getText() + "\",\"" + jTextFieldPort.getText() + "\"\r\n";
        str += "AT+TID=1," + jTextFieldTID.getText() + "\r\n";
        sendString(str);

    }//GEN-LAST:event_jButtonWriteFirmwareActionPerformed

    private void jButtonGroupSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGroupSendActionPerformed
        // TODO add your handling code here:
        sendCtrlZ();
        waitSomeTime(1);
        String str = "";

        if (jCheckBoxTSET.isSelected()) {
            str += "AT+TSET=\"" + jTextFieldPIN.getText() + "\",\"" + jTextFieldAPN.getText() + "\",\"" + jTextFieldLogin.getText() + "\",\"" + jTextFieldPassword.getText() + "\"\r\n";
        }
        if (jCheckBoxTSERVER.isSelected()) {
            str += "AT+TSERVER=\"" + jTextFieldServerIP.getText() + "\",\"" + jTextFieldPort.getText() + "\"\r\n";
        }
        if (jCheckBoxTID.isSelected()) {
            str += "AT+TID=1," + jTextFieldTID.getText() + "\r\n";
        }

        sendString(str);
        if (!jCheckBoxTID.isSelected()) {

            sendString("AT+CFUN=1\r\n");

        }

    }//GEN-LAST:event_jButtonGroupSendActionPerformed

    private void jButtonSendTIDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSendTIDActionPerformed
        // TODO add your handling code here:
        sendCtrlZ();
        String str = "AT+TID=1," + jTextFieldTID.getText() + "\r\n";

        sendString(str);
    }//GEN-LAST:event_jButtonSendTIDActionPerformed

    private void jButtonSendTSERVERActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSendTSERVERActionPerformed
        // TODO add your handling code here:
        sendCtrlZ();
        String str = "AT+TSERVER=\"" + jTextFieldServerIP.getText() + "\",\"" + jTextFieldPort.getText() + "\"\r\n";

        sendString(str);

        sendString("AT+CFUN\r\n");
    }//GEN-LAST:event_jButtonSendTSERVERActionPerformed

    private void jButtonSendTSetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSendTSetActionPerformed
        // TODO add your handling code here:
        sendCtrlZ();
        String str = "AT+TSET=\"" + jTextFieldPIN.getText() + "\",\"" + jTextFieldAPN.getText() + "\",\"" + jTextFieldLogin.getText() + "\",\"" + jTextFieldPassword.getText() + "\"\r\n";

        sendString(str);

        sendString("AT+CFUN\r\n");
    }//GEN-LAST:event_jButtonSendTSetActionPerformed

    private void jButtonReadTSETActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReadTSETActionPerformed
        // TODO add your handling code here:
        sendCtrlZ();
        String str;
        command = TSET;
        str = "AT+TSET?\r\n";
        sendString(str);
//        str = jTextAreaIn.getText();
//        System.out.println("Считана строка: " + str);
    }//GEN-LAST:event_jButtonReadTSETActionPerformed

    private void jComboBoxPortNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxPortNameActionPerformed
        // TODO add your handling code here:
        //System.out.println ("ActionPerformed");
        try {
            serialPort.closePort();
        } catch (Exception ex) {
            //Do nothing
        }
        System.out.println("Порт " + portName + " закрыт");
        setPortSettings();
    }//GEN-LAST:event_jComboBoxPortNameActionPerformed

    private void jComboBoxPortNameItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBoxPortNameItemStateChanged
        // TODO add your handling code here:
        //System.out.println ("ItemStateChanged");
    }//GEN-LAST:event_jComboBoxPortNameItemStateChanged

    private void jButtonFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonFileActionPerformed
        // TODO add your handling code here:
        if (pathFile == null) {
            openFile();
        }
        sendCtrlZ();
        waitSomeTime(1);
        writeOneFile(pathFile);
        waitSomeTime(10);
    }//GEN-LAST:event_jButtonFileActionPerformed

    private void jButtonCtrlZActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCtrlZActionPerformed
        // TODO add your handling code here:
        sendCtrlZ();
    }//GEN-LAST:event_jButtonCtrlZActionPerformed

    private void jButtonCGMRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCGMRActionPerformed
        // TODO add your handling code here:
        sendCtrlZ();
        String str = "AT+CGMR\r\n";
        sendString(str);
    }//GEN-LAST:event_jButtonCGMRActionPerformed

    private void jButtonReadTSERVERActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReadTSERVERActionPerformed
        // TODO add your handling code here:
        sendCtrlZ();
        command = TSERVER;
        String str = "AT+TSERVER?\r\n";
        sendString(str);
    }//GEN-LAST:event_jButtonReadTSERVERActionPerformed

    private void jButtonReadTIDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReadTIDActionPerformed
        // TODO add your handling code here:
        sendCtrlZ();
        command = TID;
        String str = "AT+TID?\r\n";
        sendString(str);
    }//GEN-LAST:event_jButtonReadTIDActionPerformed

    private void jButtonGroupReadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGroupReadActionPerformed
        // TODO add your handling code here:
        sendCtrlZ();
        String str = "AT+TSERVER?\r\n";
        str += "AT+TID?\r\n";
        str += "AT+TSET?\r\n";
        sendString(str);
    }//GEN-LAST:event_jButtonGroupReadActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        Component[] components = jPanel1.getComponents();
        for (Component component : components) {
            System.out.println("Компонент: " + component);
            component.setEnabled(false);
        }
        sendCtrlZ();
        String str = "AT+WDWL\r\n";
        sendString(str);
        try {
            serialPort.removeEventListener();
        } catch (SerialPortException ex) {
            //Logger.getLogger(Programmer.class.getName()).log(Level.SEVERE, null, ex);
        }
        SerialInputStream serialInputStream = new SerialInputStream(serialPort);
        SerialOutputStream serialOutputStream = new SerialOutputStream(serialPort);
        new GUIModem(serialInputStream, serialOutputStream).setVisible(true);
        
//        sendString("AT+CFUN=1\r\n");
//        waitSomeTime(10);

    }//GEN-LAST:event_jButton1ActionPerformed

    private class Reader implements SerialPortEventListener { //Класс Reader реализует интерфейс SerialPortEventListener

        private String str = "";

        /*
        Метод serialEvent принимает в качестве параметра переменную типа SerialPortEvent
         */
        public void serialEvent(SerialPortEvent spe) {

            if (spe.isRXCHAR() || spe.isRXFLAG()) { //Если установлены флаги RXCHAR и  RXFLAG                
                if (spe.getEventValue() > 0) { //Если число байт во входном буффере больше 0, то
                    //System.out.println("В буфере есть " + spe.getEventValue() + " символов");
                    try {
                        str = "";
                        byte[] buffer = serialPort.readBytes(spe.getEventValue()); //читаем из COM-порта необходимое число байт
                        str = new String(buffer); //преобразуем байты в строку символов
                        if (command != NOP) {
                            answerString += str;
                        }

                        System.out.println("Пришел ответ: " + answerString);
                        if (answerString.contains("OK")) {
                            String str;
                            int startIndex;
                            int endIndex;
                            switch (command) {
                                case NOP:
                                    answerString = "";
                                    break;
                                case TSET:
                                    startIndex = answerString.indexOf("+TSET: ");
                                    endIndex = answerString.indexOf("\r\n", startIndex);
                                    str = answerString.substring(startIndex + 7, endIndex);
                                    String pin = str.substring(4 + str.indexOf("PIN:"), str.indexOf(","));
                                    System.out.println("PIN: " + pin);
                                    String apn = str.substring(4 + str.indexOf("APN:"), str.indexOf(","));
                                    System.out.println("APN: " + apn);
                                    String login = "";
                                    String password = "";
                                    jTextFieldPIN.setText(pin);
                                    answerString = "";
                                    answerString = "";
                                    command = NOP;
                                    break;
                                case TSERVER:
                                    startIndex = answerString.indexOf("+TSERVER: ");
                                    endIndex = answerString.indexOf("\r\n", startIndex);
                                    str = answerString.substring(startIndex + 10, endIndex);
                                    jTextFieldTID.setText(str);
                                    answerString = "";
                                    answerString = "";
                                    command = NOP;
                                    break;
                                case TID:
                                    startIndex = answerString.indexOf("+TID: 1,");
                                    endIndex = answerString.indexOf("\r\n", startIndex);
                                    str = answerString.substring(startIndex + 8, endIndex);
                                    jTextFieldTID.setText(str);
                                    answerString = "";
                                    command = NOP;
                                    break;
                            }
                        }
                        SwingUtilities.invokeLater(
                                new Runnable() {
                            public void run() {
                                jTextAreaIn.append(str);
                            }
                        }
                        );
                    } catch (Exception ex) {
                        //DialogMessage dialogMessage = new DialogMessage(NewJFrame.this, DialogMessage.TYPE_ERROR, "Ошибка", ex.getMessage());

                        //Do nothing
                    }

                }
            }

        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                System.out.println (info);                
                if ("Windows Classic".equals(info.getName())) { //Windows Classic, Windows, CDE/Motif, Nimbus, Metal
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
//            System.out.println ();
//            System.out.println ("System Look and feel: " + javax.swing.UIManager.getSystemLookAndFeelClassName());
//            javax.swing.UIManager.setLookAndFeel( javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Programmer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Programmer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Programmer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Programmer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Programmer().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButtonCGMR;
    private javax.swing.JButton jButtonCtrlZ;
    private javax.swing.JButton jButtonFile;
    private javax.swing.JButton jButtonGroupRead;
    private javax.swing.JButton jButtonGroupSend;
    private javax.swing.JButton jButtonReadTID;
    private javax.swing.JButton jButtonReadTSERVER;
    private javax.swing.JButton jButtonReadTSET;
    private javax.swing.JButton jButtonSendTID;
    private javax.swing.JButton jButtonSendTSERVER;
    private javax.swing.JButton jButtonSendTSet;
    private javax.swing.JButton jButtonWriteFirmware;
    private javax.swing.JCheckBox jCheckBoxTID;
    private javax.swing.JCheckBox jCheckBoxTSERVER;
    private javax.swing.JCheckBox jCheckBoxTSET;
    private javax.swing.JComboBox<String> jComboBoxPortName;
    private javax.swing.JDialog jDialog1;
    private javax.swing.JFileChooser jFileChooserOpen;
    private javax.swing.JFileChooser jFileChooserSave;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelFirmware;
    private javax.swing.JLabel jLabelPortName;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemOpen;
    private javax.swing.JMenuItem jMenuItemSave;
    private javax.swing.JPanel jPanel1;
    public static javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextAreaIn;
    private javax.swing.JTextField jTextFieldAPN;
    private javax.swing.JTextField jTextFieldLogin;
    private javax.swing.JTextField jTextFieldPIN;
    private javax.swing.JTextField jTextFieldPassword;
    private javax.swing.JTextField jTextFieldPort;
    private javax.swing.JTextField jTextFieldServerIP;
    private javax.swing.JTextField jTextFieldTID;
    // End of variables declaration//GEN-END:variables
}
