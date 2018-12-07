# sse
Tests Pulsar with Server Sent Events (SSE) and Command Query

## Building 
gradle clean shadowJar


## Running An Event Source

### Docker (make life easy)
#### Testing with Apache Pulsar
docker pull apachepulsar/pulsar

docker run -it -p 6650:6650 -p 8080:8080 -v ~/data:/pulsar/data apachepulsar/pulsar bin/pulsar standalone

#### API
##### consumer long poll
http://localhost:8000/poll?topic=test&sname=subN

##### consumer sse (subscribe)
http://localhost:8000/sub?topic=test&sname=subN

##### producer
http://localhost:8000/post?topic=test

#### Running the SSE service

java -jar build/libs/sse-0.1-all.jar --bootstrap=pulsar://localhost

#### Running the SSE producer
java -cp build/libs/sse-0.1-all.jar io.streamz.sse.producer.Main -m 'testing' -c 10

#### Running the SSE consumer
java -cp build/libs/sse-0.1-all.jar io.streamz.sse.consumer.Main

#### Running the SSE consumer starting from a message id
java -cp build/libs/sse-0.1-all.jar io.streamz.sse.consumer.Main -f messageId -n subN
