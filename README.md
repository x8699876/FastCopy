## Fast Copy

* Copies files and directories fast

## Download

- [Downlod the latest release] (https://github.com/mhisoft/fastcopy/releases)

There is a windows executable, batch files and Unix/Linux shell script included to run the application. Unzip and put the files in a directory on the environment path. 

## How to run

If not already, down load and install the latest JRE/JDK 1.7 from Oracle.
Exploded the downloaded zip into a directory and add the directory to the system path. 
Windows: run the fastcopy.exe or fastcopyui.exe
Unix/linux : run the fastcopy.sh, fastcopyui.sh


![screen shot](doc/11-21-2014%2010-10-21%20PM.png "rdproui.exe screenshot")


## Hook to the Windows Explorer Context menu
- Edit the repro_reg.reg file change the path to point to where your rdpro is exploded.
double click to import into windows registry
- Right click on the direcotry you want ot purge, you will see the "Recursive Delete Directory" context menu
- click it to popup the rdpro GUI.

![screen shot](doc/11-22-2014 1-14-12 PM(2).png "Windows exploer context menu")


## Disclaimer
The author is not responsible for any loss of files or damage incurred by running this utility.

## License
Apache License 2.0, January 2004 http://www.apache.org/licenses/
