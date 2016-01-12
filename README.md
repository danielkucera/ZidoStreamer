# ZidoStreamer
Android app for live streaming of HDMI input on Zidoo X9 and Tronsmart Pavo M9 compatible TV boxes. 
More info: https://blog.danman.eu/using-tronsmart-pavo-m9-for-hdmi-input-streaming/

## Installation
Just copy signed.apk to your device and install (click on) it.

## Compilation
Open in android studio and compile as usual. After compiling, run sign.sh because it needs to signed by MStar key to gain access to required hardware.

## Features
* streaming as MPEG-TS to network (unicast/multicast)
* streaming in FLV format to RTMP server (e.g. Youtube)
* no need for intermediate recording file - thus no length limit 
* streaming runs in background

## TODO
* start after android boot
* settings menu to edit stream target, bitrate, startup settings, ...
* bind remote control to start/stop/restart streaming
* automatic stream restart after network or encoding failure
* indicate recording with blinking status LED
