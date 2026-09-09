-- Settles orders against offers into transactions, per environment; ${env} is the prefix, test or prod.
-- A stream-to-stream join: both sides are event streams, so the join is bounded in time. An order settles against
-- the offer for the same product that was open when the order was placed, $rowtime being the record's timestamp,
-- the time attribute Flink infers for every topic. The columns and the nested rows match transaction.proto field by
-- field; the registry already holds that schema for the sink.
INSERT INTO `${env}.transactions`
SELECT
  UUID()                                                            AS transaction_id,
  ROW(o.id, o.customer_id, o.product_id, o.created_at)              AS order_ref,
  ROW(f.offer_id, f.product_id, f.price, f.seller_id, f.created_at) AS offer_ref,
  o.customer_id,
  f.seller_id,
  f.price,
  o.created_at                                                      AS created_at
FROM `${env}.orders` o
JOIN `${env}.offers` f
  ON o.product_id = f.product_id
 AND o.`$rowtime` BETWEEN f.`$rowtime` AND f.`$rowtime` + INTERVAL '10' MINUTE
