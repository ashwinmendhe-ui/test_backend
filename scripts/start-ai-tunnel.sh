#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "$SCRIPT_DIR/aws_connect.sh" -login

echo ""
echo "Starting AI Service tunnel..."
echo "localhost:7879 -> FPT AI Service"
echo ""

aws ssm start-session \
  --profile robopilot-dev \
  --target i-0a4cf711bd8b8fa73 \
  --document-name AWS-StartPortForwardingSession \
  --parameters '{"portNumber":["7879"],"localPortNumber":["7879"]}'