#!/bin/bash -x

# does the lambda creation.  Must provide profile, otherwise default is assumed.
PROFILE=$*

ROLE=`aws $PROFILE iam list-roles --path-prefix /services/Lambda|tee /tmp/roles.json|jq ".[]"|grep Arn |grep cw-event-service|awk '{print $2}'|sed -e 's/\"//g'|sed -e 's/,//g'|sed -e 's/\r//'`

aws $PROFILE lambda create-function \
                      --function-name paas-cw-event-service \
                      --runtime nodejs \
                      --role $ROLE \
                      --handler "CWEventService.handler" \
                      --description "Event Service that creates JIRAs based on CloudWatch notifications." \
                      --timeout 60 \
                      --memory-size 1024 \
                      --zip-file fileb:///tmp/cw-event-service.zip


