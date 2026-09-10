-- Settles orders against offers into transactions, per environment; ${env} is the prefix, test or prod.
-- A stream-to-stream join, both sides are event streams, so it is bounded in time: an order settles against the
-- offer for the same product in the same region that was open when the order was placed. Time is $rowtime, the
-- record's timestamp, the time attribute Flink infers for every topic. The region comes from the CloudEvents
-- headers, headers.sql made them a column; the key is the customer id, the domain's rule for a transaction, see
-- Envelope.key(); the transaction id is the order and the offer it settles, so a reprocessed pair gets the same id.
-- The sink's headers are written here too, the same six names Converter writes, ce_time in RFC 3339.
INSERT INTO `${env}.transactions`
  (`key`, transaction_id, order_ref, offer_ref, customer_id, seller_id, price, created_at, headers)
SELECT
  CAST(o.customer_id AS BYTES),
  o.id || '/' || f.offer_id,
  CAST(ROW(o.id, o.customer_id, o.product_id, o.created_at)
    AS ROW<`id` STRING, `customer_id` STRING, `product_id` STRING, `created_at` TIMESTAMP_LTZ(9)>),
  CAST(ROW(f.offer_id, f.product_id, f.price, f.seller_id, f.created_at)
    AS ROW<`offer_id` STRING, `product_id` STRING, `price` DOUBLE, `seller_id` STRING, `created_at` TIMESTAMP_LTZ(9)>),
  o.customer_id,
  f.seller_id,
  f.price,
  o.created_at,
  MAP[
    CAST('ce_specversion' AS BYTES), CAST('1.0' AS BYTES),
    CAST('ce_id' AS BYTES),          CAST(o.id || '/' || f.offer_id AS BYTES),
    CAST('ce_source' AS BYTES),      CAST('flink/settle' AS BYTES),
    CAST('ce_type' AS BYTES),        CAST('dk.mathmagicians.playground.eventing.Transaction' AS BYTES),
    CAST('ce_time' AS BYTES),        CAST(DATE_FORMAT(o.`$rowtime`, 'yyyy-MM-dd''T''HH:mm:ss.SSS''Z''') AS BYTES),
    CAST('ce_region' AS BYTES),      o.headers[CAST('ce_region' AS BYTES)]
  ]
FROM `${env}.orders` o
JOIN `${env}.offers` f
  ON o.headers[CAST('ce_region' AS BYTES)] = f.headers[CAST('ce_region' AS BYTES)]
 AND o.product_id = f.product_id
 AND o.`$rowtime` BETWEEN f.`$rowtime` AND f.`$rowtime` + INTERVAL '10' MINUTE
