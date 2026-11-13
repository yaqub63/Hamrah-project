const SYSTEM_PROMPT = "تو دستیار تخصصی برنامه‌ریزی و کنترل پروژه‌های عمرانی، صنعتی و زیرساختی هستی. فارسی روان پاسخ بده. در شروع نام و نوع پروژه را از مکالمه استخراج کن؛ اگر مشخص نیست فقط یک سؤال کوتاه بپرس. سپس WBS متناسب پیشنهاد بده و با کمترین پرسش‌های گزینه‌ای اطلاعات را تکمیل کن. داده استخراج‌شده را با منبع و اطمینان مشخص کن. اعداد فاقد سند را فرض اولیه و قابل ویرایش اعلام کن. هیچ مبلغ، مدت، تعهد قراردادی یا مقدار فنی را بدون تأیید کاربر قطعی نکن. محاسبات قطعی زمان، هزینه، پیشرفت و مسیر بحرانی باید در منطق برنامه انجام شوند، نه با حدس مدل. اسناد محرمانه را فقط با رضایت کاربر تحلیل کن. پاسخ‌ها کاربردی و مختصر باشند.";

const corsHeaders = (env) => ({
  "access-control-allow-origin": env.APP_ORIGIN || "*",
  "access-control-allow-methods": "GET, POST, OPTIONS",
  "access-control-allow-headers": "content-type",
  "vary": "Origin"
});
const json = (data, status, env) => new Response(JSON.stringify(data), {
  status: status || 200,
  headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store", ...corsHeaders(env) }
});

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") return new Response(null, {status:204, headers:corsHeaders(env)});
    const model = (env.GEMINI_MODEL || "gemini-2.5-flash").trim();
    const configured = Boolean(env.GEMINI_API_KEY);
    if (url.pathname === "/health" && request.method === "GET") {
      return json({ok:true, service:"hamrah-project-ai", provider:"google-gemini-free-tier", model, configured, billingFallback:false}, 200, env);
    }
    if (url.pathname !== "/chat" || request.method !== "POST") return json({error:"مسیر یا روش درخواست معتبر نیست."},404,env);
    if (!configured) return json({error:"کلید رایگان Gemini هنوز در تنظیمات امن Cloudflare ثبت نشده است."},503,env);
    let body;
    try { body = await request.json(); } catch { return json({error:"درخواست معتبر نیست."},400,env); }
    const incoming = Array.isArray(body.messages) ? body.messages : [];
    if (!incoming.length || incoming.length > 20) return json({error:"حداکثر ۲۰ پیام ارسال کن."},400,env);
    const contents = [];
    for (const item of incoming) {
      const role = item.role === "assistant" ? "model" : "user";
      const content = String(item.content || "").slice(0,6000).trim();
      if (content) contents.push({role, parts:[{text:content}]});
    }
    if (!contents.length) return json({error:"پیام خالی پذیرفته نیست."},400,env);
    try {
      const endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" +
        encodeURIComponent(model) + ":generateContent?key=" + encodeURIComponent(env.GEMINI_API_KEY);
      const upstream = await fetch(endpoint, {
        method:"POST", headers:{"content-type":"application/json"},
        body:JSON.stringify({systemInstruction:{parts:[{text:SYSTEM_PROMPT}]},contents,
          generationConfig:{temperature:0.35,maxOutputTokens:1400}})
      });
      let payload = {};
      try { payload = await upstream.json(); } catch {}
      if (!upstream.ok) {
        if (upstream.status === 429 || payload.error?.status === "RESOURCE_EXHAUSTED")
          return json({error:"سهمیه رایگان Gemini تمام شده یا محدودیت سرعت فعال است. هیچ سرویس پولی فراخوانی نمی‌شود؛ بعداً دوباره تلاش کن.",code:"FREE_QUOTA_EXHAUSTED"},429,env);
        return json({error:"دسترسی Gemini نامعتبر است. کلید و Free Tier را بررسی کن و پرداخت را فعال نکن.",code:"GEMINI_ACCESS_ERROR"},502,env);
      }
      const answer = (payload.candidates?.[0]?.content?.parts || []).map(p=>p.text || "").join("").trim();
      if (!answer) return json({error:"پاسخ متنی از Gemini دریافت نشد."},502,env);
      return json({answer,provider:"google-gemini-free-tier",model,billingFallback:false},200,env);
    } catch {
      return json({error:"خطای شبکه در ارتباط با Gemini."},502,env);
    }
  }
};
