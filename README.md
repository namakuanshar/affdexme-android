#Sample App for Affdex SDK for Android

Welcome to our repository on GitHub! Here you will find example code to get you started with our Affdex SDK for Android and begin emotion-enabling you own app!

AffdexMe
--------

*Dependencies*

- Affectiva Android SDK (visit http://www.affectiva.com/solutions/apis-sdks/)

**AffdexMe** is an app that demonstrates the use of the Affectiva Android SDK.  It uses the front-facing camera on your Android device to view, process and analyze live video of your face. Start the app and you will see your own face on the screen and metrics describing your expressions. Tapping the screen will bring up a menu with options to display the Processed Frames Per Second metric, display facial tracking points, and control the rate at which frames are processed by the SDK.

Most of the methods in this file control the application's UI. Therefore, if you are just interested in learning how the Affectiva SDK works, you will find the calls relevant to the use of the SDK in the initializeCameraDetector(), startCamera(), stopCamera(), and onImageResults() methods.

The AffdexMe folder is an Android Studio project.

In order to use this project, you will need to:
- Obtain the Affectiva Android SDK 
- Copy the contents of the SDK's assets folder into this project's assets folder
- Copy the contents of the SDK's libs folder into this project's libs folder
- Copy the armeabi-v7a folder (found in the SDK libs folder) into this project's jniLibs folder
- Add your license file to the /assets/Affdex folder and uncomment the line in the startCamera() method which specifies your license file path
- Build the project
- Run the app on an Android device with a front-facing camera

Copyright (c) 2014 Affectiva. All rights reserved.


See the comment section at the top of the MainActivity.java file for more information.