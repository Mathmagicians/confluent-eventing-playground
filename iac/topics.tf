# One topic per payload type under each environment prefix, each with its dead-letter twin:
# test.orders, test.orders.DLT.
# The partition count is fixed at creation: the key picks the partition by partition count.
locals {
  names        = ["products", "offers", "orders", "transactions"]
  environments = ["test", "prod"]
  partitions   = 6

  # the topics the services use, one per environment and name: test.orders
  main = toset([for pair in setproduct(local.environments, local.names) : "${pair[0]}.${pair[1]}"])
  # plus a dead-letter twin each: test.orders.DLT
  topics = setunion(local.main, toset([for topic in local.main : "${topic}.DLT"]))
}

# the dead-letter topic keeps the partition count of its source: the recoverer publishes to the same partition number
resource "confluent_kafka_topic" "topic" {
  for_each         = local.topics
  topic_name       = each.key
  partitions_count = local.partitions
}
