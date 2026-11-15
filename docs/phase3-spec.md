# NobiBoard Phase 3 — Long-Press Emoji Editor (Spec)

**Version:** Phase 3 spec v1 — for Sam's approval before building
**Base:** Fix11 (176071849) — the current working build (XML-only edge-key fix, zero dex change)
**Goal:** Sam apne long-press popup ke emojis khud customize kar sake — har letter key ke 5 slots (center, up, down, left, right) ke liye apni pasand ke emojis chune. Abhi ke themed defaults waise ke waise rahenge jab tak Sam kuch badle nahi.

---

## 1. Editor kahan milega (Entry Point)

- **NobiBoard Settings → "Long-press emojis"** — ek naya row, Settings list me "Emoji row" wale section ke paas.
- Row par tap karte hi editor khulta hai (poora naya screen, keyboard ke upar overlay nahi).
- Entry point ka label English me hoga ("Long-press emojis") — app UI hamesha English, ye rule yahan bhi lagu hai.
- Editor khulne par pehle se jo themed defaults hain, wahi dikhenge — koi bhi key abhi tak customize nahi hui hai to sab kuch waisa hi dikhega jaisa abhi keyboard par dikhta hai.

## 2. Editor screen kaisa dikhega (Layout)

- **Upar:** ek chhota live keyboard preview — QWERTY layout ki 3 rows (Q–P, A–L, Z–M), bilkul asli keyboard jaisi keys, dark theme. Har key par uska letter dikhega; jis key ko customize kiya gaya hai, uske letter ke neeche ek chhota green dot dikhega (taaki pata chale ye key custom hai).
- **Beech me:** jab koi key select hoti hai, to uske neeche us key ka popup preview dikhega — wahi gol popup (dark charcoal circle, thin outline) jisme us key ke 5 emojis plus (+) shape me hain. Preview live hai: jaise hi tum slot ka emoji badalte ho, preview turant update hota hai.
- **Neeche:** 5 slots ki list — Center, Up, Down, Left, Right — har slot ek row hai jisme left side slot ka naam (jaise "Center — default emoji") aur right side us slot ka current emoji (bada, 32dp) dikhega. Jis slot par tap karo, wahi edit hone lagta hai.
- **Bottom bar:** do button — "Reset this key" (sirf select ki hui key ko defaults par wapas) aur "Reset all" (saari keys ko defaults par wapas). Reset par ek chhota confirm dialog aayega ("Reset 'A' to defaults?") taaki galti se reset na ho.

## 3. Key select karne par kya hota hai (Tap Flow)

- Keyboard preview me kisi bhi letter key par tap karo — key thodi highlight hoti hai (halka green tint), aur neeche us key ka popup preview + 5 slots ki list us key ke emojis se bhar jati hai.
- Dusri key par tap karo to pehli key ka preview hat jata hai aur nayi key ka preview aa jata hai. Jo changes tumne pehli key me kiye the, wo turant save ho chuke hote hain (neeche §6 dekho) — switch karne par kuch khoega nahi.
- A–Z ki saari 26 keys select ki ja sakti hain. Numbers/symbols (1–0, ?, !, etc.) is phase me editor me NAHI hain — ye ek khula faisla hai (neeche §9 dekho); agar Sam chahe to wo bhi add ho sakte hain.

## 4. Emoji kaise chune (Emoji Picker)

- Kisi slot (jaise "Up") par tap karo — neeche se ek emoji picker sheet khulti hai (bottom sheet, aadhi screen).
- Picker me: (a) ek search bar upar — emoji ka naam ya keyword likho (jaise "heart", "smile"), (b) neeche categories ke tabs (Smileys, Hearts, Animals, Food, etc.), (c) grid me emojis (6 columns, bade aur tap karne layak).
- Emoji par tap karte hi: sheet band ho jati hai, slot ka emoji badal jata hai, popup preview turant update ho jata hai, aur change save ho jata hai.
- Sirf ek emoji per slot. Emoji nahi, sirf emoji — text ya sticker is phase me nahi.
- Agar tum kisi slot ko khaali chhodna chahte ho to picker me ek "Clear" option bhi hoga — khaali slot ka matlab hai us jagah kuch nahi dikhega (popup me wo cell khaali rahega, bilkul abhi ke U+200B blank cells ki tarah).

## 5. Popup preview kaisa dikhega (Live Preview)

- Preview wahi gol popup hai jo keyboard par dikhta hai: dark charcoal circle (#3c474c), thin light outline, 5 emojis plus (+) shape me.
- Center slot ka emoji beech me, Up upar-center, Down neeche-center, Left left-center, Right right-center.
- Center slot ke emoji ke peeche green circle highlight (#4db6ac) dikhega — bilkul waisa hi jaisa asli popup me default selection par dikhta hai.
- Preview sirf dekhne ke liye hai — us par swipe/drag nahi hoga (ye editor hai, keyboard nahi).

## 6. Save kaise hoga (Storage — SharedPreferences)

- **Koi "Save" button nahi hai** — har change turant save hota hai (jaise hi tum emoji chunte ho ya reset karte ho). Ise kehte hain auto-save; professional apps (Samsung Keyboard Cafe, Gboard settings) bhi aise hi karte hain.
- Save kahan hota hai: Android SharedPreferences me, ek key ke liye ek entry — jaise `emoji_A` = us key ke 5 emojis (center, up, down, left, right — comma-separated). Sirf wahi keys save hoti hain jinhe tumne badla hai; baaki keys ke liye koi entry nahi banti.
- Reset karne par us key ki entry delete ho jati hai aur wapas themed defaults dikhne lagte hain.
- **Keyboard ko dobara kholne ki zaroorat nahi** — agli baar jab tum kisi key ko long-press karoge, to keyboard turant naye emojis dikha dega (runtime override hook SharedPreferences se padhta hai).

## 7. Keyboard par kya badlega (Runtime Override Hook)

- Jab tum kisi key ko long-press karte ho, to keyboard pehle SharedPreferences me dekhta hai ki us key ke liye custom emojis hain ya nahi.
- **Custom hain to:** wahi custom 5 emojis popup me dikhenge (plus shape, green circle, glide, swipe — sab kuch waise hi kaam karega jaise abhi karta hai).
- **Custom nahi hain to:** abhi wale themed defaults dikhenge — bilkul waisa hi jaisa abhi dikhta hai. Koi farak nahi.
- Ye hook sirf popup ke emojis badalta hai — popup ka size, shape, green circle, glide, swipe, insert — sab kuch waise ka waisa rahega.

## 8. Professional apps kya karte hain (Reference)

- **Samsung Keyboard Cafe:** har key ke liye alag-alag symbols/emojis set karne deta hai, keyboard preview ke saath. NobiBoard ka editor isi pattern par hai — preview + tap-to-edit.
- **Gboard:** long-press characters ko customize karne ka option deta hai (Settings → Languages → long-press), lekin emojis ke liye nahi. NobiBoard isse aage hai — emojis bhi customize hote hain.
- **SwiftKey:** theme aur layout customize karta hai, lekin per-key long-press emojis nahi. NobiBoard ka editor is mamle me unique hai.

## 9. Khula faisla (Sam se chahiye)

- **Popup sirf A–Z letters par ho, ya numbers/symbols par bhi?** Abhi popup sirf A–Z par hai. Agar numbers/symbols (1–0, ?, !, @, etc.) par bhi popup chahiye to editor me wo keys bhi aayengi (keyboard preview me number row bhi dikhegi). Agar nahi chahiye to editor sirf A–Z tak rahega. Ye faisla spec approve karte waqt bata do.

## 10. Kya NAHI hoga is phase me (Scope — bahar hai)

- Per-key popup size/shape/color badalna — nahi, sirf emojis badlenge.
- Dusre keyboards ya apps me ye emojis use karna — nahi, sirf NobiBoard ke andar.
- Emoji ke alawa text/sticker/image set karna — nahi, sirf emoji.
- Cloud sync ya backup — nahi, sirf phone ke SharedPreferences me local save.

## 11. Build ka plan (Approve hone ke baad)

1. **Step 1 — Runtime override hook:** `Lqmu.c()` (ya popup build wali jagah) me SharedPreferences se custom map padhne ka hook — koi custom map nahi hai to bilkul current behaviour (Fix11 jaisa). Pehle ye hook banega aur test hoga (bina editor ke, manually ek key set karke).
2. **Step 2 — Editor activity:** Settings me "Long-press emojis" entry + editor screen (preview + 5 slots + emoji picker + reset). Yehi sabse bada kaam hai.
3. **Step 3 — Verify:** poore gates (RegInitCheck, VerifyPatch, VerifyBranches, payload, TypeMergeCheck location-level, CanonDiff vs Fix11) + Sam ka on-device test — force-stop, install, ek key customize karo, long-press karke dekho.

**Version:** Fix11 (176071849) se aage — agla code 176071851 hoga (176071850 Diag3 ka hai). Entry count, cert, signing key — sab wahi rahenge.
