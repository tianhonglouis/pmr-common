package com.gopivotal.mapred.input;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

/**
 * A {@link FileInputFormat} implementation that passes the file name as the key
 * and the bytes of the file as the value. Generates one map task per file, but
 * the {@link CombineWholeFileInputFormat} could be used to batch them together
 * into a configurable number of map tasks.
 */
public class WholeFileInputFormat extends FileInputFormat<Text, BytesWritable> {

	@Override
	protected boolean isSplitable(FileSystem fs, Path filename) {
		return false;
	}

	@Override
	public RecordReader<Text, BytesWritable> getRecordReader(InputSplit split,
			JobConf conf, Reporter reporter) throws IOException {
		return new WholeFileRecordReader(split, conf);
	}

	public static class WholeFileRecordReader implements
			RecordReader<Text, BytesWritable> {

		private boolean read = false;
		private FileSystem fs = null;
		private FileSplit fSplit = null;

		public WholeFileRecordReader(InputSplit split, JobConf conf)
				throws IOException {
			read = false;

			fSplit = (FileSplit) split;

			if (fSplit.getLength() > Integer.MAX_VALUE) {
				throw new IOException("Size of file is larger than max integer");
			}

			fs = FileSystem.get(conf);
		}

		@Override
		public boolean next(Text key, BytesWritable value) throws IOException {
			if (!read) {

				// set the key to the fully qualified path
				key.set(fs.makeQualified(fSplit.getPath()).toString());

				int length = (int) fSplit.getLength();

				byte[] bytes = new byte[length];

				// get the bytes of the file for the value
				FSDataInputStream inStream = fs.open(fSplit.getPath());

				IOUtils.readFully(inStream, bytes, 0, length);
				inStream.close();

				// set the value to the byte array
				value.set(bytes, 0, bytes.length);

				read = true;
				return true;
			} else {
				return false;
			}
		}

		@Override
		public void close() throws IOException {
			// nothing to do here
		}

		@Override
		public float getProgress() throws IOException {
			return read ? 1 : 0;
		}

		@Override
		public Text createKey() {
			return new Text();
		}

		@Override
		public BytesWritable createValue() {
			return new BytesWritable();
		}

		@Override
		public long getPos() throws IOException {
			return 0;
		}
	}
}
