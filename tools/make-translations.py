"""make-translations.py - writes res/values-<lang>/strings.xml for the 8 languages beyond EN and RU of plans/01 §4.7
(ES, PT, ZH, HI, BN, FR, DE, AR - the list the owner chose for KrinikCam, researches/01 §4).

[AI] The texts are the agent's DRAFT translation of res/values/strings.xml (English) - every file says so in its header;
a native speaker's or the owner's review replaces them. EN and RU are hand-kept and are not touched by this script.
Plurals follow each language's CLDR categories (Arabic six, Chinese one form). [NOT-TESTED: shown on the device per
language - deferred by the owner («потом потестируем»)]

Usage (from the project root): python tools/make-translations.py
"""
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parent.parent
KEYS = ["home_pick_title", "home_pick_sub", "home_paste_link", "home_share_title", "home_share_text", "flow_photo",
        "flow_share", "home_offline", "setup_title", "setup_other_photo", "setup_other_short", "label_model",
        "label_scale", "model_fast_desc", "model_max_desc", "licence_bsd", "scale_default", "megapixels",
        "scale_too_big", "scale_two_pass", "upscale", "upscale_with_estimate", "info_close", "info_scale_text",
        "info_scale_list", "info_model_text", "info_model_list", "working_title", "working_tiles", "working_starting",
        "cancel", "before", "after", "save", "share", "error_too_big", "error_decode", "error_model", "error_engine",
        "notif_channel", "notif_progress", "notif_starting", "save_title", "label_format", "label_quality",
        "save_folder", "saved", "save_failed", "saving", "webp_too_big"]

T = {
    "es": ["Elige una foto", "de la galería del teléfono", "Pegar enlace de foto", "O envía una foto desde otra app",
           "Abre una foto en la galería, Telegram o el navegador, toca «Compartir» y elige KARTUN: la foto se abrirá aquí lista para mejorar.",
           "Foto", "Compartir", "Procesado en el dispositivo, sin red", "Mejorar", "Elegir otra foto", "Otra foto",
           "Modelo", "Escala", "Rápido · para fotos", "Máximo · textura viva", "Uso comercial · BSD-3", "predet.", "%1$s MP",
           "El resultado superaría %1$s MP: para esta foto elige hasta %2$d×", "2 pasadas · más lento · archivo grande",
           "Mejorar", "Mejorar · ≈ %1$s", "Cerrar",
           "La escala indica cuántas veces crecen el ancho y el alto de la foto: 1×, 2×, 4×, 8× o 16×; 2× por defecto.",
           "• 1× — mismo tamaño; la red solo quita ruido y desenfoque.\\n• 2× y 4× — una pasada de la red; suficiente para pantalla e impresión.\\n• 8× y 16× — dos pasadas: de 4 a 16 veces más lento, archivo muy grande.",
           "Un modelo es una red neuronal entrenada. Los modelos se diferencian por las imágenes con las que aprendieron: uno restaura mejor fotos, otro dibujos.",
           "• Rápido — para fotos, normalmente unos segundos.\\n• Máximo — textura viva, unas 15 veces más lento.",
           "La red está mejorando tu foto", "Mosaico %1$d de %2$d · quedan ≈ %3$s", "Quedan ≈ %1$s", "Cancelar", "Antes",
           "Después", "Guardar", "Compartir",
           "Las fotos de más de %1$s MP aún no son compatibles: llegarán con el procesamiento por partes.",
           "No se pudo abrir esta foto.", "No se pudo cargar el modelo. Reinstala la app.", "La red no pudo procesar esta foto.",
           "Procesamiento", "%1$d%% hecho", "Iniciando la red…", "Guardar en la galería", "Formato", "Calidad · %1$d",
           "Carpeta: Pictures/KARTUN · el original no cambia", "Guardado en Pictures/KARTUN", "No se pudo guardar la foto.",
           "Guardando…", "WebP admite hasta 16 383 px por lado; elige PNG o JPEG para este resultado"],
    "pt": ["Escolha uma foto", "da galeria do telefone", "Colar link da foto", "Ou envie uma foto de outro app",
           "Abra uma foto na galeria, no Telegram ou no navegador, toque em «Compartilhar» e escolha KARTUN — a foto abre aqui pronta para melhorar.",
           "Foto", "Compartilhar", "Processado no dispositivo, sem rede", "Melhorar", "Escolher outra foto", "Outra foto",
           "Modelo", "Escala", "Rápido · para fotos", "Máximo · textura viva", "Uso comercial · BSD-3", "padrão", "%1$s MP",
           "O resultado passaria de %1$s MP — para esta foto escolha até %2$d×", "2 passagens · mais demorado · arquivo grande",
           "Melhorar", "Melhorar · ≈ %1$s", "Fechar",
           "A escala diz quantas vezes a largura e a altura da foto crescem: 1×, 2×, 4×, 8× ou 16×; 2× por padrão.",
           "• 1× — mesmo tamanho; a rede só remove ruído e desfoque.\\n• 2× e 4× — uma passagem da rede; basta para tela e impressão.\\n• 8× e 16× — duas passagens: de 4 a 16 vezes mais demorado, arquivo muito grande.",
           "Um modelo é uma rede neural treinada. Os modelos diferem nas imagens com que aprenderam: um restaura melhor fotos, outro desenhos.",
           "• Rápido — para fotos, normalmente alguns segundos.\\n• Máximo — textura viva, cerca de 15 vezes mais demorado.",
           "A rede está melhorando sua foto", "Bloco %1$d de %2$d · faltam ≈ %3$s", "Faltam ≈ %1$s", "Cancelar", "Antes",
           "Depois", "Salvar", "Compartilhar",
           "Fotos acima de %1$s MP ainda não são suportadas: chegarão com o processamento em partes.",
           "Não foi possível abrir esta foto.", "Não foi possível carregar o modelo. Reinstale o app.", "A rede não conseguiu processar esta foto.",
           "Processamento", "%1$d%% concluído", "Iniciando a rede…", "Salvar na galeria", "Formato", "Qualidade · %1$d",
           "Pasta: Pictures/KARTUN · o original não muda", "Salvo em Pictures/KARTUN", "Não foi possível salvar a foto.",
           "Salvando…", "WebP aceita até 16 383 px por lado; escolha PNG ou JPEG para este resultado"],
    "fr": ["Choisissez une photo", "dans la galerie du téléphone", "Coller un lien de photo", "Ou envoyez une photo depuis une autre appli",
           "Ouvrez une photo dans la galerie, Telegram ou le navigateur, touchez « Partager » et choisissez KARTUN — la photo s’ouvre ici, prête à être améliorée.",
           "Photo", "Partager", "Traité sur l’appareil, sans réseau", "Améliorer", "Choisir une autre photo", "Autre photo",
           "Modèle", "Échelle", "Rapide · pour les photos", "Maximum · texture vivante", "Usage commercial · BSD-3", "par défaut", "%1$s Mpx",
           "Le résultat dépasserait %1$s Mpx — pour cette photo, choisissez jusqu’à %2$d×", "2 passes · plus long · gros fichier",
           "Améliorer", "Améliorer · ≈ %1$s", "Fermer",
           "L’échelle indique combien de fois la largeur et la hauteur de la photo augmentent : 1×, 2×, 4×, 8× ou 16× ; 2× par défaut.",
           "• 1× — même taille ; le réseau retire seulement le bruit et le flou.\\n• 2× et 4× — une passe du réseau ; assez pour l’écran et l’impression.\\n• 8× et 16× — deux passes : 4 à 16 fois plus long, fichier très volumineux.",
           "Un modèle est un réseau de neurones entraîné. Les modèles diffèrent par les images apprises : l’un restaure mieux les photos, l’autre les dessins.",
           "• Rapide — pour les photos, en général quelques secondes.\\n• Maximum — texture vivante, environ 15 fois plus long.",
           "Le réseau améliore votre photo", "Tuile %1$d sur %2$d · reste ≈ %3$s", "Reste ≈ %1$s", "Annuler", "Avant",
           "Après", "Enregistrer", "Partager",
           "Les photos de plus de %1$s Mpx ne sont pas encore prises en charge : elles arriveront avec le traitement par parties.",
           "Impossible d’ouvrir cette photo.", "Impossible de charger le modèle. Réinstallez l’appli.", "Le réseau n’a pas pu traiter cette photo.",
           "Traitement", "%1$d %% fait", "Démarrage du réseau…", "Enregistrer dans la galerie", "Format", "Qualité · %1$d",
           "Dossier : Pictures/KARTUN · l’original reste intact", "Enregistré dans Pictures/KARTUN", "Impossible d’enregistrer la photo.",
           "Enregistrement…", "Le WebP va jusqu’à 16 383 px par côté ; choisissez PNG ou JPEG pour ce résultat"],
    "de": ["Foto auswählen", "aus der Galerie des Telefons", "Foto-Link einfügen", "Oder ein Foto aus einer anderen App senden",
           "Öffne ein Foto in der Galerie, in Telegram oder im Browser, tippe auf „Teilen“ und wähle KARTUN – das Foto öffnet sich hier, bereit zum Verbessern.",
           "Foto", "Teilen", "Verarbeitung auf dem Gerät, ohne Netz", "Verbessern", "Anderes Foto wählen", "Anderes Foto",
           "Modell", "Maßstab", "Schnell · für Fotos", "Maximum · lebendige Textur", "Kommerzielle Nutzung · BSD-3", "Standard", "%1$s MP",
           "Das Ergebnis würde %1$s MP überschreiten – für dieses Foto bis %2$d× wählen", "2 Durchgänge · länger · große Datei",
           "Verbessern", "Verbessern · ≈ %1$s", "Schließen",
           "Der Maßstab gibt an, wie oft Breite und Höhe des Fotos wachsen: 1×, 2×, 4×, 8× oder 16×; standardmäßig 2×.",
           "• 1× – gleiche Größe; das Netz entfernt nur Rauschen und Unschärfe.\\n• 2× und 4× – ein Durchgang; reicht für Bildschirm und Druck.\\n• 8× und 16× – zwei Durchgänge: 4- bis 16-mal länger, sehr große Datei.",
           "Ein Modell ist ein trainiertes neuronales Netz. Modelle unterscheiden sich darin, womit sie gelernt haben: eines stellt Fotos besser wieder her, ein anderes Zeichnungen.",
           "• Schnell – für Fotos, meist wenige Sekunden.\\n• Maximum – lebendige Textur, etwa 15-mal länger.",
           "Das Netz verbessert dein Foto", "Kachel %1$d von %2$d · noch ≈ %3$s", "Noch ≈ %1$s", "Abbrechen", "Vorher",
           "Nachher", "Speichern", "Teilen",
           "Fotos über %1$s MP werden noch nicht unterstützt: Sie kommen mit der Verarbeitung in Teilen.",
           "Dieses Foto ließ sich nicht öffnen.", "Das Modell ließ sich nicht laden. Bitte installiere die App neu.", "Das Netz konnte dieses Foto nicht verarbeiten.",
           "Verarbeitung", "%1$d %% fertig", "Netz wird gestartet…", "In der Galerie speichern", "Format", "Qualität · %1$d",
           "Ordner: Pictures/KARTUN · das Original bleibt unverändert", "In Pictures/KARTUN gespeichert", "Das Foto ließ sich nicht speichern.",
           "Wird gespeichert…", "WebP fasst bis 16 383 px pro Seite; wähle für dieses Ergebnis PNG oder JPEG"],
    "zh": ["选择照片", "从手机相册", "粘贴照片链接", "或从其他应用发送照片",
           "在相册、Telegram 或浏览器中打开照片，点按“分享”并选择 KARTUN——照片会在这里打开，随时可以增强。",
           "照片", "分享", "在设备上处理，无需联网", "增强", "选择其他照片", "其他照片",
           "模型", "放大倍数", "快速 · 适合照片", "最佳 · 生动纹理", "可商用 · BSD-3", "默认", "%1$s 百万像素",
           "结果将超过 %1$s 百万像素——这张照片最多可选 %2$d×", "2 次处理 · 更慢 · 文件很大",
           "增强", "增强 · 约 %1$s", "关闭",
           "放大倍数表示照片的宽和高增大几倍：1×、2×、4×、8× 或 16×；默认 2×。",
           "• 1×——尺寸不变，神经网络只去除噪点和模糊。\\n• 2× 和 4×——神经网络处理一次，足够屏幕显示和打印。\\n• 8× 和 16×——处理两次：时间长 4–16 倍，文件非常大。",
           "模型是训练好的神经网络。模型的区别在于学习过的图片：有的更擅长修复照片，有的更擅长修复绘画。",
           "• 快速——适合照片，通常几秒钟。\\n• 最佳——生动纹理，约慢 15 倍。",
           "神经网络正在增强你的照片", "第 %1$d / %2$d 块 · 剩余约 %3$s", "剩余约 %1$s", "取消", "处理前",
           "处理后", "保存", "分享",
           "暂不支持超过 %1$s 百万像素的照片：分块处理上线后即可支持。",
           "无法打开这张照片。", "无法加载模型。请重新安装应用。", "神经网络无法处理这张照片。",
           "处理中", "已完成 %1$d%%", "正在启动神经网络…", "保存到相册", "格式", "质量 · %1$d",
           "文件夹：Pictures/KARTUN · 原图保持不变", "已保存到 Pictures/KARTUN", "无法保存照片。",
           "正在保存…", "WebP 每边最多 16 383 像素；此结果请选择 PNG 或 JPEG"],
    "hi": ["फ़ोटो चुनें", "फ़ोन की गैलरी से", "फ़ोटो का लिंक चिपकाएँ", "या किसी दूसरे ऐप से फ़ोटो भेजें",
           "गैलरी, Telegram या ब्राउज़र में फ़ोटो खोलें, «शेयर करें» दबाएँ और KARTUN चुनें — फ़ोटो यहीं खुलेगी, बेहतर बनाने के लिए तैयार।",
           "फ़ोटो", "शेयर करें", "डिवाइस पर प्रोसेसिंग, बिना नेटवर्क", "बेहतर बनाएँ", "दूसरी फ़ोटो चुनें", "दूसरी फ़ोटो",
           "मॉडल", "स्केल", "तेज़ · फ़ोटो के लिए", "अधिकतम · जीवंत बनावट", "व्यावसायिक उपयोग · BSD-3", "डिफ़ॉल्ट", "%1$s MP",
           "नतीजा %1$s MP से बड़ा होगा — इस फ़ोटो के लिए %2$d× तक चुनें", "2 बार · ज़्यादा समय · बड़ी फ़ाइल",
           "बेहतर बनाएँ", "बेहतर बनाएँ · ≈ %1$s", "बंद करें",
           "स्केल बताता है कि फ़ोटो की चौड़ाई और ऊँचाई कितने गुना बढ़ेगी: 1×, 2×, 4×, 8× या 16×; डिफ़ॉल्ट 2×।",
           "• 1× — आकार वही; न्यूरल नेटवर्क सिर्फ़ शोर और धुंधलापन हटाता है।\\n• 2× और 4× — नेटवर्क एक बार; स्क्रीन और प्रिंट के लिए काफ़ी।\\n• 8× और 16× — दो बार: 4–16 गुना ज़्यादा समय, बहुत बड़ी फ़ाइल।",
           "मॉडल एक प्रशिक्षित न्यूरल नेटवर्क है। मॉडल इस बात में अलग हैं कि उन्होंने किन तस्वीरों से सीखा: एक फ़ोटो बेहतर सुधारता है, दूसरा चित्र।",
           "• तेज़ — फ़ोटो के लिए, आमतौर पर कुछ सेकंड।\\n• अधिकतम — जीवंत बनावट, लगभग 15 गुना ज़्यादा समय।",
           "न्यूरल नेटवर्क आपकी फ़ोटो बेहतर बना रहा है", "टाइल %1$d / %2$d · बाक़ी ≈ %3$s", "बाक़ी ≈ %1$s", "रद्द करें", "पहले",
           "बाद में", "सहेजें", "शेयर करें",
           "%1$s MP से बड़ी फ़ोटो अभी समर्थित नहीं हैं: हिस्सों में प्रोसेसिंग के साथ आएँगी।",
           "यह फ़ोटो नहीं खुल सकी।", "मॉडल लोड नहीं हो सका। कृपया ऐप दोबारा इंस्टॉल करें।", "न्यूरल नेटवर्क यह फ़ोटो प्रोसेस नहीं कर सका।",
           "प्रोसेसिंग", "%1$d%% पूरा", "नेटवर्क शुरू हो रहा है…", "गैलरी में सहेजें", "फ़ॉर्मैट", "गुणवत्ता · %1$d",
           "फ़ोल्डर: Pictures/KARTUN · मूल फ़ोटो नहीं बदलती", "Pictures/KARTUN में सहेजा गया", "फ़ोटो सहेजी नहीं जा सकी।",
           "सहेजा जा रहा है…", "WebP में हर तरफ़ 16 383 px तक; इस नतीजे के लिए PNG या JPEG चुनें"],
    "bn": ["একটি ছবি বেছে নিন", "ফোনের গ্যালারি থেকে", "ছবির লিংক পেস্ট করুন", "অথবা অন্য অ্যাপ থেকে ছবি পাঠান",
           "গ্যালারি, Telegram বা ব্রাউজারে ছবি খুলুন, «শেয়ার» চাপুন এবং KARTUN বেছে নিন — ছবিটি এখানে খুলবে, উন্নত করার জন্য প্রস্তুত।",
           "ছবি", "শেয়ার", "ডিভাইসেই প্রক্রিয়া, নেটওয়ার্ক ছাড়া", "উন্নত করুন", "অন্য ছবি বেছে নিন", "অন্য ছবি",
           "মডেল", "স্কেল", "দ্রুত · ছবির জন্য", "সর্বোচ্চ · জীবন্ত টেক্সচার", "বাণিজ্যিক ব্যবহার · BSD-3", "ডিফল্ট", "%1$s MP",
           "ফলাফল %1$s MP ছাড়িয়ে যাবে — এই ছবির জন্য %2$d× পর্যন্ত বেছে নিন", "২ বার · বেশি সময় · বড় ফাইল",
           "উন্নত করুন", "উন্নত করুন · ≈ %1$s", "বন্ধ করুন",
           "স্কেল বলে ছবির প্রস্থ ও উচ্চতা কত গুণ বাড়বে: 1×, 2×, 4×, 8× বা 16×; ডিফল্ট 2×।",
           "• 1× — একই আকার; নিউরাল নেটওয়ার্ক শুধু নয়েজ ও ঝাপসা ভাব সরায়।\\n• 2× ও 4× — নেটওয়ার্ক একবার; স্ক্রিন ও প্রিন্টের জন্য যথেষ্ট।\\n• 8× ও 16× — দুবার: ৪–১৬ গুণ বেশি সময়, খুব বড় ফাইল।",
           "মডেল হলো একটি প্রশিক্ষিত নিউরাল নেটওয়ার্ক। মডেলগুলো আলাদা কোন ছবি থেকে শিখেছে তাতে: একটি ছবি ভালো সারায়, অন্যটি আঁকা।",
           "• দ্রুত — ছবির জন্য, সাধারণত কয়েক সেকেন্ড।\\n• সর্বোচ্চ — জীবন্ত টেক্সচার, প্রায় ১৫ গুণ বেশি সময়।",
           "নিউরাল নেটওয়ার্ক আপনার ছবি উন্নত করছে", "টাইল %1$d / %2$d · বাকি ≈ %3$s", "বাকি ≈ %1$s", "বাতিল", "আগে",
           "পরে", "সংরক্ষণ", "শেয়ার",
           "%1$s MP-এর বড় ছবি এখনো সমর্থিত নয়: অংশে অংশে প্রক্রিয়ার সঙ্গে আসবে।",
           "এই ছবিটি খোলা যায়নি।", "মডেল লোড করা যায়নি। অ্যাপটি আবার ইনস্টল করুন।", "নিউরাল নেটওয়ার্ক এই ছবি প্রক্রিয়া করতে পারেনি।",
           "প্রক্রিয়া", "%1$d%% সম্পন্ন", "নেটওয়ার্ক চালু হচ্ছে…", "গ্যালারিতে সংরক্ষণ", "ফরম্যাট", "গুণমান · %1$d",
           "ফোল্ডার: Pictures/KARTUN · মূল ছবি অপরিবর্তিত থাকে", "Pictures/KARTUN-এ সংরক্ষিত", "ছবি সংরক্ষণ করা যায়নি।",
           "সংরক্ষণ হচ্ছে…", "WebP প্রতি পাশে 16 383 px পর্যন্ত; এই ফলাফলের জন্য PNG বা JPEG বেছে নিন"],
    "ar": ["اختر صورة", "من معرض الهاتف", "الصق رابط الصورة", "أو أرسل صورة من تطبيق آخر",
           "افتح صورة في المعرض أو Telegram أو المتصفح، واضغط «مشاركة» واختر KARTUN — ستُفتح الصورة هنا جاهزة للتحسين.",
           "صورة", "مشاركة", "معالجة على الجهاز، بلا شبكة", "تحسين", "اختر صورة أخرى", "صورة أخرى",
           "النموذج", "المقياس", "سريع · للصور", "أقصى جودة · ملمس حي", "للاستخدام التجاري · BSD-3", "افتراضي", "%1$s ميغابكسل",
           "ستتجاوز النتيجة %1$s ميغابكسل — اختر لهذه الصورة حتى %2$d×", "مرّتان · أطول · ملف كبير",
           "تحسين", "تحسين · ≈ %1$s", "إغلاق",
           "المقياس هو عدد المرات التي يكبر بها عرض الصورة وارتفاعها: 1× أو 2× أو 4× أو 8× أو 16×؛ والافتراضي 2×.",
           "• 1× — الحجم نفسه؛ تزيل الشبكة الضجيج والضبابية فقط.\\n• 2× و4× — مرور واحد للشبكة؛ يكفي للشاشة والطباعة.\\n• 8× و16× — مروران: أطول بـ 4 إلى 16 مرة، وملف كبير جدًا.",
           "النموذج شبكة عصبية مدرَّبة. تختلف النماذج في الصور التي تعلّمت منها: أحدها يرمّم الصور أفضل، والآخر الرسوم.",
           "• سريع — للصور، عادةً بضع ثوانٍ.\\n• أقصى جودة — ملمس حي، أطول بنحو 15 مرة.",
           "الشبكة العصبية تحسّن صورتك", "البلاطة %1$d من %2$d · المتبقي ≈ %3$s", "المتبقي ≈ %1$s", "إلغاء", "قبل",
           "بعد", "حفظ", "مشاركة",
           "الصور الأكبر من %1$s ميغابكسل غير مدعومة بعد: ستأتي مع المعالجة على أجزاء.",
           "تعذّر فتح هذه الصورة.", "تعذّر تحميل النموذج. أعد تثبيت التطبيق.", "تعذّر على الشبكة معالجة هذه الصورة.",
           "المعالجة", "اكتمل %1$d%%", "جارٍ تشغيل الشبكة…", "الحفظ في المعرض", "الصيغة", "الجودة · %1$d",
           "المجلد: Pictures/KARTUN · تبقى الصورة الأصلية كما هي", "حُفظت في Pictures/KARTUN", "تعذّر حفظ الصورة.",
           "جارٍ الحفظ…", "تتسع WebP حتى 16 383 بكسل لكل جانب؛ اختر PNG أو JPEG لهذه النتيجة"],
}

# Plural forms per language (CLDR categories). Each entry: quantity -> (seconds, minutes).
P = {
    "es": {"one": ("%1$d segundo", "%1$d minuto"), "many": ("%1$d segundos", "%1$d minutos"), "other": ("%1$d segundos", "%1$d minutos")},
    "pt": {"one": ("%1$d segundo", "%1$d minuto"), "many": ("%1$d segundos", "%1$d minutos"), "other": ("%1$d segundos", "%1$d minutos")},
    "fr": {"one": ("%1$d seconde", "%1$d minute"), "many": ("%1$d secondes", "%1$d minutes"), "other": ("%1$d secondes", "%1$d minutes")},
    "de": {"one": ("%1$d Sekunde", "%1$d Minute"), "other": ("%1$d Sekunden", "%1$d Minuten")},
    "zh": {"other": ("%1$d 秒", "%1$d 分钟")},
    "hi": {"one": ("%1$d सेकंड", "%1$d मिनट"), "other": ("%1$d सेकंड", "%1$d मिनट")},
    "bn": {"one": ("%1$d সেকেন্ড", "%1$d মিনিট"), "other": ("%1$d সেকেন্ড", "%1$d মিনিট")},
    "ar": {"zero": ("%1$d ثانية", "%1$d دقيقة"), "one": ("ثانية واحدة", "دقيقة واحدة"), "two": ("ثانيتان", "دقيقتان"),
           "few": ("%1$d ثوانٍ", "%1$d دقائق"), "many": ("%1$d ثانية", "%1$d دقيقة"), "other": ("%1$d ثانية", "%1$d دقيقة")},
}

# Arabic reads right to left: the flow arrow and the «from → to» line point the other way.
EXTRA = {"ar": {"arrow": "←", "size_to": "%1$s ← %2$s"}}


def xml_text(s: str) -> str:
    """Escape for an Android string resource: XML entities, then the apostrophe and the double quote."""
    return escape(s).replace("'", "\\'").replace('"', '\\"')


def main():
    for lang, values in T.items():
        assert len(values) == len(KEYS), (lang, len(values), len(KEYS))
        lines = [f'    <string name="{k}">{xml_text(v).replace(chr(92) + chr(92) + "n", chr(92) + "n")}</string>' for k, v in zip(KEYS, values)]
        for name, idx in (("duration_seconds", 0), ("duration_minutes", 1)):
            items = "\n".join(f'        <item quantity="{q}">{xml_text(forms[idx])}</item>' for q, forms in P[lang].items())
            lines.append(f'    <plurals name="{name}">\n{items}\n    </plurals>')
        for k, v in EXTRA.get(lang, {}).items():
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        body = "\n".join(lines)
        out = ROOT / "app" / "src" / "main" / "res" / f"values-{lang}" / "strings.xml"
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by tools/make-translations.py - edit the script, not this file. [AI] the agent's DRAFT translation of
     values/strings.xml into "{lang}" (plans/01 §4.7); awaits a native speaker's or the owner's review. -->
<resources>
{body}
</resources>
""", encoding="utf-8", newline="\n")
        print(f"written values-{lang}/strings.xml ({len(KEYS)} strings)")


if __name__ == "__main__":
    main()
