# sse
Tests various MOM(s) with Server Sent Events (SSE) and Command Query

## Building 
gradle clean shadowJar


## Running An Event Source

### Docker (make life easy)
#### Testing with Redis
docker pull redis

docker run -it -p 6379:6379 redis

#### Testing with Apache Pulsar
docker pull apachepulsar/pulsar

docker run -it -p 6650:6650 -p 8080:8080 -v ~/data:/pulsar/data apachepulsar/pulsar bin/pulsar standalone


#### Testing with Apache Kafka
https://docs.confluent.io/current/installation/docker/docs/quickstart.html

git clone https://github.com/confluentinc/cp-docker-images

cd examples/kafka-single-node/

docker-compose up

in a separate terminal create the topic:

cd examples/kafka-single-node/

export KAFKA_TOPIC_NAME=test

docker-compose exec kafka  kafka-topics --create --topic $KAFKA_TOPIC_NAME --partitions 1 --replication-factor 1 --if-not-exists --zookeeper localhost:32181

#### API
##### consumer long poll
http://localhost:8000/poll?topic=test

##### producer
http://localhost:8000/post?topic=test

#### Running the SSE service
export SOURCE=pulsar
java -jar build/libs/sse-0.1-all.jar --bootstrap=$SOURCE://localhost

#### Running the SSE consumer
java -cp build/libs/sse-0.1-all.jar io.streamz.sse.consumer.Main

#### Running the SSE consumer starting from a message id
java -cp build/libs/sse-0.1-all.jar io.streamz.sse.consumer.Main -f messageId

