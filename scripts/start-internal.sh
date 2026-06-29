#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "========================================="
echo " ROBOPILOT - Internal Release"
echo "========================================="

echo ""
echo "Project Root:"
echo "$PROJECT_ROOT"

echo ""
echo "Loading AWS SSO..."

source "$SCRIPT_DIR/aws_connect.sh" -login

echo ""
echo "Loading .env.internal..."


ENV_FILE="$PROJECT_ROOT/.env.internal"

if [ ! -f "$ENV_FILE" ]; then
    echo ""
    echo "ERROR: .env.internal not found."
    echo ""
    echo "Create .env.internal before starting the application."
    exit 1
fi

set -a
source "$PROJECT_ROOT/.env.internal"
set +a

echo ""
echo "Starting Spring Boot..."

cd "$PROJECT_ROOT/Cloud_Service/poc"

mvn spring-boot:run