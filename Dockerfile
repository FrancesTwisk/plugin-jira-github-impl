FROM ubuntu:24.04

WORKDIR /app

COPY . .

RUN apt-get update && apt-get install -y \
    curl \
    unzip

CMD ["bash", "-c", "cd /app/JiraGithub && ./restart.sh"]