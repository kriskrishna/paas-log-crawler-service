#!/bin/bash

PROFILE=$*
NAMESPACE=CWEventService
LOG_GROUP_NAME=/aws/lambda/paas-cw-event-service

create_log_filter() {
  FILTER_NAME=$1
  FILTER_PATTERN=$2
  METRIC_NAME=$3
  METRIC_VALUE=$4

  aws $PROFILE logs put-metric-filter --log-group-name $LOG_GROUP_NAME --filter-name "$FILTER_NAME" --filter-pattern "$FILTER_PATTERN" --metric-transformations metricName=$METRIC_NAME,metricNamespace=$NAMESPACE,metricValue=$METRIC_VALUE
}

create_log_group() {
  GROUP=`aws $PROFILE logs describe-log-groups |grep logGroupName|grep $1|awk '{print $2}'|sed -e 's/,//'`

  if [ -z $GROUP ]; then
    aws $PROFILE logs create-log-group --log-group-name $1 
  fi
  
}

# Make sure the log group has been created before creating filters on it.
create_log_group $LOG_GROUP_NAME

# Add metric filter for Bad Alarm Description 
create_log_filter ConfigurationError 'Error occurred trying to get configuration' Configuration 1

# Add metric filter for JIRA Credential issues.
create_log_filter JiraCredentialsError '401 Unauthorized' Unauthorized 1

# Add metric filter for Unknown issues.
create_log_filter UnknownError 'Error occurred trying to create notification' Unknown 1

# Add metric filter for JIRA Create Error issues.
create_log_filter JiraCreateError 'Error occurred trying to create JIRA'  Create 1

# Add metric filter for JIRA Search Error issues.
create_log_filter JiraSearchError 'Error occurred trying to search for existing JIRA'  Search 1

# Add metric filter for JIRA Login Error issues.
create_log_filter JiraLoginError 'Error occurred trying to login'  Login 1

# Add metric filter for JIRA Update Error issues.
create_log_filter JiraUpdateError 'Error occurred trying to update JIRA'  Update 1


