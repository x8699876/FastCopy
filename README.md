## FastCopy

* A very handy tool for copying or backup a large set of directories and files, recursively, i.e. all the sub directories under will be copied over to the target. Support all the hidden and system files, long file names, file names of all languages. 
* Fast! Use multiple workers to copy from/to the SSD drives. 
* Support mounted external drives on Mac.
* Drive speed tester/Benchmark tool.
* Cross platform support: Windows, MacOS, Unix/Linux where JRE is supported. 


## Download

- Downlod the latest release: https://github.com/mhisoft/fastcopy/releases

## Instructions

* Requires java JDK. If not already availabe on your system, download and install the JDK 1.8+ from Oracle.
https://www.oracle.com/technetwork/java/javase/overview/index.html

* Windows: 
  Exploded the downloaded zip into a c:\bin\fastcopy and add the directory to the system path. </br>
  Run the fc.bat, fc.exe or  fastcopy-console.exe for the UI version. </br>
  Also see below on how to integrate with the windows explorer. </br>

* Mac OS: 
  Exploded the downloaded zip to ~/bin/fastcopy</br>
  The "Fastcopy(1.2).app" is the MacOS app,  copy it to the ~/Applications and run.</br>
  For the first time, it might alert saying the app can't be opened because it is from a untrusted source. Go to the Finder, right click to bring up the context menu and click "Open" from there. You will have the option to open it there after. <br>
  Also see below on how to add as service to the context menu in the Finder.</br> 


* Unix/linux, Mac command line : run the ./fc.sh

ex:

```
    
$ ./fc.sh -help
MHISoft FastCopy (v1.3, build Dec 2018)
(https://github.com/mhisoft/fastcopy)
Usages:
         fc [option] -from source_dir -to target_dir
         source-dir: The source files and directories delimited with semicolon.
         target-dir: The target directory.
         Options:
                 -v      verbose mode.
                 -verify verify each file copy by comparing the file content hash.
                 -m      use multiple threads, best for copying across the SSD drives.
                 -w      number of worker threads in the multi threads mode, default:2.
                 -o      always override.
                 -n      override only when the source file newer or different in size.
                 -f      flat copy, copy everything to the same target directory.
                 -pack   Package the small files first to speed up the copy, requires write access on the source folder or drive.
                 -k      Keep the original file timestamp.
                 -sf     Create the same source folder under the target and copies to it.
Examples:
                 copy from current dir to the backup directory: fastcopy t:\backup
                 fastcopy -from s:\projects\dir1;s:\projects\dir2 -to t:\backup
   
```



## GUI version:

![Screenshot](/doc/fastcopy-v1.2.6-screenshot-2018-11-19_23-31-58.png "screenshot")


* The source can be a combination of multiple directories and files delimited by the semicolon ";". You can either use the browse button to choose directories or copy paste directly into the input box. The file choose supports multiple directories selections.
* The target should always be one directory or drive. 
* Check the SSD deivers to leverage multiple works for copying files simuteneously.  usually it should speed things up if there no other bottlenecks on such as the network or calbe, ports.
* The "override only if newer or a size difference is detected" is handy to when you need to stop and resume a copy task. say you stop a copying task in the middle , when you resume it by running again , it will skip the already copied files on the target directory and copies only the not yet copied over files. 
* "Create the same source folder..." option, for example: 
source is "/Users/myhome/Repository", target is "/Volumes/externaldrive/backup", with this option checked, a "Repository" diorectory will be created under /Volumes/externaldrive/backup and the contents under the source will be copied to /Volumes/externaldrive/backup/Repository



## Hook to the Windows Explorer Context menu
- Edit the fastcopy_reg.reg file change the path to point to where your rdpro is exploded.
double click to import into windows registry


## Add to the Mac Finder's Context menu 
* Since this approach launches the java fromt he shell scripts, it will require the full java SDK to be installed on the Mac. 
* Open Automator, Files menu --> "new"
* On the "Choose a type for your document" prompt, select "Service"; "quick actions" for moJave.
* On the left in the search bar, type in "Run Shell Scripts" and select it. 
* For "Services receives selected", choose "Folders", in "Finder". Edit the path to where you installed the jar if needed. 
* for the "Pass input",  select "as arguments"
* Save as "Fast Copy"
 
 ![screen shot](doc/fastcopy-automator-setup2.png "Add to the Mac Finder's Context menu")
 ![screen shot](doc/fastcopy%20context%20menu.png "Context Men->Services")


## Disclaimer
The author is not responsible for any loss of files or damage incurred by running this utility.

## License
Apache License 2.0, January 2004 http://www.apache.org/licenses/
