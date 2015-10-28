#!/bin/bash

THIS_DIR=`dirname $0`

PROFILE=$*

SNS_TOPIC=`$THIS_DIR/sns-lookup.sh ps-sns-paas-cw-event-service-prod-.*-lambda $PROFILE`

aws $PROFILE cloudwatch put-metric-alarm \
  --alarm-name "PAAS CWES ConfigurationError" \
  --alarm-description "Problems with the alarm configuration.  See run book." \
  --actions-enabled \
  --alarm-actions $SNS_TOPIC \
  --metric-name "Configuration" \
  --namespace "CWEventService" \
  --statistic "Sum" \
  --period 300 \
  --evaluation-periods 1 \
  --threshold 0.0 \
  --comparison-operator "GreaterThanOrEqualToThreshold"

aws $PROFILE cloudwatch put-metric-alarm \
  --alarm-name "PAAS CWES JiraCredentialsError" \
  --alarm-description "Problems with the JIRA Credentails.  See run book." \
  --actions-enabled \
  --alarm-actions $SNS_TOPIC \
  --metric-name "Unauthorized" \
  --namespace "CWEventService" \
  --statistic "Sum" \
  --period 300 \
  --evaluation-periods 1 \
  --threshold 0.0 \
  --comparison-operator "GreaterThanOrEqualToThreshold"

aws $PROFILE cloudwatch put-metric-alarm \
  --alarm-name "PAAS CWES UnknownError" \
  --alarm-description "Unknown issues must be investigated. See run book." \
  --actions-enabled \
  --alarm-actions $SNS_TOPIC \
  --metric-name "Unknown" \
  --namespace "CWEventService" \
  --statistic "Sum" \
  --period 300 \
  --evaluation-periods 1 \
  --threshold 0.0 \
  --comparison-operator "GreaterThanOrEqualToThreshold"

