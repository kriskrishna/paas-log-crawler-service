#!/bin/bash

SNS_TOPIC_PATTERN=$1
shift
PROFILE=$*
NEXT_TOKEN=
TOPIC_ARN=

until [ "$TOPIC_ARN" ]; do
  aws $PROFILE sns list-topics $NEXT_TOKEN >/tmp/list-topics.json
  NEXT_TOKEN=`grep NextToken /tmp/list-topics.json |awk '{print $2}'`
  TOPIC_ARN=`grep ${SNS_TOPIC_PATTERN} /tmp/list-topics.json |awk '{print $2}'|sed -e 's/\"//g'`

  if [ -z $NEXT_TOKEN ]; then
    break;
  fi
done

echo $TOPIC_ARN
