package org.mhisoft.fc;

/**
 * Run time properties
 */
public class RunTimeProperties {

	public static final int DEFAULT_THREAD_NUM = 2;
	public static String userHome = System.getProperty("user.home") ;
	public static boolean compressSmallFiles = Boolean.getBoolean("compressSmallFiles");
	public static boolean debug = Boolean.getBoolean("debug");

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
		sb.append(", compressSmallFiles=").append(RunTimeProperties.compressSmallFiles);
		sb.append('}');
		return sb.toString();
	}
}
