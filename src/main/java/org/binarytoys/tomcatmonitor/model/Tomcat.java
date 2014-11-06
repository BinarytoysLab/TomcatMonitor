package org.binarytoys.tomcatmonitor.model;

/**
 * Created by alekspo on 11/5/2014.
 */
public class Tomcat {
	private String instanceName;

	private String processId;

	private String pingCheckUrl;

	public Tomcat(String instanceName, String pingCheckUrl) {
		this.instanceName = instanceName;
		this.pingCheckUrl = pingCheckUrl;
	}

	public String getTomcatStartupCommand() {
		return "/home/viktork/Downloads/tomcats/" + instanceName + "/bin/startup.sh start";
	}

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public String getPingCheckUrl() {
		return pingCheckUrl;
	}

	@Override
	public String toString() {
		return "Tomcat {" +
				"instanceName='" + instanceName + '\'' +
				", processId='" + processId + '\'' +
				'}';
	}
}
