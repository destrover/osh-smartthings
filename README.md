### OSH SmartApps

This repository contains SmartThings applications (SmartApps) for accessing sensors connected to a SmartThings hub through OSH.

#### Installing the SmartApp
Installing the SmartApp requires a Samsung SmartThings account and a SmartThings Hub, though the app can be simulated
through the IDE available on the SmartThings website.

1. Clone the repository into your SmartThings IDE. 
    *  From the *My SmartApps* section of the IDE, select the *Settings* button.
    *  Select *Add new repository*
    *  Under the *Owner*, *Name*, and *Branch* fields, enter "opensensorhub", "osh-smartthings", and the name of the branch
    you wish to pull from.
    * **NOTE:** If you have forked the app, you will need to enter the corresponding information to properly connect your own
    repository.
    
2. Now you can *Update from Repo* and select the correct repository from the dropdown menu.

3. Check the box in the new column (far right) and the *Publish* box in the bottom and then *Execute Update*.

4. The app is now ready to be installed in the SmartThings Android or iOS application. To do this, navigate to the 
*Automation* tab, select *SmartApps* and *Add a SmartApp*

5. At the very bottom of the list, select the *My Apps* option and all of your SmartApps will appear there. Select
*OSH SmartThings Integration. You will then be able to configure the SmartApp.

#### Configuring the SmartApp
Sensors are divided into categories based upon their capabilities. 
* **NOTE:** Sensors with multiple capabilities will
appear in more than one location, you need not select them more than once as the App will add all their supported 
capabilities in one entry once connected to an OSH node, though there is no problem created by doing so.

1. Select the sensors you wish to add to the OSH node.
2. Enter the SOS-T endpoint url you wish to connect to (ex: http://\[testnode]:\[portNo.]/sensorhub/sos)
3. Tap *Done*. The app will now connect to your specified OSH node. As long as the app is installed it will send regular
updates to the node in 1 minute intervals, as well as soon as an active sensor triggers an event. 