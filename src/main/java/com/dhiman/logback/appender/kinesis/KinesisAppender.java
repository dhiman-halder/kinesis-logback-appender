package com.dhiman.logback.appender.kinesis;


import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.LayoutBase;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordRequest;


public class KinesisAppender extends AppenderBase<ILoggingEvent> {
    
	private int bufferSize = 2000;
	private int threadCount = 20;
	private int shutdownTimeout = 30;

	
	private AWSCredentialsProvider credentialsProvider = new CustomCredentialsProviderChain();
	private AmazonKinesisAsync kinesis;
	
	private String region;
	private String streamName;
	private ThreadPoolExecutor threadPoolExecutor;
	private LayoutBase<ILoggingEvent> layout;
	
	private LayoutBase<ILoggingEvent> getLayout() {
		return layout;
	}
	public void setLayout(LayoutBase<ILoggingEvent> layout) {
		this.layout = layout;
		
	}
	public String getRegion() {
		return region;
	}
	public void setRegion(String region) {
		this.region = region;
	}
    public String getStreamName() {
		return streamName;
	}
	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}
	
	@Override 
	public void start() {
		if (streamName == null || "".equals(streamName)) {
            addError("streamName is not set for KinesisAppender.");
            return;
        }
		if (region == null || "".equals(region)) {
            addError("region is not set for KinesisAppender.");
            return;
        }
		credentialsProvider = new ProfileCredentialsProvider();
		try {
	        credentialsProvider.getCredentials();
	    } catch (Exception e) {
	        throw new AmazonClientException("Cannot load AWS credentials", e);
	    }
	    
		ClientConfiguration clientConfiguration = new ClientConfiguration();
		clientConfiguration.setMaxErrorRetry(3);
	    clientConfiguration
	        .setRetryPolicy(new RetryPolicy(PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION,
	                                        PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY, 3, true));
	    BlockingQueue<Runnable> taskBuffer = new LinkedBlockingDeque<Runnable>(bufferSize);
	    threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount,30, TimeUnit.SECONDS,
	                                                taskBuffer, new BlockFastProducerPolicy());
	    threadPoolExecutor.prestartAllCoreThreads();
	    
		kinesis = AmazonKinesisAsyncClientBuilder.standard()
	            .withCredentials(credentialsProvider)
	            .withRegion(getRegion())
	            .withClientConfiguration(clientConfiguration)
	            .build();
	    super.start();
	}
    @Override
    protected void append(final ILoggingEvent event) {
        
    	 try {
    		 String message = getLayout().doLayout(event);
    		 //String message = event.getFormattedMessage();
	         ByteBuffer data = ByteBuffer.wrap(message.getBytes());
	         kinesis.putRecordAsync(new PutRecordRequest().withPartitionKey(UUID.randomUUID().toString())
	                .withStreamName(getStreamName()).withData(data));
         } catch (Exception e) {
             addError("Failed to schedule log entry for publishing into Kinesis stream: " + streamName, e);
         }
  
    }
    @Override
    public void stop() {
      threadPoolExecutor.shutdown();
      BlockingQueue<Runnable> taskQueue = threadPoolExecutor.getQueue();
      int bufferSizeBeforeShutdown = threadPoolExecutor.getQueue().size();
      boolean gracefulShutdown = true;
      try {
        gracefulShutdown = threadPoolExecutor.awaitTermination(shutdownTimeout, TimeUnit.SECONDS);
      }
      catch(InterruptedException e) {
        // we are anyways cleaning up
      }
      finally {
        int bufferSizeAfterShutdown = taskQueue.size();
        if(!gracefulShutdown || bufferSizeAfterShutdown > 0) {
          String errorMsg = "Kinesis Appender (" + name + ") waited for " + shutdownTimeout
                            + " seconds before terminating but could send only "
                            + (bufferSizeAfterShutdown - bufferSizeBeforeShutdown) + " logevents, it failed to send "
                            + bufferSizeAfterShutdown + " pending log events from it's processing queue";
          addError(errorMsg);
        }
      }
      kinesis.shutdown();
    }

}
