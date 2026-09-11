-- The headers column on ${env}.products, the Flink-side addition the catalog reads. A metadata column is declared
-- on the table, so headers from kafka topic make it to the products flink tables.
ALTER TABLE `${env}.products` ADD (`headers` MAP<BYTES, BYTES> METADATA VIRTUAL)
