# kinesis-logback-appender

logback.xml sample
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="kinesis" class="com.dhiman.logback.appender.kinesis.KinesisAppender">
        <streamName>logback-kinesis-stream</streamName>
    	<region>us-east-1</region>
    	<layout class="ch.qos.logback.classic.PatternLayout">
      		<pattern>%d %-5level [%thread] %logger{0}: %msg%n</pattern>
    	</layout> 	
    </appender>
    <root level="info">
        <appender-ref ref="kinesis"/>
    </root>
</configuration>
```
