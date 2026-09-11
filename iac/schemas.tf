# Each service topic carries the schema of its payload, the message's proto file registered under the topic's
# subject, TopicNameStrategy: order.proto on test.orders-value. Keys are plain strings and have no subject;
# dead-letter topics carry the bytes and their schema id unchanged and need none either.

# BACKWARD is Confluent's default; pinned here so a change to it is a reviewed change
resource "confluent_schema_registry_cluster_config" "main" {
  compatibility_level = "BACKWARD"
}

resource "confluent_schema" "topic" {
  for_each = local.subjects

  subject_name = "${each.key}-value"
  format       = "PROTOBUF"
  schema       = file("${local.proto}/${each.value}")
  # FIXME hard_delete = true while nothing is live: a destroy removes the subject for real, so a rebuild starts clean
}

# transaction.proto imports order.proto and offer.proto; the registry resolves the imports through references to
# the subjects above, the reference name being the import's filename
resource "confluent_schema" "transaction" {
  for_each = toset(local.environments)

  subject_name = "${each.key}.transactions-value"
  format       = "PROTOBUF"
  schema       = file("${local.proto}/transaction.proto")
  # the referenced versions are unknown while the plan creates them in the same run; the registry validates on apply
  skip_validation_during_plan = true
  # FIXME hard_delete = true, same as above

  dynamic "schema_reference" {
    for_each = ["orders", "offers"]
    content {
      name         = local.schemas[schema_reference.value]
      subject_name = confluent_schema.topic["${each.key}.${schema_reference.value}"].subject_name
      version      = confluent_schema.topic["${each.key}.${schema_reference.value}"].version
    }
  }
}
