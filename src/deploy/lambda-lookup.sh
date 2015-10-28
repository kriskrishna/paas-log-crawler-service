#!/bin/bash

LAMBDA_PATTERN=$1
shift
PROFILE=$*
NEXT_TOKEN=
LAMBDA_ARN=

until [ "$LAMBDA_ARN" ]; do
  aws $PROFILE lambda list-functions $NEXT_TOKEN >/tmp/list-lambda.json
  NEXT_TOKEN=`grep NextToken /tmp/list-lambda.json |awk '{print "--starting-token " $2}'|sed -e 's/\"//g'`
  LAMBDA_ARN=`grep ${LAMBDA_PATTERN} /tmp/list-lambda.json| grep FunctionArn |awk '{print $2}'|sed -e 's/\"//g'|sed -e 's/,//g'`

  if [ -z "$NEXT_TOKEN" ]; then
    break;
  fi
done

echo $LAMBDA_ARN
