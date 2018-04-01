package com.dhiman.logback.appender.kinesis;


import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;

/**
 * 
 * Custom credentials provider chain to look for AWS credentials.
 *
 * Implementation will look for credentials in the following priority:
 *  - AwsCredentials.properties file in class path
 *  - Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY
 *  - Java System Properties - aws.accessKeyId and aws.secretKey
 *  - Profile Credentials - default profile unless AWS_PROFILE environment variable set
 */
public final class CustomCredentialsProviderChain extends AWSCredentialsProviderChain {
  public CustomCredentialsProviderChain() {
    super(new ClasspathPropertiesFileCredentialsProvider(),
        new SystemPropertiesCredentialsProvider(), new EnvironmentVariableCredentialsProvider(),
        new ProfileCredentialsProvider());
  }
}
