#!/bin/bash

# does the lambda updates.  Must provide profile, otherwise default is assumed.
PROFILE=$*

aws $PROFILE lambda update-function-code --function-name paas-cw-event-service --zip-file fileb:///tmp/cw-event-service.zip

