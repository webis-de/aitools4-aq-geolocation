AItools 4 - Acquisition - Geolocation
=====================================

Library and program for the historical geolocalization of IP addresses.


The program allows you to geolocate IP addresses for specific time instants in the past. It incorporates data from the different regional Internet registries (RIRs) as well as from IPlocation database files (you can get a current IPlocation file from https://lite.ip2location.com/database-ip-country-region-city-latitude-longitude-zipcode-timezone ). More IPlocation databases will improve the reliability of the geolocalization, but the program will in any case only output geolocations if it is somewhat sure about it. However, you will need older IPlocation databases when you want to geolocate addresses at older times. This site or product includes IP2Location LITE data available from <a href="http://lite.ip2location.com">http://lite.ip2location.com</a>.


Provided time data version: April 2016

Requirements
------------
  - Java 8
  - You need to have the following libraries in your Java class path:
    - Geotools 15.1  http://www.geotools.org/
    - Trove 3.0.0:   http://trove.starlight-systems.com/
  - Github: The provided jar (in lib/) contains these libraries, so you can just put the jar in your class path

Setup
-----
  - Update the following files if you want to geolocate IP addresses after the time data version listed at the top of this document (not necessary otherwise):
      - /resources/de/aitools/aq/geolocating/timezones/zone.tab and backward from the IANA Time Zone Database: http://www.iana.org/time-zones
      - /resources/de/aitools/aq/geolocating/timezones/tz_world.* from http://efele.net/maps/tz/world/
  - You also might have to update the time zone database of your Java VM (if you get errors that some time zone is unknown)
      - Get the Java Time Zone Updater Tool from http://www.oracle.com/technetwork/java/javase/downloads/index.html
      - After uncompressing, it should suffice to do:
      
            sudo java -jar tzupdater.jar -l

      - If not, see the readme accompanying the tool
  - Process IPlocation databases.
      - Put all your IPlocation database CSV files in one directory (we here use "data/iplocation")
      - The files may have to be renamed (file format is detected by file name). The format is displayed when you run with your classpath:

            java -Xmx8G -cp <classpath> de.aitools.aq.geolocating.iplocations.IplocationIpBlocks

      - After renaming, run with your classpath:

            java -Xmx8G -cp <classpath> de.aitools.aq.geolocating.iplocations.IplocationIpBlocks data/iplocation data/iplocation-parsed

  - Update RIR database if you want to geolocate IP addresses after the time data version listed at the top of this document (not necessary otherwise):
      - Put all RIR registry files in a directory structure starting at "data/rir" (they are called something like delegated-.*-<date>)
      - Yes, you need all such registry files ever, as each file only contains the last assignment of an IP. You might also want to ask johannes.kiesel@uni-weimar.de for a more up-to-date version.
      - Run with your classpath:

            java -Xmx8G -cp <classpath> de.aitools.aq.geolocating.rir.RirIpBlocks data/rir data/rir-parsed

Quickstart
----------
  - Run with your <classpath>:

        java -Xmx8G -cp <classpath> de.aitools.aq.geolocating.Geolocator data/iplocation-parsed data/rir-parsed <input> <time-format> <output>

    Where:
      - input is a file containing the IPv4 addresses and times for the historical geolocation. One address per line:
      
            <address>[TAB]<time>

      - time-format is the format of the <time> field in the input file. The format needs to be specified for Java SimpleDateFormat: http://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
      - output is the file where the output should be written to. Output format is either (on successful geolocalization)

            <address>[TAB]<time>[TAB]<time-zone>[TAB]<country-code>

        or (on failed geolocalization)

            <address>[TAB]<time>

        and can be deserialized again using de.aitools.aq.geolocating.Geolocalization#parse(InputStream)

  - You can test if everything works using

            java -Xmx8G -cp <classpath> de.aitools.aq.geolocating.Geolocator data/iplocation-parsed data/rir-parsed example.txt "YYYY-MM-dd'T'HH:mm:ss" example-geolocated.txt

    The output should be something like this (written to standard output, in this case it shows that it decided three times for "true" (i.e.: valid geolocalization) as in all three cases there was information from RIR and Iplocation, and this information was not inconsistent but time zone consistent):

           Decisions:
           3
           RIR = true	3
             IPlocation = true	3
               inconsistent = true	0	false
               inconsistent = false	3
                 time zone consistent = true	3	true
                 time zone consistent = false	0
                   locally time zone consistent = true	0	true
                   locally time zone consistent = false	0	false
             IPlocation = false	0
               1 time zone = true	0	true
               1 time zone = false	0	false
           RIR = false	0	false

    And this (written to example-geolocated.txt):

           70.19.29.244	2016-01-04T07:42:27Z	America/New_York	US
           31.121.85.30	2016-01-04T07:29:15Z	Europe/London	GB
           86.23.18.214	2014-12-29T14:36:33Z	Europe/London	GB

    Where the third column gives the Olson time zone and the fourth column gives the country code. Third and fourth column will be missing if not enough or conflicting geolocation information is available.

