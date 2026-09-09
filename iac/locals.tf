# Derived values, one place: the environments and the names, and what the resource files build from them.

locals {
  # --- topics ---------------------------------------------------------------------------------------------------
  names        = ["products", "offers", "orders", "transactions"]
  environments = ["test", "prod"]
  # the partition count is fixed at creation: the key picks the partition by partition count
  partitions = 6

  # the topics the services use, one per environment and name: test.orders
  main = toset([for pair in setproduct(local.environments, local.names) : "${pair[0]}.${pair[1]}"])
  # plus a dead-letter twin each: test.orders.DLT
  topics = setunion(local.main, toset([for topic in local.main : "${topic}.DLT"]))

  # --- schemas --------------------------------------------------------------------------------------------------
  proto = "${path.module}/../src/main/proto"
  # the schema of each topic; transactions is in schemas.tf, its imports are references to these
  schemas = {
    products = "product.proto"
    offers   = "offer.proto"
    orders   = "order.proto"
  }
  # test.orders => order.proto
  subjects = {
    for pair in setproduct(local.environments, keys(local.schemas)) : "${pair[0]}.${pair[1]}" => local.schemas[pair[1]]
  }

  # --- flink ----------------------------------------------------------------------------------------------------
  flink_rest_endpoint = data.confluent_flink_region.main.rest_endpoint
  organization = data.confluent_organization.main.id

  # FIXME flink_sql: { env => { env = env } } over local.environments, the templatefile variables
  # FIXME flink_properties: sql.current-catalog and sql.current-database from the two display names in data.tf
}
