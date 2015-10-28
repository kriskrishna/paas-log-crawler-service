#!/bin/bash 

THIS_DIR=`dirname $0`;

PROFILE=$*

SNS_TOPIC=`$THIS_DIR/sns-lookup.sh ps-sns-paas-cw-event-service-prod-.*-lambda $PROFILE`
EMAIL=${EMAIL:=fh-developer-tools}

aws $PROFILE sns list-subscriptions-by-topic --topic-arn $SNS_TOPIC|grep Endpoint|grep $EMAIL >/dev/null
RESULT=$?
if [ $RESULT -eq 1 ]; then
 aws $PROFILE sns subscribe --topic-arn $SNS_TOPIC --protocol email --notification-endpoint ${EMAIL}@familysearch.org 
fi

