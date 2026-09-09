# One topic per payload type under each environment prefix, each with its dead-letter twin: test.orders,
# test.orders.DLT. The dead-letter topic keeps the partition count of its source: the recoverer publishes to the
# same partition number.
resource "confluent_kafka_topic" "topic" {
  for_each         = local.topics
  topic_name       = each.key
  partitions_count = local.partitions
}
