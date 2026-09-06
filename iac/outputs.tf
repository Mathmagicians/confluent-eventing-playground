# the topics as created, partition count per name; shown at the end of every run and in the plan's summary
output "topics" {
  value = { for topic in confluent_kafka_topic.topic : topic.topic_name => topic.partitions_count }
}

# the schema version per subject, as registered
output "schemas" {
  value = { for schema in confluent_schema.envelope : schema.subject_name => schema.version }
}
