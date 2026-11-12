# Secure AI backend for Hamrah Project

This Worker proxies Gemini requests. The Gemini key is stored only as a Cloudflare secret and must never be placed in the Android APK.

## Deploy (one-time account authorization)
1. Create a Cloudflare account and open Workers & Pages.
2. Create a Worker named `hamrah-project-ai` and deploy the contents of `src/index.js`.
3. In Worker Settings → Variables and Secrets, add secret `GEMINI_API_KEY` (create the key in Google AI Studio; never send it in chat or commit it).
4. Optional variable: `GEMINI_MODEL` = `gemini-2.5-flash`.
5. Test `https://YOUR-WORKER.workers.dev/health`; it should return `configured: true`.

Endpoint: POST `/chat`
Request: `{"messages":[{"role":"user","content":"می‌خواهم یک مخزن آب بسازم"}]}`
Response: `{"answer":"...","model":"gemini-2.5-flash"}`

The backend reports quota and configuration failures honestly. Do not send confidential contracts or personal data until the provider's data-use terms have been reviewed. Free-tier limits apply.
