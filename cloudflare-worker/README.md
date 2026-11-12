# Secure AI backend for Hamrah Project

The Worker is configured for xAI Grok 4.7 through the OpenAI-compatible Chat Completions API. The Android app does not contain the provider API key.

## Cloudflare settings

The repository's `wrangler.toml` already sets these non-secret variables:

- `AI_PROVIDER`: `xai`
- `AI_BASE_URL`: `https://api.x.ai/v1`
- `AI_MODEL`: `grok-4.7`

Add only this value in Cloudflare Worker → Settings → Variables and Secrets:

- Secret `AI_API_KEY`: create an API key in the xAI Console and save it as a secret. Never commit it to GitHub, put it in the Android app, or send it in chat.

## Test

Open `https://YOUR-WORKER.workers.dev/health`. It should return `configured: true` after the secret is saved and the Worker is deployed.

Endpoint: POST `/chat`

Request:
`{"messages":[{"role":"user","content":"می‌خواهم یک مخزن آب بسازم"}]}`

The provider must support the OpenAI-compatible Chat Completions format. Confirm account access, billing, regional availability, and current pricing in the xAI Console before production use. API compatibility does not guarantee access from every country.

## Deployment

Deploy the `cloudflare-worker` directory as the Worker source (entry point `src/index.js`). If the Worker is still configured with Dashboard source, publish the repository changes through the Cloudflare Git integration or deploy this directory using Wrangler. The xAI API key must remain a Cloudflare secret.
