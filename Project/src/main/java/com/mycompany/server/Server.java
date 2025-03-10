package com.mycompany.server;

import java.util.*;
import java.net.*;
import java.io.*;

import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.FFmpegExecutor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.EventQueue;

import java.util.logging.Logger;

public class Server
{
    private JFrame serverFrame;
    
    private Socket clientSocket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    
    // bool variable identifying even one format or analysis missing
    private boolean missing = false;
    
    public Server()
    {
        // create a user interface window
        serverFrame = new JFrame("Streaming Server");
        serverFrame.setSize(435, 300);
        serverFrame.setResizable(false);
        serverFrame.setLocationRelativeTo(null);
        serverFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	serverFrame.getContentPane().setLayout(null);
        serverFrame.setVisible(true);

        // create a user manual button for the application
        JButton helpBtn = new JButton("Manual");
        helpBtn.setBounds(150, 20, 95, 30);
        serverFrame.getContentPane().add(helpBtn);
        
        // create a button to display the file names in the "videos" folder
        JButton filesBtn = new JButton("Files");
        filesBtn.setBounds(40, 60, 90, 30);
        serverFrame.getContentPane().add(filesBtn);
        
        // create a button to display movie information (format and resolutions)
        JButton infoBtn = new JButton("Info");
        infoBtn.setBounds(265, 60, 110, 30);
        serverFrame.getContentPane().add(infoBtn);
        
        // create a button to check the missing formats and resolutions for each movie and display them
        JButton checkBtn = new JButton("Check");
        checkBtn.setBounds(40, 150, 90, 30);
        serverFrame.getContentPane().add(checkBtn);
        
        // create a button to create the missing formats and resolutions for each movie
        JButton createBtn = new JButton("Create");
        createBtn.setBounds(265, 150, 110, 30);
        serverFrame.getContentPane().add(createBtn);
        
        // create a button to start a connection while waiting for clients
        JButton connectBtn = new JButton("Start connection");
        connectBtn.setBounds(130, 210, 135, 30);
        serverFrame.getContentPane().add(connectBtn);
        
        // create an object to report logs to the console
        Logger logger = Logger.getLogger(Server.class.getName());
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s [%1$tc]%n");
        
        // define a reference object to the path of the original "videos" folder
        File folder = new File("videos/");
        
        // check if the original "videos" folder is in the same location as the application, otherwise terminate the application
        if (!folder.exists())
        {
            logger.info("Folder \"videos\" does not exist and needs to be created in the same location as the application.");
            logger.info("Terminating application..");
            JOptionPane.showMessageDialog(null, "Folder \"videos\" does not exist\n\nCreate it at the same location as the application", "Warning!", JOptionPane.ERROR_MESSAGE);
            JOptionPane.showMessageDialog(null, "Terminating application", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } else {
            logger.info("Folder \"videos\" is created at the same location as the application.");
        }
        
        // create a list of the file names of the original folder
        File[] files = folder.listFiles();
        
        // check for files in the home folder, otherwise terminate the application
        if (files == null || files.length == 0)
        {
            logger.info("Folder \"videos\" is empty.");
            logger.info("Terminating application..");
            JOptionPane.showMessageDialog(null, "Folder \"videos\" is empty\n\nAdd video file(s) with format type .avi/.mp4/.mkv and resolution in the range 240p/360p/480p/720p/1080p under the correct name", "Warning!", JOptionPane.ERROR_MESSAGE);
            JOptionPane.showMessageDialog(null, "Terminating application", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        
        // the large map in which for each movie the formats and their resolutions present in the original "videos" folder will be stored
        Map<String, Map<String, List<String>>> movieFormatsAndResolutions = new HashMap<>();
        
        // list in which the paths of all files in the original "videos" folder will be stored
        List<String> filePaths = new ArrayList<>();
        
        // define valid format types
        Set<String> allowedFormats = new HashSet<>(Arrays.asList("AVI (Audio Video Interleaved)", "QuickTime / MOV", "Matroska / WebM"));
        
        // define valid resolutions
        Set<String> allowedResolutions = new HashSet<>(Arrays.asList("1080p", "720p", "480p", "360p", "240p"));
        
        // set a counter of the total number of files in the "videos" folder
        int fileCount = 0;
        
        // examine each file present in the original folder
        for (File file : files)
        {
            if (file.isFile())
            {
                fileCount++;
                try {
                    // create an FFMPEG object to examine the file information 
                    FFprobe ffprobe = new FFprobe();
                    FFmpegProbeResult probeResult = ffprobe.probe(file.getAbsolutePath());
                    
                    // add the file path to the list
                    filePaths.add(file.getAbsolutePath());
                    
                    // extract the name of the film
                    String movieName = file.getName().substring(0, file.getName().lastIndexOf('.'));
                    String[] parts1 = movieName.split("-");
                    
                    // check the validity of the file name
                    if (parts1.length != 2)
                    {
                        logger.info("File name found in folder \"videos\" not compatible with the application specifications.");
                        logger.info("Terminating application..");
                        JOptionPane.showMessageDialog(null, "File name found in folder \"videos\" not compatible with the application specifications\n\nFollow the format: movie_name-resolutionp", "Warning!", JOptionPane.ERROR_MESSAGE);
                        JOptionPane.showMessageDialog(null, "Terminating application", "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(0);
                    }
                    
                    // extract movie name
                    int indexOfResolution = movieName.lastIndexOf("-");
                    String baseName = movieName.substring(0, indexOfResolution);
                    
                    // if a file's movie name appears for the first time create a place on the map for it, otherwise get its formats and their resolutions
                    Map<String, List<String>> formatAndResolutions = movieFormatsAndResolutions.computeIfAbsent(baseName, k -> new HashMap<>());
                    
                    // extract file format
                    FFmpegFormat format = probeResult.getFormat();
                    String formatName = format.format_long_name;
                    
                    // check for files with formats beyond the three types supported by the application
                    if (!allowedFormats.contains(formatName))
                    {
                        logger.info("File found in folder \"videos\" with format type other than \".avi\", \".mp4\" και \".mkv\".");
                        logger.info("Terminating application..");
                        JOptionPane.showMessageDialog(null, "File found in folder \"videos\" with format type other than \".avi\", \".mp4\" and \".mkv\"\n\nConfirm format type of files is supported", "Warning!", JOptionPane.ERROR_MESSAGE);
                        JOptionPane.showMessageDialog(null, "Terminating application", "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(0);
                    }
                    
                    // extract resolution of file format
                    String resolutionLabel = "";
                    FFmpegStream stream = probeResult.getStreams().get(0);

                    if (stream.height >= 1080 && stream.height < 1440) {
                        resolutionLabel = "1080p";
                    } else if (stream.height >= 720 && stream.height < 1080) {
                        resolutionLabel = "720p";
                    } else if (stream.height >= 480 && stream.height < 720) {
                        resolutionLabel = "480p";
                    } else if (stream.height >= 360 && stream.height < 480) {
                        resolutionLabel = "360p";
                    } else if (stream.height >= 240 && stream.height < 360){
                        resolutionLabel = "240p";
                    } else {
                        logger.info("File found in folder \"videos\" with resolution other than the five supported.");
                        logger.info("Terminating application..");
                        JOptionPane.showMessageDialog(null, "File found in folder \"videos\" with resolution other than the five supported\n\nConfirm that the resolution listed on the file name is supported", "Warning!", JOptionPane.ERROR_MESSAGE);
                        JOptionPane.showMessageDialog(null, "Terminating application", "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(0);
                    }
                    
                    String[] parts2 = parts1[1].split("\\.");
                    
                    // equivalence check between the resolution listed on the file name and the actual resolution of the file
                    if (!parts2[0].equals(resolutionLabel))
                    {
                        logger.info("File found in folder \"videos\" in which the resolution listed on the name does not match the actual resolution of the file.");
                        logger.info("Terminating application..");
                        JOptionPane.showMessageDialog(null, "File found in folder \"videos\" in which the resolution listed on the name does not match the actual resolution of the file.\n\nConfirm that the resolution listed on file name is correct", "Warning!", JOptionPane.ERROR_MESSAGE);
                        JOptionPane.showMessageDialog(null, "Terminating application", "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(0);
                    }
                    
                    // if the file format appears for the first time create a place on the map for it, otherwise get its resolutions
                    List<String> resolutions = formatAndResolutions.computeIfAbsent(formatName, k -> new ArrayList<>());

                    // adding the resolution of the format to its list
                    resolutions.add(resolutionLabel);
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        // set counter of unique movies in the "videos" folder
        int movieCount = 0;
        
        // update the counter for each unique movie name
        for (String movie : movieFormatsAndResolutions.keySet()) {
            movieCount++;
        }
        
        // check the existence of all 15 files for each unique movie in the "videos" folder
        if (movieCount * 15 != fileCount) {
            missing = true;
        }
        
        // add listener to the button press
        // display the user manual by pressing the 'Manual' button
        helpBtn.addActionListener((ActionEvent e) -> {
            logger.info("Pressed the 'Manual' button.");
            
            JOptionPane.showMessageDialog(null, "Click 'Files' button to display a list with the file names located in folder \"videos\"\n\n"
                    + "Click 'Info' button to display the information of each movie and specifically the format types and their resolutions located in folder \"videos\"\n\n"
                    + "Click 'Check' button to display format types and resolutions missing for each movie in folder \"videos\"\n\n"
                    + "Click 'Create' button to create format types and resolutions missing for each movie in folder \"videos\"\n\n"
                    + "Click 'Start connection' button to establish a connection with the client", "User Manual of Streaming Server Application", JOptionPane.INFORMATION_MESSAGE);
        });
        
        // display the file names of the "videos" folder when the 'Files' button is pressed
        filesBtn.addActionListener((ActionEvent e) -> {
            logger.info("Pressed the 'Files' button.");

            // create a window to display the files
            JFrame filesFrame = new JFrame("Files in folder \"videos\"");
            filesFrame.setBounds(300, 50, 400, 400);
            filesFrame.setResizable(false);
            filesFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            filesFrame.getContentPane().setLayout(null);
            filesFrame.setVisible(true);

            // informational label for the window contents
            JLabel infoLbl = new JLabel("Files available for transmission in \"videos\" folder:");
            infoLbl.setBounds(25, 10, 325, 15);
            filesFrame.getContentPane().add(infoLbl);

            // create a list with the names of the video files
            DefaultListModel<String> listModel = new DefaultListModel<>();

            // refresh the list data with file names
            File[] videoFiles = folder.listFiles();
            for (File videoFile : videoFiles)
            {
                if (videoFile.isFile())
                {
                    listModel.addElement(videoFile.getName());
                }
            }

            JList<String> videoFileList = new JList<>(listModel);

            // create a scroll pane and add the file name list to it
            JScrollPane scrollPane = new JScrollPane(videoFileList);
            scrollPane.setBounds(50, 40, 250, 300);
            filesFrame.getContentPane().add(scrollPane);
        });
        
        // display movie information from the "videos" folder when the 'Info' button is pressed
        infoBtn.addActionListener((ActionEvent e) -> {
            logger.info("Pressed the 'Info' button.");

            // create a window to display movie information
            JFrame infoFrame = new JFrame("Movie Information");
            infoFrame.setBounds(1200, 370, 400, 400);
            infoFrame.setResizable(false);
            infoFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            infoFrame.getContentPane().setLayout(null);
            infoFrame.setVisible(true);

            // create a text area to display movie information
            JTextArea textArea = new JTextArea();
            textArea.setEditable(false);

            // refresh the display field with data
            textArea.append("Format types and resolutions of movies in the \"videos\" folder:\n");
            for (Map.Entry<String, Map<String, List<String>>> entry : movieFormatsAndResolutions.entrySet())
            {
                textArea.append("\nMovie: " + entry.getKey() + "\n");
                for (Map.Entry<String, List<String>> formatEntry : entry.getValue().entrySet())
                {
                    textArea.append("Format type: " + formatEntry.getKey() + "\n");
                    textArea.append("Format type resolutions: " + formatEntry.getValue() + "\n");
                }
            }

            // create a scroll pane and add the text area to it
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBounds(10, 10, 365, 340);
            infoFrame.getContentPane().add(scrollPane);
        });
        
        // define sets of all possible format types and resolutions
        Set<String> allFormats = new HashSet<>(Arrays.asList("AVI (Audio Video Interleaved)", "QuickTime / MOV", "Matroska / WebM"));
        Set<String> allResolutions = new HashSet<>(Arrays.asList("1080", "720", "480", "360", "240"));
        
        // check for missing formats and resolutions for each movie and display them when the 'Check' button is pressed
        checkBtn.addActionListener((ActionEvent e) -> {
            logger.info("Pressed the 'Check' button.");

            // create a window to display missing formats and resolutions
            JFrame checkFrame = new JFrame("Missing Files Check");
            checkFrame.setBounds(115, 470, 585, 400);
            checkFrame.setResizable(false);
            checkFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            checkFrame.getContentPane().setLayout(null);
            checkFrame.setVisible(true);

            // create a text area to display missing formats and resolutions
            JTextArea textArea = new JTextArea();
            textArea.setEditable(false);

            // map to store the largest resolution of each format available in the folder for each movie
            Map<String, Map<String, Integer>> movieFormatsLargestResolution = new HashMap<>();

            // iterate through the main map to find each movie, format, and resolution
            for (Map.Entry<String, Map<String, List<String>>> entry : movieFormatsAndResolutions.entrySet())
            {
                String movieName = entry.getKey();
                Map<String, List<String>> formatAndResolutions = entry.getValue();

                // add the movie to the largest resolution map if encountered for the first time
                movieFormatsLargestResolution.putIfAbsent(movieName, new HashMap<>());

                // retrieve the current largest resolution map for the format
                Map<String, Integer> formatCurrentResolution = movieFormatsLargestResolution.get(movieName);

                // iterate through each format's resolution list
                for (Map.Entry<String, List<String>> formatEntry : formatAndResolutions.entrySet())
                {
                    String format = formatEntry.getKey();
                    List<String> resolutions = formatEntry.getValue();

                    for (String resolution : resolutions)
                    {
                        // extract the numeric part of the resolution (excluding 'p')
                        String number = resolution.substring(0, resolution.length() - 1);

                        // convert string to integer
                        int resolutionInt = Integer.parseInt(number);

                        // compare the largest resolution of the format so far with the new resolution found
                        int currentLargestResolution = formatCurrentResolution.getOrDefault(format, 0);
                        formatCurrentResolution.put(format, Math.max(currentLargestResolution, resolutionInt));
                    }
                }
            }

            textArea.append("Missing formats and resolutions for each movie:\n");

            // iterate through the main map to find each movie, format, and resolutions
            for (Map.Entry<String, Map<String, List<String>>> entry : movieFormatsAndResolutions.entrySet())
            {
                String movieName = entry.getKey();
                Map<String, List<String>> formatAndResolutions = entry.getValue();

                // determine missing formats for each movie
                Set<String> missingFormats = new HashSet<>(allFormats);
                missingFormats.removeAll(formatAndResolutions.keySet());

                // add missing formats for each movie to the display field
                textArea.append("\nMovie: " + movieName);
                if (!missingFormats.isEmpty())
                {
                    textArea.append("\nMissing format types: " + missingFormats);
                    for (String missingFormat : missingFormats)
                    {
                        missing = true;
                        textArea.append("\nMissing resolutions for format type " + missingFormat + ": " + allResolutions); 
                    }
                } else {
                    textArea.append("\nNo missing format types");
                }

                // iterate through each format of the specific movie
                for (Map.Entry<String, List<String>> formatEntry : formatAndResolutions.entrySet())
                {
                    String format = formatEntry.getKey();
                    List<String> resolutions = formatEntry.getValue();

                    // remove the 'p' suffix from each resolution in the list
                    for (int i = 0; i < resolutions.size(); i++)
                    {
                        String resolution = resolutions.get(i);
                        resolution = resolution.replace("p", "");
                        resolutions.set(i, resolution);
                    }

                    // determine missing resolutions for each format
                    Set<String> missingResolutionsForFormat = new HashSet<>(allResolutions);
                    missingResolutionsForFormat.removeAll(resolutions);

                    // add missing resolutions for the specific format to the display field
                    if (!missingResolutionsForFormat.isEmpty())
                    {
                        missing = true;
                        textArea.append("\nMissing resolutions for format type " + format + ": " + missingResolutionsForFormat);
                    } else {
                        textArea.append("\nNo missing resolutions for format type " + format);
                    }
                }

                textArea.append("\n");
            }

            // create a scroll pane and add the text area to it
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBounds(10, 10, 550, 340);
            checkFrame.getContentPane().add(scrollPane);
        });
        
        // create the missing formats and resolutions for each movie and update the main map when the 'Create' button is pressed
        createBtn.addActionListener((ActionEvent e) -> {
            logger.info("Pressed the 'Create' button.");

            // map to determine the width based on the height of the video file dimensions
            Map<String, Integer> resolutionWidthMap = new HashMap<>();
            resolutionWidthMap.put("1080", 1920);
            resolutionWidthMap.put("720", 1280);
            resolutionWidthMap.put("480", 854);
            resolutionWidthMap.put("360", 640);
            resolutionWidthMap.put("240", 426);

            // if any format or resolution is missing for a movie, create it using FFMPEG
            if (missing)
            {
                JOptionPane.showMessageDialog(null, "The creation of the missing format types and resolutions is starting\n\nPlease wait..", "Notification", JOptionPane.INFORMATION_MESSAGE);

                // iterate through the main map to find each movie, format, and resolutions
                for (Map.Entry<String, Map<String, List<String>>> entry : movieFormatsAndResolutions.entrySet())
                {
                    String movieName = entry.getKey();
                    Map<String, List<String>> formatAndResolutions = entry.getValue();

                    // determine the missing formats for each movie
                    Set<String> missingFormats = new HashSet<>(allFormats);
                    missingFormats.removeAll(formatAndResolutions.keySet());

                    // set the FFMPEG parameter with the input file path of the movie for conversion
                    String inputFilePath = "";
                    for (String filePath : filePaths)
                    {
                        if (filePath.contains(movieName))
                        {
                            inputFilePath = filePath;
                            break;
                        }
                    }

                    if (!missingFormats.isEmpty())
                    {
                        // iterate through the list of missing formats for each movie
                        for (String missingFormat : missingFormats)
                        {
                            // create missing format files and all their resolutions
                            for (String missingResolution : allResolutions)
                            {
                                // set the FFMPEG parameter with the file extension based on the missing formats
                                String formatExtension1 = "";
                                String formatExtension2 = "";
                                if (missingFormat.contains("Matroska")) {
                                    formatExtension1 = "mkv";
                                    formatExtension2 = "matroska";
                                } else if (missingFormat.contains("AVI")) {
                                    formatExtension1 = "avi";
                                    formatExtension2 = "avi";
                                } else if (missingFormat.contains("QuickTime")) {
                                    formatExtension1 = "mp4";
                                    formatExtension2 = "mp4";
                                }

                                // set the FFMPEG parameter with the output file path for each missing format and resolution
                                String outputFilePath = "videos/" + movieName + "-" + missingResolution + "p." + formatExtension1;

                                // determine the video bitrate based on the missing resolutions for each missing format
                                long bitrate = 0;
                                if (missingResolution.equals("240")) {
                                    bitrate = 500000L;
                                } else if (missingResolution.equals("360")) {
                                    bitrate = 1000000L;
                                } else if (missingResolution.equals("480")) {
                                    bitrate = 2500000L;
                                } else if (missingResolution.equals("720")) {
                                    bitrate = 5000000L;
                                } else if (missingResolution.equals("1080")) {
                                    bitrate = 8000000L;
                                }

                                int width = resolutionWidthMap.getOrDefault(missingResolution, 0);

                                logger.info("Creating a missing resolution for a missing format type.");
                                // FFMPEG object that takes all parameters and creates the missing files
                                try {
                                    FFmpeg ffmpeg = new FFmpeg();

                                    FFmpegBuilder builder = new FFmpegBuilder()
                                        .setInput(inputFilePath)
                                        .addOutput(outputFilePath)
                                        .setFormat(formatExtension2)
                                        .setVideoBitRate(bitrate)
                                        .setVideoResolution(width, Integer.parseInt(missingResolution))
                                        .done();

                                    FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
                                    executor.createJob(builder).run();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }

                    // iterate through the map containing formats and their resolutions for the specific movie
                    for (Map.Entry<String, List<String>> formatEntry : formatAndResolutions.entrySet())
                    {
                        String format = formatEntry.getKey();
                        List<String> resolutions = formatEntry.getValue();

                        // iterate through the list of resolutions for the format and remove the 'p' suffix
                        for (int i = 0; i < resolutions.size(); i++)
                        {
                            String resolution = resolutions.get(i);
                            resolution = resolution.replace("p", "");
                            resolutions.set(i, resolution);
                        }

                        // determine the missing resolutions for each format
                        Set<String> missingResolutionsForFormat = new HashSet<>(allResolutions);
                        missingResolutionsForFormat.removeAll(resolutions);

                        // create missing resolutions for existing formats
                        if (!missingResolutionsForFormat.isEmpty())
                        {
                            // define the FFMPEG parameter with the file extension that will be created based on the missing resolutions for the existing formats
                            for (String missingResolution : missingResolutionsForFormat)
                            {  
                                String formatExtension1 = "";
                                String formatExtension2 = "";
                                if (format.contains("Matroska")) {
                                    formatExtension1 = "mkv";
                                    formatExtension2 = "matroska";
                                } else if (format.contains("AVI")) {
                                    formatExtension1 = "avi";
                                    formatExtension2 = "avi";
                                } else if (format.contains("QuickTime")) {
                                    formatExtension1 = "mp4";
                                    formatExtension2 = "mp4";
                                }

                                // define the FFMPEG parameter with the output file path that will be created for each missing resolution from the existing formats
                                String outputFilePath = "videos/" + movieName + "-" + missingResolution + "p." + formatExtension1;

                                // determine the video bitrate that will be set based on the missing resolutions for the existing formats
                                long bitrate = 0;
                                if (missingResolution.equals("240")) {
                                    bitrate = 500000L;
                                } else if (missingResolution.equals("360")) {
                                    bitrate = 1000000L;
                                } else if (missingResolution.equals("480")) {
                                    bitrate = 2500000L;
                                } else if (missingResolution.equals("720")) {
                                    bitrate = 5000000L;
                                } else if (missingResolution.equals("1080")) {
                                    bitrate = 8000000L;
                                }

                                int width = resolutionWidthMap.getOrDefault(missingResolution, 0);

                                logger.info("Creating missing resolution for an existing format type.");
                                // FFMPEG object that takes all parameters and creates the missing files
                                try {
                                    FFmpeg ffmpeg = new FFmpeg();

                                    FFmpegBuilder builder = new FFmpegBuilder()
                                        .setInput(inputFilePath)
                                        .addOutput(outputFilePath)
                                        .setFormat(formatExtension2)
                                        .setVideoBitRate(bitrate)
                                        .setVideoResolution(width, Integer.parseInt(missingResolution))
                                        .done();

                                    FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
                                    executor.createJob(builder).run();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }
                
                // create list with files of new folder
                File[] newFiles = folder.listFiles();

                // clear main storage map of format types and resolutions of each movie after creating those missing
                movieFormatsAndResolutions.clear();
                
                // examine each file that has been created in the "videos" folder
                for (File file : newFiles)
                {
                    if (file.isFile())
                    {
                        // FFmpeg object to analyze the file information
                        try {
                            FFprobe ffprobe = new FFprobe();
                            FFmpegProbeResult probeResult = ffprobe.probe(file.getAbsolutePath());

                            // extract the movie name
                            String movieName = file.getName().substring(0, file.getName().lastIndexOf('.'));
                            int indexOfResolution = movieName.lastIndexOf("-");
                            String baseName = movieName.substring(0, indexOfResolution);

                            // if the movie name appears for the first time, create an entry in the map for it, else, retrieve its formats and resolutions
                            Map<String, List<String>> formatAndResolutions = movieFormatsAndResolutions.computeIfAbsent(baseName, k -> new HashMap<>());

                            // extract file format
                            FFmpegFormat format = probeResult.getFormat();
                            String formatName = format.format_long_name;

                            // extract the resolution of the file format
                            String resolutionLabel = "";
                            FFmpegStream stream = probeResult.getStreams().get(0);
                            if (stream.height >= 1080 && stream.height < 1440) {
                                resolutionLabel = "1080p";
                            } else if (stream.height >= 720) {
                                resolutionLabel = "720p";
                            } else if (stream.height >= 480) {
                                resolutionLabel = "480p";
                            } else if (stream.height >= 360) {
                                resolutionLabel = "360p";
                            } else if (stream.height >= 240){
                                resolutionLabel = "240p";
                            }

                            // if the file format appears for the first time, create an entry in the map for it, else, retrieve its resolutions
                            List<String> resolutions = formatAndResolutions.computeIfAbsent(formatName, k -> new ArrayList<>());

                            // add the resolution of the format to its list
                            resolutions.add(resolutionLabel);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }

                // indicate that there are no missing files
                missing = false;
                JOptionPane.showMessageDialog(null, "The creation of missing format types and resolutions has been completed", "Notification", JOptionPane.INFORMATION_MESSAGE);

                } else {
                    JOptionPane.showMessageDialog(null, "All format types and their resolutions already exist for each movie in the \"videos\" folder", "Warning!", JOptionPane.WARNING_MESSAGE);
                }
        });
        
        // activate the connection and wait for clients when the 'Start Connection' button is pressed
        connectBtn.addActionListener((ActionEvent e) -> {
            logger.info("Pressed the 'Start Connection' button.");

            if (!missing) {
                // use a separate thread for executing the connection so that the application's window and other functions do not 'freeze'
                Thread serverThread = new Thread(() -> {
                    try {
                        // open the server socket and bind a port for the connection
                        ServerSocket serverSocket = new ServerSocket(5000);
                        logger.info("Activating the connection and waiting for clients..");
                        JOptionPane.showMessageDialog(null, "The Server has activated the connection and is waiting for clients to connect", "Notification", JOptionPane.INFORMATION_MESSAGE);

                        connectBtn.setEnabled(false);

                        while (true)
                        {
                            // server waits for the client connection request, and when found, it accepts the connection on the same port and address the client specified
                            clientSocket = serverSocket.accept();

                            // objects for receiving and sending data from and to the client
                            inputStream = new ObjectInputStream(clientSocket.getInputStream());
                            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

                            logger.info("The client connected.");

                            // receive the connection speed in Kbps and the selected format type from the client
                            String sentRateStr = (String) inputStream.readObject();
                            String sentFormat = (String) inputStream.readObject();

                            logger.info("The client sent a request to retrieve the video files list with a speed of " + sentRateStr + " Kbps and format type " + sentFormat);

                            // create a list of files in the initial folder
                            File[] filesAfter = folder.listFiles();

                            List<String> fileList = new ArrayList<>();

                            // based on the data received from the client, compatible file names are calculated for sending
                            for (File file : filesAfter)
                            {
                                String fileName = file.getName();

                                String[] parts1 = fileName.split("-");
                                String[] parts2 = parts1[1].split("\\.");

                                parts2[0] = parts2[0].replace("p", "");
                                int resolution = Integer.parseInt(parts2[0]);

                                String format = parts2[1];

                                // determine the minimum Kbps value based on the resolution of the specific file
                                int minBitRate;
                                if (resolution == 240) {
                                    minBitRate = 300;
                                } else if (resolution == 360) {
                                    minBitRate = 400;
                                } else if (resolution == 480) {
                                    minBitRate = 500;
                                } else if (resolution == 720) {
                                    minBitRate = 1500;
                                } else if (resolution == 1080) {
                                    minBitRate = 3000;
                                } else {
                                    minBitRate = Integer.MAX_VALUE;
                                }

                                int sentRate = Integer.parseInt(sentRateStr);
                                sentFormat = sentFormat.replace(".", "");

                                // the file is compatible if the user's speed supports its resolution and if the selected format matches the file's format
                                if (sentRate >= minBitRate && format.equals(sentFormat)){
                                    fileList.add(fileName);
                                }
                            }

                            logger.info("Sending the list of compatible file names to the client.");
                            // send the list of compatible file names to the client
                            outputStream.writeObject(fileList);

                            // receive the selected video file and transmission protocol from the client
                            String sentFileName = (String) inputStream.readObject();
                            String sentProtocol = (String) inputStream.readObject();

                            logger.info("The client sent a request to transmit the file " + sentFileName + " using the protocol " + sentProtocol);

                            // list to store the composition of the command that will run the process to start the video file transmission
                            List<String> command = new ArrayList<>();

                            String inputFile = folder.getAbsolutePath() + "\\" + sentFileName;
                            String rtpOutputFile = System.getProperty("user.dir") + "\\video.sdp";

                            // check the selected transmission protocol and construct the corresponding command for the transmission process
                            if (sentProtocol.equals("TCP")) {
                                command.addAll(Arrays.asList("ffmpeg", "-re", "-i", inputFile, "-f", "mpegts", "tcp://127.0.0.1:2000?listen"));
                            } else if (sentProtocol.equals("UDP")) {
                                command.addAll(Arrays.asList("ffmpeg", "-re", "-i", inputFile, "-f", "mpegts", "udp://127.0.0.1:3000"));
                            } else {
                                command.addAll(Arrays.asList("ffmpeg", "-re", "-i", inputFile, "-c:v", "copy", "-an", "-f", "rtp", "-sdp_file", rtpOutputFile, "rtp://127.0.0.1:4000?rtcpport=4005"));
                            }

                            // define the process builder object that will execute the transmission command
                            ProcessBuilder processBuilder = new ProcessBuilder(command);

                            logger.info("Starting the transmission.");
                            // start the transmission process
                            Process stream = processBuilder.start();

                            // server closes the input and output streams to terminate the connection with the client
                            inputStream.close();
                            outputStream.close();

                            // server terminates the connection with the specific client
                            clientSocket.close();

                            logger.info("The client disconnected. Waiting for a new client connection..");
                        }
                    } catch (ClassNotFoundException ex) {
                        ex.printStackTrace();
                    }
                    catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });

                // start the separate server-client communication thread
                serverThread.start();
            } else {
                logger.info("Failed to start the connection due to missing format types or resolutions in the 'videos' folder.");
                JOptionPane.showMessageDialog(null, "There are missing format types or resolutions in the 'videos' folder.\n\nPlease create them first and try again.", "Warning!", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    public static void main(String args[])
    {
        // method to start the application window after a slight delay so that initial checks and functions can be executed first
        EventQueue.invokeLater(() -> {
            new Server();
        });
    }
}
