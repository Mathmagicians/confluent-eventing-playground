# Automation

The tasks of each feature in the [catalog](release-0.0.8.md), each with its owner and the scenario it turns green.

## Eventing over Cloudflare

| Task | Owner | Turns green |
|---|---|---|
| The publisher and the pull listener over the Queues HTTP API, `adapter/cloudflare` | themathmagician | the map's scenarios |
| The queues and their dead-letter queues in `iac/`, provider `cloudflare/cloudflare` | pair | the queues exist per environment |
| `TRANSPORT=cloudflare` in `platform.mk`, the account's token in `.env.<ENV>.private` and a GitHub environment | pair | `make run-tiny TRANSPORT=cloudflare` |
| The deck, "Same stories, three transports" with the fourth | pair | the slide shows four |
