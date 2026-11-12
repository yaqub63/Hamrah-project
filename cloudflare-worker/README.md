# Secure AI backend for Hamrah Project

This Worker uses an OpenAI-compatible Chat Completions API. It is provider-independent: change the endpoint, model, and secret in Cloudflare without rebuilding the Android app.

## Cloudflare settings

In Worker Settings → Variables and Secrets, configure:

- Secret `AI_API_KEY`: API key from the provider you choose.
- Variable `AI_BASE_URL`: provider API base URL (the Worker appends `/chat/completions`, unless already present).
- Variable `AI_MODEL`: exact model identifier supported by that provider.
- Optional variable `AI_PROVIDER`: a display label such as `provider-name`.
- Optional variable `APP_ORIGIN`: restrict browser origins if needed.

Do not add API keys to GitHub, the Android app, or chat messages.

## Test

Open `https://YOUR-WORKER.workers.dev/health`. It should return `configured: true` after all three required settings are saved.

Endpoint: POST `/chat`

Request:
`{"messages":[{"role":"user","content":"می‌خواهم یک مخزن آب بسازم"}]}`

Response:
`{"answer":"...","model":"...","provider":"..."}`

The provider must support the OpenAI-compatible Chat Completions format. Availability, regional access, pricing, and data-use terms depend on the selected provider and must be checked before production use. Never promise unrestricted access based only on API compatibility.
