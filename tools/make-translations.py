"""make-translations.py - writes res/values-<lang>/strings.xml for the 8 languages beyond EN and RU of plans/01 §4.7
(ES, PT, ZH, HI, BN, FR, DE, AR - the list the owner chose for KrinikCam, researches/01 §4).

The texts are the agent's translation of res/values/strings.xml (English), checked and accepted by the owner 2026-10-15
≈ 13:26 +03:00 («я проверил, все ок» - MASTER_PLAN decision log); a native speaker's corrections go into this script. EN and RU are hand-kept and are not touched by this script.
Plurals follow each language's CLDR categories (Arabic six, Chinese one form). [NOT-TESTED: shown on the device per
language - deferred by the owner («потом потестируем»)]

Usage (from the project root): python tools/make-translations.py [--check]
"""
import sys
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parent.parent
KEYS = ["home_pick_title", "home_pick_sub", "home_paste_link", "home_share_title", "home_share_text", "flow_photo",
        "flow_share", "setup_title", "setup_other_photo", "setup_other_short", "label_model",
        "label_scale", "model_fast_desc", "model_max_desc", "licence_bsd", "megapixels",
        "scale_too_big", "scale_two_pass", "upscale", "upscale_with_estimate", "info_close", "info_scale_text",
        "info_scale_list", "info_model_text", "info_model_list", "working_title", "working_tiles", "working_starting",
        "cancel", "before", "after", "save", "share", "error_too_big", "error_decode", "error_model", "error_engine",
        "notif_channel", "notif_progress", "notif_starting", "save_title", "label_format", "label_quality",
        "save_folder", "saved", "save_failed", "saving", "webp_too_big"]

T = {
    "es": ["Elige una foto", "de la galería del teléfono", "Pegar enlace de foto", "O envía una foto desde otra app",
           "Abre una foto en la galería, Telegram o el navegador, toca «Compartir» y elige KARTUN: la foto se abrirá aquí lista para mejorar.",
           "Foto", "Compartir", "Mejorar", "Elegir otra foto", "Otra foto",
           "Modelo", "Escala", "Rápido · para fotos con ruido y desenfoque", "Detallado · para fotos donde importan los detalles finos", "Uso comercial permitido · BSD-3", "%1$s MP",
           "El resultado superaría %1$s MP: para esta foto elige hasta %2$d×", "2 pasadas · más lento · archivo grande",
           "Mejorar", "Mejorar · ≈ %1$s", "Cerrar",
           "La escala indica cuántas veces crecen el ancho y el alto de la foto: 1×, 2×, 4×, 8× o 16×; 4× por defecto.",
           "• 4× — la escala nativa de los modelos: están entrenados para ampliar exactamente 4 veces y dan el mejor resultado a 4×.\\n• 1× y 2× — la misma pasada de 4×, luego el resultado se reduce al tamaño elegido; 1× solo quita ruido y desenfoque.\\n• 8× y 16× — dos pasadas: de 4 a 16 veces más lento, archivo muy grande.",
           "Un modelo es una red neuronal entrenada. Los modelos se diferencian por las imágenes con las que aprendieron: uno restaura mejor fotos, otro dibujos.",
           "• Rápido — quita ruido y desenfoque, normalmente unos segundos.\\n• Detallado — detalles finos más nítidos, tarda más.",
           "La red está mejorando tu foto", "Mosaico %1$s de %2$d", "Quedan ≈ %1$s", "Cancelar", "Antes",
           "Después", "Guardar", "Compartir",
           "Las fotos de más de %1$s MP no son compatibles.",
           "No se pudo abrir esta foto.", "No se pudo cargar el modelo. Reinstala la app.", "La red no pudo procesar esta foto.",
           "Procesamiento", "%1$d%% hecho", "Iniciando la red…", "Guardar en la galería", "Formato", "Calidad · %1$d",
           "Carpeta: Pictures/KARTUN · el original no cambia", "Guardado en Pictures/KARTUN", "No se pudo guardar la foto.",
           "Guardando…", "WebP admite hasta 16 383 px por lado; elige PNG o JPEG para este resultado"],
    "pt": ["Escolha uma foto", "da galeria do telefone", "Colar link da foto", "Ou envie uma foto de outro app",
           "Abra uma foto na galeria, no Telegram ou no navegador, toque em «Compartilhar» e escolha KARTUN — a foto abre aqui pronta para melhorar.",
           "Foto", "Compartilhar", "Melhorar", "Escolher outra foto", "Outra foto",
           "Modelo", "Escala", "Rápido · para fotos com ruído e desfoque", "Detalhado · para fotos em que os detalhes finos importam", "Uso comercial permitido · BSD-3", "%1$s MP",
           "O resultado passaria de %1$s MP — para esta foto escolha até %2$d×", "2 passagens · mais demorado · arquivo grande",
           "Melhorar", "Melhorar · ≈ %1$s", "Fechar",
           "A escala diz quantas vezes a largura e a altura da foto crescem: 1×, 2×, 4×, 8× ou 16×; 4× por padrão.",
           "• 4× — a escala nativa dos modelos: eles são treinados para ampliar exatamente 4 vezes e dão o melhor resultado em 4×.\\n• 1× e 2× — a mesma passagem de 4×, depois o resultado é reduzido ao tamanho escolhido; 1× só remove ruído e desfoque.\\n• 8× e 16× — duas passagens: de 4 a 16 vezes mais demorado, arquivo muito grande.",
           "Um modelo é uma rede neural treinada. Os modelos diferem nas imagens com que aprenderam: um restaura melhor fotos, outro desenhos.",
           "• Rápido — remove ruído e desfoque, normalmente alguns segundos.\\n• Detalhado — detalhes finos mais nítidos, demora mais.",
           "A rede está melhorando sua foto", "Bloco %1$s de %2$d", "Faltam ≈ %1$s", "Cancelar", "Antes",
           "Depois", "Salvar", "Compartilhar",
           "Fotos acima de %1$s MP não são suportadas.",
           "Não foi possível abrir esta foto.", "Não foi possível carregar o modelo. Reinstale o app.", "A rede não conseguiu processar esta foto.",
           "Processamento", "%1$d%% concluído", "Iniciando a rede…", "Salvar na galeria", "Formato", "Qualidade · %1$d",
           "Pasta: Pictures/KARTUN · o original não muda", "Salvo em Pictures/KARTUN", "Não foi possível salvar a foto.",
           "Salvando…", "WebP aceita até 16 383 px por lado; escolha PNG ou JPEG para este resultado"],
    "fr": ["Choisissez une photo", "dans la galerie du téléphone", "Coller un lien de photo", "Ou envoyez une photo depuis une autre appli",
           "Ouvrez une photo dans la galerie, Telegram ou le navigateur, touchez « Partager » et choisissez KARTUN — la photo s’ouvre ici, prête à être améliorée.",
           "Photo", "Partager", "Améliorer", "Choisir une autre photo", "Autre photo",
           "Modèle", "Échelle", "Rapide · pour les photos avec bruit et flou", "Détaillé · pour les photos où les fins détails comptent", "Usage commercial autorisé · BSD-3", "%1$s Mpx",
           "Le résultat dépasserait %1$s Mpx — pour cette photo, choisissez jusqu’à %2$d×", "2 passes · plus long · gros fichier",
           "Améliorer", "Améliorer · ≈ %1$s", "Fermer",
           "L’échelle indique combien de fois la largeur et la hauteur de la photo augmentent : 1×, 2×, 4×, 8× ou 16× ; 4× par défaut.",
           "• 4× — l’échelle native des modèles : ils sont entraînés pour agrandir exactement 4 fois et donnent le meilleur résultat à 4×.\\n• 1× et 2× — la même passe à 4×, puis le résultat est réduit à la taille choisie ; 1× retire seulement le bruit et le flou.\\n• 8× et 16× — deux passes : 4 à 16 fois plus long, fichier très volumineux.",
           "Un modèle est un réseau de neurones entraîné. Les modèles diffèrent par les images apprises : l’un restaure mieux les photos, l’autre les dessins.",
           "• Rapide — enlève le bruit et le flou, en général quelques secondes.\\n• Détaillé — petits détails plus nets, prend plus de temps.",
           "Le réseau améliore votre photo", "Tuile %1$s sur %2$d", "Reste ≈ %1$s", "Annuler", "Avant",
           "Après", "Enregistrer", "Partager",
           "Les photos de plus de %1$s Mpx ne sont pas prises en charge.",
           "Impossible d’ouvrir cette photo.", "Impossible de charger le modèle. Réinstallez l’appli.", "Le réseau n’a pas pu traiter cette photo.",
           "Traitement", "%1$d %% fait", "Démarrage du réseau…", "Enregistrer dans la galerie", "Format", "Qualité · %1$d",
           "Dossier : Pictures/KARTUN · l’original reste intact", "Enregistré dans Pictures/KARTUN", "Impossible d’enregistrer la photo.",
           "Enregistrement…", "Le WebP va jusqu’à 16 383 px par côté ; choisissez PNG ou JPEG pour ce résultat"],
    "de": ["Foto auswählen", "aus der Galerie des Telefons", "Foto-Link einfügen", "Oder ein Foto aus einer anderen App senden",
           "Öffne ein Foto in der Galerie, in Telegram oder im Browser, tippe auf „Teilen“ und wähle KARTUN – das Foto öffnet sich hier, bereit zum Verbessern.",
           "Foto", "Teilen", "Verbessern", "Anderes Foto wählen", "Anderes Foto",
           "Modell", "Maßstab", "Schnell · für Fotos mit Rauschen und Unschärfe", "Detailliert · für Fotos, bei denen feine Details zählen", "Kommerzielle Nutzung erlaubt · BSD-3", "%1$s MP",
           "Das Ergebnis würde %1$s MP überschreiten – für dieses Foto bis %2$d× wählen", "2 Durchgänge · länger · große Datei",
           "Verbessern", "Verbessern · ≈ %1$s", "Schließen",
           "Der Maßstab gibt an, wie oft Breite und Höhe des Fotos wachsen: 1×, 2×, 4×, 8× oder 16×; standardmäßig 4×.",
           "• 4× – der native Maßstab der Modelle: Sie sind darauf trainiert, genau 4-fach zu vergrößern, und liefern bei 4× das beste Ergebnis.\\n• 1× und 2× – derselbe 4×-Durchgang, danach wird das Ergebnis auf die gewählte Größe verkleinert; 1× entfernt nur Rauschen und Unschärfe.\\n• 8× und 16× – zwei Durchgänge: 4- bis 16-mal länger, sehr große Datei.",
           "Ein Modell ist ein trainiertes neuronales Netz. Modelle unterscheiden sich darin, womit sie gelernt haben: eines stellt Fotos besser wieder her, ein anderes Zeichnungen.",
           "• Schnell — entfernt Rauschen und Unschärfe, meist ein paar Sekunden.\\n• Detailliert — feine Details schärfer, dauert länger.",
           "Das Netz verbessert dein Foto", "Kachel %1$s von %2$d", "Noch ≈ %1$s", "Abbrechen", "Vorher",
           "Nachher", "Speichern", "Teilen",
           "Fotos über %1$s MP werden nicht unterstützt.",
           "Dieses Foto ließ sich nicht öffnen.", "Das Modell ließ sich nicht laden. Bitte installiere die App neu.", "Das Netz konnte dieses Foto nicht verarbeiten.",
           "Verarbeitung", "%1$d %% fertig", "Netz wird gestartet…", "In der Galerie speichern", "Format", "Qualität · %1$d",
           "Ordner: Pictures/KARTUN · das Original bleibt unverändert", "In Pictures/KARTUN gespeichert", "Das Foto ließ sich nicht speichern.",
           "Wird gespeichert…", "WebP fasst bis 16 383 px pro Seite; wähle für dieses Ergebnis PNG oder JPEG"],
    "zh": ["选择照片", "从手机相册", "粘贴照片链接", "或从其他应用发送照片",
           "在相册、Telegram 或浏览器中打开照片，点按“分享”并选择 KARTUN——照片会在这里打开，随时可以增强。",
           "照片", "分享", "增强", "选择其他照片", "其他照片",
           "模型", "放大倍数", "快速 · 适用于有噪点和模糊的照片", "细节 · 适用于注重细节的照片", "允许商业使用 · BSD-3", "%1$s 百万像素",
           "结果将超过 %1$s 百万像素——这张照片最多可选 %2$d×", "2 次处理 · 更慢 · 文件很大",
           "增强", "增强 · 约 %1$s", "关闭",
           "放大倍数表示照片的宽和高增大几倍：1×、2×、4×、8× 或 16×；默认 4×。",
           "• 4×——模型的原生倍数：模型经过训练，正好放大 4 倍，在 4× 时效果最好。\\n• 1× 和 2×——同样先处理一次 4×，再把结果缩小到所选尺寸；1× 只去除噪点和模糊。\\n• 8× 和 16×——处理两次：时间长 4–16 倍，文件非常大。",
           "模型是训练好的神经网络。模型的区别在于学习过的图片：有的更擅长修复照片，有的更擅长修复绘画。",
           "• 快速 — 去除噪点和模糊，通常几秒钟。\\n• 细致 — 细节更清晰，耗时更长。",
           "神经网络正在增强你的照片", "第 %1$s / %2$d 块", "剩余约 %1$s", "取消", "处理前",
           "处理后", "保存", "分享",
           "不支持超过 %1$s 百万像素的照片。",
           "无法打开这张照片。", "无法加载模型。请重新安装应用。", "神经网络无法处理这张照片。",
           "处理中", "已完成 %1$d%%", "正在启动神经网络…", "保存到相册", "格式", "质量 · %1$d",
           "文件夹：Pictures/KARTUN · 原图保持不变", "已保存到 Pictures/KARTUN", "无法保存照片。",
           "正在保存…", "WebP 每边最多 16 383 像素；此结果请选择 PNG 或 JPEG"],
    "hi": ["फ़ोटो चुनें", "फ़ोन की गैलरी से", "फ़ोटो का लिंक चिपकाएँ", "या किसी दूसरे ऐप से फ़ोटो भेजें",
           "गैलरी, Telegram या ब्राउज़र में फ़ोटो खोलें, «शेयर करें» दबाएँ और KARTUN चुनें — फ़ोटो यहीं खुलेगी, बेहतर बनाने के लिए तैयार।",
           "फ़ोटो", "शेयर करें", "बेहतर बनाएँ", "दूसरी फ़ोटो चुनें", "दूसरी फ़ोटो",
           "मॉडल", "स्केल", "तेज़ · शोर और धुंधलेपन वाली फ़ोटो के लिए", "बारीक · उन फ़ोटो के लिए जिनमें बारीकियाँ अहम हैं", "व्यावसायिक उपयोग की अनुमति · BSD-3", "%1$s MP",
           "नतीजा %1$s MP से बड़ा होगा — इस फ़ोटो के लिए %2$d× तक चुनें", "2 बार · ज़्यादा समय · बड़ी फ़ाइल",
           "बेहतर बनाएँ", "बेहतर बनाएँ · ≈ %1$s", "बंद करें",
           "स्केल बताता है कि फ़ोटो की चौड़ाई और ऊँचाई कितने गुना बढ़ेगी: 1×, 2×, 4×, 8× या 16×; डिफ़ॉल्ट 4×।",
           "• 4× — मॉडलों का मूल स्केल: उन्हें ठीक 4 गुना बड़ा करने के लिए प्रशिक्षित किया गया है और 4× पर वे सबसे अच्छा नतीजा देते हैं।\\n• 1× और 2× — वही 4× वाली प्रोसेसिंग, फिर नतीजे को चुने गए आकार तक छोटा किया जाता है; 1× सिर्फ़ शोर और धुंधलापन हटाता है।\\n• 8× और 16× — दो बार: 4–16 गुना ज़्यादा समय, बहुत बड़ी फ़ाइल।",
           "मॉडल एक प्रशिक्षित न्यूरल नेटवर्क है। मॉडल इस बात में अलग हैं कि उन्होंने किन तस्वीरों से सीखा: एक फ़ोटो बेहतर सुधारता है, दूसरा चित्र।",
           "• तेज़ — शोर और धुंधलापन हटाता है, आमतौर पर कुछ सेकंड।\\n• विस्तृत — बारीक विवरण अधिक स्पष्ट, ज़्यादा समय लेता है।",
           "न्यूरल नेटवर्क आपकी फ़ोटो बेहतर बना रहा है", "टाइल %1$s / %2$d", "बाक़ी ≈ %1$s", "रद्द करें", "पहले",
           "बाद में", "सहेजें", "शेयर करें",
           "%1$s MP से बड़ी फ़ोटो समर्थित नहीं हैं।",
           "यह फ़ोटो नहीं खुल सकी।", "मॉडल लोड नहीं हो सका। कृपया ऐप दोबारा इंस्टॉल करें।", "न्यूरल नेटवर्क यह फ़ोटो प्रोसेस नहीं कर सका।",
           "प्रोसेसिंग", "%1$d%% पूरा", "नेटवर्क शुरू हो रहा है…", "गैलरी में सहेजें", "फ़ॉर्मैट", "गुणवत्ता · %1$d",
           "फ़ोल्डर: Pictures/KARTUN · मूल फ़ोटो नहीं बदलती", "Pictures/KARTUN में सहेजा गया", "फ़ोटो सहेजी नहीं जा सकी।",
           "सहेजा जा रहा है…", "WebP में हर तरफ़ 16 383 px तक; इस नतीजे के लिए PNG या JPEG चुनें"],
    "bn": ["একটি ছবি বেছে নিন", "ফোনের গ্যালারি থেকে", "ছবির লিংক পেস্ট করুন", "অথবা অন্য অ্যাপ থেকে ছবি পাঠান",
           "গ্যালারি, Telegram বা ব্রাউজারে ছবি খুলুন, «শেয়ার» চাপুন এবং KARTUN বেছে নিন — ছবিটি এখানে খুলবে, উন্নত করার জন্য প্রস্তুত।",
           "ছবি", "শেয়ার", "উন্নত করুন", "অন্য ছবি বেছে নিন", "অন্য ছবি",
           "মডেল", "স্কেল", "দ্রুত · নয়েজ ও ঝাপসা ছবির জন্য", "বিস্তারিত · যে ছবিতে সূক্ষ্ম খুঁটিনাটি জরুরি তার জন্য", "বাণিজ্যিক ব্যবহারের অনুমতি আছে · BSD-3", "%1$s MP",
           "ফলাফল %1$s MP ছাড়িয়ে যাবে — এই ছবির জন্য %2$d× পর্যন্ত বেছে নিন", "২ বার · বেশি সময় · বড় ফাইল",
           "উন্নত করুন", "উন্নত করুন · ≈ %1$s", "বন্ধ করুন",
           "স্কেল বলে ছবির প্রস্থ ও উচ্চতা কত গুণ বাড়বে: 1×, 2×, 4×, 8× বা 16×; ডিফল্ট 4×।",
           "• 4× — মডেলগুলোর নিজস্ব স্কেল: এগুলোকে ঠিক 4 গুণ বড় করার জন্য প্রশিক্ষণ দেওয়া হয়েছে, আর 4×-এ এগুলো সবচেয়ে ভালো ফল দেয়।\\n• 1× ও 2× — সেই একই 4× প্রক্রিয়া, তারপর ফলাফল বেছে নেওয়া আকারে ছোট করা হয়; 1× শুধু নয়েজ ও ঝাপসা ভাব সরায়।\\n• 8× ও 16× — দুবার: ৪–১৬ গুণ বেশি সময়, খুব বড় ফাইল।",
           "মডেল হলো একটি প্রশিক্ষিত নিউরাল নেটওয়ার্ক। মডেলগুলো আলাদা কোন ছবি থেকে শিখেছে তাতে: একটি ছবি ভালো সারায়, অন্যটি আঁকা।",
           "• দ্রুত — নয়েজ ও ঝাপসা ভাব দূর করে, সাধারণত কয়েক সেকেন্ড।\\n• বিস্তারিত — সূক্ষ্ম খুঁটিনাটি আরও স্পষ্ট, সময় বেশি লাগে।",
           "নিউরাল নেটওয়ার্ক আপনার ছবি উন্নত করছে", "টাইল %1$s / %2$d", "বাকি ≈ %1$s", "বাতিল", "আগে",
           "পরে", "সংরক্ষণ", "শেয়ার",
           "%1$s MP-এর বড় ছবি সমর্থিত নয়।",
           "এই ছবিটি খোলা যায়নি।", "মডেল লোড করা যায়নি। অ্যাপটি আবার ইনস্টল করুন।", "নিউরাল নেটওয়ার্ক এই ছবি প্রক্রিয়া করতে পারেনি।",
           "প্রক্রিয়া", "%1$d%% সম্পন্ন", "নেটওয়ার্ক চালু হচ্ছে…", "গ্যালারিতে সংরক্ষণ", "ফরম্যাট", "গুণমান · %1$d",
           "ফোল্ডার: Pictures/KARTUN · মূল ছবি অপরিবর্তিত থাকে", "Pictures/KARTUN-এ সংরক্ষিত", "ছবি সংরক্ষণ করা যায়নি।",
           "সংরক্ষণ হচ্ছে…", "WebP প্রতি পাশে 16 383 px পর্যন্ত; এই ফলাফলের জন্য PNG বা JPEG বেছে নিন"],
    "ar": ["اختر صورة", "من معرض الهاتف", "الصق رابط الصورة", "أو أرسل صورة من تطبيق آخر",
           "افتح صورة في المعرض أو Telegram أو المتصفح، واضغط «مشاركة» واختر KARTUN — ستُفتح الصورة هنا جاهزة للتحسين.",
           "صورة", "مشاركة", "تحسين", "اختر صورة أخرى", "صورة أخرى",
           "النموذج", "المقياس", "سريع · للصور التي فيها تشويش وضبابية", "مفصّل · للصور التي تهم فيها التفاصيل الدقيقة", "مسموح بالاستخدام التجاري · BSD-3", "%1$s ميغابكسل",
           "ستتجاوز النتيجة %1$s ميغابكسل — اختر لهذه الصورة حتى %2$d×", "مرّتان · أطول · ملف كبير",
           "تحسين", "تحسين · ≈ %1$s", "إغلاق",
           "المقياس هو عدد المرات التي يكبر بها عرض الصورة وارتفاعها: 1× أو 2× أو 4× أو 8× أو 16×؛ والافتراضي 4×.",
           "• 4× — المقياس الأصلي للنماذج: دُرِّبت على التكبير 4 مرات بالضبط، وتعطي أفضل نتيجة عند 4×.\\n• 1× و2× — المرور نفسه بمقياس 4×، ثم تُصغَّر النتيجة إلى الحجم المختار؛ و1× يزيل الضجيج والضبابية فقط.\\n• 8× و16× — مروران: أطول بـ 4 إلى 16 مرة، وملف كبير جدًا.",
           "النموذج شبكة عصبية مدرَّبة. تختلف النماذج في الصور التي تعلّمت منها: أحدها يرمّم الصور أفضل، والآخر الرسوم.",
           "• سريع — يزيل التشويش والضبابية، عادةً بضع ثوانٍ.\\n• مفصّل — تفاصيل دقيقة أوضح، يستغرق وقتًا أطول.",
           "الشبكة العصبية تحسّن صورتك", "البلاطة %1$s من %2$d", "المتبقي ≈ %1$s", "إلغاء", "قبل",
           "بعد", "حفظ", "مشاركة",
           "الصور الأكبر من %1$s ميغابكسل غير مدعومة.",
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

# «Вставить ссылку» strings: link_title · link_failed · link_downloading · link_download (the empty-clipboard window).
LINK = {
    "es": ("Enlace a la foto", "No se pudo descargar la foto del enlace.", "Descargando la foto…", "Descargar"),
    "pt": ("Link da foto", "Não foi possível baixar a foto do link.", "Baixando a foto…", "Baixar"),
    "fr": ("Lien de la photo", "Impossible de télécharger la photo depuis le lien.", "Téléchargement de la photo…", "Télécharger"),
    "de": ("Foto-Link", "Das Foto ließ sich nicht über den Link laden.", "Foto wird geladen…", "Herunterladen"),
    "zh": ("照片链接", "无法通过链接下载照片。", "正在下载照片…", "下载"),
    "hi": ("फ़ोटो का लिंक", "लिंक से फ़ोटो डाउनलोड नहीं हो सकी।", "फ़ोटो डाउनलोड हो रही है…", "डाउनलोड करें"),
    "bn": ("ছবির লিংক", "লিংক থেকে ছবি ডাউনলোড করা যায়নি।", "ছবি ডাউনলোড হচ্ছে…", "ডাউনলোড করুন"),
    "ar": ("رابط الصورة", "تعذّر تنزيل الصورة من الرابط.", "جارٍ تنزيل الصورة…", "تنزيل"),
}

# Settings strings (third batch), in the order of SETTINGS_KEYS.
SETTINGS_KEYS = ["settings_title", "settings_processing", "settings_app", "settings_gpu", "settings_gpu_sub", "settings_language",
                 "settings_language_sub", "info_gpu_text", "info_gpu_list", "info_language_text"]
SETTINGS = {
    "es": ["Ajustes", "Procesamiento", "Aplicación", "Aceleración GPU", "Vulkan · %1$s", "Idioma", "Como el sistema o uno propio para KARTUN",
           "La red calcula en el chip gráfico del teléfono con Vulkan: unas 15 veces más rápido que en el procesador.",
           "• Activado — el modo normal.\\n• Desactivado — solo el procesador: lento, pero ayuda si el chip gráfico de este teléfono falla.",
           "Se abre la lista del sistema con los idiomas de KARTUN: el idioma de la app puede ser distinto al del teléfono."],
    "pt": ["Configurações", "Processamento", "App", "Aceleração por GPU", "Vulkan · %1$s", "Idioma", "Como o sistema ou um próprio para o KARTUN",
           "A rede calcula no chip gráfico do telefone via Vulkan — cerca de 15 vezes mais rápido que no processador.",
           "• Ligado — o modo normal.\\n• Desligado — só o processador: lento, mas ajuda se o chip gráfico deste telefone falhar.",
           "Abre a lista do sistema com os idiomas do KARTUN: o idioma do app pode ser diferente do idioma do telefone."],
    "fr": ["Réglages", "Traitement", "Application", "Accélération GPU", "Vulkan · %1$s", "Langue", "Comme le système ou propre à KARTUN",
           "Le réseau calcule sur la puce graphique du téléphone via Vulkan — environ 15 fois plus vite que sur le processeur.",
           "• Activé — le mode normal.\\n• Désactivé — le processeur seul : lent, mais utile si la puce graphique de ce téléphone défaille.",
           "La liste système des langues de KARTUN s’ouvre : la langue de l’appli peut différer de celle du téléphone."],
    "de": ["Einstellungen", "Verarbeitung", "App", "GPU-Beschleunigung", "Vulkan · %1$s", "Sprache", "Wie das System oder eigene für KARTUN",
           "Das Netz rechnet über Vulkan auf dem Grafikchip des Telefons – etwa 15-mal schneller als auf dem Prozessor.",
           "• An – der normale Modus.\\n• Aus – nur der Prozessor: langsam, hilft aber, wenn der Grafikchip dieses Telefons versagt.",
           "Die Systemliste der Sprachen von KARTUN öffnet sich: Die App-Sprache kann von der Telefonsprache abweichen."],
    "zh": ["设置", "处理", "应用", "GPU 加速", "Vulkan · %1$s", "语言", "跟随系统或为 KARTUN 单独设置",
           "神经网络通过 Vulkan 在手机图形芯片上运算——比处理器快约 15 倍。",
           "• 开启——常规模式。\\n• 关闭——只用处理器：较慢，但当这部手机的图形芯片出错时有用。",
           "将打开系统中 KARTUN 的语言列表：应用语言可以与手机语言不同。"],
    "hi": ["सेटिंग्स", "प्रोसेसिंग", "ऐप", "GPU एक्सेलेरेशन", "Vulkan · %1$s", "भाषा", "सिस्टम जैसी या KARTUN की अपनी",
           "न्यूरल नेटवर्क Vulkan से फ़ोन की ग्राफ़िक्स चिप पर चलता है — प्रोसेसर से लगभग 15 गुना तेज़।",
           "• चालू — सामान्य मोड।\\n• बंद — सिर्फ़ प्रोसेसर: धीमा, पर मदद करता है अगर इस फ़ोन की ग्राफ़िक्स चिप गड़बड़ करे।",
           "KARTUN की भाषाओं की सिस्टम सूची खुलेगी: ऐप की भाषा फ़ोन की भाषा से अलग हो सकती है।"],
    "bn": ["সেটিংস", "প্রক্রিয়া", "অ্যাপ", "GPU ত্বরণ", "Vulkan · %1$s", "ভাষা", "সিস্টেমের মতো বা KARTUN-এর নিজস্ব",
           "নিউরাল নেটওয়ার্ক Vulkan দিয়ে ফোনের গ্রাফিক্স চিপে চলে — প্রসেসরের চেয়ে প্রায় ১৫ গুণ দ্রুত।",
           "• চালু — সাধারণ মোড।\\n• বন্ধ — শুধু প্রসেসর: ধীর, তবে এই ফোনের গ্রাফিক্স চিপ সমস্যা করলে কাজে আসে।",
           "KARTUN-এর ভাষার সিস্টেম তালিকা খুলবে: অ্যাপের ভাষা ফোনের ভাষা থেকে আলাদা হতে পারে।"],
    "ar": ["الإعدادات", "المعالجة", "التطبيق", "تسريع GPU", "Vulkan · %1$s", "اللغة", "مثل النظام أو لغة خاصة بـ KARTUN",
           "تعمل الشبكة على شريحة رسوم الهاتف عبر Vulkan — أسرع بنحو 15 مرة من المعالج.",
           "• تشغيل — الوضع العادي.\\n• إيقاف — المعالج فقط: أبطأ، لكنه يفيد إذا تعطّلت شريحة رسوم هذا الهاتف.",
           "ستُفتح قائمة النظام بلغات KARTUN: يمكن أن تختلف لغة التطبيق عن لغة الهاتف."],
}

# User guide strings (fourth batch): manual_title · settings_manual · settings_manual_sub. The guide's BODY is
# res/raw/manual.md (EN) and res/raw-ru/manual.md; the other languages show the English body until translated.
GUIDE = {
    "es": ("Guía", "Guía del usuario", "Cómo usar KARTUN y sus modelos"),
    "pt": ("Guia", "Guia do usuário", "Como usar o KARTUN e seus modelos"),
    "fr": ("Guide", "Guide de l’utilisateur", "Comment utiliser KARTUN et ses modèles"),
    "de": ("Anleitung", "Benutzeranleitung", "So nutzt du KARTUN und seine Modelle"),
    "zh": ("指南", "用户指南", "如何使用 KARTUN 及其模型"),
    "hi": ("गाइड", "उपयोगकर्ता गाइड", "KARTUN और उसके मॉडल कैसे इस्तेमाल करें"),
    "bn": ("নির্দেশিকা", "ব্যবহারকারী নির্দেশিকা", "KARTUN ও এর মডেল কীভাবে ব্যবহার করবেন"),
    "ar": ("الدليل", "دليل المستخدم", "كيف تستخدم KARTUN ونماذجه"),
}

# «Открыть руководство полностью» in the (i) windows.
OPEN_GUIDE = {"es": "Abrir la guía completa", "pt": "Abrir o guia completo", "fr": "Ouvrir le guide complet", "de": "Die ganze Anleitung öffnen",
              "zh": "打开完整指南", "hi": "पूरी गाइड खोलें", "bn": "পুরো নির্দেশিকা খুলুন", "ar": "افتح الدليل كاملًا"}

# The first-launch speed check (ideas/04; the owner, chat 2026-10-16 ≈ 20:0x +03:00: «"Измеряем производительность GPU и
# CPU устройства..."» · «Закрыть можно кнопкой [Отмена]»): bench_title · bench_cancel.
BENCH = {"es": ("Midiendo el rendimiento de la GPU y la CPU del dispositivo…", "Cancelar"),
         "pt": ("Medindo o desempenho da GPU e da CPU do dispositivo…", "Cancelar"),
         "fr": ("Mesure des performances du GPU et du CPU de l’appareil…", "Annuler"),
         "de": ("Leistung von GPU und CPU des Geräts wird gemessen…", "Abbrechen"),
         "zh": ("正在测量设备 GPU 和 CPU 的性能…", "取消"),
         "hi": ("डिवाइस के GPU और CPU का प्रदर्शन मापा जा रहा है…", "रद्द करें"),
         "bn": ("ডিভাইসের GPU ও CPU-এর কার্যক্ষমতা মাপা হচ্ছে…", "বাতিল"),
         "ar": ("جارٍ قياس أداء GPU وCPU في الجهاز…", "إلغاء")}

# The gold label under the owner's favourite models (2026-10-16 ≈ 21:4x: «золотая звезда и ниже золотом текст ВЫБОР KARTUN»).
CHOICE = {"es": "ELECCIÓN DE KARTUN", "pt": "ESCOLHA DO KARTUN", "fr": "CHOIX DE KARTUN", "de": "KARTUNS WAHL",
          "zh": "KARTUN 精选", "hi": "KARTUN की पसंद", "bn": "KARTUN-এর পছন্দ", "ar": "اختيار KARTUN"}

# «О приложении»: about_title · about_tagline · about_device · about_model · about_engine · about_licences ·
# about_project · about_inspired · about_author · about_source (the owner, chat 2026-10-14 ≈ 20:58 +03:00: «о приложении
# и авторе нужна и ссылка на наш GH»; plan 03 step 21).
ABOUT_KEYS = ["about_title", "about_tagline", "about_device", "about_model", "about_engine", "about_licences",
              "about_project", "about_inspired", "about_author", "about_source", "settings_about_sub", "terms_title", "terms_sub",
              "about_engine_fork"]
ABOUT = {
    "es": ["Acerca de", "Versión %1$s", "Dispositivo", "Modelo del dispositivo", "Motor", "Licencias", "Proyecto", "Inspiración y motor", "Autor", "Código fuente en GitHub", "Versión, dispositivo, autor, licencias", "Condiciones de uso", "Derechos de los modelos y responsabilidad", "fork propio, modificado y mejorado"],
    "pt": ["Sobre", "Versão %1$s", "Dispositivo", "Modelo do dispositivo", "Motor", "Licenças", "Projeto", "Inspiração e motor", "Autor", "Código-fonte no GitHub", "Versão, dispositivo, autor, licenças", "Termos de uso", "Direitos dos modelos e responsabilidade", "fork próprio, modificado e aprimorado"],
    "fr": ["À propos", "Version %1$s", "Appareil", "Modèle de l’appareil", "Moteur", "Licences", "Projet", "Inspiration et moteur", "Auteur", "Code source sur GitHub", "Version, appareil, auteur, licences", "Conditions d’utilisation", "Droits des modèles et responsabilité", "fork personnalisé, modifié et amélioré"],
    "de": ["Über", "Version %1$s", "Gerät", "Gerätemodell", "Engine", "Lizenzen", "Projekt", "Inspiration und Engine", "Autor", "Quellcode auf GitHub", "Version, Gerät, Autor, Lizenzen", "Nutzungsbedingungen", "Rechte an Modellen und Haftung", "eigener, angepasster und verbesserter Fork"],
    "zh": ["关于", "版本 %1$s", "设备", "设备型号", "引擎", "许可证", "项目", "灵感与引擎", "作者", "GitHub 上的源代码", "版本、设备、作者、许可证", "使用条款", "模型权利与责任", "定制、修改并改进的分支"],
    "hi": ["ऐप के बारे में", "संस्करण %1$s", "डिवाइस", "डिवाइस मॉडल", "इंजन", "लाइसेंस", "प्रोजेक्ट", "प्रेरणा और इंजन", "लेखक", "GitHub पर सोर्स कोड", "संस्करण, डिवाइस, लेखक, लाइसेंस", "उपयोग की शर्तें", "मॉडलों के अधिकार और ज़िम्मेदारी", "कस्टम, संशोधित और बेहतर फ़ोर्क"],
    "bn": ["অ্যাপ সম্পর্কে", "সংস্করণ %1$s", "ডিভাইস", "ডিভাইসের মডেল", "ইঞ্জিন", "লাইসেন্স", "প্রকল্প", "অনুপ্রেরণা ও ইঞ্জিন", "লেখক", "GitHub-এ সোর্স কোড", "সংস্করণ, ডিভাইস, লেখক, লাইসেন্স", "ব্যবহারের শর্তাবলি", "মডেলের অধিকার ও দায়", "কাস্টম, পরিবর্তিত ও উন্নত ফর্ক"],
    "ar": ["حول التطبيق", "الإصدار %1$s", "الجهاز", "طراز الجهاز", "المحرك", "التراخيص", "المشروع", "الإلهام والمحرك", "المؤلف", "الشيفرة المصدرية على GitHub", "الإصدار، الجهاز، المؤلف، التراخيص", "شروط الاستخدام", "حقوق النماذج والمسؤولية", "نسخة متفرعة مخصّصة ومعدّلة ومحسّنة"],
}

# «Фото готово» notification: notif_done · notif_done_text.
DONE = {"es": ("Tu foto está lista", "Toca para comparar antes y después"), "pt": ("Sua foto está pronta", "Toque para comparar antes e depois"),
        "fr": ("Votre photo est prête", "Touchez pour comparer avant et après"), "de": ("Dein Foto ist fertig", "Tippe, um Vorher und Nachher zu vergleichen"),
        "zh": ("照片已完成", "点按以对比处理前后"), "hi": ("आपकी फ़ोटो तैयार है", "पहले और बाद की तुलना के लिए दबाएँ"),
        "bn": ("আপনার ছবি প্রস্তুত", "আগে ও পরে তুলনা করতে চাপুন"), "ar": ("صورتك جاهزة", "اضغط للمقارنة بين قبل وبعد")}


# Notification permission (interview #004): settings_notifications · _on · _off · info_notifications_text · notif_denied_title · notif_denied_text · notif_open_settings.
NOTIF_KEYS = ["settings_notifications", "settings_notifications_on", "settings_notifications_off", "info_notifications_text",
              "notif_denied_title", "notif_denied_text", "notif_open_settings"]
NOTIF = {
    "es": ["Notificaciones", "Permitidas: progreso y «Tu foto está lista»", "Desactivadas — toca para permitir",
           "Mientras la red trabaja, la barra muestra el porcentaje y «Cancelar», y una notificación avisa cuando la foto está lista. Sin el permiso el procesamiento sigue, solo sin la barra.",
           "Notificaciones desactivadas", "El permiso de notificaciones se rechazó antes y KARTUN ya no puede pedirlo por sí mismo. Activa las notificaciones a mano en los ajustes de Android para KARTUN.", "Abrir ajustes"],
    "pt": ["Notificações", "Permitidas: progresso e «Sua foto está pronta»", "Desativadas — toque para permitir",
           "Enquanto a rede trabalha, a barra mostra a porcentagem e «Cancelar», e uma notificação avisa quando a foto fica pronta. Sem a permissão o processamento continua, só sem a barra.",
           "Notificações desativadas", "A permissão de notificações foi recusada antes e o KARTUN não pode mais pedi-la sozinho. Ative as notificações manualmente nas configurações do Android para o KARTUN.", "Abrir configurações"],
    "fr": ["Notifications", "Autorisées : progression et « Votre photo est prête »", "Désactivées — touchez pour autoriser",
           "Pendant que le réseau travaille, le volet montre le pourcentage et « Annuler », et une notification prévient quand la photo est prête. Sans l’autorisation le traitement continue, seulement sans le volet.",
           "Notifications désactivées", "L’autorisation des notifications a été refusée auparavant et KARTUN ne peut plus la demander lui-même. Activez les notifications à la main dans les réglages Android de KARTUN.", "Ouvrir les réglages"],
    "de": ["Benachrichtigungen", "Erlaubt: Fortschritt und „Dein Foto ist fertig“", "Aus – tippen zum Erlauben",
           "Während das Netz arbeitet, zeigt die Leiste den Prozentsatz und „Abbrechen“, und eine Benachrichtigung meldet das fertige Foto. Ohne die Erlaubnis läuft die Verarbeitung trotzdem, nur ohne Leiste.",
           "Benachrichtigungen aus", "Die Benachrichtigungen wurden früher abgelehnt, und KARTUN kann nicht mehr selbst fragen. Schalte sie in den Android-Einstellungen für KARTUN von Hand ein.", "Einstellungen öffnen"],
    "zh": ["通知", "已允许：进度和“照片已完成”", "已关闭——点按以允许",
           "神经网络运行时，通知栏会显示百分比和“取消”，照片完成时会收到通知。没有此权限也能处理，只是通知栏中不显示。",
           "通知已关闭", "通知权限之前被拒绝，KARTUN 无法再自行请求。请在 Android 设置中为 KARTUN 手动开启通知。", "打开设置"],
    "hi": ["सूचनाएँ", "अनुमति है: प्रगति और «आपकी फ़ोटो तैयार है»", "बंद — अनुमति देने के लिए दबाएँ",
           "जब न्यूरल नेटवर्क काम करता है, शेड में प्रतिशत और «रद्द करें» दिखते हैं, और फ़ोटो तैयार होने पर सूचना आती है। अनुमति के बिना भी प्रोसेसिंग चलती है, बस शेड में नहीं दिखती।",
           "सूचनाएँ बंद हैं", "सूचनाओं की अनुमति पहले अस्वीकार की गई थी, और KARTUN अब ख़ुद नहीं पूछ सकता। KARTUN के लिए Android सेटिंग्स में सूचनाएँ हाथ से चालू करें।", "सेटिंग्स खोलें"],
    "bn": ["বিজ্ঞপ্তি", "অনুমোদিত: অগ্রগতি ও «আপনার ছবি প্রস্তুত»", "বন্ধ — অনুমতি দিতে চাপুন",
           "নিউরাল নেটওয়ার্ক কাজ করার সময় শেডে শতাংশ ও «বাতিল» দেখা যায়, আর ছবি তৈরি হলে বিজ্ঞপ্তি আসে। অনুমতি ছাড়াও প্রক্রিয়া চলে, শুধু শেডে দেখা যায় না।",
           "বিজ্ঞপ্তি বন্ধ", "বিজ্ঞপ্তির অনুমতি আগে প্রত্যাখ্যান করা হয়েছিল, আর KARTUN নিজে আর চাইতে পারে না। KARTUN-এর জন্য Android সেটিংসে হাতে বিজ্ঞপ্তি চালু করুন।", "সেটিংস খুলুন"],
    "ar": ["الإشعارات", "مسموح بها: التقدم و«صورتك جاهزة»", "متوقفة — اضغط للسماح",
           "أثناء عمل الشبكة يظهر في الشريط العلوي النسبة المئوية و«إلغاء»، ويصلك إشعار عندما تجهز الصورة. بدون الإذن تستمر المعالجة، لكن دون الشريط.",
           "الإشعارات متوقفة", "رُفض إذن الإشعارات سابقًا، ولم يعد بإمكان KARTUN طلبه بنفسه. فعّل الإشعارات يدويًا في إعدادات Android الخاصة بـ KARTUN.", "فتح الإعدادات"],
}

# The spinner over the before/after picture while the file for «Save» / «Share» is rendered (the owner, chat 2026-10-15
# ≈ 11:38 +03:00: «нужно крутить спиннер "Рендер" пока фото рендерится для Сожранения или Поделиться»).
RENDERING = {
    "es": "Renderizando…", "pt": "Renderizando…", "fr": "Rendu…", "de": "Wird gerendert…",
    "zh": "正在渲染…", "hi": "रेंडर हो रहा है…", "bn": "রেন্ডার হচ্ছে…", "ar": "جارٍ التصيير…",
}

# The save dialog (the owner, chat 2026-10-15 ≈ 12:00-12:01): save_estimate · format_points_png · _jpeg · _webp.
# "\\n" here is the two characters the Android resource reads as a line break.
SAVE_INFO = {
    "es": ("Archivo ≈ %1$s MB",
           "• Sin pérdidas: cada píxel como en el resultado\\n• El archivo más grande\\n• Para imprimir y seguir editando",
           "• Se abre en todas partes\\n• Mucho más ligero que PNG\\n• Para enviar y redes sociales",
           "• El archivo más pequeño con el mismo aspecto\\n• Lo abren Android, navegadores y mensajeros\\n• Para guardar y enviar"),
    "pt": ("Arquivo ≈ %1$s MB",
           "• Sem perdas: cada pixel como no resultado\\n• O maior arquivo\\n• Para imprimir e continuar editando",
           "• Abre em qualquer lugar\\n• Bem menor que PNG\\n• Para enviar e redes sociais",
           "• O menor arquivo com a mesma aparência\\n• Android, navegadores e mensageiros abrem\\n• Para guardar e enviar"),
    "fr": ("Fichier ≈ %1$s Mo",
           "• Sans perte : chaque pixel comme dans le résultat\\n• Le fichier le plus lourd\\n• Pour l’impression et la retouche",
           "• S’ouvre partout\\n• Bien plus léger que PNG\\n• Pour l’envoi et les réseaux sociaux",
           "• Le fichier le plus léger pour le même rendu\\n• Android, navigateurs et messageries l’ouvrent\\n• Pour garder et envoyer"),
    "de": ("Datei ≈ %1$s MB",
           "• Verlustfrei: jedes Pixel wie im Ergebnis\\n• Die größte Datei\\n• Für Druck und Weiterbearbeitung",
           "• Öffnet sich überall\\n• Deutlich kleiner als PNG\\n• Zum Verschicken und für soziale Netzwerke",
           "• Die kleinste Datei bei gleichem Aussehen\\n• Android, Browser und Messenger öffnen sie\\n• Zum Aufbewahren und Verschicken"),
    "zh": ("文件 ≈ %1$s MB",
           "• 无损：每个像素与结果一致\\n• 文件最大\\n• 适合打印和后期编辑",
           "• 到处都能打开\\n• 比 PNG 小得多\\n• 适合发送和社交媒体",
           "• 画质相同、文件最小\\n• Android、浏览器和聊天应用都能打开\\n• 适合保存和发送"),
    "hi": ("फ़ाइल ≈ %1$s MB",
           "• बिना नुकसान: हर पिक्सेल नतीजे जैसा\\n• सबसे बड़ी फ़ाइल\\n• प्रिंट और आगे की एडिटिंग के लिए",
           "• हर जगह खुलती है\\n• PNG से काफ़ी छोटी\\n• भेजने और सोशल मीडिया के लिए",
           "• वैसी ही दिखावट में सबसे छोटी फ़ाइल\\n• Android, ब्राउज़र और मैसेंजर खोलते हैं\\n• रखने और भेजने के लिए"),
    "bn": ("ফাইল ≈ %1$s MB",
           "• ক্ষতিহীন: প্রতিটি পিক্সেল ফলাফলের মতো\\n• সবচেয়ে বড় ফাইল\\n• প্রিন্ট ও পরবর্তী সম্পাদনার জন্য",
           "• সব জায়গায় খোলে\\n• PNG-এর চেয়ে অনেক ছোট\\n• পাঠানো ও সোশ্যাল মিডিয়ার জন্য",
           "• একই চেহারায় সবচেয়ে ছোট ফাইল\\n• Android, ব্রাউজার ও মেসেঞ্জার খোলে\\n• রাখা ও পাঠানোর জন্য"),
    "ar": ("الملف ≈ %1$s م.ب",
           "• بلا فقدان: كل بكسل كما في النتيجة\\n• أكبر ملف\\n• للطباعة ومتابعة التعديل",
           "• يفتح في كل مكان\\n• أصغر بكثير من PNG\\n• للإرسال ووسائل التواصل",
           "• أصغر ملف بالمظهر نفسه\\n• يفتحه Android والمتصفحات وتطبيقات المراسلة\\n• للحفظ والإرسال"),
}

# The processing panel B1 (the owner, chat 2026-10-15 ≈ 12:30 +03:00): metric_pass · metric_tile · metric_elapsed ·
# metric_left · duration_short_ms · duration_short_s · duration_short_m.
PANEL = {
    "es": ("Pasada", "Mosaico", "Transcurrido", "Quedan", "%1$d min %2$d s", "%1$d s", "%1$d min"),
    "pt": ("Passagem", "Bloco", "Decorrido", "Faltam", "%1$d min %2$d s", "%1$d s", "%1$d min"),
    "fr": ("Passe", "Tuile", "Écoulé", "Reste", "%1$d min %2$d s", "%1$d s", "%1$d min"),
    "de": ("Durchgang", "Kachel", "Vergangen", "Noch", "%1$d Min %2$d s", "%1$d s", "%1$d Min"),
    "zh": ("次", "块", "已用", "剩余", "%1$d 分 %2$d 秒", "%1$d 秒", "%1$d 分"),
    "hi": ("पास", "टाइल", "बीता", "बाक़ी", "%1$d मि %2$d से", "%1$d से", "%1$d मि"),
    "bn": ("পাস", "টাইল", "পেরিয়েছে", "বাকি", "%1$d মি %2$d সে", "%1$d সে", "%1$d মি"),
    "ar": ("المرور", "البلاطة", "مضى", "المتبقي", "%1$d د %2$d ث", "%1$d ث", "%1$d د"),
}

# The model catalogue and «All models» (plans/05 step 5), keyed by name - not positional (EXP-0011). `[AI]` drafts like
# the rest; `info_model_list` replaces the positional one of KEYS (DROP below), and `licence_bsd` gave way to
# `licence_commercial` with the licence as its argument. `working_tiles` / `working_starting` gave way to the metric tiles of
# panel B1 (PANEL below) - lint: unused.
# `model_fast_desc` / `model_max_desc` gave way to the per-model card lines of MODEL_TEXTS (2026-10-16).
DROP = {"licence_bsd", "info_model_list", "working_tiles", "working_starting", "model_fast_desc", "model_max_desc", "webp_too_big"}
CATALOGUE = {
    "es": {
        "about_previews": "Fotos de las vistas previas de modelos",
        "licence_commercial": "Uso comercial permitido · %1$s",
        "licence_noncommercial": "No apto para uso comercial",
        "models_title": "Modelos",
        "models_all": "Todos los modelos",
        "models_count": "Catálogo: %1$d · Descargados: %2$d",
        "models_choose": "Elegir",
        "filter_all": "Todos",
        "filter_photo": "Fotos",
        "filter_anime": "Anime",
        "filter_commercial": "Uso comercial",
        "model_size_mb": "%1$s MB",
        "model_progress_mb": "%1$s de %2$s MB",
        "model_download": "Descargar",
        "model_delete": "Eliminar",
        "model_builtin": "Incluido en la app",
        "model_author": "El modelo pertenece a su autor: %1$s",
        "model_download_failed": "No se pudo descargar el modelo. Comprueba la conexión e inténtalo de nuevo.",
        "model_damaged": "El archivo descargado estaba dañado y se ha eliminado. Inténtalo de nuevo.",
        "error_model_downloaded": "No se pudo cargar el modelo. Elimínalo en «Todos los modelos» y descárgalo de nuevo.",
        "info_model_list": "• Cada modelo del catálogo está entrenado para ampliar 4 veces: a 4× da el mejor resultado.\\n• Esta pantalla muestra los dos modelos que usaste por última vez.\\n• «Todos los modelos» lista cada modelo: para qué sirve, su autor, licencia y tamaño. Descarga los que necesites; funcionan sin red.",
    },
    "pt": {
        "about_previews": "Fotos das prévias dos modelos",
        "licence_commercial": "Uso comercial permitido · %1$s",
        "licence_noncommercial": "Não para uso comercial",
        "models_title": "Modelos",
        "models_all": "Todos os modelos",
        "models_count": "Catálogo: %1$d · Baixados: %2$d",
        "models_choose": "Escolher",
        "filter_all": "Todos",
        "filter_photo": "Fotos",
        "filter_anime": "Anime",
        "filter_commercial": "Uso comercial",
        "model_size_mb": "%1$s MB",
        "model_progress_mb": "%1$s de %2$s MB",
        "model_download": "Baixar",
        "model_delete": "Excluir",
        "model_builtin": "Incluído no app",
        "model_author": "O modelo pertence ao seu autor: %1$s",
        "model_download_failed": "Não foi possível baixar o modelo. Verifique a conexão e tente de novo.",
        "model_damaged": "O arquivo baixado estava corrompido e foi excluído. Tente de novo.",
        "error_model_downloaded": "Não foi possível carregar o modelo. Exclua-o em «Todos os modelos» e baixe de novo.",
        "info_model_list": "• Todo modelo do catálogo é treinado para ampliar 4 vezes: em 4× ele dá o melhor resultado.\\n• Esta tela mostra os dois modelos que você usou por último.\\n• «Todos os modelos» lista cada modelo: para que serve, autor, licença e tamanho. Baixe os que precisar; funcionam sem rede.",
    },
    "fr": {
        "about_previews": "Photos des aperçus des modèles",
        "licence_commercial": "Usage commercial autorisé · %1$s",
        "licence_noncommercial": "Pas d’usage commercial",
        "models_title": "Modèles",
        "models_all": "Tous les modèles",
        "models_count": "Catalogue : %1$d · Téléchargés : %2$d",
        "models_choose": "Choisir",
        "filter_all": "Tous",
        "filter_photo": "Photos",
        "filter_anime": "Anime",
        "filter_commercial": "Usage commercial",
        "model_size_mb": "%1$s Mo",
        "model_progress_mb": "%1$s sur %2$s Mo",
        "model_download": "Télécharger",
        "model_delete": "Supprimer",
        "model_builtin": "Intégré à l’appli",
        "model_author": "Le modèle appartient à son auteur : %1$s",
        "model_download_failed": "Impossible de télécharger le modèle. Vérifiez la connexion et réessayez.",
        "model_damaged": "Le fichier téléchargé était endommagé et a été supprimé. Réessayez.",
        "error_model_downloaded": "Impossible de charger le modèle. Supprimez-le dans « Tous les modèles » et téléchargez-le à nouveau.",
        "info_model_list": "• Chaque modèle du catalogue est entraîné pour agrandir 4 fois : à 4×, il donne le meilleur résultat.\\n• Cet écran montre les deux derniers modèles utilisés.\\n• « Tous les modèles » liste chaque modèle : à quoi il sert, son auteur, sa licence et sa taille. Téléchargez ceux qu’il vous faut ; ils marchent sans réseau.",
    },
    "de": {
        "about_previews": "Fotos der Modellvorschauen",
        "licence_commercial": "Kommerzielle Nutzung erlaubt · %1$s",
        "licence_noncommercial": "Nicht für kommerzielle Nutzung",
        "models_title": "Modelle",
        "models_all": "Alle Modelle",
        "models_count": "Katalog: %1$d · Heruntergeladen: %2$d",
        "models_choose": "Wählen",
        "filter_all": "Alle",
        "filter_photo": "Fotos",
        "filter_anime": "Anime",
        "filter_commercial": "Kommerzielle Nutzung",
        "model_size_mb": "%1$s MB",
        "model_progress_mb": "%1$s von %2$s MB",
        "model_download": "Herunterladen",
        "model_delete": "Löschen",
        "model_builtin": "In der App enthalten",
        "model_author": "Das Modell gehört seinem Autor: %1$s",
        "model_download_failed": "Das Modell konnte nicht heruntergeladen werden. Prüfe die Verbindung und versuche es erneut.",
        "model_damaged": "Die heruntergeladene Datei war beschädigt und wurde gelöscht. Versuche es erneut.",
        "error_model_downloaded": "Das Modell konnte nicht geladen werden. Lösche es unter „Alle Modelle“ und lade es erneut herunter.",
        "info_model_list": "• Jedes Modell im Katalog ist darauf trainiert, 4-fach zu vergrößern: Bei 4× liefert es das beste Ergebnis.\\n• Dieser Bildschirm zeigt die zwei zuletzt benutzten Modelle.\\n• „Alle Modelle“ listet jedes Modell: wofür es gut ist, Autor, Lizenz und Größe. Lade die benötigten herunter; sie arbeiten ohne Netz.",
    },
    "zh": {
        "about_previews": "模型预览中的照片",
        "licence_commercial": "允许商业使用 · %1$s",
        "licence_noncommercial": "不可用于商业用途",
        "models_title": "模型",
        "models_all": "所有模型",
        "models_count": "目录：%1$d · 已下载：%2$d",
        "models_choose": "选择",
        "filter_all": "全部",
        "filter_photo": "照片",
        "filter_anime": "动漫",
        "filter_commercial": "商业使用",
        "model_size_mb": "%1$s MB",
        "model_progress_mb": "%1$s / %2$s MB",
        "model_download": "下载",
        "model_delete": "删除",
        "model_builtin": "内置于应用",
        "model_author": "该模型归其作者所有：%1$s",
        "model_download_failed": "无法下载模型。请检查网络后重试。",
        "model_damaged": "下载的文件已损坏并已删除。请重试。",
        "error_model_downloaded": "无法加载模型。请在“所有模型”中删除后重新下载。",
        "info_model_list": "• 目录中的每个模型都经过训练，可放大 4 倍：在 4× 时效果最好。\\n• 此页面显示你最近使用的两个模型。\\n• “所有模型”列出每个模型：用途、作者、许可证和大小。下载你需要的模型，它们无需网络即可工作。",
    },
    "hi": {
        "about_previews": "मॉडल प्रीव्यू की फ़ोटो",
        "licence_commercial": "व्यावसायिक उपयोग की अनुमति · %1$s",
        "licence_noncommercial": "व्यावसायिक उपयोग के लिए नहीं",
        "models_title": "मॉडल",
        "models_all": "सभी मॉडल",
        "models_count": "कैटलॉग: %1$d · डाउनलोड किए गए: %2$d",
        "models_choose": "चुनें",
        "filter_all": "सभी",
        "filter_photo": "फ़ोटो",
        "filter_anime": "एनीमे",
        "filter_commercial": "व्यावसायिक उपयोग",
        "model_size_mb": "%1$s MB",
        "model_progress_mb": "%2$s में से %1$s MB",
        "model_download": "डाउनलोड",
        "model_delete": "हटाएँ",
        "model_builtin": "ऐप में शामिल",
        "model_author": "यह मॉडल अपने लेखक का है: %1$s",
        "model_download_failed": "मॉडल डाउनलोड नहीं हो सका। कनेक्शन जाँचें और फिर कोशिश करें।",
        "model_damaged": "डाउनलोड की गई फ़ाइल खराब थी और हटा दी गई। फिर कोशिश करें।",
        "error_model_downloaded": "मॉडल लोड नहीं हो सका। इसे «सभी मॉडल» में हटाएँ और फिर से डाउनलोड करें।",
        "info_model_list": "• कैटलॉग का हर मॉडल 4 गुना बड़ा करने के लिए प्रशिक्षित है: 4× पर यह सबसे अच्छा नतीजा देता है।\\n• इस स्क्रीन पर आपके पिछली बार इस्तेमाल किए गए दो मॉडल हैं।\\n• «सभी मॉडल» में हर मॉडल है: किस काम का है, लेखक, लाइसेंस और आकार। ज़रूरी मॉडल डाउनलोड करें; वे बिना नेटवर्क के चलते हैं।",
    },
    "bn": {
        "about_previews": "মডেল প্রিভিউয়ের ছবি",
        "licence_commercial": "বাণিজ্যিক ব্যবহার অনুমোদিত · %1$s",
        "licence_noncommercial": "বাণিজ্যিক ব্যবহারের জন্য নয়",
        "models_title": "মডেল",
        "models_all": "সব মডেল",
        "models_count": "ক্যাটালগ: %1$d · ডাউনলোড করা: %2$d",
        "models_choose": "বেছে নিন",
        "filter_all": "সব",
        "filter_photo": "ছবি",
        "filter_anime": "অ্যানিমে",
        "filter_commercial": "বাণিজ্যিক ব্যবহার",
        "model_size_mb": "%1$s MB",
        "model_progress_mb": "%2$s এর মধ্যে %1$s MB",
        "model_download": "ডাউনলোড",
        "model_delete": "মুছুন",
        "model_builtin": "অ্যাপে অন্তর্ভুক্ত",
        "model_author": "মডেলটি এর লেখকের: %1$s",
        "model_download_failed": "মডেল ডাউনলোড করা যায়নি। সংযোগ দেখে আবার চেষ্টা করুন।",
        "model_damaged": "ডাউনলোড করা ফাইলটি নষ্ট ছিল এবং মুছে ফেলা হয়েছে। আবার চেষ্টা করুন।",
        "error_model_downloaded": "মডেল লোড করা যায়নি। «সব মডেল»-এ এটি মুছে আবার ডাউনলোড করুন।",
        "info_model_list": "• ক্যাটালগের প্রতিটি মডেলকে 4 গুণ বড় করার জন্য প্রশিক্ষণ দেওয়া হয়েছে: 4×-এ এটি সবচেয়ে ভালো ফল দেয়।\\n• এই স্ক্রিনে আপনার শেষ ব্যবহার করা দুটি মডেল থাকে।\\n• «সব মডেল»-এ প্রতিটি মডেল আছে: কী কাজে লাগে, লেখক, লাইসেন্স ও আকার। দরকারি মডেল ডাউনলোড করুন; এগুলো নেটওয়ার্ক ছাড়াই চলে।",
    },
    "ar": {
        "about_previews": "صور معاينات النماذج",
        "licence_commercial": "مسموح بالاستخدام التجاري · %1$s",
        "licence_noncommercial": "ليس للاستخدام التجاري",
        "models_title": "النماذج",
        "models_all": "كل النماذج",
        "models_count": "الكتالوج: %1$d · تم التنزيل: %2$d",
        "models_choose": "اختيار",
        "filter_all": "الكل",
        "filter_photo": "صور",
        "filter_anime": "أنمي",
        "filter_commercial": "استخدام تجاري",
        "model_size_mb": "%1$s ميغابايت",
        "model_progress_mb": "%1$s من %2$s ميغابايت",
        "model_download": "تنزيل",
        "model_delete": "حذف",
        "model_builtin": "مضمّن في التطبيق",
        "model_author": "النموذج ملك لمؤلفه: %1$s",
        "model_download_failed": "تعذّر تنزيل النموذج. تحقّق من الاتصال وحاول مرة أخرى.",
        "model_damaged": "كان الملف الذي تم تنزيله تالفًا وتم حذفه. حاول مرة أخرى.",
        "error_model_downloaded": "تعذّر تحميل النموذج. احذفه من «كل النماذج» ثم نزّله من جديد.",
        "info_model_list": "• كل نموذج في الكتالوج مدرَّب على التكبير 4 مرات: يعطي أفضل نتيجة عند 4×.\\n• تعرض هذه الشاشة آخر نموذجين استخدمتهما.\\n• تسرد «كل النماذج» كل نموذج: الغرض منه ومؤلفه وترخيصه وحجمه. نزّل ما تحتاجه؛ تعمل النماذج دون شبكة.",
    },
}

# The agent door's notification (plans/07 step 6): «Задание агента · 42 %» while an AI agent's job runs. [AI] draft, like the rest.
AGENT = {"es": "Tarea del agente · %1$d%%", "pt": "Tarefa do agente · %1$d%%", "fr": "Tâche de l’agent · %1$d%%",
         "de": "Agent-Auftrag · %1$d%%", "zh": "代理任务 · %1$d%%", "hi": "एजेंट का काम · %1$d%%",
         "bn": "এজেন্টের কাজ · %1$d%%", "ar": "مهمة الوكيل · %1$d%%"}

# The Working screen while an API job holds the engine (variant C, the owner 2026-10-16: «нужно писать, что KARTUN занят
# заданием по API»): waiting_title, waiting_text, metric_waiting, metric_work, stop_api_job. [AI] draft, like the rest.
WAITING_KEYS = ["waiting_title", "waiting_text", "metric_waiting", "metric_work", "stop_api_job"]
WAITING = {
    "es": ["En cola", "KARTUN está ocupado con una tarea de la API. Tu foto es la siguiente.", "Espera", "Trabajo", "Detener la tarea de la API"],
    "pt": ["Na fila", "O KARTUN está ocupado com uma tarefa da API. Sua foto é a próxima.", "Espera", "Trabalho", "Parar a tarefa da API"],
    "fr": ["En file d’attente", "KARTUN traite une tâche reçue par l’API. Votre photo est la suivante.", "Attente", "Travail", "Arrêter la tâche de l’API"],
    "de": ["Wartet", "KARTUN bearbeitet gerade einen Auftrag über die API. Ihr Foto ist als Nächstes dran.", "Wartet", "Arbeit", "API-Auftrag stoppen"],
    "zh": ["排队中", "KARTUN 正在处理一个 API 任务。您的照片排在下一个。", "等待", "处理", "停止 API 任务"],
    "hi": ["कतार में", "KARTUN अभी API से मिले काम में व्यस्त है। आपकी फ़ोटो अगली है।", "इंतज़ार", "काम", "API का काम रोकें"],
    "bn": ["সারিতে", "KARTUN এখন API-র একটি কাজে ব্যস্ত। আপনার ছবি পরেরটি।", "অপেক্ষা", "কাজ", "API-র কাজ থামান"],
    "ar": ["في الانتظار", "KARTUN مشغول بمهمة عبر الـ API. صورتك هي التالية.", "انتظار", "العمل", "إيقاف مهمة الـ API"],
}

# The save dialog's format size limit (the owner: warn about the limit and offer to reduce) - (save_too_big, save_reduced);
# they replace webp_too_big (DROP above). %1$s the format name, %2$s the limit, %3$s × %4$s the new size. [AI] draft, like the rest.
SAVE_LIMIT = {
    "es": ("%1$s guarda como máximo %2$s px por lado. La imagen se reducirá a %3$s × %4$s.", "Reducir y guardar"),
    "pt": ("%1$s armazena no máximo %2$s px por lado. A imagem será reduzida para %3$s × %4$s.", "Reduzir e salvar"),
    "fr": ("Le %1$s accepte au plus %2$s px par côté. L’image sera réduite à %3$s × %4$s.", "Réduire et enregistrer"),
    "de": ("%1$s speichert höchstens %2$s px pro Seite. Das Bild wird auf %3$s × %4$s verkleinert.", "Verkleinern und speichern"),
    "zh": ("%1$s 每边最多 %2$s 像素。图片将缩小到 %3$s × %4$s。", "缩小并保存"),
    "hi": ("%1$s में हर तरफ़ ज़्यादा से ज़्यादा %2$s px आते हैं। तस्वीर %3$s × %4$s तक छोटी की जाएगी।", "छोटा करें और सहेजें"),
    "bn": ("%1$s প্রতি পাশে সর্বোচ্চ %2$s px রাখে। ছবিটি %3$s × %4$s-এ ছোট করা হবে।", "ছোট করে সংরক্ষণ"),
    "ar": ("تحفظ صيغة %1$s حتى %2$s بكسل لكل جانب كحد أقصى. ستُصغَّر الصورة إلى %3$s × %4$s.", "تصغير وحفظ"),
}

# Why a photo did not open (the owner 2026-10-16 ≈ 16:26: «можем в ошибке писать причину понятную?») -
# (error_not_photo, error_incomplete, error_no_access, error_memory). [AI] draft, like the rest.
OPEN_ERRORS = {
    "es": ("KARTUN no encontró una foto en este archivo: el formato no es compatible o el archivo está dañado.",
           "El archivo de la foto está incompleto: parece que no se descargó del todo.",
           "Sin acceso al archivo. Si la foto está en la nube, descárgala primero al teléfono.",
           "No hay memoria suficiente en el dispositivo para abrir esta foto. Cierra otras apps e inténtalo de nuevo."),
    "pt": ("O KARTUN não encontrou uma foto neste arquivo: o formato não é compatível ou o arquivo está danificado.",
           "O arquivo da foto está incompleto: parece que não foi baixado por inteiro.",
           "Sem acesso ao arquivo. Se a foto está na nuvem, baixe-a primeiro para o telefone.",
           "Não há memória suficiente no dispositivo para abrir esta foto. Feche outros apps e tente novamente."),
    "fr": ("KARTUN n’a trouvé aucune photo dans ce fichier : le format n’est pas pris en charge ou le fichier est endommagé.",
           "Le fichier de la photo est incomplet : il semble ne pas avoir été entièrement téléchargé.",
           "Pas d’accès au fichier. Si la photo est dans le cloud, téléchargez-la d’abord sur le téléphone.",
           "Mémoire insuffisante sur l’appareil pour ouvrir cette photo. Fermez d’autres applis et réessayez."),
    "de": ("KARTUN hat in dieser Datei kein Foto gefunden: Das Format wird nicht unterstützt oder die Datei ist beschädigt.",
           "Die Fotodatei ist unvollständig – offenbar wurde sie nicht ganz heruntergeladen.",
           "Kein Zugriff auf die Datei. Liegt das Foto in der Cloud, lade es zuerst auf das Telefon herunter.",
           "Nicht genug Gerätespeicher, um dieses Foto zu öffnen. Schließe andere Apps und versuche es erneut."),
    "zh": ("KARTUN 在此文件中未找到照片：格式不受支持或文件已损坏。",
           "照片文件不完整，似乎没有下载完。",
           "无法访问该文件。如果照片存放在云端，请先将其下载到手机。",
           "设备内存不足，无法打开这张照片。请关闭其他应用后重试。"),
    "hi": ("KARTUN को इस फ़ाइल में फ़ोटो नहीं मिली: फ़ॉर्मैट समर्थित नहीं है या फ़ाइल ख़राब है।",
           "फ़ोटो की फ़ाइल अधूरी है — लगता है वह पूरी डाउनलोड नहीं हुई।",
           "फ़ाइल तक पहुँच नहीं है। अगर फ़ोटो क्लाउड में है, तो पहले उसे फ़ोन पर डाउनलोड करें।",
           "इस फ़ोटो को खोलने के लिए डिवाइस में पर्याप्त मेमोरी नहीं है। दूसरे ऐप बंद करें और फिर से कोशिश करें।"),
    "bn": ("এই ফাইলে KARTUN কোনো ছবি পায়নি: ফরম্যাটটি সমর্থিত নয় বা ফাইলটি নষ্ট।",
           "ছবির ফাইলটি অসম্পূর্ণ — মনে হচ্ছে এটি পুরোপুরি ডাউনলোড হয়নি।",
           "ফাইলটিতে প্রবেশাধিকার নেই। ছবিটি ক্লাউডে থাকলে আগে সেটি ফোনে ডাউনলোড করুন।",
           "এই ছবি খোলার মতো যথেষ্ট মেমরি ডিভাইসে নেই। অন্য অ্যাপ বন্ধ করে আবার চেষ্টা করুন।"),
    "ar": ("لم يجد KARTUN صورة في هذا الملف: الصيغة غير مدعومة أو الملف تالف.",
           "ملف الصورة غير مكتمل، ويبدو أنه لم يُنزَّل بالكامل.",
           "لا يمكن الوصول إلى الملف. إذا كانت الصورة محفوظة في السحابة، فنزّلها إلى الهاتف أولًا.",
           "لا تكفي ذاكرة الجهاز لفتح هذه الصورة. أغلق التطبيقات الأخرى وحاول مرة أخرى."),
}

# Over the output limit the slider's last step runs at the largest factor that fits (plans/08 step 9; the owner
# 2026-10-16 ≈ 16:10 «да, правильно») - scale_limited: %1$s the limit in MP, %2$d the step, %3$s the factor, %4$s its size.
SCALE_LIMITED = {
    "es": "Límite: %1$s MP. %2$d× no cabe para esta foto; cabe %3$s× (%4$s)",
    "pt": "Limite: %1$s MP. %2$d× não cabe para esta foto; cabe %3$s× (%4$s)",
    "fr": "Limite : %1$s Mpx. %2$d× ne tient pas pour cette photo ; %3$s× tient (%4$s)",
    "de": "Grenze: %1$s MP. %2$d× passt für dieses Foto nicht; %3$s× passt (%4$s)",
    "zh": "上限 %1$s 百万像素。此照片放不下 %2$d×，可以 %3$s×（%4$s）",
    "hi": "सीमा — %1$s MP। इस फ़ोटो के लिए %2$d× नहीं समाएगा; %3$s× हो सकता है (%4$s)",
    "bn": "সীমা — %1$s MP। এই ছবির জন্য %2$d× আঁটবে না; %3$s× সম্ভব (%4$s)",
    "ar": "الحد %1$s ميغابكسل. لا يتسع %2$d× لهذه الصورة؛ يمكن %3$s× (%4$s)",
}

# The round back button's name for TalkBack and test automation (ui/Common.kt RoundIconButton). [AI] like the rest.
ACTION_BACK = {"es": "Atrás", "pt": "Voltar", "fr": "Retour", "de": "Zurück", "zh": "返回", "hi": "वापस", "bn": "ফিরে যান",
               "ar": "رجوع"}
# TalkBack names on the result screen (compare_handle, panel_show). [AI] like the rest.
A11Y = {"es": ("Divisor de antes y después", "Mostrar el resumen"), "pt": ("Divisor de antes e depois", "Mostrar o resumo"),
        "fr": ("Séparateur avant / après", "Afficher le résumé"), "de": ("Trennlinie vorher / nachher", "Zusammenfassung zeigen"),
        "zh": ("前后对比分隔线", "显示摘要"), "hi": ("पहले और बाद की विभाजक रेखा", "सारांश दिखाएँ"),
        "bn": ("আগে ও পরের বিভাজক", "সারসংক্ষেপ দেখান"), "ar": ("فاصل قبل وبعد", "إظهار الملخص")}
# The (i) button's name for TalkBack, %1$s = what it explains (ui/Common.kt InfoButton). [AI] like the rest.
INFO_ABOUT = {"es": "Más sobre: %1$s", "pt": "Mais sobre: %1$s", "fr": "En savoir plus : %1$s", "de": "Mehr über: %1$s",
              "zh": "详细了解：%1$s", "hi": "और जानें: %1$s", "bn": "আরও জানুন: %1$s", "ar": "المزيد عن: %1$s"}

# The model card lines (model_purpose_*) and the (i) texts (model_info_*) of every catalogue model, one data file for the 8
# languages (tools/model-texts.json, `[AI]` like the rest; sources researches/11 and 12). Its keys must be exactly the
# model_purpose_* / model_info_* names of values/strings.xml - a key missing on either side reddens here.
import json as _json
import re as _re
MODEL_TEXTS = _json.loads((ROOT / "tools" / "model-texts.json").read_text(encoding="utf-8"))
MODEL_TEXTS.pop("_comment", None)
_EN_MODEL_KEYS = _re.findall(r'<string name="(model_(?:purpose|info)_\w+)"',
                             (ROOT / "app" / "src" / "main" / "res" / "values" / "strings.xml").read_text(encoding="utf-8"))

# Arabic reads right to left: the flow arrow and the «from → to» line point the other way.
EXTRA = {"ar": {"arrow": "←", "size_to": "%1$s ← %2$s"}}


def xml_text(s: str) -> str:
    """Escape for an Android string resource: XML entities, then the apostrophe and the double quote."""
    return escape(s).replace("'", "\\'").replace('"', '\\"')


def main():
    # --check: compare instead of writing (exit 1 when a generated file was edited by hand).
    check = "--check" in sys.argv
    drift = 0
    for lang, values in T.items():
        assert len(values) == len(KEYS), (lang, len(values), len(KEYS))
        lines = [f'    <string name="{k}">{xml_text(v).replace(chr(92) + chr(92) + "n", chr(92) + "n")}</string>' for k, v in zip(KEYS, values) if k not in DROP]
        for name, idx in (("duration_seconds", 0), ("duration_minutes", 1)):
            items = "\n".join(f'        <item quantity="{q}">{xml_text(forms[idx])}</item>' for q, forms in P[lang].items())
            lines.append(f'    <plurals name="{name}">\n{items}\n    </plurals>')
        for k, v in zip(("link_title", "link_failed", "link_downloading", "link_download"), LINK[lang]):
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        assert len(SETTINGS[lang]) == len(SETTINGS_KEYS), (lang, "settings")
        for k, v in zip(SETTINGS_KEYS, SETTINGS[lang]):
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        for k, v in zip(("manual_title", "settings_manual", "settings_manual_sub"), GUIDE[lang]):
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        lines.append(f'    <string name="info_open_guide">{xml_text(OPEN_GUIDE[lang])}</string>')
        lines.append(f'    <string name="bench_title">{xml_text(BENCH[lang][0])}</string>')
        lines.append(f'    <string name="bench_cancel">{xml_text(BENCH[lang][1])}</string>')
        lines.append(f'    <string name="model_choice">{xml_text(CHOICE[lang])}</string>')
        assert len(NOTIF[lang]) == len(NOTIF_KEYS), (lang, "notif")
        for k, v in zip(NOTIF_KEYS, NOTIF[lang]):
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        for k, v in zip(("notif_done", "notif_done_text"), DONE[lang]):
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        assert len(ABOUT[lang]) == len(ABOUT_KEYS), (lang, "about")
        for k, v in zip(ABOUT_KEYS, ABOUT[lang]):
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        lines.append(f'    <string name="rendering">{xml_text(RENDERING[lang])}</string>')
        for k, v in zip(("metric_pass", "metric_tile", "metric_elapsed", "metric_left", "duration_short_ms", "duration_short_s", "duration_short_m"), PANEL[lang]):
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        for k, v in zip(("save_estimate", "format_points_png", "format_points_jpeg", "format_points_webp"), SAVE_INFO[lang]):
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        assert set(CATALOGUE[lang]) == set(CATALOGUE["es"]), (lang, "catalogue keys")
        for k, v in CATALOGUE[lang].items():
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        assert list(MODEL_TEXTS[lang]) == _EN_MODEL_KEYS, (lang, "model texts: keys differ from values/strings.xml")
        for k, v in MODEL_TEXTS[lang].items():
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        lines.append(f'    <string name="agent_job_progress">{xml_text(AGENT[lang])}</string>')
        assert len(WAITING[lang]) == len(WAITING_KEYS), (lang, "waiting")
        for k, v in zip(WAITING_KEYS, WAITING[lang]):
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        for k, v in zip(("save_too_big", "save_reduced"), SAVE_LIMIT[lang]):
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        for k, v in zip(("error_not_photo", "error_incomplete", "error_no_access", "error_memory"), OPEN_ERRORS[lang]):
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        lines.append(f'    <string name="scale_limited">{xml_text(SCALE_LIMITED[lang])}</string>')
        lines.append(f'    <string name="action_back">{xml_text(ACTION_BACK[lang])}</string>')
        lines.append(f'    <string name="info_about">{xml_text(INFO_ABOUT[lang])}</string>')
        for k, v in zip(("compare_handle", "panel_show"), A11Y[lang]):
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        for k, v in EXTRA.get(lang, {}).items():
            lines.append(f'    <string name="{k}">{xml_text(v)}</string>')
        body = "\n".join(lines)
        out = ROOT / "app" / "src" / "main" / "res" / f"values-{lang}" / "strings.xml"
        content = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by tools/make-translations.py - edit the script, not this file. The agent's translation of
     values/strings.xml into "{lang}" (plans/01 §4.7), accepted by the owner 2026-10-15 («я проверил, все ок»). -->
<resources>
{body}
</resources>
"""
        if check:
            # The pair «this script ↔ values-<lang>/strings.xml»: a hand edit of a generated file reddens here.
            if not out.exists() or out.read_text(encoding="utf-8") != content:
                print(f"DRIFT values-{lang}/strings.xml differs from the script")
                drift += 1
            continue
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(content, encoding="utf-8", newline="\n")
        print(f"written values-{lang}/strings.xml ({len(KEYS)} strings)")
    if drift:
        sys.exit(1)
    check_locale_config()


def check_locale_config():
    """The pair «res/values-* folders ↔ res/xml/locales_config.xml»: the system's language list must name every shipped
    language and nothing else (EN is the unqualified values/). Exits 1 on drift."""
    import re
    import sys
    res = ROOT / "app" / "src" / "main" / "res"
    shipped = {"en"} | {d.name[len("values-"):] for d in res.glob("values-*") if (d / "strings.xml").exists()}
    listed = set(re.findall(r'<locale android:name="([^"]+)"', (res / "xml" / "locales_config.xml").read_text(encoding="utf-8")))
    if shipped != listed:
        print(f"DRIFT locales_config.xml: missing {sorted(shipped - listed)}, extra {sorted(listed - shipped)}")
        sys.exit(1)
    print(f"locales_config.xml matches the {len(shipped)} shipped languages")


if __name__ == "__main__":
    main()
