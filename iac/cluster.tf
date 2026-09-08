# The cluster and the registry as Confluent Cloud describes them, read through the Cloud API key. Both exist before
# this configuration; they are facts, not resources, and the outputs carry them.
data "confluent_kafka_cluster" "main" {
  id = var.kafka_id
  environment {
    id = var.environment_id
  }
}

data "confluent_schema_registry_cluster" "main" {
  id = var.schema_registry_id
  environment {
    id = var.environment_id
  }
}
