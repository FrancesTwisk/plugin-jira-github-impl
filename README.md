This project links a Jira board with a Github repository:
1. For every new task in Jira, it creates an issue in Github with the same name.
2. The new task in Jira is updated by adding the corresponding Github issue number to a custom field.
3. A link to corresponding Github issue commented on the new task in Jira.
4. When an issue is closed in Github, the corresponding task in Jira is set to Done.

To use:
1. Fill in your data in JiraGithub/src/main/configurations/JiraGithub/Configuration.xml.
2. Run docker build to create a Docker image.
3. Run docker run to create and run a Docker container.
