# اتصال رایگان هوش مصنوعی همراه پروژه

این Worker فقط از Gemini API در Free Tier استفاده می‌کند؛ هیچ مسیر جایگزین پولی ندارد و در خطای سهمیه، درخواست را متوقف می‌کند.

## راه‌اندازی
1. در Google AI Studio یک API key بساز: https://aistudio.google.com/apikey
2. پروژه را در Free Tier نگه دار و Billing را فعال نکن.
3. در Cloudflare Worker با نام hamrah-project-ai به Settings → Variables and Secrets برو.
4. یک Secret با نام GEMINI_API_KEY بساز و کلید را همان‌جا وارد کن. کلید را در GitHub، اپ یا چت قرار نده.
5. Worker را از پوشه cloudflare-worker با فایل src/index.js و wrangler.toml مستقر کن.
6. نشانی /health باید configured:true و billingFallback:false نشان دهد.
7. نشانی Worker را در اپ در بخش دستیار وارد کن.

## API
POST /chat
درخواست JSON شامل آرایه messages با نقش‌های user و assistant است.

## محدودیت و حریم خصوصی
- مدل پیش‌فرض gemini-2.5-flash است؛ سهمیه Free Tier و نرخ درخواست به حساب و مدل وابسته است و نامحدود نیست.
- با اتمام سهمیه، برنامه خطای FREE_QUOTA_EXHAUSTED می‌گیرد؛ پرداخت یا مدل پولی خودکار فعال نمی‌شود.
- در Free Tier ممکن است محتوای ارسال‌شده برای بهبود محصولات Google استفاده شود. اسناد محرمانه قراردادی، مالی یا فنی را بدون رضایت و بررسی سیاست سازمان ارسال نکن. پیش از تحلیل، اطلاعات حساس را حذف یا پوشانده کن.
- این نسخه فقط متن مکالمه را می‌فرستد؛ تحلیل فایل‌ها به مسیر مجزای آپلود، استخراج و رضایت نیاز دارد.
