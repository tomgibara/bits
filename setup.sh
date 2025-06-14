#!/bin/bash
# Simple setup script to ensure Maven and Java are installed.
set -e

# Install Java if it is not present
if ! command -v java >/dev/null 2>&1; then
  echo "Java not found. Installing OpenJDK..."
  apt-get update
  DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-11-jdk
fi

# Install Maven if it is not present
if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven not found. Installing Maven..."
  apt-get update
  DEBIAN_FRONTEND=noninteractive apt-get install -y maven
fi

# Configure Maven to use a proxy
mkdir -p ~/.m2 && cat > ~/.m2/settings.xml <<'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">
</settings>
EOF

sed -i '/<\/settings>/ i\
  <proxies>\
    <proxy>\
      <id>defaultProxy</id>\
      <active>true</active>\
      <protocol>http</protocol>\
      <host>proxy</host>\
      <port>8080</port>\
    </proxy>\
    <proxy>\
      <id>defaultSecureProxy</id>\
      <active>true</active>\
      <protocol>https</protocol>\
      <host>proxy</host>\
      <port>8080</port>\
    </proxy>\
  </proxies>' ~/.m2/settings.xml

# Set JAVA_HOME if not already set
if [ -z "$JAVA_HOME" ]; then
  JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))
  export JAVA_HOME
fi

# Display versions for verification
java -version
mvn -version
