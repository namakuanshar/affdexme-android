[![Build Status](https://travis-ci.org/Affectiva/affdexme-android.svg)](https://travis-ci.org/Affectiva/affdexme-android)
![Affectiva Logo](http://developer.affectiva.com/images/logo.png)

###Copyright (c) 2016 Affectiva Inc. <br> See the file [license.txt](license.txt) for copying permission.

*****************************

**AffdexMe** is an app that demonstrates the use of the Affectiva Android SDK.  It uses the camera on your Android device to view, process and analyze live video of your face. Start the app and you will see your face on the screen and metrics describing your expressions. Tapping the screen will bring up a menu with options to display the Processed Frames Per Second metric, display facial tracking points, and control the rate at which frames are processed by the SDK.

Most of the methods in this file control the application's UI. Therefore, if you are just interested in learning how the Affectiva SDK works, you will find the calls relevant to the use of the SDK in the initializeCameraDetector(), startCamera(), stopCamera(), and onImageResults() methods.

This is an Android Studio project.

In order to use this project, you will need to:
- Obtain the Affectiva Android SDK (visit http://www.affectiva.com/solutions/apis-sdks/)
- Copy the contents of the SDK's assets folder into this project's app/src/main/assets folder
- Copy the contents of the SDK's libs folder into this project's app/libs folder
- Copy the armeabi-v7a folder (found in the SDK libs folder) into this project's app/jniLibs folder
- Copy your license file to this project's app/src/main/assets/Affdex folder and rename to license.txt
- Build the project
- Run the app and smile!

See the comment section at the top of the MainActivity.java file for more information.

***

This app uses some of the excellent [Emoji One emojis](http://emojione.com).
