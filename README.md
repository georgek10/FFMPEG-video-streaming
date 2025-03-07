# FFMPEG-Video-Streaming
A 2-way communication between a Server and a Client focusing on streaming video files using the FFMPEG tool and Java wrapper.

- **Streaming Server** is responsible for creating a full list of video files and establishing a connection with the Client to wait for streaming requests.
- **Streaming Client** can request a specific format type and resolution of a video file to stream after achieving connection with the Server.

# Specifications to run
- The 'Project' can be opened in any IDE. Run the 'Server' and 'Client' files to start the process (more details for each in the next section).
- It is required that the FFMPEG tool is properly installed on the user's operating system.
- The 'videos' folder contains example sample files for a test run. Users can add their own video files in it with the following rules:
  1. Files must have supported format types (avi, mkv, mp4) and resolutions (240p, 360p, 480p, 720p, 1080p).
  2. Alter the file name to match the real format type and resolution, following the format 'file_name-resolutionp', e.g. 'video-720p'.
  Frequent checks are performed and the user is prompted through the apps to follow the necessary steps.
- It is assumed that the user does not make any external changes to the 'videos' folder, such as adding, deleting or renaming files.
- The download test link on the Client side has to be active in order to estimate the download speed.

# Features
## Streaming Server
The user interface of the Server has the following buttons:
- **Manual** that provides general information to run the app.
- **Files** for a list of present files in 'videos' folder.
- **Info** for a list of present format types and their resolutions for each video file.
- **Check** for a list of missing format types and their resolutions for each video file.
- **Create** **must** be pressed to create all the missing video files **before** starting the connection. There is a short waiting time and the user is prompted upon completion.
- **Start connection** to finally establish the connection and wait for the Client's request.

## Streaming Client
The user interface of the Client has the following buttons:
- **Manual** that provides general information to run the app.
- **Connect** to establish connection with the server. The Server **must** have enabled its connection first.
Execute these steps to be able to stream a video file:
1. Press the **Download Test** button to start a test of 5 seconds to estimate the download speed.
2. Select a **Format type** from the drop-down list.
3. Press the **Send** button to send the data to the Server, in order to receive the video list based on the sent data.
4. Select a video file to stream from the drop-down list of the returned files.
5. Select a protocol from the drop-down list. If none is selected, it is automatically filled based on the resolution of the selected file.
6. Press the **Stream** button to begin the video transmission. 
