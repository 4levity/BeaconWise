BeaconWise
==========

Wearable technology can make invisible things visible. If someone is trying to use ultrasound
beacons to track people where you are, you could be alerted via wearables (lights, sounds,
vibration). LEDs would be good as you could get various information about the beacons at a
glance.

A cell phone can detect all kinds of things like this. Tiny RFduino attached to wearable can
receive data from phone/laptop, and represent visually (etc). Could also alert user to RF
signals/wifi, bluetooth/NFC beacons, any imaginable data from geo APIs, etc.


Current Functionality
=====================
This proof-of-concept uses an Android phone to detect ultrasound
that might indicate a tracking beacon. It sends BLE signals to 
a "wearable" device with two indicator lights. One of the lights
indicates that there is generally a high amount of ultrasound.
A second light indicates that a specific signal was recieved on
two specific preprogrammed frequencies.

More / Future Ideas
===================

More analysis of real tracking protocols like Lisnr, Chirp,io etc.
would provide the ability to more accurately identify real beacons
or even analyze their content.

Add support for BLE, NFC beacons.

Alex Glo suggested that API on crime data could alert you via wearable if you're entering for example an area
with higher crime.

Matt Bellis pointed out that if we wanted to, we could detect ultrasound signals using simple
analog circuits which would be much smaller and use a lot less power than doing fast Fourier
transforms on digital data - so that ultrasound beacon detectors could be made very small and low
power - built into the wearable instead of connected to another device over Bluetooth Low Energy.
But now I don't really know how to do that except in theory, so I'm using software!

Inspiration and Credits
=======================

Using Ultrasonic Beacons to Track Users
https://www.schneier.com/blog/archives/2017/05/using_ultrasoni.html

Sample Android App for RFduino (used some code Copyright 2013 Lann under MIT license)
https://github.com/lann/RFDuinoTest
