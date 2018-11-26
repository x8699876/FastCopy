package org.mhisoft.fc;

/**
 * Description:
 *
 * @author Tony Xue
 * @since Nov, 2018
 */
public class DirecotryStat {
	long numberOfFiles;
	long totalFileSize;

	long smallFileCount;
	long totalSmallFileSize;
	boolean fail;
	String failMsg;

	public long getNumberOfFiles() {
		return numberOfFiles;
	}

	public void setNumberOfFiles(long numberOfFiles) {
		this.numberOfFiles = numberOfFiles;
	}

	public double getTotalFileSize() {
		return  totalFileSize;
	}

	public void setTotalFileSize(long totalFileSize) {
		this.totalFileSize = totalFileSize;
	}

	public double getAverageFileSize() {
		return totalFileSize/totalFileSize;
	}


	public void incrementSmallFileCount() {
		 smallFileCount++;
	}

	public void setSmallFileCount(long smallFileCount) {
		this.smallFileCount = smallFileCount;
	}

	public long getSmallFileCount() {
		return smallFileCount;
	}

	public void addToTotalSmallFileSize(long v) {
		this.totalSmallFileSize+=v;
	}

	public long getTotalSmallFileSize() {
		return totalSmallFileSize;
	}

	public boolean isFail() {
		return fail;
	}

	public void setFail(boolean fail) {
		this.fail = fail;
	}

	public String getFailMsg() {
		return failMsg;
	}

	public void setFailMsg(String failMsg) {
		this.failMsg = failMsg;
	}
}
