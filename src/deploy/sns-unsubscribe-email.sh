#!/bin/bash

THIS_DIR=`dirname $0`;

EMAIL=$1
shift
PROFILE=$*

SNS_TOPIC=`$THIS_DIR/sns-lookup.sh ps-sns-paas-cw-event-service-prod-.*-lambda $PROFILE`

SUB_ARN=`aws $PROFILE sns list-subscriptions-by-topic --topic-arn $SNS_TOPIC|grep -E "Endpoint|SubscriptionArn"|grep -A1 $EMAIL|grep SubscriptionArn|awk '{print $2}'|sed -e 's/\"//g'`

if [ "$SUB_ARN" ]; then
 aws $PROFILE sns unsubscribe --subscription-arn $SUB_ARN
fi

