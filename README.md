# Custom Pairer for Meross Devices
Hi there! If you came accross this repository it means you are looking for a way to 
take control over your Meross Hardware devices. Well, let me congratulate with you: that's 
exactly what made me start the journey to develop the MerossIot API, Homeassistant component,
Homeassitant addon and finally the Custom Pairer App.

<img src="https://github.com/albertogeniola/meross_pair/blob/main/docs/logos/Custom%20pairer.png"/>

## What's this?
This app, *Custom pairer for Meross Devices* is just a tool that is able to configure Meross 
Devices so that they connect to an arbitrary MQTT broker, rather than the official Meross one.

I probably don't need to explain why that is necessary: that's just the reason you are here 
after all. In any case, let me be crystal clear: if you are looking for an app to pair your 
meross smart devices to the official Meross cloud, then you *MUST* use the original Meross App.
(although this app is capable of doing so). 

You should use this app if trying to achieve one of the following goals:
- You want to pair your Meross Devices with [Homeassistant Meross Lan-Only Addon](https://github.com/albertogeniola/meross-homeassistant)
- You have your own MQTT broker and you know how [Meross protocol works](https://albertogeniola.github.io/MerossIot/meross-protocol.html#client-device-pairing).

# Call for testers!
The app is now accepting alpha/beta testers: if you are interested into that, please [visit
this link](https://play.google.com/apps/testing/com.albertogeniola.merossconf) or ask to 
join [this Google Group](https://groups.google.com/g/meross_pairer_testers) meross_pairer_testers@googlegroups.com.

## Donate!
If you liked or appreciated by work, why don't you buy me a beer?
It would really motivate me to continue working on this repository to improve documentation, code and extend the supported meross devices.

By issuing a donation, you will:
1. Give me the opportunity to buy new hardware (can you imagine how much ram you need when running 2 IntelliJ instances, a couple of Docker containers, an 8Gb VM, PyCharm and Chrome togerher? 
1. Pay for licensed development tools 
(Note that they are used for Unit-Testing on the continuous integration engine when someone pushes a PR... I love DEVOPing!)  
1. You'll increase the quality of my coding sessions with free-beer!

[![Buy me a coffee!](https://www.buymeacoffee.com/assets/img/custom_images/black_img.png)](https://www.buymeacoffee.com/albertogeniola)

[![Apply for GitHub sponsorship](https://korlibs.soywiz.com/i/github_sponsors_big_box_small.png)](https://github.com/sponsors/albertogeniola)

## Disclaimer
This app has been developed for free and for fun. The developer is in no way affiliate with Meross
and Meross is in no way responsible for this product. This app comes with absolutely no warranty:
use it at your own risk. The developer declines any responsibility derived by the usage of this 
app.
