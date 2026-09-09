# Terraform variables of the Terraform Cloud workspace: what exists before this configuration, by id, endpoint,
# or key. Everything else is looked up in data.tf or derived in locals.tf.

# --- the Kafka cluster and its API key --------------------------------------------------------------------------
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

# --- the Schema Registry of the environment and its API key -------------------------------------------------------
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

# --- the environment, read with a key of scope Cloud resource management ------------------------------------------
variable "cloud_api_key" {
  type      = string
  sensitive = true
}

variable "cloud_api_secret" {
  type      = string
  sensitive = true
}

# --- the environment in Confluent
variable "environment_id" {
  type = string
  validation {
    condition     = startswith(var.environment_id, "env-")
    error_message = "The environment id must start with 'env-'."
  }
}

# --- Flink: the pool by name, the principal the statements run as, the key of scope Flink region ------------------
variable "flink_compute_pool_name" {
  type = string
}

variable "flink_principal_id" {
  type = string
  validation {
    condition     = startswith(var.flink_principal_id, "u-") || startswith(var.flink_principal_id, "sa-")
    error_message = "The Flink principal id must start with 'u-' or 'sa-'."
  }
}

variable "flink_api_key" {
  type      = string
  sensitive = true
}

variable "flink_api_secret" {
  type      = string
  sensitive = true
}
