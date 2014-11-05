package org.binarytoys.tomcatmonitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Admin on 11/4/2014.
 */
public class TomcatMonitor {
	public static void main(String[] args) throws InterruptedException {
		while (true) {
			try {
				createThreadsDump(getTomcatPID());
			} catch (IOException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}

			Thread.sleep(10000);
		}
	}

	private static String getTomcatPID() throws IOException {
		String cmd[] = {
				"/bin/sh",
				"-c",
				"ps -e -f | grep tomcat"};
		Process process = Runtime.getRuntime().exec(cmd);
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String commandOutput = bufferedReader.readLine();
		bufferedReader.close();
		return commandOutput.split(" ")[2];
	}

	private static void createThreadsDump(String pid) throws IOException {
		String jstackCmd = "jstack " + pid + " >> /var/speedo/logs/" + System.currentTimeMillis() + ".dump";
		String cmd[] = {
				"/bin/sh",
				"-c",
				jstackCmd};
		Process process = Runtime.getRuntime().exec(cmd);
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String commandOutput = bufferedReader.readLine();
		bufferedReader.close();
	}
}
