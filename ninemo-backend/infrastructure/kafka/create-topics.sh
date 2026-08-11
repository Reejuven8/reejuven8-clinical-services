#!/bin/bash
# Wait for Kafka to be ready
echo "Waiting for Kafka..."
sleep 10

KAFKA_BOOTSTRAP=localhost:9092

kafka-topics --create --if-not-exists \
  --bootstrap-server $KAFKA_BOOTSTRAP \
  --topic abdm.consent.granted \
  --partitions 3 \
  --replication-factor 1

kafka-topics --create --if-not-exists \
  --bootstrap-server $KAFKA_BOOTSTRAP \
  --topic abdm.data.received \
  --partitions 3 \
  --replication-factor 1

kafka-topics --create --if-not-exists \
  --bootstrap-server $KAFKA_BOOTSTRAP \
  --topic document.data.parsed \
  --partitions 3 \
  --replication-factor 1

echo "Topics created:"
kafka-topics --list --bootstrap-server $KAFKA_BOOTSTRAP
