# ZidoStreamer
Android app for live streaming of HDMI input on Zidoo X9 and Tronsmart Pavo M9 compatible TV boxes. 
More info: https://blog.danman.eu/using-tronsmart-pavo-m9-for-hdmi-input-streaming/

## Installation
* Copy signed.apk to your device and install it.
* Get ffmpeg binary from https://github.com/WritingMinds/ffmpeg-android
* Copy ffmpeg binary to /mnt/sdcard/ 

## Running
* Start app from menu
* Press menu button and edit and confirm all settings
* (optional) Install Startup Manager from Google Play and set ZidoStreamer to start after boot
* Restart your device

## Configuration
* Valid ffmpeg commands:
* streaming to network in MPEG-TS:
<pre>
/mnt/sdcard/ffmpeg -i - -codec:v copy -codec:a copy -bsf:v dump_extra -f mpegts udp://[IP]:1234
</pre>
* streaming to web (e.g. Youtube):
<pre>
/mnt/sdcard/ffmpeg  -i - -strict -2 -codec:v copy -codec:a aac -b:a 128k -f flv rtmp://a.rtmp.youtube.com/live2/[Stream name/key]
</pre>

## Compilation
Open in android studio and compile as usual. After compiling, run sign.sh because it needs to signed by MStar key to gain access to required hardware.

## Features
* streaming as MPEG-TS to network (unicast/multicast)
* streaming in FLV format to RTMP server (e.g. Youtube)
* no need for intermediate recording file - thus no length limit 
* streaming runs in background

## TODO
* bind remote control to start/stop/restart streaming
* automatic stream restart after network or encoding failure
* indicate recording with blinking status LED

## Output screenshot

![ScreenShot](youtube-screenshot.png)
