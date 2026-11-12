const SYSTEM_PROMPT = `تو دستیار هوشمند و متخصص مدیریت پروژه‌های عمرانی و زیرساختی هستی. به فارسی روان پاسخ بده. ابتدا نیاز کاربر را بفهم و فقط یک سؤال ضروری در هر نوبت بپرس. برای پروژه‌های مخزن آب، خطوط انتقال، ایستگاه پمپاژ و تصفیه‌خانه، ساختار شکست کار، فعالیت‌ها، وابستگی‌ها، مدت و برآورد هزینه پیشنهاد بده. هر عددی که از اسناد یا داده کاربر استخراج نشده، فرض اولیه و قابل ویرایش است؛ آن را قطعی جلوه نده. برای تغییر یا حذف داده‌های ثبت‌شده تأیید بگیر. خروجی را کاربردی، مرحله‌ای و مختصر نگه دار.`;

function json(data, status = 200, env = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store", ...corsHeaders(env) }
  });
}

function corsHeaders(env) {
  return {
    "access-control-allow-origin": env.APP_ORIGIN || "*",
    "access-control-allow-methods": "GET, POST, OPTIONS",
    "access-control-allow-headers": "content-type"
  };
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: corsHeaders(env) });

    const provider = (env.AI_PROVIDER || "openai-compatible").toLowerCase();
    const configured = Boolean(env.AI_API_KEY && env.AI_BASE_URL && env.AI_MODEL);

    if (url.pathname === "/health" && request.method === "GET") {
      return json({ ok: true, service: "hamrah-project-ai", provider, configured }, 200, env);
    }
    if (url.pathname !== "/chat" || request.method !== "POST") {
      return json({ error: "مسیر یا روش درخواست معتبر نیست." }, 404, env);
    }
    if (!configured) {
      return json({ error: "تنظیمات سرویس هوش مصنوعی کامل نیست." }, 503, env);
    }

    let body;
    try { body = await request.json(); }
    catch { return json({ error: "بدنه درخواست معتبر نیست." }, 400, env); }

    const incoming = Array.isArray(body.messages) ? body.messages : [];
    if (!incoming.length || incoming.length > 30) {
      return json({ error: "تعداد پیام‌ها معتبر نیست." }, 400, env);
    }

    const messages = [{ role: "system", content: SYSTEM_PROMPT }];
    for (const item of incoming) {
      const role = item.role === "assistant" ? "assistant" : "user";
      const content = String(item.content || "").slice(0, 8000);
      if (!content.trim()) return json({ error: "پیام خالی پذیرفته نیست." }, 400, env);
      messages.push({ role, content });
    }

    try {
      const base = env.AI_BASE_URL.replace(/\/+$/, "");
      const endpoint = base.endsWith("/chat/completions") ? base : base + "/chat/completions";
      const upstream = await fetch(endpoint, {
        method: "POST",
        headers: {
          "content-type": "application/json",
          "authorization": "Bearer " + env.AI_API_KEY
        },
        body: JSON.stringify({
          model: env.AI_MODEL,
          messages,
          temperature: 0.45,
          max_tokens: 1800
        })
      });

      let payload = {};
      try { payload = await upstream.json(); } catch {}
      if (!upstream.ok) {
        if (upstream.status === 429) return json({ error: "سهمیه یا ظرفیت سرویس هوش مصنوعی موقتاً در دسترس نیست." }, 429, env);
        return json({ error: "سرویس هوش مصنوعی پاسخ موفق نداد. تنظیمات ارائه‌دهنده را بررسی کنید." }, 502, env);
      }

      const answer = payload.choices?.[0]?.message?.content;
      if (typeof answer !== "string" || !answer.trim()) {
        return json({ error: "پاسخ قابل‌استفاده‌ای از هوش مصنوعی دریافت نشد." }, 502, env);
      }
      return json({ answer: answer.trim(), model: env.AI_MODEL, provider }, 200, env);
    } catch {
      return json({ error: "خطای شبکه در ارتباط با سرویس هوش مصنوعی." }, 502, env);
    }
  }
};
