/*
To get rid of "errors" in the VS Code source presentation, change the .classpath to see the libraries


Note there are some settable parameters located at the SKULL in the right wide scroller.

The change from the standard FRCVISION example project is:

Changes to and creation of the compile the project cmd file:

RPiVisionCompile.cmd

A tasks.json task is added to VS Code to run that command file
====

RaspBerry Pi setup:
RaspBerry Pi setup:
RaspBerry Pi setup:
RaspBerry Pi setup:
RaspBerry Pi setup:
RaspBerry Pi setup:
RaspBerry Pi setup:

Download frcvision image (from some WPI Github repository)
Load image on SD card with balena Etcher [or others]
Add auto mount of our camera image log USB flash drive to /etc/fstab

# USB flash drive mounted for logging
/dev/sda1	/mnt/usb	vfat	auto,users,rw,uid=1000,gid=100,umask=0002,nofail	0	0
Copy of the file fstab is included in this project and can be used.

Make camera image log directory mount point [sudo mkdir /mnt/usb]

Directories for the camera images on the flash drive are automatically made if the flash drive is inserted before our program runs
   [mkdir /mnt/usb/B; mkdir /mnt/usb/BR; mkdir /mnt/usb/E; mkdir /mnt/usb/ER]

Configure cameras [browser frcvision.local/]
The configuration file is then saved in /boot/frc.json
Copy of frc.json included in this project can be used for the Genius camera from ELevator and Microsoft HD-3000 camera for the bumper.

Program starts execution in Main.java - main.java

Threads are spawned (optionally) for
    UdpReceive (test receive data if no roboRIO)
    CameraProcessB (TurretB camera)
    CameraProcessE (IntakeE camera)
    ImageMerge (show picture in picture from the 2 cameras)
--

Some of this project is based on the frc provided example thus:
*/
/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.opencv.core.Core;

import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;

/*
   JSON format:
   {
       "team": <team number>,
       "ntmode": <"client" or "server", "client" if unspecified>
       "cameras": [
           {
               "name": <camera name>
               "path": <path, e.g. "/dev/video0">
               "pixel format": <"MJPEG", "YUYV", etc>   // optional
               "width": <video mode width>              // optional
               "height": <video mode height>            // optional
               "fps": <video mode fps>                  // optional
               "brightness": <percentage brightness>    // optional
               "white balance": <"auto", "hold", value> // optional
               "exposure": <"auto", "hold", value>      // optional
               "properties": [                          // optional
                   {
                       "name": <property name>
                       "value": <property value>
                   }
               ],
               "stream": {                              // optional
                   "properties": [
                       {
                           "name": <stream property name>
                           "value": <stream property value>
                       }
                   ]
               }
           }
       ]
       "switched cameras": [
           {
               "name": <virtual camera name>
               "key": <network table key used for selection>
               // if NT value is a string, it's treated as a name
               // if NT value is a double, it's treated as an integer index
           }
       ]
   }
 */

public final class Main
{
    {
        try
        {
            Thread.sleep(10000);
        }
        catch (InterruptedException ex) 
        { }
    } 

    private static final String pId = new String("[Main]");

    private static String output(InputStream inputStream) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;

        try
        {
            br = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = br.readLine()) != null)
            {
                sb.append(line + System.getProperty("line.separator"));
            }
        }
        finally
        {
            br.close();
        }

        return sb.toString();
    }

    private static String configFile = "/boot/frc.json";

    @SuppressWarnings("MemberName")
    public static class CameraConfig
    {
        public String name;
        public String path;
        public JsonObject config;
        public JsonElement streamConfig;
    }
	
    @SuppressWarnings("MemberName")
    public static class SwitchedCameraConfig
    {
        public String name;
        public String key;
    };

    private static CameraProcessB cpB;
    private static CameraProcessE cpE;
    private static ImageMerge imageDriver;

    private static Thread visionThreadB;
    private static Thread visionThreadE;
    private static Thread imageMergeThread;

    // TODO:
    // all messages go to one UDP sender defined for one port but could have two
    // senders on different ports if that makes it easier to separate the messages
    protected static UdpSend sendMessage;

    private static UdpReceive testUDPreceive; // test UDP receiver in place of a roboRIO
    private static Thread UDPreceiveThread; // remove these or at least don't start this thread if using the roboRIO

    static Images bumperCamera;
    static Images bumperPipeline;
    static Images elevatorCamera;
    static Images elevatorPipeline;
    static AtomicInteger tapeDistance;
    static ShuffleboardTab cameraTab;
    static Object tabLock;

// Settable parameters for some outputs listed below the skull


    // _______________uu$$$$$$$$$$uu______________
    // ____________uu$$$$$$$$$$$$$$$$uu___________
    // __________u$$$$$$$$$$$$$$$$$$$$$u__________
    // _________u$$$$$$$$$$$$$$$$$$$$$$$u_________
    // ________u$$$$$$$$$$$$$$$$$$$$$$$$$u________
    // ________u$$$$$$$$$$$$$$$$$$$$$$$$$u________
    // ________u$$$$$$"___"$$$"___"$$$$$$u________
    // ________"$$$$"______u$u_______$$$$"________
    // _________$$$________u$u_______u$$$_________
    // _________$$$u______u$$$u______u$$$_________
    // __________"$$$$uu$$$___$$$uu$$$$"__________
    // ___________"$$$$$$$"___"$$$$$$$"___________
    // _____________u$$$$$$$u$$$$$$$u_____________
    // ______________u$"$"$"$"$"$"$u______________
    // ___uuu________$$u$_$_$_$_$u$$_______uuu____
    // __u$$$$________$$$$$u$u$u$$$_______u$$$$___
    // ___$$$$$uu______"$$$$$$$$$"_____uu$$$$$$___
    // _u$$$$$$$$$$$uu____"""""____uuuu$$$$$$$$$$_
    // _$$$$"""$$$$$$$$$$uuu___uu$$$$$$$$$"""$$$"_
    // __"""______""$$$$$$$$$$$uu_""$"""__________
    // ____________uuuu_""$$$$$$$$$$uuu___________
    // ___u$$$uuu$$$$$$$$$uu_""$$$$$$$$$$$uuu$$$__
    // ___$$$$$$$$$$""""___________""$$$$$$$$$$$"_
    // ____"$$$$$"______________________""$$$$""__



// Settable parameters for some outputs listed below

    static String version = "2020 RPi Vision 2/3/20";
    static boolean runTestUDPreceiver = false;
    static String UDPreceiverName = "jwoodard-hp16.local";
    // static String UDPreceiverName = "TEAM4237-1.local";
    //static String UDPreceiverName = "RKT-LapTop.local";
    
    //static String UDPreceiverName = "0.0.0.0";
    // "0.0.0.0" should be any computer but doesn't work for other computers - they don't see any packets
    // "roborio-4237-frc.local"
    static boolean runImageMerge = false;
    static boolean debug = false;
    static boolean displayBumperContours = true;
    static boolean displayElevatorContours = true;

// Shuffleboard display video streams commented out for contour images and merged images
// No settable variables here for that
// See the code to uncomment 
/////////////////////////////////////////////////////////////////////////////////////////////////////

    static boolean logImage = false;
    public static int team;
    public static boolean server;
    public static List<CameraConfig> cameraConfigs = new ArrayList<>();
    public static List<SwitchedCameraConfig> switchedCameraConfigs = new ArrayList<>();
    public static List<VideoSource> cameras = new ArrayList<>();
 
    private Main()
    {

    }

    /**
     * Report parse error.
     */
    public static void parseError(String str)
    {
        System.err.println(pId + " config error in '" + configFile + "': " + str);
    }

    /**
     * Read single camera configuration.
     */
    public static boolean readCameraConfig(JsonObject config)
    {
        CameraConfig cam = new CameraConfig();

        // name
        JsonElement nameElement = config.get("name");
        if (nameElement == null) {
            parseError(pId + " could not read camera name");
            return false;
        }
        cam.name = nameElement.getAsString();

        // path
        JsonElement pathElement = config.get("path");
        if (pathElement == null) {
            parseError(pId + " camera '" + cam.name + "': could not read path");
            return false;
        }
        cam.path = pathElement.getAsString();

        // stream properties
        cam.streamConfig = config.get("stream");

        cam.config = config;

        cameraConfigs.add(cam);
        return true;
    }

    /**
     * Read single switched camera configuration.
     */
    public static boolean readSwitchedCameraConfig(JsonObject config)
    {
        SwitchedCameraConfig cam = new SwitchedCameraConfig();

        // name
        JsonElement nameElement = config.get("name");
        if (nameElement == null)
        {
            parseError("could not read switched camera name");
            return false;
        }
        cam.name = nameElement.getAsString();

        // path
        JsonElement keyElement = config.get("key");
        if (keyElement == null)
        {
            parseError("switched camera '" + cam.name + "': could not read key");
            return false;
        }
        cam.key = keyElement.getAsString();

        switchedCameraConfigs.add(cam);
        return true;
    }

    /**
     * Read configuration file.
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public static boolean readConfig()
    {
        // parse file
        JsonElement top;
        try
        {
            top = new JsonParser().parse(Files.newBufferedReader(Paths.get(configFile)));
        }
        catch (IOException ex)
        {
            System.err.println(pId + " could not open '" + configFile + "': " + ex + "\n");
            return false;
        }

        // top level must be an object
        if (!top.isJsonObject())
        {
            parseError(pId + " must be JSON object");
            return false;
        }

        JsonObject obj = top.getAsJsonObject();

        // team number
        JsonElement teamElement = obj.get("team");

        if (teamElement == null)
        {
            parseError(pId + " could not read team number");
            return false;
        }
        team = teamElement.getAsInt();

        // ntmode (optional)
        if (obj.has("ntmode")) 
        {
            String str = obj.get("ntmode").getAsString();
            if ("client".equalsIgnoreCase(str)) 
            {
                server = false;
            } 
            else if ("server".equalsIgnoreCase(str)) 
            {
                server = true;
            } 
            else
            {
                parseError(pId + " could not understand ntmode value '" + str + "'");
            }
        }

        // cameras
        JsonElement camerasElement = obj.get("cameras");
        if (camerasElement == null) 
        {
            parseError(pId + " could not read cameras");
            return false;
        }

        JsonArray cameras = camerasElement.getAsJsonArray();
        for (JsonElement camera : cameras) 
        {
            if (!readCameraConfig(camera.getAsJsonObject())) 
            {
                return false;
            }
        }

        if (obj.has("switched cameras"))
        {
            JsonArray switchedCameras = obj.get("switched cameras").getAsJsonArray();
            for (JsonElement camera : switchedCameras) 
            {
                if (!readSwitchedCameraConfig(camera.getAsJsonObject()))
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Start running the camera.
     */
    public static VideoSource startCamera(CameraConfig config) 
    {
        System.out.println(pId + " Starting camera '" + config.name + "' on path " + config.path);

        // this
        CameraServer inst = CameraServer.getInstance();
        UsbCamera camera = new UsbCamera(config.name, config.path);
        MjpegServer server = inst.startAutomaticCapture(camera);

        // or this and need port to be passed in
        // UsbCamera camera = new UsbCamera(config.name, config.path);
        // MjpegServer server = new MjpegServer("serve_" + config.name, port);
        // server.setSource(camera);

        Gson gson = new GsonBuilder().create();

        camera.setConfigJson(gson.toJson(config.config));
        camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

        if (config.streamConfig != null) 
        {
            server.setConfigJson(gson.toJson(config.streamConfig));
        }

        return camera;
    }

    /**
     * Start running the switched camera.
     */
    public static MjpegServer startSwitchedCamera(SwitchedCameraConfig config)
    {
        System.out.println("Starting switched camera '" + config.name + "' on " + config.key);
        MjpegServer server = CameraServer.getInstance().addSwitchedCamera(config.name);

        NetworkTableInstance.getDefault()
            .getEntry(config.key)
            .addListener(event -> 
            {
                if (event.value.isDouble())
                {
                    int i = (int) event.value.getDouble();
                    if (i >= 0 && i < cameras.size()) 
                    {
                        server.setSource(cameras.get(i));
                    }
                }
                else if (event.value.isString())
                {
                    String str = event.value.getString();
                    for (int i = 0; i < cameraConfigs.size(); i++) 
                    {
                        if (str.equals(cameraConfigs.get(i).name)) 
                        {
                            server.setSource(cameras.get(i));
                            break;
                        }
                    }
                }
            },
            EntryListenerFlags.kImmediate | EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

        return server;
    }

    /**
     * Main.
     */
    public static void main(String... args)
    {
        Thread.currentThread().setName("4237Main");

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        if (args.length > 0) 
        {
            configFile = args[0];
        }

        // read configuration
        if (!readConfig()) 
        {
            System.out.println(pId + " FATAL ERROR - could not read camera configuration file " + configFile);
            return;
        }

        bumperCamera = new Images();
        bumperPipeline = new Images();
        elevatorCamera = new Images();
        elevatorPipeline = new Images();
        tabLock = new Object();
        tapeDistance = new AtomicInteger();

        // // sleep needed on RPi 4 before datagram address resolution and also RPi 3 before mount
        // // and for some other unknown reason after a power on boot up of the RPi 4
        // try {
        //     Thread.sleep(5000);
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }

        sendMessage = new UdpSend(5800, UDPreceiverName);
        
        if(runTestUDPreceiver)
        {
            // start test UDP receiver since we don't have a roboRIO to test with
            // this would go on the roboRIO not here on the RPi
            testUDPreceive = new UdpReceive(5800);
            UDPreceiveThread = new Thread(testUDPreceive, "4237UDPreceive");
            UDPreceiveThread.start();
        }
            
        // start NetworkTables
        NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
        if (server) 
        {
            System.out.println(pId + " Setting up NetworkTables server");
            ntinst.startServer();
        } 
        else 
        {
            System.out.println(pId + " Setting up NetworkTables client for team " + team);
            ntinst.startClientTeam(team);
        }

        // Create the Camera tab on the shuffleboard
        synchronized(tabLock)
        {
            cameraTab = Shuffleboard.getTab("Camera");
        }
 
        // see if USB Flash Drive mounted and if so, log the images
        // try
        // {
        //     System.out.println(pId + " Sleeping 3 seconds so auto mount will be done by now, we are hopeful.");
        //     Thread.sleep(3000);// 3000
        // } catch (InterruptedException exc)
        // {
        //     System.out.println(pId + " Sleep 3 seconds was interrupted");
        // }

        try
        {
            // execute command to check for flash drive mounted
            List<String> command = new ArrayList<String>(); // build my command as a list of strings
            command.add("bash");
            command.add("-c");
            command.add("mountpoint -q /mnt/usb ; echo $?");

            System.out.println(pId + " Run mountpoint /mnt/usb command");
            ProcessBuilder pb1 = new ProcessBuilder(command);
            Process process1 = pb1.start();
            int errCode1 = process1.waitFor();
            command.clear();
            System.out.println(pId + " mountpoint command executed, any errors? " + (errCode1 == 0 ? "No" : "Yes"));
            String mountOutput = output(process1.getInputStream());
            System.out.println(pId + " mountpoint output:\n" + mountOutput);
            System.out.println(pId + " mountpoint errors:\n" + output(process1.getErrorStream()));
            logImage = mountOutput.startsWith("0");
            if (logImage)
            {
                System.out.println(pId + " Flash Drive Mounted /mnt/usb and image logging is on");
                // mkdir in case they don't exist. Don't bother checking for existance - just do
                // it.

                command.add("bash");
                command.add("-c");
                command.add("sudo mkdir /mnt/usb/B /mnt/usb/BR /mnt/usb/E /mnt/usb/ER");

                // execute command
                System.out.println(pId + " Run mkdir B BR E ER command");
                ProcessBuilder pb2 = new ProcessBuilder(command);
                Process process2 = pb2.start();
                int errCode2 = process2.waitFor();
                System.out.println(pId + " mkdir command executed, any errors? " + (errCode2 == 0 ? "No" : "Yes"));
                System.out.println(pId + " mkdir output:\n" + output(process2.getInputStream()));
                System.out.println(pId + " mkdir errors:\n" + output(process2.getErrorStream()));
            }
            else
            {
                System.out.println(pId + " No Flash Drive Mounted");
            }

        }
        catch (Exception ex2)
        {
            System.out.println(pId + " Error in mount process " + ex2);
        }

        System.out.flush();

        // start cameras
        for (CameraConfig config : cameraConfigs) 
        {
            // assume each camera name appears only once in the list - that is a requirement
	        System.out.println(pId + " Checking for TurretB camera");
            if (config.name.equalsIgnoreCase("TurretB"))
            {
                System.out.println(pId + " Starting TurretB camera");
                VideoSource Bcamera = startCamera(config);
                ///////////////////
                // Widget in Shuffleboard Tab
                Map<String, Object> mapBumperCamera = new HashMap<String, Object>();
                mapBumperCamera.put("Show crosshair", false);
                mapBumperCamera.put("Show controls", false);
                   
                synchronized(tabLock)
                {
                    cameraTab.add("TurretB Camera", Bcamera)
                        .withWidget(BuiltInWidgets.kCameraStream)
                        .withPosition(20, 0)
                        .withSize(13, 13)
                        .withProperties(mapBumperCamera);
                }
                //////////////////
                cpB = new CameraProcessB(Bcamera);
                visionThreadB = new Thread(cpB, "4237TurretB Camera");
                visionThreadB.start(); // start thread using the class' run() method (just saying run() won't start a
                // thread - that just runs run() once)
		        continue;
            }

	        System.out.println(pId + " Checking for IntakeE camera");
            if (config.name.equalsIgnoreCase("IntakeE"))
            {
                System.out.println(pId + " Starting IntakeE camera");
                VideoSource Ecamera = startCamera(config);
                ///////////////////
                // Widget in Shuffleboard Tab
                Map<String, Object> mapElevatorCamera = new HashMap<String, Object>();
                mapElevatorCamera.put("Show crosshair", false);
                mapElevatorCamera.put("Show controls", false);
          
                synchronized(tabLock)
                {
                    cameraTab.add("IntakeE Camera", Ecamera)
                        .withWidget(BuiltInWidgets.kCameraStream)
                        .withPosition(0, 0)
                        .withSize(20, 17)
                        .withProperties(mapElevatorCamera);
                }
                //////////////////
                cpE = new CameraProcessE(Ecamera);
                visionThreadE = new Thread(cpE, "4237IntakeECamera");
                visionThreadE.start();
                continue;
	        }

            System.out.println(pId + " Unknown camera in cameraConfigs " + config.name);
        }
        
        Shuffleboard.update();
 
        // start switched cameras
        for (SwitchedCameraConfig config : switchedCameraConfigs) 
        {
            startSwitchedCamera(config);
        }

        if(runImageMerge)
        {
            // start processed images merge and serve thread
            try
            {
                // Wait for other processes to make some images otherwise first time though gets
                // an error
                Thread.sleep(2000);
            }
            catch (InterruptedException ex)
            { }

            imageDriver = new ImageMerge();
            imageMergeThread = new Thread(imageDriver, "4237ImageMerge");
            imageMergeThread.start();
        }
        
        // visionThreadB.setDaemon(true); // defines a sort of "background" task that
        // just keeps running (until all the normal threads have terminated; must set
        // before the ".start"

        // loop forever
        for (;;)
        {
            try 
            {
                System.out.println(pId + " Program Version " + version + "  current time ms " + System.currentTimeMillis());

                // Map<String, String> env = System.getenv(); // or get just one - String myEnv = System.getenv("env_name");
                // for (String envName : env.keySet()) {
                //     System.out.format("%s=%s%n",
                //               envName,
                //               env.get(envName));
                // }

                Thread.sleep(10000);
            } 
            catch (InterruptedException ex) 
            {
                return;
            }
        }
    }
}
