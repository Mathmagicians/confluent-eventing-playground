# Flink on the environment's compute pool, orthogonal to the stories: it reads what the generator writes, never
# what a story writes. The product catalog, a materialized table over the products stream, per environment, the
# SQL under src/main/flink. The pool and the endpoint come from data.tf, the principal from the variables.

# the headers column on products, a Flink-side addition to the inferred table that the catalog reads: an ALTER, one
# shot, whose life is the topic's, re-run exactly when the topic is recreated and never when its text changes
resource "confluent_flink_statement" "products_headers" {
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
  statement = templatefile(local.flink_headers_sql_file, local.flink_sql[each.key])
  properties = {
    "sql.current-catalog"  = data.confluent_environment.main.display_name
    "sql.current-database" = data.confluent_kafka_cluster.main.display_name
  }

  depends_on = [confluent_schema.topic]
}

# the table declares its own topic and subject, so neither appears in topics.tf or schemas.tf
resource "confluent_flink_materialized_table" "product_lvs" {
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

  display_name = "${each.key}.products.lvs"
  kafka_cluster {
    id = var.kafka_id
  }
  query = templatefile(local.flink_product_catalog_sql_file, local.flink_sql[each.key])
  # one row per product, its latest version kept on the compacted topic; the same partition count as the topics
  distribution {
    kind         = "HASH"
    keys         = ["region", "product_id"]
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

  depends_on = [confluent_schema.topic, confluent_flink_statement.products_headers]
}
