/**
 * Created by kriskrishna on Oct/24/15.
 */

console.log("Loading Function");
var AWS = require('aws-sdk');
var LineStream = require('byline').LineStream;
var parse = require('clf-parser');  // Apache Common Log Format
var path = require('path');
var stream = require('stream');

/* Globals */
var esDomain = {
    endpoint: 'my-search-endpoint.amazonaws.com',
    region: 'my-region',
    index: 'logs',
    doctype: 'apache'
};
var endpoint =  new AWS.Endpoint(esDomain.endpoint);
var s3 = new AWS.S3();
var totLogLines = 0;    // Total number of log lines in the file
var numDocsAdded = 0;   // Number of log lines added to ES so far

/*
 * The AWS credentials are picked up from the environment.
 * They belong to the IAM role assigned to the Lambda function.
 * Since the ES requests are signed using these credentials,
 * make sure to apply a policy that permits ES domain operations
 * to the role.
 */
var creds = new AWS.EnvironmentCredentials('AWS');


/* == Streams ==
 * To avoid loading an entire (typically large) log file into memory,
 * this is implemented as a pipeline of filters, streaming log data
 * from S3 to ES.
 * Flow: S3 file stream -> Log Line stream -> Log Record stream -> ES
 */
var lineStream = new LineStream();
// A stream of log records, from parsing each log line
var recordStream = new stream.Transform({objectMode: true})
recordStream._transform = function(line, encoding, done) {
    var logRecord = parse(line.toString());
    var serializedRecord = JSON.stringify(logRecord);
    this.push(serializedRecord);
    totLogLines ++;
    done();
}

/*
* Entrance point for the Lambda Function
**/
exports.handler = function(event, context) {
    var exec = require('child_process').exec;
    var fs = require('fs');

    var json = JSON.stringify(event);
    console.log(json);
    fs.writeFile("/tmp/kinesisfirehose-elasticsearch.json", json, function(error) {
        if (error) {
            console.log('"Error writing sns-notification.json file! Error=' + error + '"');
            context.done(error)
        }
        console.log('"successfully wrote /tmp/sns-notification.json"');
    });

    var cmd = "java -jar ./target/paas-log-crawler-service-jar-with-dependencies.jar /tmp/sns-notification.json";
    var child = exec(cmd, function (error, stdout, stderr) {
        console.log('"Standard out: ' + stdout + '"');
        console.log('"Standard error: ' + stderr + '"');
        if (!error) {
            context.done();
        } else {
            context.done(error, 'lambda');
        }
    });
    
    
    
    console.log('Received event: ', JSON.stringify(event, null, 2));
    event.Records.forEach(function(record) {
        var bucket = record.s3.bucket.name;
        var objKey = decodeURIComponent(record.s3.object.key.replace(/\+/g, ' '));
        s3LogsToES(bucket, objKey, context);
    });
    
    /*
     * Get the log file from the given S3 bucket and key.  Parse it and add
     * each log record to the ES domain.
     */
    function s3LogsToES(bucket, key, context) {
        // Note: The Lambda function should be configured to filter for .log files
        // (as part of the Event Source "suffix" setting).

        var s3Stream = s3.getObject({Bucket: bucket, Key: key}).createReadStream();

        // Flow: S3 file stream -> Log Line stream -> Log Record stream -> ES
        s3Stream
          .pipe(lineStream)
          .pipe(recordStream)
          .on('data', function(parsedEntry) {
              postDocumentToES(parsedEntry, context);
          });

        s3Stream.on('error', function() {
            console.log(
                'Error getting object "' + key + '" from bucket "' + bucket + '".  ' +
                'Make sure they exist and your bucket is in the same region as this function.');
            context.fail();
        });
    }

    /*
     * Add the given document to the ES domain.
     * If all records are successfully added, indicate success to lambda
     * (using the "context" parameter).
     */
    function postDocumentToES(doc, context) {
        var req = new AWS.HttpRequest(endpoint);

        req.method = 'POST';
        req.path = path.join('/', esDomain.index, esDomain.doctype);
        req.region = esDomain.region;
        req.body = doc;
        req.headers['presigned-expires'] = false;
        req.headers['Host'] = endpoint.host;

        // Sign the request (Sigv4)
        var signer = new AWS.Signers.V4(req, 'es');
        signer.addAuthorization(creds, new Date());

        // Post document to ES
        var send = new AWS.NodeHttpClient();
        send.handleRequest(req, null, function(httpResp) {
            var body = '';
            httpResp.on('data', function (chunk) {
                body += chunk;
            });
            httpResp.on('end', function (chunk) {
                numDocsAdded ++;
                if (numDocsAdded === totLogLines) {
                    // Mark lambda success.  If not done so, it will be retried.
                	console.log('All ' + numDocsAdded + ' log records added to ES.');
                    context.succeed();
                }
            });
        }, function(err) {
            console.log('Error: ' + err);
            console.log(numDocsAdded + 'of ' + totLogLines + ' log records added to ES.');
            context.fail();
        });
};
