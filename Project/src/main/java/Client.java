import java.util.*;
import java.net.*;
import java.io.*;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.swing.*;
import java.awt.event.ActionEvent;

import java.util.logging.Logger;
import java.util.logging.Level;

public class Client
{
    private JFrame clientFrame;
    
    private Socket clientSocket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    
    // configure the parameters of the method converting the user's computer speed from bps to Kbps
    private static final BigDecimal VALUE_PER_SECONDS = new BigDecimal(1000);
    private static final int DEFAULT_SCALE = 0;
    private static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_EVEN;
    
    private String tRate;
    
    public Client()
    {
        // create user interface window
        clientFrame = new JFrame("Streaming Client");
        clientFrame.setSize(420, 350);
        clientFrame.setResizable(false);
        clientFrame.setLocationRelativeTo(null);
        clientFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        clientFrame.getContentPane().setLayout(null);
        clientFrame.setVisible(true);

        // create help button for the application manual
        JButton helpBtn = new JButton("Manual");
        helpBtn.setBounds(50, 20, 95, 30);
        clientFrame.getContentPane().add(helpBtn);

        // create connection button for server connection
        JButton connectBtn = new JButton("Connect");
        connectBtn.setBounds(270, 20, 85, 30);
        clientFrame.getContentPane().add(connectBtn);

        // create download test button
        JButton testBtn = new JButton("Download Test");
        testBtn.setBounds(10, 100, 118, 30);
        clientFrame.getContentPane().add(testBtn);

        // create label for the format type list to choose from
        JLabel formatLbl = new JLabel("Format type");
        formatLbl.setBounds(155, 75, 77, 30);
        clientFrame.getContentPane().add(formatLbl);

        // create list containing format types with the ability to select one
        JComboBox formatComboBox = new JComboBox();
        formatComboBox.setBounds(150, 105, 90, 20);
        clientFrame.getContentPane().add(formatComboBox);
        formatComboBox.addItem("avi");
        formatComboBox.addItem("mp4");
        formatComboBox.addItem("mkv");

        // create send button to send speed and selected format type to server
        JButton sendBtn = new JButton("Send");
        sendBtn.setBounds(270, 100, 92, 30);
        clientFrame.getContentPane().add(sendBtn);
        sendBtn.setEnabled(false);

        // create label for the list of compatible file names returned for selection
        JLabel videoLbl = new JLabel("Video file");
        videoLbl.setBounds(70, 160, 75, 30);
        clientFrame.getContentPane().add(videoLbl);

        // create list containing names of compatible files returned with the ability to select one
        JComboBox videoComboBox = new JComboBox();
        videoComboBox.setBounds(10, 190, 195, 20);
        clientFrame.getContentPane().add(videoComboBox);

        // create label for the list of available transmission protocols for selection
        JLabel protocolLbl = new JLabel("Protocol");
        protocolLbl.setBounds(275, 160, 75, 30);
        clientFrame.getContentPane().add(protocolLbl);

        // create list containing available transmission protocols with the ability to select one
        JComboBox protocolComboBox = new JComboBox();
        protocolComboBox.setBounds(270, 190, 90, 20);
        clientFrame.getContentPane().add(protocolComboBox);
        protocolComboBox.addItem("");
        protocolComboBox.addItem("TCP");
        protocolComboBox.addItem("UDP");
        protocolComboBox.addItem("RTP/UDP");

        // create stream button for the selected video file using the selected transmission protocol
        JButton playBtn = new JButton("Stream");
        playBtn.setBounds(160, 250, 80, 30);
        clientFrame.getContentPane().add(playBtn);
        playBtn.setEnabled(false);

        clientFrame.repaint();

        // create logger object for console logging
        Logger logger = Logger.getLogger(Client.class.getName());
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s [%1$tc]%n");
        
        // add listener for button presses and event handling
        // display the user manual upon pressing the 'Manual' button
        helpBtn.addActionListener((ActionEvent e) -> {
            logger.info("Pressed the 'Manual' button.");
            JOptionPane.showMessageDialog(null, "Follow the next steps:\n"
                    + "1. Press the 'Connect' button to establist Client connection to the Server\n\n"
                    + "2. Press the 'Download Test' button to start a 5 second download test to estimate user's computer speed\n"
                    + "    Select a format type to return the list of corresponding video file names\n"
                    + "    Press the 'Send' button to send the speed and format type to the Server\n\n"
                    + "3. Select a video file from the returned list to broadcast\n" 
                    + "    Select the transmission protocol of the video file\n"
                    + "    Press the 'Stream' button to start transmitting the video file\n\n"
                    + "4. Press the 'Stop' button to terminate the connection to the Server", "User Manual of Streaming Client application", JOptionPane.INFORMATION_MESSAGE);
        });

        // connect to the server upon pressing the 'Έναρξη σύνδεσης' button
        connectBtn.addActionListener((ActionEvent e) -> {
            try {
                logger.info("Pressed the 'Connect' button.");

                // open client socket to request connection to server address on the same port
                clientSocket = new Socket("127.0.0.1", 5000);
                logger.info("Connection to the server is established.");

                // objects for sending and receiving data to and from the server
                outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                inputStream = new ObjectInputStream(clientSocket.getInputStream());

                JOptionPane.showMessageDialog(null, "The connection to the Server was successful", "Notification", JOptionPane.INFORMATION_MESSAGE);

                connectBtn.setEnabled(false);
                sendBtn.setEnabled(true);
            } catch (ConnectException ex) {
                JOptionPane.showMessageDialog(null, "Connection failed\n\nThe Server does not have its connection activated", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        
        // start the download test by pressing the 'Download Test' button
        testBtn.addActionListener((ActionEvent e) -> {
            logger.info("Pressed the 'Download Test' button.");
            
            logger.info("Starting a 5-second download test.");
            JOptionPane.showMessageDialog(null, "Starting a 5-second download test\n\nPlease wait for it to complete", "Notification", JOptionPane.INFORMATION_MESSAGE);
            
            // define a variable to store the estimated speed
            BigDecimal[] transferRate = new BigDecimal[1];
            transferRate[0] = BigDecimal.ZERO;
            
            // create a SpeedTestSocket
            SpeedTestSocket speedTestSocket = new SpeedTestSocket();
            
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
                // method called when the download test is completed to print the results
                @Override
                public void onCompletion(SpeedTestReport report)
                {
                    //called when download/upload is complete
                    logger.info("The download test for speed estimation is completed.");
                    logger.info("The packet size is: " + report.getTotalPacketSize() + " byte(s).");
                    logger.info("The estimated speed is: " + report.getTransferRateBit().divide(VALUE_PER_SECONDS, DEFAULT_SCALE, DEFAULT_ROUNDING_MODE) + " Kbps.");
                    transferRate[0] = report.getTransferRateBit().divide(VALUE_PER_SECONDS, DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
                    JOptionPane.showMessageDialog(null, "The download test completed successfully\n\n"
                            + "The estimated speed of the client's computer connection is "
                            + transferRate[0]
                            + " Kbps ("
                            + transferRate[0].doubleValue() / 1000.0
                            + " Mbps)", "Notification", JOptionPane.INFORMATION_MESSAGE);
                }

                // method called when an error occurs to report it
                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage)
                {
                    if (logger.isLoggable(Level.SEVERE))
                    {
                        logger.log(Level.SEVERE, errorMessage);
                    }
                }

                // method called during the download test to report progress
                @Override
                public void onProgress(float percent, SpeedTestReport downloadReport)
                {
                    //logger.info("Completed: " + downloadReport.getProgressPercent() + "%");
                    //logger.info("Transfer rate: " + downloadReport.getTransferRateBit().divide(VALUE_PER_SECONDS, DEFAULT_SCALE, DEFAULT_ROUNDING_MODE) + " Kbps.");
                    //logger.info("Size downloaded so far: " + downloadReport.getTemporaryPacketSize() + "/" + downloadReport.getTotalPacketSize());
                }
            });

            // method that uses the SpeedTestSocket object to perform the download test of a file for a fixed duration
            speedTestSocket.startFixedDownload(
                    "https://link.testfile.org/15MB",
                    5000
            );
            
            // wait for the application to finish the download test
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            
            tRate = transferRate[0].toString();
        });
        
        // send the download test speed and selected format to the Server by pressing the 'Send' button
        sendBtn.addActionListener((ActionEvent e) -> {
            logger.info("Pressed the 'Send' button.");

            // check if the download test has been done before sending the data to the server
            if (tRate == null)
            {
                JOptionPane.showMessageDialog(null, "Please perform the download test first to estimate the speed", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }           

            List<String> fileList = new ArrayList<>();

            try {
                // send connection speed in Kbps and selected format type to the server
                outputStream.writeObject(tRate);
                outputStream.writeObject(formatComboBox.getSelectedItem().toString());

                logger.info("Sending connection speed " + tRate + " Kbps and format type " + formatComboBox.getSelectedItem().toString() + " to the server.");
                JOptionPane.showMessageDialog(null, "Sending connection speed " + tRate + " Kbps and format type " + formatComboBox.getSelectedItem().toString() + " to the Server", "Notification", JOptionPane.INFORMATION_MESSAGE);

                // receive the list of compatible file names from the server
                fileList = (List<String>) inputStream.readObject();

                logger.info("Received the list of compatible file names for transmission from the server.");
                JOptionPane.showMessageDialog(null, "The connection speed of "
                        + tRate
                        + " Kbps and the format type "
                        + formatComboBox.getSelectedItem().toString()
                        + " were successfully sent to the Server"
                        + "\n\nThe Server returned the list of compatible file names for transmission", "Notification", JOptionPane.INFORMATION_MESSAGE);

                // disable buttons to avoid errors after performing the operation
                testBtn.setEnabled(false);
                formatComboBox.setEnabled(false);
                sendBtn.setEnabled(false);

                playBtn.setEnabled(true);
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // add returned video file names to the list
            for (String video : fileList)
            {
                videoComboBox.addItem(video);
            }
        });
        
        // receive the transmission of the selected video file via the selected transmission protocol from the Server by pressing the 'Stream' button
        playBtn.addActionListener((ActionEvent e) -> {
            logger.info("Pressed the 'Stream' button.");
            
            String chosenFileName = (String) videoComboBox.getSelectedItem();
            String chosenProtocol = (String) protocolComboBox.getSelectedItem();
            
            // if the user doesn't choose a protocol, the choice is made based on the resolution of the selected video file
            if (chosenProtocol.isEmpty())
            {
                logger.info("Automatic protocol selection.");
                JOptionPane.showMessageDialog(null, "You did not choose one of the available transmission protocols\n\nThe selection will be made automatically based on the resolution of the selected file", "Warning!", JOptionPane.WARNING_MESSAGE);
                String[] parts1 = chosenFileName.split("-");
                String[] parts2 = parts1[1].split("\\.");

                if (parts2[0].equals("240p")) {
                    chosenProtocol = "TCP";
                } else if (parts2[0].equals("360p") || parts2[0].equals("480p")) {
                    chosenProtocol = "UDP";
                } else {
                    chosenProtocol = "RTP/UDP";
                }
            }
            
            try {
                // send the selected file name and protocol to the server
                outputStream.writeObject(chosenFileName);
                outputStream.writeObject(chosenProtocol);
                
                logger.info("Sending video file name " + chosenFileName + " and transmission protocol " + chosenProtocol + " to the server.");
                JOptionPane.showMessageDialog(null, "Sending video file name " + chosenFileName + " and transmission protocol " + chosenProtocol + " to the server", "Notification", JOptionPane.INFORMATION_MESSAGE);
                
                // list for storing the command composition that will run the process for receiving the video file transmission
                List<String> command = new ArrayList<>();

                // check the selected transmission protocol and compose the corresponding command for the srteaming process
                if (chosenProtocol.equals("TCP")) {
                    command.addAll(Arrays.asList("ffplay", "tcp://127.0.0.1:2000"));
                } else if(chosenProtocol.equals("UDP")) {
                    command.addAll(Arrays.asList("ffplay", "udp://127.0.0.1:3000"));
                } else {
                    command.addAll(Arrays.asList("ffplay", "-protocol_whitelist", "file,rtp,udp", "-i", "video.sdp"));
                }

                JOptionPane.showMessageDialog(null, "Sending video file name " + chosenFileName + " and transmission protocol " + chosenProtocol + " to the Server was successful", "Notification", JOptionPane.INFORMATION_MESSAGE);

                // process builder object that will execute the stream command
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                // start the streaming process
                Process play = processBuilder.start();
                
                // client closes the input and output streams to terminate the connection with the server
                outputStream.close();
                inputStream.close();

                // client closes the connection with the server
                clientSocket.close();
                logger.info("Terminating connection with the server and starting the stream.");
                JOptionPane.showMessageDialog(null, "The connection with the Server was successfully terminated\n\nThe video file stream is starting..", "Notification", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            } catch (IOException ex) {
                ex.printStackTrace();
            } 
        });
    }
    
    public static void main(String[] args)
    {
        new Client();
    }
}
