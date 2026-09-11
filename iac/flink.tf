# Flink on the environment's compute pool, orthogonal to the stories: it reads what the generator writes, never
# what a story writes. The product catalog, a materialized table over the products stream, per environment, the
# SQL under src/main/flink. The pool and the endpoint come from data.tf, the principal from the variables.

# the table declares its own topic and subject, so neither appears in topics.tf or schemas.tf
resource "confluent_flink_materialized_table" "product_catalog" {
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

  display_name = "${each.key}.product_catalog"
  kafka_cluster {
    id = var.kafka_id
  }
  query = templatefile(local.flink_product_catalog_sql_file, local.flink_sql[each.key])
  # one row per product, its latest version kept on the compacted topic; the same partition count as the topics
  distribution {
    kind         = "HASH"
    keys         = ["product_id"]
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
  depends_on = [confluent_schema.topic]
}
