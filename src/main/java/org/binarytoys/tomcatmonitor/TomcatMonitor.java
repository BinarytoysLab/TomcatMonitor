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
import org.binarytoys.tomcatmonitor.model.Tomcat;

/**
 * Created by Admin on 11/4/2014.
 */
public class TomcatMonitor {
	private static final Integer HTTPCLIENT_TIMEOUT = 10000;

	private static final Boolean debugMode = true;

	private static final Tomcat[] tomcatsToWatch = {
//			new Tomcat("tomcat-gates", "http://localhost:8081/admin")
			new Tomcat("tomcat", "http://localhost:8080/admin")
	};

	public static void main(String[] args) throws Exception {
		System.out.println("Obtaining initial tomcats PIDs...");
		for (Tomcat tomcat : tomcatsToWatch) {
			String tomcatPID = getTomcatPID(tomcat);
			if (tomcatPID.isEmpty()) {
				System.out.println("ERROR: Couldn't find running tomcat process ID for instance " + tomcat.getInstanceName());
				return;
			}
			tomcat.setProcessId(tomcatPID);
			System.out.println(tomcat);
		}

		System.out.println("Tomcats will be watched once per minute");

		while (true) {
			for (Tomcat tomcat : tomcatsToWatch) {
				if (!isTomcatAlive(tomcat.getPingCheckUrl())) {
					System.out.println("ERROR: " + tomcat + " is not responding, creating thread dump...");

					try {
						createThreadsDumpByPID(tomcat);
					} catch (IOException e) {
						System.out.println("Failed to create thread dump: " + e.getMessage());
						continue;
					}

					if (!debugMode) {
						System.out.println("Killing " + tomcat + " process...");
						runShellCommand("kill -9 " + tomcat.getProcessId());
						Thread.sleep(1000);

						System.out.println("Restarting tomcat...");
						runShellCommand(tomcat.getTomcatStartupCommand());
						String newTomcatPID = getTomcatPID(tomcat);
						if (newTomcatPID.isEmpty()) {
							System.out.println("ERROR: Failed to obtain PID for restarted tomcat");
							return;
						}
						tomcat.setProcessId(newTomcatPID);
						System.out.println("Tomcat was restarted with new PID " + tomcat);
					}
				}
			}

			Thread.sleep(60000);
		}
	}

	private static String getTomcatPID(Tomcat tomcat) throws IOException {
		Process process = runShellCommand("ps -e -f | grep " + tomcat.getInstanceName());
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String commandOutput = bufferedReader.readLine();
		bufferedReader.close();
		StringTokenizer st = new StringTokenizer(commandOutput);
		for (int i = 0; i < 2; i++) {
			commandOutput = st.nextToken();
		}
		return commandOutput;
	}

	private static void createThreadsDumpByPID(Tomcat tomcat) throws IOException {
		String jstackCmd = "jstack " + tomcat.getProcessId() + " >> /var/speedo/logs/" + System.currentTimeMillis()
				+ "." + tomcat.getInstanceName() + ".dump";
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

	private static boolean isTomcatAlive(String pingCheckUrl) {
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(HTTPCLIENT_TIMEOUT)
				.setSocketTimeout(HTTPCLIENT_TIMEOUT).setConnectionRequestTimeout(HTTPCLIENT_TIMEOUT)
				.setStaleConnectionCheckEnabled(true).build();
		CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

		try {
			HttpGet httpGet = new HttpGet(pingCheckUrl);
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
