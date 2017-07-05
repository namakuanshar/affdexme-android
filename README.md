**AffdexMe** is an Android app that demonstrates the use of the Affdex SDK.  It uses the camera on your Android device to view, process and analyze live video of your face. Start the app and you will see your face on the screen and metrics describing your expressions. Tapping the screen will bring up a menu with options to display the Processed Frames Per Second metric, display facial tracking points, and control the rate at which frames are processed by the SDK.

To use this project, you will need to:
- Build the project using Android Studio
- Run the app and smile!
- Please dont anger with me!

If you are interested in learning how the Affectiva SDK works, you will find the calls relevant to the use of the SDK in the initializeCameraDetector(), startCamera(), stopCamera(), and onImageResults() methods.  See the comment section at the top of the MainActivity.java file for more information.

This is a-edited version of affdexMe that speasially detect a anger emotion of someone and automatically play a alarm sound if the anger score is more than 85%

**Fitur Added or remove**
+ add tornado_sound in raw file
+ change versioncode and versionname to miss the error
+ change to "comentar" off affdexme Logo
+ add AIMP String, and change the loading_string 

**Coming Soon**
+ find the detect_anger_score parameter
+ build play_sound function
+ start play_sound when anger in >85%
+ make a toast notification when user is anger
+ stop play_sound when anger in <85%

***
Copyright (c) 2016 Affectiva Inc. <br> See the file [license.txt](license.txt) for copying permission.

This app uses some of the excellent [Emoji One emojis](http://emojione.com).

Edited by : namakuanshar - Solo Fighter

