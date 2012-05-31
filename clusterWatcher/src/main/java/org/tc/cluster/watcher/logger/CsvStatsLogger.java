package org.tc.cluster.watcher.logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import au.com.bytecode.opencsv.CSVWriter;

public class CsvStatsLogger {

	private CSVWriter log;

	public CsvStatsLogger(String file){
		Writer writer;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			this.log = new CSVWriter(writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void log(String... stats) {
		logToCSV(stats);
	}

	public void header(String... headers) {
		logToCSV(headers);
	}

	private void logToCSV(String[] items){
		try {
			log.writeNext(items);
			log.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() throws IOException{
		log.close();
	}
}
