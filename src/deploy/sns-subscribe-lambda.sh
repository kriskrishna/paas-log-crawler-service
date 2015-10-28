#!/bin/bash

THIS_DIR=`dirname $0`;

PROFILE=$*

SNS_TOPIC=`$THIS_DIR/sns-lookup.sh ps-sns-paas-cw-event-service-prod-.*-events $PROFILE`
LAMBDA_ENDPOINT=`$THIS_DIR/lambda-lookup.sh paas-cw-event-service $PROFILE`

aws $PROFILE sns list-subscriptions-by-topic --topic-arn $SNS_TOPIC|grep Endpoint|grep "$LAMBDA_ENDPOINT" >/dev/null
RESULT=$?
if [ $RESULT -eq 1 ]; then
 aws $PROFILE sns subscribe --topic-arn $SNS_TOPIC --protocol lambda --notification-endpoint $LAMBDA_ENDPOINT

 aws $PROFILE lambda add-permission --function-name $LAMBDA_ENDPOINT --statement-id default --action "lambda:invokeFunction" --principal "sns.amazonaws.com" --source-arn $SNS_TOPIC
fi

