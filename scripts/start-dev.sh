#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "========================================="
echo " ROBOPILOT - Local Development"
echo "========================================="

echo ""
echo "Project Root:"
echo "$PROJECT_ROOT"

echo ""
echo "Loading AWS SSO..."

source "$SCRIPT_DIR/aws_connect.sh" -login

echo ""
echo "Loading .env.dev..."

ENV_FILE="$PROJECT_ROOT/.env.dev"

if [ ! -f "$ENV_FILE" ]; then
    echo ""
    echo "ERROR: .env.dev not found."
    echo ""
    echo "Copy .env.example to .env.dev and update the values."
    exit 1
fi

set -a
source "$PROJECT_ROOT/.env.dev"
set +a

echo ""
echo "Starting Spring Boot..."

cd "$PROJECT_ROOT/Cloud_Service/poc"

mvn spring-boot:run