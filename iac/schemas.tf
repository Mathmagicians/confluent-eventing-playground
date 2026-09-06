# Every value on the wire is an Envelope, so every service topic carries the same schema: the checked-in proto file,
# registered under the topic's subject, TopicNameStrategy. The payload messages are in the same file, so the
# registry describes them too. Keys are plain strings and have no subject; dead-letter topics carry the bytes and
# their schema id unchanged and need none either.
locals {
  schema = file("${path.module}/../src/main/proto/schemas.proto")
}

# BACKWARD is Confluent's default; pinned here so a change to it is a reviewed change
resource "confluent_schema_registry_cluster_config" "main" {
  compatibility_level = "BACKWARD"
}

resource "confluent_schema" "envelope" {
  for_each = local.main

  subject_name = "${each.key}-value"
  format       = "PROTOBUF"
  schema       = local.schema
}
