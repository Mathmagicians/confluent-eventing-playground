-- The record headers of ${table} as a table column, a map of raw bytes: the CloudEvents envelope the producer
-- writes, ce_id, ce_source, ce_type, ce_time, and the extension ce_region. VIRTUAL on a source, read only; without
-- it on a sink, where the column is written and every INSERT must supply it. One-shot: the catalog keeps it.
ALTER TABLE `${table}` ADD (`headers` MAP<BYTES, BYTES> METADATA${virtual})
