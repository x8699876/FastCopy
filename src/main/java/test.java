import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.mhisoft.fc.FastCopy;
import org.mhisoft.fc.FileCopyStatistics;
import org.mhisoft.fc.ui.UI;

/**
 * Description:
 *
 * @author Tony Xue
 * @since Feb, 2015
 */
public class test {


	public static void nioBufferCopy(final File source, final File target, FileCopyStatistics statistics, final UI rdProUI) {
		FileChannel in = null;
		FileChannel out = null;
		double size = 0;

		try {
			in = new FileInputStream(source).getChannel();
			out = new FileOutputStream(target).getChannel();
			size = in.size();
			double size2InKB = size / 1024 ;
			rdProUI.print(String.format("\nCopying file %s, size:%s KBytes", target.getAbsolutePath(), df.format(size2InKB)));

			ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER);
			int readSize = in.read(buffer);
			long totalSize = 0;
			int progress = 0;

			long startTime, endTime  ;
			long overallT1 =  System.currentTimeMillis();

			while (readSize != -1) {

				if (FastCopy.isStopThreads()) {
					rdProUI.println("[warn]Cancelled by user. Stoping copying.");
					return;
				}

				totalSize = totalSize + readSize;
				startTime = System.currentTimeMillis();
				progress = (int) (totalSize * 100 / size);
				rdProUI.showProgress(progress, statistics);

				buffer.flip();

				while (buffer.hasRemaining()) {
					out.write(buffer);
					//System.out.printf(".");
					//showPercent(rdProUI, totalSize/size );

				}
				buffer.clear();
				readSize = in.read(buffer);


				endTime = System.currentTimeMillis();
				statistics.addToTotalFileSizeAndTime(readSize / 1024, (endTime - startTime));
			}

			statistics.setFilesCount(statistics.getFilesCount()+1);
			long overallT2 =  System.currentTimeMillis();
			statistics.setSpeedForBucket(readSize/1024, 0,  (overallT2 - overallT1));


			//long t2 = System.currentTimeMillis();

//			double speed = 0;
//			if (size > 0 && t2 - t1 > 0) {
//				speed = size2InKB * (10 ^ 6) / (t2 - t1);  //KB/s
//				statistics.setSpeedForBucket(size2InKB, speed, t2 - t1);
//			}

			//rdProUI.println(String.format(", speed:%s KByte/Second.", df.format(speed)));

		} catch (IOException e) {
			rdProUI.println(String.format("[error] Copy file %s to %s: %s", source.getAbsoluteFile(), target.getAbsolutePath(), e.getMessage()));
		} finally {
			close(in);
			close(out);

		}
	}

}
