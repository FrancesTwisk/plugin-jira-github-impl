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
import org.frankframework.core.IPullingListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;
import org.jetbrains.annotations.NotNull;
import org.json.*;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;

public class JiraListener implements IPullingListener {
	private CloseableHttpClient httpClient;
	private @Getter String jiraBaseUrl;
	private @Getter String jiraUser;
	private @Getter String jiraToken;
	private @Getter String jiraProject;
	private @Getter HttpPost httpGetNewIssues;
	private @Getter Instant lastTimePolled;

	@Override
	public RawMessageWrapper getRawMessage(@NotNull Map threadContext) throws ListenerException {
		httpGetNewIssues.setURI(URI.create("https://" + jiraBaseUrl + "/rest/api/3/search/jql"));
		try {
			StringEntity getIssuesEntity = new StringEntity(createGetIssuesBody());
			httpGetNewIssues.setEntity(getIssuesEntity);

			CloseableHttpResponse response = httpClient.execute(httpGetNewIssues);
			HttpEntity responseEntity = response.getEntity();
			String responseString = EntityUtils.toString(responseEntity);

			if (extractNewIssues(responseString).isEmpty()) {
				lastTimePolled = Instant.now();
				return null;
			} else {
				Message message = new Message(String.valueOf(extractNewIssues(responseString)));
				lastTimePolled = Instant.now();
				String messageId = MessageUtils.generateFallbackMessageId();
				MessageWrapper wrapper = new MessageWrapper<>(message, messageId, null);
				return wrapper;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public ArrayList<JSONObject> extractNewIssues(String responseString) throws JSONException {
		JSONObject jsonObject = new JSONObject(responseString);
		JSONArray jsonArray = jsonObject.getJSONArray("issues");
		ArrayList<JSONObject> newIssues = new ArrayList<>();

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject issue = jsonArray.getJSONObject(i);

			String jiraIssueCreated = issue.getJSONObject("fields").getString("created");
			DateTimeFormatter formatter = new DateTimeFormatterBuilder()
					.appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
					.parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
					.toFormatter();
			OffsetDateTime offsetDateTime = OffsetDateTime.parse(jiraIssueCreated, formatter);
			Instant jiraIssueCreatedInstant = offsetDateTime.toInstant();

			if (jiraIssueCreatedInstant.isAfter(lastTimePolled)) {
				newIssues.add(issue);
			}
		}
		return newIssues;
	}

	@Override
	public void closeThread(@NotNull Map threadContext) throws ListenerException {

	}

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

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper rawMessage, PipeLineSession pipeLineSession) throws ListenerException {

	}

	@Override
	public Message extractMessage(@NotNull RawMessageWrapper rawMessage, @NotNull Map context) throws ListenerException {
		return null;
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
		httpGetNewIssues = new HttpPost();
		httpGetNewIssues.setHeader("Content-Type", "application/json");
		httpGetNewIssues.setHeader("Accept", "application/json");
		httpGetNewIssues.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((jiraUser + ":" + jiraToken).getBytes()));

		lastTimePolled = Instant.now();
	}

	public String createGetIssuesBody() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
		String jiraDate = formatter.format(lastTimePolled);
		String getIssuesBody = String.format(
				"{\"jql\": \"project = %s AND created >= %s\", \"fields\": [\"summary\", \"description\", \"created\"]}",
				jiraProject, jiraDate
		);
		return getIssuesBody;
	}

	@Override
	public void setName(String name) {

	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

	}

	@NotNull
	@Override
	public Map<String, Object> openThread() throws ListenerException {
		return Map.of();
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
}
