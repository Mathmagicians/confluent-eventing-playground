# Flink on the environment's compute pool: the settle statement and the customer spend table per environment, the
# SQL under src/main/flink. The pool and the endpoint come from data.tf, the principal from the variables, through
# the provider block.

resource "confluent_flink_statement" "settle_sql" {
  for_each = toset(local.environments)
  organization {
    id = data.confluent_organization.main.id
  }
  environment {
    id = local.confluent_environment
  }
  compute_pool {
    id = data.confluent_flink_compute_pool.main.id
  }

  principal {
    id = var.flink_principal_id
  }

  rest_endpoint = local.flink_rest_endpoint

  credentials {
    key    = var.flink_api_key
    secret = var.flink_api_secret
  }

  properties = {
    "sql.current-catalog"  = data.confluent_environment.main.display_name
    "sql.current-database" = data.confluent_kafka_cluster.main.display_name
  }
  statement  = templatefile(local.flink_settle_sql_file, local.flink_sql[each.key])
  depends_on = [confluent_schema.transaction, confluent_flink_statement.headers]
}

# the record headers as a table column, one-shot per table: virtual on the sources the join reads, persisted on the
# sink it writes, see headers.sql
resource "confluent_flink_statement" "headers" {
  for_each = local.flink_headers
  organization {
    id = data.confluent_organization.main.id
  }
  environment {
    id = local.confluent_environment
  }
  compute_pool {
    id = data.confluent_flink_compute_pool.main.id
  }

  principal {
    id = var.flink_principal_id
  }

  rest_endpoint = local.flink_rest_endpoint

  credentials {
    key    = var.flink_api_key
    secret = var.flink_api_secret
  }

  properties = {
    "sql.current-catalog"  = data.confluent_environment.main.display_name
    "sql.current-database" = data.confluent_kafka_cluster.main.display_name
  }
  statement  = templatefile(local.flink_headers_sql_file, each.value)
  depends_on = [confluent_kafka_topic.topic, confluent_schema.topic, confluent_schema.transaction]
}

# the table declares its own topic and subject, so neither appears in topics.tf or schemas.tf
resource "confluent_flink_materialized_table" "customer_spend" {
  for_each = toset(local.environments)
  organization {
    id = data.confluent_organization.main.id
  }
  environment {
    id = local.confluent_environment
  }
  compute_pool {
    id = data.confluent_flink_compute_pool.main.id
  }

  principal {
    id = var.flink_principal_id
  }

  rest_endpoint = local.flink_rest_endpoint

  credentials {
    key    = var.flink_api_key
    secret = var.flink_api_secret
  }

  display_name = "${each.key}.customer_spend"
  kafka_cluster {
    id = var.kafka_id
  }
  query = templatefile(local.flink_customer_spend_sql_file, local.flink_sql[each.key])
  # one row per customer, the latest kept on the compacted topic; the same partition count as the topics
  distribution {
    kind         = "HASH"
    keys         = ["customer_id"]
    bucket_count = local.partitions
  }
  table_options = {
    "changelog.mode" = "upsert"
    "value.format"   = "proto-registry"
  }
  session_options = {
    "sql.current-catalog"  = data.confluent_environment.main.display_name
    "sql.current-database" = data.confluent_kafka_cluster.main.display_name
  }
  # a destroy takes the topic and the schema the table owns; lifted on purpose, never by a plan
  lifecycle {
    prevent_destroy = true
  }
  depends_on = [confluent_flink_statement.settle_sql]
}
