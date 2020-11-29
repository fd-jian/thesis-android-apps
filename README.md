# Project THRILLER Android Project

This is the Android application for the project THRILLER. This application is currently supporting Android applications for Smartwatch and Smartphone.

## Features

The application is collecting sensor data from the Smartwatch and smartphone and sending it via MQTT to an external infrastructure. Furthermore, experience sampling is done on the Smartwatch to collect annotations for the sensor data and conduct surveys.  

# Bugs

## Gradle Build failing with java.lang.IllegalStateException: Expected BEGIN_ARRAY but was ...

This is an exception thrown by the Gradle plugin. The solution might be to update the gradle wrapper properties to use an updated version, i.e., ./gradle/wrapper/gradle-wrapper.properties.

# Miscellaneous

## How to handle Android SDK Versions

https://medium.com/androiddevelopers/picking-your-compilesdkversion-minsdkversion-targetsdkversion-a098a0341ebd

# Credentials

## Regular User Access
user:michael2020

## ESM Admin Access
root:jackson2020

# Testing Battery Consumption
- A battery report for both wear and phone has been created and was [uploaded to the wiki](https://gitlab.tba-hosting.de/edutec/thriller/android-application/-/wikis/Battery-Consumption-Report-(2020-11-25))
