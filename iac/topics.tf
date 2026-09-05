# One topic per payload type under each environment prefix, each with its dead-letter twin: test.orders, test.orders.DLT.
# The partition count is fixed at creation: the key picks the partition by partition count.
locals {
  names        = ["products", "offers", "orders", "transactions"]
  environments = ["test", "prod"]
  partitions   = 6

  topics = toset(flatten([
    for name in local.names : [
      for environment in local.environments : ["${environment}.${name}", "${environment}.${name}.DLT"]
    ]
  ]))
}

# the dead-letter topic keeps the partition count of its source: the recoverer publishes to the same partition number
resource "confluent_kafka_topic" "topic" {
  for_each = local.topics

  topic_name       = each.key
  partitions_count = local.partitions
}
