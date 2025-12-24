## FastCopy

A very handy tool for copying or backing up large sets of directories and files recursively—that is, all subdirectories are copied to the target. It supports hidden and system files, long file names, and file names in all languages.

**Fast performance**: Uses multiple workers to copy data to and from SSD drives efficiently.

**External drive support**: Works with mounted external drives on macOS.

**Drive speed tester / benchmark tool**:  
FastCopy reports real-world file transfer speeds across different file size groups. You’ll notice that transfer speeds for large files and small files can differ significantly. When used together with synthetic benchmark tools such as CrystalDiskMark and Anvil’s Storage Utilities, it provides a more complete picture of your system’s storage performance.

**Cross-platform support**: Available on Windows, macOS, and Unix/Linux systems where a JRE is supported.



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


- **Source**: The source can be a combination of multiple directories and files, delimited by a semicolon (`;`). You can use the **Browse** button to select directories, or copy and paste paths directly into the input box. The file chooser supports selecting multiple directories.

- **Target**: The target must always be a single directory or drive.

- **Multi-workers**: Enable multiple workers for SSD drives to copy files simultaneously. This usually improves performance, as long as there are no other bottlenecks such as network speed, cables, or port limitations.

- **“Override only if newer or size differs”**: This option is useful when you need to stop and resume a copy task. For example, if a copy operation is interrupted and later resumed, files that were already copied will be skipped, and only files that are new or incomplete will be copied to the target directory.

- **“Create the same source folder…” option**:  
  **Example:**  
  - Source: `/Users/myhome/Repository`  
  - Target: `/Volumes/externaldrive/backup`  

  When this option is enabled, a `Repository` directory will be created under `/Volumes/externaldrive/backup`, and the source contents will be copied to:  
  `/Volumes/externaldrive/backup/Repository`

- **Package small files first**: This option speeds up the overall copy process when copying a large number of small files. Small files are zipped together first and then copied as a single package. This greatly improves performance when copying over a network or to externally attached USB drives, where copying many small files one by one is typically very slow regardless of the software used.



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
