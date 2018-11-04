package org.mhisoft.fc;

/**
 * Description:
 *
 * @author Tony Xue
 * @since Nov, 2018
 */
public class DirecotryStat {
	long numberOfFiles;
	double totalFileSize;

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
}
