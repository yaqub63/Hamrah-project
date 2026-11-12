const SYSTEM_PROMPT = `تو دستیار هوشمند و متخصص مدیریت پروژه‌های عمرانی و زیرساختی هستی. به فارسی روان پاسخ بده. ابتدا نیاز کاربر را بفهم و فقط یک سؤال ضروری در هر نوبت بپرس. برای پروژه‌های مخزن آب، خطوط انتقال، ایستگاه پمپاژ و تصفیه‌خانه، ساختار شکست کار، فعالیت‌ها، وابستگی‌ها، مدت و برآورد هزینه پیشنهاد بده. هر عددی که از اسناد یا داده کاربر استخراج نشده، فرض اولیه و قابل ویرایش است؛ آن را قطعی جلوه نده. برای تغییر یا حذف داده‌های ثبت‌شده تأیید بگیر. خروجی را کاربردی، مرحله‌ای و مختصر نگه دار.`;

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" }
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders(env) });
    }
    if (url.pathname === "/health" && request.method === "GET") {
      return json({ ok: true, service: "hamrah-project-ai", configured: Boolean(env.GEMINI_API_KEY) });
    }
    if (url.pathname !== "/chat" || request.method !== "POST") return json({ error: "مسیر یا روش درخواست معتبر نیست." }, 404);
    if (!env.GEMINI_API_KEY) return json({ error: "سرویس هوش مصنوعی هنوز پیکربندی نشده است." }, 503);
    let body;
    try { body = await request.json(); } catch { return json({ error: "بدنه درخواست معتبر نیست." }, 400); }
    const messages = Array.isArray(body.messages) ? body.messages : [];
    if (!messages.length || messages.length > 30) return json({ error: "تعداد پیام‌ها معتبر نیست." }, 400);
    const contents = messages.map(m => ({
      role: m.role === "assistant" ? "model" : "user",
      parts: [{ text: String(m.content || "").slice(0, 8000) }]
    }));
    if (contents.some(c => !c.parts[0].text.trim())) return json({ error: "پیام خالی پذیرفته نیست." }, 400);

    const model = env.GEMINI_MODEL || "gemini-2.5-flash";
    const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${env.GEMINI_API_KEY}`;
    try {
      const upstream = await fetch(endpoint, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          systemInstruction: { parts: [{ text: SYSTEM_PROMPT }] },
          contents,
          generationConfig: { temperature: 0.45, maxOutputTokens: 1800 }
        })
      });
      const payload = await upstream.json();
      if (!upstream.ok) {
        const status = upstream.status === 429 ? 429 : 502;
        return json({ error: upstream.status === 429 ? "سهمیه رایگان هوش مصنوعی موقتاً تمام شده است؛ کمی بعد دوباره تلاش کنید." : "ارتباط با سرویس هوش مصنوعی برقرار نشد." }, status);
      }
      const answer = (payload.candidates?.[0]?.content?.parts || []).map(p => p.text || "").join("").trim();
      if (!answer) return json({ error: "پاسخ قابل‌استفاده‌ای از هوش مصنوعی دریافت نشد." }, 502);
      return json({ answer, model });
    } catch {
      return json({ error: "خطای شبکه در ارتباط با هوش مصنوعی." }, 502);
    }
  }
};

function corsHeaders(env) {
  return {
    "access-control-allow-origin": env.APP_ORIGIN || "*",
    "access-control-allow-methods": "GET, POST, OPTIONS",
    "access-control-allow-headers": "content-type"
  };
}
