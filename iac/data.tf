# Lookups with the Cloud API key, reads only: ids, names, and endpoints the variables do not carry.

data "confluent_organization" "main" {
}

# returns the id of the environment, env-xxx + display name
data "confluent_environment" "main" {
  id = var.environment_id
}

# used to get region and display name of kafka cluster for flink statements
data "confluent_kafka_cluster" "main" {
  id = var.kafka_id
  environment {
    id = data.confluent_environment.main.id
  }
}

data "confluent_flink_compute_pool" "main" {
  id = var.flink_compute_pool_id
  environment {
    id = data.confluent_environment.main.id
  }
}

data "confluent_flink_region" "main" {
  cloud  = data.confluent_flink_compute_pool.main.cloud
  region = data.confluent_flink_compute_pool.main.region
}
