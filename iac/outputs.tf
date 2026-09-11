# the cluster and the registry as this workspace knows them: the data plane, what the topics and schemas live on
output "cluster" {
  value = {
    id   = var.kafka_id
    rest = var.kafka_rest_endpoint
  }
}

output "schema_registry" {
  value = {
    id       = var.schema_registry_id
    endpoint = var.schema_registry_rest_endpoint
  }
}

# the topics that are created
output "topics" {
  value = { for topic in confluent_kafka_topic.topic : topic.topic_name => topic.partitions_count }
}

# the schema version per subject, as registered
output "schemas" {
  value = {
    for schema in concat(values(confluent_schema.topic), values(confluent_schema.transaction)) :
    schema.subject_name => schema.version
  }
}

# information about Flink compute pools, from data sources, not resources
output "compute_pool" {
  value = {
    environment = data.confluent_environment.main.id
    id          = data.confluent_flink_compute_pool.main.id
    name        = data.confluent_flink_compute_pool.main.display_name
    endpoint    = local.flink_rest_endpoint
    principal   = var.flink_principal_id
    # where a statement resolves unqualified table names: the environment as the catalog, the cluster as the database
    catalog  = data.confluent_environment.main.display_name
    database = data.confluent_kafka_cluster.main.display_name
  }
}

# the tables on the pool, per environment; make flink-verify reads them
output "flink" {
  value = {
    for env in local.environments : env => {
      product_catalog = confluent_flink_materialized_table.product_lvs[env].display_name
    }
  }
}
