#!/bin/bash -x

THIS_DIR=`dirname $0`;

USER_NAME=$1
shift
PROFILE=$*

SNS_TOPIC=`$THIS_DIR/sns-lookup.sh ps-sns-paas-cw-event-service-prod-.*-events $PROFILE`

# First we put a bogus Alarm in.  I.e. something that will fire easily.
aws $PROFILE cloudwatch put-metric-alarm \
  --alarm-name "PAAS CWES invocations" \
  --alarm-description "{\"project\":\"EMS\",\"component\":\"Default\",\"priority\":\"3\",\"recipient\":\"$USER_NAME\"}" \
  --actions-enabled \
  --alarm-actions $SNS_TOPIC \
  --metric-name "Invocations" \
  --namespace "AWS/Lambda" \
  --statistic "Sum" \
  --period 60 \
  --evaluation-periods 1 \
  --threshold 0.0 \
  --comparison-operator "GreaterThanOrEqualToThreshold"


echo "{\"key1\":\"value1\"}" > /tmp/bad-inputs.json
# next we invoke the lambda function.  Note we are pushing a 'bad' input that will cause an error.
# this is so that we get the notification that the Lambda function is not working properly.
aws $PROFILE lambda invoke-async --function-name paas-cw-event-service --invoke-args /tmp/bad-inputs.json


# next we wait for the evaluation period to expire.
sleep 70s

# finally we clean up after ourselves
aws $PROFILE cloudwatch delete-alarms --alarm-names "PAAS CWES invocations"
