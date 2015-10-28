package org.familysearch.firehose.crawler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient;
import com.amazonaws.services.kinesisfirehose.model.BufferingHints;
import com.amazonaws.services.kinesisfirehose.model.CreateDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.S3DestinationConfiguration;

public class Helper {
	private static Properties properties;

	public static Properties properties() {
		if (properties == null) {
			InputStream input = null;

			try {
				input = new FileInputStream("config.properties");
				properties = new Properties();
				properties.load(input);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return properties;
	}

	public static AmazonKinesisFirehoseClient setupFirehoseKinesisClient() {
		// Load AWS Credentials
		BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(Helper.properties().getProperty(
				"awsSecretKey"), Helper.properties().getProperty("awsAccessKey"));

		AmazonKinesisFirehoseClient kinesisfirehoseClient = new AmazonKinesisFirehoseClient(basicAWSCredentials);
		CreateDeliveryStreamRequest createDeliveryStreamRequest = new CreateDeliveryStreamRequest();
		createDeliveryStreamRequest.setDeliveryStreamName( Helper.properties().getProperty("kinesisStreamName"));
		
		S3DestinationConfiguration s3DestinationConfiguration = new S3DestinationConfiguration();
		s3DestinationConfiguration.setBucketARN( Helper.properties().getProperty("s3bucketARN"));
		s3DestinationConfiguration.setRoleARN( Helper.properties().getProperty("roleARN"));
		BufferingHints bufferingHints = new BufferingHints();
		bufferingHints.setIntervalInSeconds(300);
		bufferingHints.setSizeInMBs(5);
		s3DestinationConfiguration.setBufferingHints(bufferingHints);
		
		createDeliveryStreamRequest.setS3DestinationConfiguration(s3DestinationConfiguration);
		
		kinesisfirehoseClient.createDeliveryStream(createDeliveryStreamRequest);
		
		return kinesisfirehoseClient;
	}
}
