# the cluster: what a client connects to, and where it lives
output "cluster" {
  value = {
    id        = data.confluent_kafka_cluster.main.id
    name      = data.confluent_kafka_cluster.main.display_name
    bootstrap = data.confluent_kafka_cluster.main.bootstrap_endpoint
    rest      = data.confluent_kafka_cluster.main.rest_endpoint
    cloud     = data.confluent_kafka_cluster.main.cloud
    region    = data.confluent_kafka_cluster.main.region
  }
}

# the registry: what the serializer and the test suite talk to
output "schema_registry" {
  value = {
    id       = data.confluent_schema_registry_cluster.main.id
    endpoint = data.confluent_schema_registry_cluster.main.rest_endpoint
    package  = data.confluent_schema_registry_cluster.main.package
  }
}

# the topics as created, partition count per name; shown at the end of every run and in the plan's summary
output "topics" {
  value = { for topic in confluent_kafka_topic.topic : topic.topic_name => topic.partitions_count }
}

# the schema version per subject, as registered
output "schemas" {
  value = {
    for schema in concat(values(confluent_schema.topic), values(confluent_schema.transaction)) :
    schema.subject_name => schema.version
  }
}
