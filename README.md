# Ishbow Browser

متصفح أندرويد خفيف بمبدأ **لا لجوجل**، مع مضيف سكربتات مستخدم (User Scripts) شبيه بـ Via.

## المميزات
- 🚫 **بدون جوجل افتراضياً**: محرك بحث DuckDuckGo، حظر نطاقات Google (بحث، تحليلات، إعلانات، خطوط، gstatic، يوتيوب...)، تعطيل Safe Browsing، لا Play Services ولا Firebase ولا أي مكتبة خارجية.
- ✅ يمكن للمستخدم السماح بجوجل: الإعدادات ← إيقاف «حظر خدمات Google» أو اختيار محرك Google.
- 🧩 **مضيف سكربتات**: تثبيت `.user.js` من رابط أو بفتح الرابط مباشرة، كتابة/تعديل السكربتات، تفعيل/تعطيل، دعم `@match/@include/@exclude/@run-at`.
- واجهات GM مدعومة: `GM_getValue/setValue/deleteValue/listValues`, `GM_addStyle`, `GM_log`, `GM_openInTab`, `GM_setClipboard`, `GM_xmlhttpRequest` (مبسّطة), `GM_info`, `unsafeWindow`.
- محركات بحث: DuckDuckGo، Brave، Startpage، Qwant، Ecosia، Mojeek.

## البناء
يُبنى تلقائياً عبر GitHub Actions (تبويب Actions ← Build APK ← Artifacts). أو محلياً: `gradle assembleDebug`.
