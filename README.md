# A video processing and transmitting application using a Client – Server model

The purpose of this project is to create an application capable of processing and transmitting video(streaming) using a Client – Server model. 

For this application we have used the programming language Java with the JavaFX library for the user interfaces and the ffmpeg-cli-wrapper for java which is a java wrapper around the FFmpeg command line tool.

The application supports three types of formats: 
  
  * Audio Video Interleave - **AVI**
  
  * **MP4**
  
  * Matroska - **MKV**
  
It also supports three types of streaming protocols:

  * Transmission Control Protocol - **TCP**
  
  * User Datagram Protocol - **UDP**
  
  * Real-time Transport Protocol - **RTP**
  
And finally the supported resolutions are:
  * 1920 x **1080**
    
  * 1280 x **720**
    
  * 854 x **480**
    
  * 640 x **360**
    
  * 320 x **240**
    
Future improvements can be easily made such as adding more resolutions, formats and protocols.    
We could also improve our player, instead of opening up a new window with the video streaming 
we could integrate it to our own type of player which will have capabilities of moving to a particular time on the video, 
volume control and even a start/stop functionality. With this type of improvement we could hopefully make it have better UI and functionality.

