# The one Kafka cluster and its API key, Terraform variables of the Terraform Cloud workspace.
variable "kafka_id" {
  type = string
}

variable "kafka_rest_endpoint" {
  type = string
  validation {
    condition     = startswith(var.kafka_rest_endpoint, "https://")
    error_message = "The cluster's REST endpoint, https://pkc-...:443, not the bootstrap server."
  }
}

variable "kafka_api_key" {
  type      = string
  sensitive = true
}

variable "kafka_api_secret" {
  type      = string
  sensitive = true
}

# The Schema Registry of the environment and its API key, Terraform variables of the workspace as well.
variable "schema_registry_id" {
  type = string
}

variable "schema_registry_rest_endpoint" {
  type = string
  validation {
    condition     = startswith(var.schema_registry_rest_endpoint, "https://")
    error_message = "The Schema Registry endpoint, https://psrc-...confluent.cloud."
  }
}

variable "schema_registry_api_key" {
  type      = string
  sensitive = true
}

variable "schema_registry_api_secret" {
  type      = string
  sensitive = true
}

# The Confluent Cloud environment and a Cloud API key: cloud-level reads, the cluster's endpoints, and cloud-level
# resources, service accounts, role bindings, Flink; Terraform variables of the workspace as well.
variable "environment_id" {
  type = string
  validation {
    condition     = startswith(var.environment_id, "env-")
    error_message = "The environment id, env-..., from the environment's settings page."
  }
}

variable "cloud_api_key" {
  type      = string
  sensitive = true
}

variable "cloud_api_secret" {
  type      = string
  sensitive = true
}
