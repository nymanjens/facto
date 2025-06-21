# Start with a minimal base with OpenJDK (required by Facto)
FROM openjdk:11-slim

# Metadata
LABEL description="Docker image for Facto using prebuilt binaries"

# Set environment
ENV FACTO_VERSION=v4.08
ENV FACTO_URL=https://github.com/nymanjens/facto/releases/download/${FACTO_VERSION}/release-${FACTO_VERSION}.tar.gz

RUN mkdir /opt/facto-install && \
    mkdir /opt/facto && \
    mkdir /opt/facto/conf

# Install dependencies and Facto
RUN apt-get update && \
    apt-get install -y curl tar bash sed perl default-mysql-client && \
    curl -L "$FACTO_URL" | tar xz -C /opt/facto-install && \
    cp -r /opt/facto-install/bin /opt/facto/ && \
    cp -r /opt/facto-install/lib /opt/facto/ && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Default working directory
WORKDIR /opt/facto

# Expose the default Facto port
EXPOSE 9000

COPY ./entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

ENTRYPOINT ["/opt/facto/entrypoint.sh"]
