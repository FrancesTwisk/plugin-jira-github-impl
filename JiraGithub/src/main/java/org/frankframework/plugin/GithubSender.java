package org.frankframework.plugin;

import lombok.Getter;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
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
import java.util.ArrayList;
import java.util.Base64;

public class GithubSender  implements ISender {
	private CloseableHttpClient httpClient;
	private @Getter HttpPost httpRequestCreateNewIssue;
	private @Getter HttpGet httpRequestGetGithubIssues;
	private @Getter String githubUser;
	private @Getter String githubToken;
	private @Getter String githubRepo;
	private @Getter String jiraUser;
	private @Getter String jiraToken;
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
			JSONArray newIssuesArray = new JSONArray(message.asString());
			ArrayList<String> responses = new ArrayList<>();

			for (int i = 0; i < newIssuesArray.length(); i++) {
				JSONObject newIssue = newIssuesArray.getJSONObject(i);
				try {
					httpRequestCreateNewIssue.setEntity(new StringEntity(createNewIssueBody(newIssue)));
					CloseableHttpResponse response = httpClient.execute(httpRequestCreateNewIssue);
					HttpEntity responseEntity = response.getEntity();
					String responseString = EntityUtils.toString(responseEntity);
					updateJiraIssueWithResponse(new JSONObject(responseString), newIssue);
					responses.add(responseString);
				} catch (IOException e) {
					throw new RuntimeException(e);
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

	@Override
	public void configure() throws ConfigurationException {
		httpRequestCreateNewIssue = new HttpPost("https://api.github.com/repos/" + githubUser + "/" + githubRepo + "/issues");
		httpRequestCreateNewIssue.setHeader("Accept", "application/vnd.github+json");
		httpRequestCreateNewIssue.setHeader("Authorization", "Bearer " + githubToken);
		httpRequestCreateNewIssue.setHeader("X-GitHub-Api-Version", "2022-11-28");

		httpRequestGetGithubIssues = new HttpGet("https://api.github.com/repos/" + githubUser + "/" + githubRepo + "/issues");
		httpRequestGetGithubIssues.setHeader("Accept", "application/vnd.github+json");
		httpRequestGetGithubIssues.setHeader("Authorization", "Bearer " + githubToken);
		httpRequestGetGithubIssues.setHeader("X-GitHub-Api-Version", "2022-11-28");
	}

	public String createNewIssueBody(JSONObject newIssue) throws JSONException {
		String body = "";
		if (!newIssue.getJSONObject("fields").isNull("description")) {
			body = newIssue.getJSONObject("fields").getJSONObject("description").getJSONArray("content").getJSONObject(0).getJSONArray("content").getJSONObject(0).getString("text");
		}

		String newIssueBody = String.format(
				"{\"title\":\"%s\",\"body\":\"%s\"}",
				newIssue.getJSONObject("fields").getString("summary"),
				body
		);

		return newIssueBody;
	}

	public void updateJiraIssueWithResponse(JSONObject githubResponse, JSONObject newIssue) throws JSONException, IOException {
		HttpPut put = new HttpPut("https://" + newIssue.getString("self").split("/")[2] + "/rest/api/3/issue/" + newIssue.getString("key"));
		put.setHeader("Content-Type", "application/json");
		put.setHeader("Accept", "application/json");
		put.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((jiraUser + ":" + jiraToken).getBytes()));

		String newIssueBody = String.format(
				"{\"fields\":{\"%s\":\"%s\"},\"update\":{\"comment\":[{\"add\":{\"body\":{\"type\":\"doc\",\"version\":1,\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"inlineCard\",\"attrs\":{\"url\":\"%s\"}}]}]}}}]}}",
				this.jiraField,
				githubResponse.get("number").toString(),
				githubResponse.getString("html_url"),
				githubResponse.getString("html_url")
		);
		try {
			StringEntity getJiraIssuesEntity = new StringEntity(newIssueBody);
			put.setEntity(getJiraIssuesEntity);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		httpClient.execute(put);
	}

	@Override
	public void setName(String name) {

	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

	}

	public void setGithubUser(String githubUser) {
		this.githubUser = githubUser;
	}

	public void setGithubToken(String githubToken) {
		this.githubToken = githubToken;
	}

	public void setGithubRepo(String githubRepo) {
		this.githubRepo = githubRepo;
	}

	public void setJiraUser (String jiraUser) {
		this.jiraUser = jiraUser;
	}

	public void setJiraToken(String jiraToken) {
		this.jiraToken = jiraToken;
	}

	public void setJiraField(String jiraField) {
		this.jiraField = jiraField;
	}
}
