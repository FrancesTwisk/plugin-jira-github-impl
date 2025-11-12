package org.frankframework.plugin;

import lombok.Getter;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

public class GithubListener implements IPullingListener {
	private CloseableHttpClient httpClient;
	private @Getter HttpGet httpGetNewGithubIssues;
	private @Getter String githubUser;
	private @Getter String githubToken;
	private @Getter String githubRepo;
	private @Getter Instant lastTimePolled;

	@Override
	public RawMessageWrapper getRawMessage(@NotNull Map threadContext) throws ListenerException {
		try {
			httpGetNewGithubIssues.setURI(URI.create("https://api.github.com/repos/" + githubUser + "/" + githubRepo + "/issues" + "?since=" + lastTimePolled.toString() + "&state=closed"));
			lastTimePolled = Instant.now();
			CloseableHttpResponse response = httpClient.execute(httpGetNewGithubIssues);
			HttpEntity responseEntity = response.getEntity();
			String responseString = EntityUtils.toString(responseEntity);
			if (new JSONArray(responseString).length() <= 0) {
				return null;
			} else {
				Message message = new Message(responseString);
				String messageId = MessageUtils.generateFallbackMessageId();
				MessageWrapper wrapper = new MessageWrapper<>(message, messageId, null);
				return wrapper;
			}
		} catch (IOException | JSONException e) {
			throw new RuntimeException(e);
		}
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
		httpGetNewGithubIssues = new HttpGet();
		httpGetNewGithubIssues.setHeader("Accept", "application/vnd.github+json");
		httpGetNewGithubIssues.setHeader("Authorization", "Bearer " + githubToken);
		httpGetNewGithubIssues.setHeader("X-GitHub-Api-Version", "2022-11-28");

		lastTimePolled = Instant.now();
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

	public void setGithubUser(String githubUser) {
		this.githubUser = githubUser;
	}

	public void setGithubToken(String githubToken) {
		this.githubToken = githubToken;
	}

	public void setGithubRepo(String githubRepo) {
		this.githubRepo = githubRepo;
	}
}
