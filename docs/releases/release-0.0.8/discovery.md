# Discovery

The example map of each feature in the [catalog](release-0.0.8.md): its story, its rules with examples, its questions,
and the decisions it raises for [docs/adr.md](../../adr.md).

## Eventing over Cloudflare

**Goal:** a story plays on Cloudflare as a fourth transport, `cloudflare`, beside `cli`, `kafka`, and `confluent`,
through adapters of its own at the rim of the hexagon.

- 0.0.7 makes the transport a profile beside the environment.
- Queues takes a message over HTTP, `POST /accounts/{account}/queues/{queue}/messages`, a body with the content type
  `json` or `text`, under a token with `Queues Edit`
  ([publish via HTTP](https://developers.cloudflare.com/queues/examples/publish-to-a-queue-via-http/),
  [push API](https://developers.cloudflare.com/api/resources/queues/subresources/messages/methods/push/)).
- A pull consumer reads over HTTP, `messages/pull` and `messages/ack`, up to 100 messages a pull, each with `body`,
  `id`, `timestamp_ms`, `attempts`, and `lease_id`, and a visibility timeout up to 12 hours
  ([pull consumers](https://developers.cloudflare.com/queues/configuration/pull-consumers/)).
- A message is its body; the attributes a Kafka record carries as headers ride inside it.
- Queues delivers at least once, and the consumer recognises a repeat by an id the producer writes
  ([delivery guarantees](https://developers.cloudflare.com/queues/reference/delivery-guarantees/)); the order of
  delivery is best effort ([JavaScript APIs](https://developers.cloudflare.com/queues/configuration/javascript-apis/)).
- A message the consumer keeps failing goes to the queue's dead-letter queue
  ([dead-letter queues](https://developers.cloudflare.com/queues/configuration/dead-letter-queues/)).
- A Durable Object is one single-threaded instance per name, reached from a Worker
  ([Durable Objects](https://developers.cloudflare.com/durable-objects/)).
- Pipelines takes JSON over HTTP or a Worker binding, transforms it row by row in SQL, and writes it to R2 as Iceberg
  tables or Parquet and JSON files; it is in open beta, and its streams, SQL, and sinks are fixed at creation
  ([Pipelines](https://developers.cloudflare.com/pipelines/)).
- `wrangler dev` simulates Queues for Workers through Miniflare
  ([local development](https://developers.cloudflare.com/queues/configuration/local-development/)).
- On Kafka the key picks the partition, so the records of one key stay in order (README, Purpose), and the envelope
  rides as CloudEvents headers (README, Kafka and Confluent Cloud).

**Story:** I play a story on Cloudflare with `TRANSPORT=cloudflare`, and the stories stay as they are.

### Rule: a generator publishes each payload type to a queue of its own, the counterpart of its topic, from the platform over HTTP
### Rule: a listening story pulls its queues and acknowledges what it handled; a message it keeps failing goes to the dead-letter queue
### Rule: a story handles a repeated message as it handled the first, by the envelope id

**Questions**
1. Order per key: the README's rule holds through a Durable Object per key, with a Worker in front of the queues;
   or the transport delivers in best-effort order, and the features about order run on `kafka` and `confluent`?
2. The envelope in the body: CloudEvents structured mode in JSON with the payload as `data_base64`, or the
   `Envelope` message of `envelope.proto` in its JSON form?
3. The schema: the Confluent framing with its schema id, base64 in the body, against a registry the platform
   reaches; or the Protobuf JSON form, checked against the platform's own descriptors?
4. The runs: a Cloudflare account for the developer machine and for CI, its token in `.env.<ENV>.private` and a
   GitHub environment `cloudflare-<env>`?
5. Pipelines: the counterpart of the `@confluent` Flink feature, the generator's JSON in an R2 table, with the
   latest value snapshot as a query over it?
