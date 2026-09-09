# Lookups with the Cloud API key, reads only: ids, names, and endpoints the variables do not carry.

data "confluent_organization" "main" {
    cloud_api_key    = var.cloud_api_key
    cloud_api_secret = var.cloud_api_secret
}

# returns the id of the environment, env-xxx + display name
data "confluent_environment" "main" {
  cloud_api_key    = var.cloud_api_key
  cloud_api_secret = var.cloud_api_secret
}

# used to get region and display name of kafka cluster for flink statements
data "confluent_kafka_cluster" "main" {
  id = var.kafka_id
  environment {
    id = data.confluent_environment.main.id
  }
}

data "confluent_flink_compute_pool" "main" {
  display_name = var.flink_compute_pool_name
  environment {
    id = data.confluent_environment.main.id
  }
}

data "confluent_flink_region" "main" {
  cloud  = data.confluent_flink_compute_pool.main.cloud
  region = data.confluent_flink_compute_pool.main.region
}
