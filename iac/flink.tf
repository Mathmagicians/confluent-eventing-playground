# Flink on the environment's compute pool: the settle statement and the customer spend table per environment, the
# SQL under flink/. The pool and the endpoint come from data.tf, the principal from the variables, through the
# provider block.

resource "confluent_flink_statement" "settle" {
  # FIXME for_each over local.environments
  # FIXME statement = templatefile("${path.module}/flink/settle.sql", local.flink_sql[each.key])
  # FIXME properties = local.flink_properties
  # FIXME depends_on the transaction schema of the environment, the statement writes into that subject
}

# the table declares its own topic and subject, so neither appears in topics.tf or schemas.tf
resource "confluent_flink_materialized_table" "customer_spend" {
  # FIXME for_each over local.environments
  # FIXME display_name "<env>.customer_spend", kafka_cluster { id = var.kafka_id }
  # FIXME query = templatefile("${path.module}/flink/customer_spend.sql", local.flink_sql[each.key])
  # FIXME distribution { keys = ["customer_id"], buckets = local.partitions }
  # FIXME table_options: changelog.mode upsert, value.format proto-registry; session_options = local.flink_properties
  # FIXME lifecycle { prevent_destroy = true }, depends_on the settle statement of the environment
}
