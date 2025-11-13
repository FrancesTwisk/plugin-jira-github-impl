package org.frankframework.plugin;

import lombok.Getter;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.*;
import org.frankframework.stream.Message;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;

public class JiraSender  implements ISender {
	private CloseableHttpClient httpClient;
	private @Getter HttpPost httpRequestCloseJiraIssue;
	private @Getter HttpPost httpRequestGetJiraIssue;
	private @Getter String jiraBaseUrl;
	private @Getter String jiraUser;
	private @Getter String jiraToken;
	private @Getter String jiraProject;
	private @Getter String jiraField;

	@Override
	public void start() {
		httpClient = HttpClients.createDefault();
	}

	@Override
	public void stop() {
		if(httpClient != null) {
			try {
				httpClient.close();
			} catch (IOException e) {
			}
		}
	}

	@NotNull
	@Override
	public SenderResult sendMessage(@NotNull Message message, @NotNull PipeLineSession session) throws SenderException, TimeoutException {
		try {
			JSONArray newIssues = new JSONArray(message.asString());
			ArrayList<String> responses = new ArrayList<>();

			for (int i = 0; i < newIssues.length(); i++) {
				JSONObject newIssue = newIssues.getJSONObject(i);
				JSONObject jiraIssue = findIssueByGithubID(newIssue);
				httpRequestCloseJiraIssue.setURI(URI.create("https://" + jiraBaseUrl + "/rest/api/3/issue/" + jiraIssue.getString("key") + "/transitions"));

				CloseableHttpResponse response = httpClient.execute(httpRequestCloseJiraIssue);
				HttpEntity responseEntity = response.getEntity();
				if (responseEntity != null) {
					String responseString = EntityUtils.toString(responseEntity);
					responses.add(responseString);
				}
			}
			return new SenderResult(responses.toString());
		} catch (JSONException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ApplicationContext getApplicationContext() {
		return null;
	}

	@Override
	public String getName() {
		return "";
	}

	public JSONObject findIssueByGithubID(JSONObject newIssue) throws IOException, JSONException {
		String getJiraIssueBody = String.format("{\"jql\": \"project = %s AND %s ~ %s\", \"fields\":[\"key\"]}", jiraProject, jiraField, newIssue.get("number").toString());
		StringEntity getJiraIssueBodyEntity = new StringEntity(getJiraIssueBody);
		httpRequestGetJiraIssue.setEntity(getJiraIssueBodyEntity);

		CloseableHttpResponse response = httpClient.execute(httpRequestGetJiraIssue);
		HttpEntity responseEntity = response.getEntity();
		if (responseEntity != null) {
			String responseString = EntityUtils.toString(responseEntity);
			return new JSONObject(responseString).getJSONArray("issues").getJSONObject(0);
		}
		return null;
	}

	@Override
	public void configure() throws ConfigurationException {
		httpRequestCloseJiraIssue = new HttpPost();
		httpRequestCloseJiraIssue.setHeader("Content-Type", "application/json");
		httpRequestCloseJiraIssue.setHeader("Accept", "application/json");
		httpRequestCloseJiraIssue.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((jiraUser + ":" + jiraToken).getBytes()));
		String closeJiraIssueBody = "{\"transition\": {\"id\":\"31\"}}";
		try {
			StringEntity closeJiraIssueBodyEntity = new StringEntity(closeJiraIssueBody);
			httpRequestCloseJiraIssue.setEntity(closeJiraIssueBodyEntity);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		httpRequestGetJiraIssue = new HttpPost("https://" + jiraBaseUrl + "/rest/api/3/search/jql");
		httpRequestGetJiraIssue.setHeader("Content-Type", "application/json");
		httpRequestGetJiraIssue.setHeader("Accept", "application/json");
		httpRequestGetJiraIssue.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((jiraUser + ":" + jiraToken).getBytes()));
	}

	@Override
	public void setName(String name) {

	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

	}

	public void setJiraBaseUrl(String jiraBaseUrl) {
		this.jiraBaseUrl = jiraBaseUrl;
	}

	public void setJiraUser(String jiraUser) {
		this.jiraUser = jiraUser;
	}

	public void setJiraToken(String jiraToken) {
		this.jiraToken = jiraToken;
	}

	public void setJiraProject(String jiraProject) {
		this.jiraProject = jiraProject;
	}

	public void setJiraField(String jiraField) { this.jiraField = jiraField; }
}