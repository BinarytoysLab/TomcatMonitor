package org.binarytoys.tomcatmonitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

/**
 * Created by Admin on 11/4/2014.
 */
public class TomcatMonitor {
	private static final Integer HTTPCLIENT_TIMEOUT = 10000;

	public static void main(String[] args) throws Exception {
		String tomcatPID = getTomcatPID();
		if (tomcatPID.isEmpty()) {
			System.out.println("Couldn't find running tomcat process");
			return;
		}

		System.out.println("Tomcat process with ID: " + tomcatPID + " is being checked once per minute");

		while (true) {
			if (!isTomcatAlive()) {
				System.out.println("Tomcat is not responding, creating thread dump...");
				try {
					createThreadsDumpByPID(tomcatPID);
				} catch (IOException e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
				}

				System.out.println("Killing current tomcat process...");
				runShellCommand("kill -9 " + tomcatPID);
				System.out.println("Restarting tomcat...");
				runShellCommand("/home/viktork/Downloads/tomcat-gates/bin/startup.sh start");
				tomcatPID = getTomcatPID();
				System.out.println("New tomcat PID is " + tomcatPID);
			}

			Thread.sleep(60000);
		}
	}

	private static String getTomcatPID() throws IOException {
		Process process = runShellCommand("ps -e -f | grep tomcat-gates");
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String commandOutput = bufferedReader.readLine();
		bufferedReader.close();
        StringTokenizer st = new StringTokenizer(commandOutput);
        for (int i = 0; i < 2; i++) {
            commandOutput = st.nextToken();
        }
        return commandOutput;
	}

	private static void createThreadsDumpByPID(String pid) throws IOException {
		String jstackCmd = "jstack " + pid + " >> /var/speedo/logs/" + System.currentTimeMillis() + ".dump";
		Process process = runShellCommand(jstackCmd);
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		bufferedReader.close();
	}

	private static Process runShellCommand(String cmd) throws IOException {
		String cmdLine[] = {
				"/bin/sh",
				"-c",
				cmd};
		return Runtime.getRuntime().exec(cmdLine);
	}

	private static boolean isTomcatAlive() {
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(HTTPCLIENT_TIMEOUT)
				.setSocketTimeout(HTTPCLIENT_TIMEOUT).setConnectionRequestTimeout(HTTPCLIENT_TIMEOUT)
				.setStaleConnectionCheckEnabled(true).build();
		CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

		try {
			HttpGet httpGet = new HttpGet("http://localhost:8080/admin");
			CloseableHttpResponse response = client.execute(httpGet);

			EntityUtils.consume(response.getEntity());
			response.close();
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception();
			}
		} catch (Exception e) {
			return false;
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}

		return true;
	}
}
