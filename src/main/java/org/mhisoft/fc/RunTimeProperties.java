package org.mhisoft.fc;

/**
 * Run time properties
 */
public class RunTimeProperties {

	public static final int DEFAULT_THREAD_NUM = 2;
	public static final int DEFAULT_PACKAGE_SMALL_FILES_THREAD_NUM = 5;
	public static String userHome = System.getProperty("user.home") ;

	public static String zip_prefix ="_fastcopy_auto_create_";

	static boolean packageSmallFiles = Boolean.valueOf(System.getProperty("compressSmallFiles", "true"));
	private static boolean verifyAfterCopy = Boolean.valueOf(System.getProperty("verify", "true"));
	private  static boolean debug = Boolean.getBoolean("debug");

	private static boolean stopThreads = false;
	private boolean running;



	public static RunTimeProperties instance = new RunTimeProperties();

	private RunTimeProperties() {
		//
	}


	String sourceDir = null;
	String destDir = null;
	boolean success;
	boolean verbose;
	int numOfThreads=1;
	boolean overwrite;
	boolean overwriteIfNewerOrDifferent;
	boolean flatCopy;
	boolean debugArg;
	boolean createTheSameSourceFolderUnderTarget;


	public  boolean isStopThreads() {
		return stopThreads;
	}


	public  void setStopThreads(boolean stopThreads) {
		stopThreads = stopThreads;
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

	public int getNumberOfThreadsForPackageSmallFiles() {
		return isPackageSmallFiles()?  DEFAULT_PACKAGE_SMALL_FILES_THREAD_NUM : 1;
	}

	public void setNumOfThreads(int numOfThreads) {
		this.numOfThreads = numOfThreads;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
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
		RunTimeProperties.verifyAfterCopy = verifyAfterCopy;
	}


	public  boolean isVerifyAfterCopy() {
		return verifyAfterCopy;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("RunTimeProperties{");
		sb.append("sourceDir='").append(sourceDir).append('\'');
		sb.append(", destDir='").append(destDir).append('\'');
		sb.append(", verbose=").append(verbose);
		sb.append(", numOfThreads=").append(numOfThreads);
		sb.append(", overwrite=").append(overwrite);
		sb.append(", overwriteIfNewerOrDifferent=").append(overwriteIfNewerOrDifferent);
		sb.append(", createTheSameSourceFolderUnderTarget=").append(createTheSameSourceFolderUnderTarget);
		sb.append(", flatCopy=").append(flatCopy);
		sb.append(", debugArg=").append(debugArg);
		sb.append(", compressSmallFiles=").append(RunTimeProperties.packageSmallFiles);
		sb.append(", verifyAfterCopy=").append(RunTimeProperties.verifyAfterCopy);
		sb.append('}');
		return sb.toString();
	}
}
