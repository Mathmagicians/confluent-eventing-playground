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
