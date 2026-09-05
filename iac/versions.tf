terraform {
  # state lives in Terraform Cloud; organization and workspace come from TF_CLOUD_ORGANIZATION and TF_WORKSPACE
  cloud {}

  required_version = ">= 1.5"

  required_providers {
    confluent = {
      source  = "confluentinc/confluent"
      version = "~> 2.85"
    }
  }
}

# one Kafka cluster; KAFKA_ID, KAFKA_REST_ENDPOINT, KAFKA_API_KEY and KAFKA_API_SECRET come from the environment
provider "confluent" {}
