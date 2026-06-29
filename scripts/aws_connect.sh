#!/bin/bash

AWS_PROFILE="robopilot-dev"
AWS_REGION="ap-northeast-2"

BE_INSTANCE_ID="i-0a4cf711bd8b8fa73"
RDS_HOST="dhive-main.cpskayqwuzja.ap-northeast-2.rds.amazonaws.com"

export AWS_PROFILE
export AWS_REGION

check_sso() {
  echo "Checking AWS SSO session..."

  aws sts get-caller-identity --profile "$AWS_PROFILE" >/dev/null 2>&1

  if [ $? -ne 0 ]; then
    echo "AWS SSO token expired or missing."
    echo "Starting AWS SSO login..."
    aws sso login --profile "$AWS_PROFILE"

    if [ $? -ne 0 ]; then
      echo "AWS SSO login failed."
      exit 1
    fi
  fi

  echo "AWS SSO session is valid."
}

usage() {
  echo ""
  echo "Run:"
  echo "  aws_connect.sh -help"
  echo "Usage:"
  echo "  aws_connect.sh -db robopilot-prod"
  echo "  aws_connect.sh -ec2 ROBOPILOT-BE"
  echo "  aws_connect.sh -server be-prod"
  echo "  aws_connect.sh -login"
  echo ""
  exit 1
}

show_help() {
cat << EOF

=========================================
ROBOPILOT AWS Utility
=========================================

Usage:

  aws_connect.sh -help

      Show this help menu

AWS Login
--------------------
source aws_connect.sh -login

    Checks AWS SSO session.
    If expired, opens AWS SSO login.

    Exports:

      AWS_PROFILE=robopilot-dev
      AWS_REGION=ap-northeast-2

    into the CURRENT terminal session.

    Use this before running local backend server
    when local backend needs AWS/S3 access.

    Example:

      source aws_connect.sh -login
      mvn spring-boot:run

Database Connections
--------------------

  aws_connect.sh -db robopilot-prod

      Opens SSM tunnel to Production RDS

      DBeaver:
        Host     : localhost
        Port     : 5433
        Database : dhive-main


EC2 Access
----------

  aws_connect.sh -ec2 ROBOPILOT-BE

      Opens SSM shell to Backend EC2


  aws_connect.sh -ec2 ROBOPILOT-AI-LLM

      Opens SSM shell to AI/LLM EC2


Server Access
-------------

  aws_connect.sh -server be-prod

      Opens ROBOPILOT Backend server shell

      Useful for:
        - checking logs
        - running backend commands
        - checking services
        - accessing production environment


AWS SSO
--------

The script automatically:

  1. Checks current SSO session
  2. If expired:
       aws sso login --profile robopilot-dev
  3. Exports:

       AWS_PROFILE=robopilot-dev
       AWS_REGION=ap-northeast-2


Examples
---------
  aws_connect.sh -login
  # OR
  source aws_connect.sh -login

  
  aws_connect.sh -db robopilot-prod

  aws_connect.sh -ec2 ROBOPILOT-BE

  aws_connect.sh -ec2 ROBOPILOT-AI-LLM

  aws_connect.sh -server be-prod


Future Commands
---------------

  aws_connect.sh -redis prod
  aws_connect.sh -logs be-prod
  aws_connect.sh -deploy be-prod
  aws_connect.sh -db local

=========================================

EOF
}
if [ "$1" = "-help" ] || [ "$1" = "--help" ]; then
    show_help
    exit 0
fi

if [ "$1" = "-login" ]; then
  check_sso

  export AWS_PROFILE="robopilot-dev"
  export AWS_REGION="ap-northeast-2"

  echo ""
  echo "AWS environment configured:"
  echo "AWS_PROFILE=$AWS_PROFILE"
  echo "AWS_REGION=$AWS_REGION"
  echo ""

  return 0 2>/dev/null || exit 0
fi

if [ $# -lt 2 ]; then
  usage
fi



TYPE="$1"
TARGET="$2"

check_sso

if [ "$TYPE" = "-db" ]; then

  if [ "$TARGET" = "robopilot-prod" ]; then
    echo ""
    echo "Starting Production RDS tunnel..."
    echo "DBeaver connection:"
    echo "  Host: localhost"
    echo "  Port: 5433"
    echo "  DB  : dhive-main"
    echo ""

    aws ssm start-session \
      --target "$BE_INSTANCE_ID" \
      --document-name AWS-StartPortForwardingSessionToRemoteHost \
      --parameters "{\"host\":[\"$RDS_HOST\"],\"portNumber\":[\"5432\"],\"localPortNumber\":[\"5433\"]}"
  else
    echo "Unknown database target: $TARGET"
    exit 1
  fi

elif [ "$TYPE" = "-ec2" ]; then

  INSTANCE_ID=$(aws ec2 describe-instances \
    --filters \
        "Name=tag:Name,Values=$TARGET" \
        "Name=instance-state-name,Values=running" \
    --query "Reservations[*].Instances[*].InstanceId" \
    --output text)

  if [ -z "$INSTANCE_ID" ] || [ "$INSTANCE_ID" = "None" ]; then
    echo "Instance not found: $TARGET"
    exit 1
  fi

  echo ""
  echo "Opening SSM shell..."
  echo "Instance: $TARGET"
  echo "ID      : $INSTANCE_ID"
  echo ""

  aws ssm start-session --target "$INSTANCE_ID"

elif [ "$TYPE" = "-server" ]; then

  if [ "$TARGET" = "be-prod" ]; then
    echo ""
    echo "Opening ROBOPILOT-BE server shell..."
    echo "Instance ID: $BE_INSTANCE_ID"
    echo ""
    echo "After connection, run your backend commands inside EC2."
    echo ""

    aws ssm start-session --target "$BE_INSTANCE_ID"
  else
    echo "Unknown server target: $TARGET"
    exit 1
  fi

else
  usage
fi