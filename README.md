# Project THRILLER Android Project

This is the Android application for the project THRILLER. This application is currently supporting Android applications for Smartwatch and Smartphone.

## Features

The application is collecting sensor data from the Smartwatch and smartphone and sending it via MQTT to an external infrastructure. Furthermore, experience sampling is done on the Smartwatch to collect annotations for the sensor data and conduct surveys.  


## Installierung mittels APK-Datei
Die Installation der APK Datein kann über WLAN oder USB erfolgen. Dabei wird das "adb Tool" benötigt und die jeweiligen APK Dateien.

### APK Dateien generieren
 Im Android Studio findet man unter dem Reiter "Build" eine Sektion "Build Bundle(s)/Apk(s)". Dort lässt sich das bauen der APK's automatisiert durchführen. Die jeweiligen APK Dateien befinden sich dann in den Verzeichnissen:

> experience-sampling-service/mobile/build/outputs/apk/debug
> experience-sampling-service/wear/build/outputs/apk/debug

### Installierung ADB-TOOL
Anleitungen zur Installation findet man für [Linux](https://wiki.ubuntuusers.de/adb/) und [Windows/Mac](https://forum.xda-developers.com/showthread.php?t=2588979) unter den gegeben Links. Diese Anleitung ist nur für Linux getestet worden.


### Voreinstellungen am Endgerät
Das Endgerät muss in die Entwickleroptionen freigeschaltet haben. Je nach gewünschter Installierungsart muss das Debugging über USB oder WLAN eingeschaltet sein. Die Entwickleroptionen lassen sich über folgendes freischalten:
*WEAR*: Einstellungen -> Über das Gerät -> mehrmaliges Tippen auf Buildnummer
*PHONE*: Einstellungen -> Telefoninfo -> Softwareinformationen -> mehrmaliges Tippen auf Buildnummer

### APK Installierung über USB
Um es so einfach wie möglich zu halten empfiehlt sich die Geräte nach einander am Computer anzuschließen. Ist ein Gerät verbunden kann man mittels dem Befehl `adb devices` die derzeitig verbundenen Geräte sichtbar machen. Mit dem Befehl `adb push <pfad zur apk> /sdcard/` die apk auf das Gerät bringen. Mit dem weiterem Befehl `adb install <pfad zur apk>` die apk installieren. Dies funktioniert sowohl für das Smartphone als auch für die Smartwatch.

### APK Installierung über WLAN

Zunächst muss man die adb Shell mit dem Endgerät verbinden. Die benötigten IP's findet man in den Endgeräten wie folgt:
*WEAR*: Einstellungen -> Konnektivität -> WLAN -> "WLAN-Name"
*PHONE*: Einstellungen -> Netzwerk/Internet -> WLAN -> "WLAN-Name" -> Erweitert

Mittels dem Befehl `adb connect IP:5555` verbindet man die Shell mit dem Endgerät. Anschließend bringt der Befehl `adb -e push <pfad zur apk> /sdcard/` die apk auf das Endgerät. Der Befehl `adb -e install <pfad zur apk>` installiert diese. Anschließend trennt man die Verbindung mit `adb disconnect`.

## Technical Overview

### Sensor Recording

#### Interactions with the Backend

The following sequence diagram provides a general overview of the interaction between the phone application and the learning analytics backend during a sensor recording session. Communication within the backend is included to gain an abstract overview of the 

The internals of the backend communication are included as well to gain a simplified overview of the internals of a recording session. The wearable application is excluded and simplified in the actor "Student". Interaction with the [Experience Sampling Backend](../../../../experience-sampling-service-backend) is not included.

![Sensor Recording Session: Component Interaction](../../../wikis/uploads/1f30513839560ef1df56e1c7417812bf/sensor_session.png)

#### Interactions between Phone and Wearable
The following two sequence diagrams visualizes interactions between wearable and phone application. Devices communicate through Google's Weararble Data Layer API. The first diagram depicts the inner workings of the wearable device, the second diagram those of the phone. `Wearable API` is an abstraction of Google's Wearable Data Layer API to simplify how device communication is establisehd on each side.

##### Wearable Communication
![mobile_wear](../../../wikis/uploads/4517f1eb476b314027b4df0936b4918a/mobile_wear.png)

##### Phone Communication
![mobile_phone](../../../wikis/uploads/3d9ce475b71732814242a13425826390/mobile_phone.png)

#### Authorization and Account Management
Android's account manager API is used to store login credentials and manage accounts for the MQTT connection. The following diagram visualizes an abstraction of the account management mechanisms.

![account_manager](../../../wikis/uploads/68762f1a7065e7201bec065215108a1c/account_manager.png)

## Bugs

### Gradle Build failing with java.lang.IllegalStateException: Expected BEGIN_ARRAY but was ...

This is an exception thrown by the Gradle plugin. The solution might be to update the gradle wrapper properties to use an updated version, i.e., ./gradle/wrapper/gradle-wrapper.properties.

## Miscellaneous

### How to handle Android SDK Versions

https://medium.com/androiddevelopers/picking-your-compilesdkversion-minsdkversion-targetsdkversion-a098a0341ebd

## Testing Battery Consumption
- A battery report for both wear and phone has been created and was [uploaded to the wiki](https://gitlab.tba-hosting.de/edutec/thriller/android-application/-/wikis/Battery-Consumption-Report-(2020-11-25))
