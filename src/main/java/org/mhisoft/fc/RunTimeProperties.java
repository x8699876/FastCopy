package org.mhisoft.fc;

/**
 * Run time properties
 */
public class RunTimeProperties  implements  java.io.Serializable{

	public static final int DEFAULT_THREAD_NUM = 2;
	public static final int DEFAULT_PACKAGE_SMALL_FILES_THREAD_NUM = 5;
	public static String userHome = System.getProperty("user.home") ;
	public static String zip_prefix ="_fastcopy_auto_create_";

	private static boolean debug = Boolean.getBoolean("debug");
	private static boolean stopThreads = false;

	public static RunTimeProperties instance = new RunTimeProperties();



	private transient  boolean running;

	private transient LogLevel logLevel=LogLevel.info;;



	private RunTimeProperties() {
		//
	}


	String sourceDir = null;
	transient String destDir = null;
	transient boolean success;

	transient boolean debugArg;

   //the below will be saved along with the  UserPreferences
    boolean verifyAfterCopy = Boolean.valueOf(System.getProperty("verify", "false"));
    boolean verbose; //show info, details
	int numOfThreads=1;
	boolean overrideTarget;
	boolean overwriteIfNewerOrDifferent;
	boolean flatCopy;
	boolean createTheSameSourceFolderUnderTarget;
	boolean keepOriginalFileDates = Boolean.valueOf(System.getProperty("keepOriginalFileDates", "false"));;
	boolean skipEmptyDirs=Boolean.valueOf(System.getProperty("skipEmptyDirs", "true"));;;
	boolean packageSmallFiles = Boolean.valueOf(System.getProperty("packageSmallFiles", "true"));



	public  boolean isStopThreads() {
		return stopThreads;
	}


	public  void setStopThreads(boolean v) {
		stopThreads = v;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}



	public String getSourceDir() {
		return sourceDir;
	}

	public void setSourceDir(String sourceDir) {
		this.sourceDir = sourceDir;
	}

	public String getDestDir() {
		return destDir;
	}

	public void setDestDir(String destDir) {
		this.destDir = destDir;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public int getNumOfThreads() {
		return numOfThreads;
	}


	public  boolean isPackageSmallFiles() {
		return packageSmallFiles;
	}

	public void setPackageSmallFiles(boolean packageSmallFiles) {
		this.packageSmallFiles = packageSmallFiles;
	}

	public int getNumberOfThreadsForPackageSmallFiles() {
		return isPackageSmallFiles()?  DEFAULT_PACKAGE_SMALL_FILES_THREAD_NUM : 1;
	}

	public void setNumOfThreads(int numOfThreads) {
		this.numOfThreads = numOfThreads;
	}

	public boolean isOverrideTarget() {
		return overrideTarget;
	}

	public void setOverrideTarget(boolean overrideTarget) {
		this.overrideTarget = overrideTarget;
	}

	public boolean isFlatCopy() {
		return flatCopy;
	}

	public void setFlatCopy(boolean flatCopy) {
		this.flatCopy = flatCopy;
	}

	public boolean isOverwriteIfNewerOrDifferent() {
		return overwriteIfNewerOrDifferent;
	}

	public void setOverwriteIfNewerOrDifferent(boolean overwriteIfNewerOrDifferent) {
		this.overwriteIfNewerOrDifferent = overwriteIfNewerOrDifferent;
	}

	public boolean isDebug() {
		return debugArg || debug;
	}

	public void setDebug(boolean debugArg) {
		this.debugArg = debugArg;
	}

	public boolean isCreateTheSameSourceFolderUnderTarget() {
		return createTheSameSourceFolderUnderTarget;
	}

	public void setCreateTheSameSourceFolderUnderTarget(boolean createTheSameSourceFolderUnderTarget) {
		this.createTheSameSourceFolderUnderTarget = createTheSameSourceFolderUnderTarget;
	}

	public  void setVerifyAfterCopy(boolean verifyAfterCopy) {
		this.verifyAfterCopy = verifyAfterCopy;
	}


	public  boolean isVerifyAfterCopy() {
		return verifyAfterCopy;
	}

	public boolean isKeepOriginalFileDates() {
		return keepOriginalFileDates;
	}

	public void setKeepOriginalFileDates(boolean keepOriginalFileDates) {
		this.keepOriginalFileDates = keepOriginalFileDates;
	}

	public LogLevel getLogLevel() {
		if (logLevel==null && System.getProperty("logLevel")!=null) {
			logLevel = LogLevel.valueOf(System.getProperty("logLevel"));
		}

		return logLevel;
	}

	public void setLogLevel(LogLevel logLevel) {
		this.logLevel = logLevel;
	}

	public boolean isSkipEmptyDirs() {
		return skipEmptyDirs;
	}

	public void setSkipEmptyDirs(boolean skipEmptyDirs) {
		this.skipEmptyDirs = skipEmptyDirs;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("RunTimeProperties{");
		sb.append("sourceDir='").append(sourceDir).append('\'');
		sb.append(", destDir='").append(destDir).append('\'');
		sb.append(", verbose=").append(verbose);
		sb.append(", numOfThreads=").append(numOfThreads);
		sb.append(", overwrite=").append(overrideTarget);
		sb.append(", overwriteIfNewerOrDifferent=").append(overwriteIfNewerOrDifferent);
		sb.append(", createTheSameSourceFolderUnderTarget=").append(createTheSameSourceFolderUnderTarget);
		sb.append(", flatCopy=").append(flatCopy);
		sb.append(", debugArg=").append(debugArg);
		sb.append(", compressSmallFiles=").append(RunTimeProperties.instance.isPackageSmallFiles());
		sb.append(", verifyAfterCopy=").append(this.verifyAfterCopy);
		sb.append(", keepOriginalFileDates=").append(RunTimeProperties.instance.isKeepOriginalFileDates());
		sb.append(", skipEmptyDirs=").append(RunTimeProperties.instance.isSkipEmptyDirs());
		sb.append('}');
		return sb.toString();
	}
}
