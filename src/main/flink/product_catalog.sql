-- The product catalog, a materialized table over the products stream, per environment;
-- The placeholder ${env} is substituted by terraform, and it's function is to be the prefix of the materialized table's name.
-- Every product record is a new version of the same thing: the name stays -it's the id, the description and the producer can be updated.
-- The catalog keeps each product once, as it was last described, and counts its versions.
-- This file holds the query alone: the table's name, key, buckets, changelog mode, and format are the resource's arguments in iac/flink.tf.
-- Terraform prefixes this with CREATE MATERILIZED TABLE ... AS 

SELECT region, product_id,
          LAST_VALUE(product_name) AS product_name,
          LAST_VALUE(producer_id) AS producer_id,
          LAST_VALUE(product_description) AS product_descriptions,
          COUNT(*) AS versions,
          MAX(arrived_at) AS updated_at
   FROM(
           SELECT COALESCE(DECODE(headers[ENCODE('ce_region', 'UTF-8')], 'UTF-8'), 'NONE') AS region,
                  product_id, product_name, producer_id, product_description,
                  `$rowtime` AS arrived_at
           FROM `${env}.test.products`
       )
   GROUP BY region, product_id

-- FIXME two window functions over the same partition, each product's records in record-time order:
-- FIXME   ROW_NUMBER() OVER (PARTITION BY product_id ORDER BY `$rowtime` DESC) AS rn, 1 for the latest version
-- FIXME   COUNT(*)     OVER (PARTITION BY product_id)                            AS versions
-- FIXME an outer query keeps rn = 1: product_id, product_name, producer_id, product_description, versions,
-- FIXME   and `$rowtime` AS updated_at, the time the latest version arrived
-- FIXME Flink recognises the rn = 1 filter over ROW_NUMBER as deduplication and keeps one row per product in
-- FIXME   upsert mode; without the ORDER BY DESC it would keep the first version instead of the latest
-- FIXME no trailing semicolon, the resource submits the query as is
