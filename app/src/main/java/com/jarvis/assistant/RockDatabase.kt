package com.jarvis.assistant

/**
 * RockDatabase — قاعدة بيانات صخور ومعادن أوفلاين
 * 40 صخر/معدن شائع مع بيانات التعريف الميداني
 * لا إنترنت — لا SQLite — مضمّنة مباشرة في الكود
 * المسار: app/src/main/java/com/jarvis/assistant/RockDatabase.kt
 */

enum class RockCategory { MINERAL, IGNEOUS, SEDIMENTARY, METAMORPHIC }

data class RockRecord(
    val nameAr: String,          // الاسم بالعربية
    val nameEn: String,          // الاسم بالإنجليزية
    val aliases: List<String>,   // أسماء بديلة / شائعة
    val category: RockCategory,
    val hardness: String,        // صلابة موس أو نطاق
    val colors: List<String>,    // الألوان الشائعة
    val luster: String,          // البريق
    val keyFeature: String,      // الخاصية المميزة الأسرع في الميدان
    val formation: String,       // بيئة التكوين
    val fieldId: String,         // كيف تعرفه في الميدان (جملة واحدة)
    val uses: String             // الاستخدامات
)

object RockDatabase {

    val records: List<RockRecord> = listOf(

        // ══════════════════ معادن ══════════════════════════════════

        RockRecord(
            nameAr = "كوارتز", nameEn = "Quartz",
            aliases = listOf("مرو", "صوان", "quartz"),
            category = RockCategory.MINERAL,
            hardness = "7",
            colors = listOf("شفاف", "أبيض", "وردي", "بنفسجي", "أسود دخاني"),
            luster = "زجاجي",
            keyFeature = "صلابة عالية جداً — يخدش الزجاج بسهولة",
            formation = "صخور نارية وتحولية ورسوبية، واسع الانتشار",
            fieldId = "اكسر عينة صغيرة: الكسر مقعّر (محاري)، لا انفصام واضح، بريق زجاجي",
            uses = "صناعة الزجاج، الإلكترونيات، المجوهرات"
        ),

        RockRecord(
            nameAr = "كالسيت", nameEn = "Calcite",
            aliases = listOf("كربونات الكالسيوم", "calcite"),
            category = RockCategory.MINERAL,
            hardness = "3",
            colors = listOf("أبيض", "عديم اللون", "رمادي", "وردي فاتح"),
            luster = "زجاجي إلى لؤلؤي",
            keyFeature = "يُفور مع حمض الهيدروكلوريك المخفف فوراً",
            formation = "الصخر الكلسي، العروق الهيدروثيرمية، قشرة البحر",
            fieldId = "ضع قطرة HCl: فوران فوري. انفصام مثالي ثلاثي الاتجاه (معيّن)",
            uses = "صناعة الأسمنت، الجير، البناء"
        ),

        RockRecord(
            nameAr = "دولوميت (معدن)", nameEn = "Dolomite mineral",
            aliases = listOf("dolomite"),
            category = RockCategory.MINERAL,
            hardness = "3.5-4",
            colors = listOf("أبيض", "رمادي فاتح", "وردي"),
            luster = "زجاجي",
            keyFeature = "يُفور مع HCl فقط إذا كان مسحوقاً (عكس الكالسيت)",
            formation = "الصخور الكربوناتية، العروق الهيدروثيرمية",
            fieldId = "اسحق قليلاً ثم ضع HCl — الفوران الضعيف يميّزه عن الكالسيت",
            uses = "الصناعة، مواد بناء، المجوهرات"
        ),

        RockRecord(
            nameAr = "فلسبار (أورثوكلاز)", nameEn = "Orthoclase Feldspar",
            aliases = listOf("فلسبار بوتاسي", "orthoclase", "feldspar"),
            category = RockCategory.MINERAL,
            hardness = "6",
            colors = listOf("وردي", "أبيض", "رمادي"),
            luster = "زجاجي إلى صدفي",
            keyFeature = "انفصام متعامد (90°) في اتجاهين واضحين",
            formation = "الصخور النارية الحامضية (جرانيت)، الصخور التحولية",
            fieldId = "وجوه مستوية براقة بزاوية 90°، صلابة تكسر الزجاج",
            uses = "صناعة الخزف والبورسلين"
        ),

        RockRecord(
            nameAr = "ميكا (مسكوفيت)", nameEn = "Muscovite Mica",
            aliases = listOf("ميكا بيضاء", "muscovite"),
            category = RockCategory.MINERAL,
            hardness = "2-2.5",
            colors = listOf("فضي", "شفاف", "أبيض"),
            luster = "لؤلؤي لامع جداً",
            keyFeature = "ورقي الشكل — ينقسم إلى صفائح رفيعة مرنة",
            formation = "الجرانيت، الشيست، البجماتيت",
            fieldId = "قشور رفيعة لامعة كالفضة تنثلم بالظفر",
            uses = "صناعة العازل الكهربائي، مستحضرات التجميل"
        ),

        RockRecord(
            nameAr = "بيوتيت (ميكا سوداء)", nameEn = "Biotite Mica",
            aliases = listOf("ميكا سوداء", "biotite"),
            category = RockCategory.MINERAL,
            hardness = "2.5-3",
            colors = listOf("أسود", "بني داكن"),
            luster = "لؤلؤي لامع",
            keyFeature = "ورقي أسود — صفائح مرنة داكنة",
            formation = "الجرانيت، الجنيس، الشيست",
            fieldId = "قشور رفيعة براقة سوداء أو بنية داكنة، مرنة",
            uses = "صناعة، مادة عازلة"
        ),

        RockRecord(
            nameAr = "بيريت", nameEn = "Pyrite",
            aliases = listOf("ذهب الأحمق", "كبريتيد الحديد", "pyrite", "fool's gold"),
            category = RockCategory.MINERAL,
            hardness = "6-6.5",
            colors = listOf("ذهبي فاتح"),
            luster = "معدني قوي",
            keyFeature = "وجوه مكعبة مثالية، أثقل من الذهب الحقيقي، خط أسود",
            formation = "الصخور الرسوبية، الرواسب الهيدروثيرمية",
            fieldId = "مكعبات ذهبية لامعة + خط أسود على الخزف = ليس ذهباً",
            uses = "تصنيع حمض الكبريتيك، تعدين النحاس"
        ),

        RockRecord(
            nameAr = "هيماتيت", nameEn = "Hematite",
            aliases = listOf("أكسيد الحديد", "حجر الدم", "hematite"),
            category = RockCategory.MINERAL,
            hardness = "5-6",
            colors = listOf("رمادي فولاذي", "أسود", "بني أحمر"),
            luster = "معدني إلى ترابي",
            keyFeature = "خط أحمر-بني مميز على الخزف",
            formation = "الصخور الرسوبية البحرية، الرواسب الهيدروثيرمية",
            fieldId = "اخدش على الخزف — خط بني أحمر مهما كان لون العينة",
            uses = "خام حديد رئيسي، أصباغ"
        ),

        RockRecord(
            nameAr = "ماغنيتيت", nameEn = "Magnetite",
            aliases = listOf("الحجر المغناطيسي", "magnetite"),
            category = RockCategory.MINERAL,
            hardness = "5.5-6.5",
            colors = listOf("أسود"),
            luster = "معدني",
            keyFeature = "يُشدّ بالمغناطيس — الميزة الوحيدة في الميدان",
            formation = "الصخور النارية القاعدية، الرواسب المتحوّلة",
            fieldId = "قرّب مغناطيساً — إن التصق فهو ماغنيتيت. خط أسود",
            uses = "خام حديد، تصنيع الفيريت، الأسمنت"
        ),

        RockRecord(
            nameAr = "جبس", nameEn = "Gypsum",
            aliases = listOf("كبريتات الكالسيوم", "gypsum", "سيلينايت"),
            category = RockCategory.MINERAL,
            hardness = "2",
            colors = listOf("أبيض", "شفاف", "رمادي"),
            luster = "زجاجي إلى حريري",
            keyFeature = "يُخدش بالظفر (صلابة 2) — أقل الصلابة بين الشائعة",
            formation = "تبخر البحيرات والبحار الضحلة، المناطق الجافة",
            fieldId = "حاول خدشه بظفرك — إن نجح فهو جبس أو أكثر ليونة",
            uses = "الجبسة البلاستر، الأسمنت، الزراعة"
        ),

        RockRecord(
            nameAr = "هاليت (ملح صخري)", nameEn = "Halite",
            aliases = listOf("ملح الطعام", "halite", "ملح الصخر"),
            category = RockCategory.MINERAL,
            hardness = "2-2.5",
            colors = listOf("شفاف", "أبيض", "وردي", "برتقالي"),
            luster = "زجاجي",
            keyFeature = "مذاق مالح — الطريقة الأسرع والأوثق في الميدان",
            formation = "تبخر البحار والبحيرات المالحة، المناطق الجافة",
            fieldId = "ذوق قطعة صغيرة — ملوحة فورية. انفصام مكعبي مثالي",
            uses = "ملح الطعام، صناعة الكلور والصودا"
        ),

        RockRecord(
            nameAr = "زبرجد (أوليفين)", nameEn = "Olivine",
            aliases = listOf("olivine", "peridot", "زيتوني"),
            category = RockCategory.MINERAL,
            hardness = "6.5-7",
            colors = listOf("أخضر زيتوني", "أصفر-أخضر"),
            luster = "زجاجي",
            keyFeature = "لون أخضر زيتوني مميز + موجود في الصخور القاعدية",
            formation = "البازلت، الجابرو، البريدوتيت (وشاح الأرض)",
            fieldId = "حبيبات خضراء زيتونية داخل صخرة داكنة",
            uses = "مجوهرات (بيريدوت)، دراسة باطن الأرض"
        ),

        // ══════════════════ صخور نارية ══════════════════════════════

        RockRecord(
            nameAr = "جرانيت", nameEn = "Granite",
            aliases = listOf("granite"),
            category = RockCategory.IGNEOUS,
            hardness = "6-7",
            colors = listOf("وردي", "رمادي", "أبيض مع بقع"),
            luster = "معدني متنوع (حبيبات مختلفة)",
            keyFeature = "نسيج حبيبي خشن — ترى البلورات بالعين المجردة",
            formation = "تصلّد بطيء في الأعماق (نارية اقتحامية)",
            fieldId = "ثلاثة معادن مرئية: كوارتز (رمادي) + فلسبار (وردي/أبيض) + ميكا (أسود/فضي)",
            uses = "بناء، رصف، تزيين"
        ),

        RockRecord(
            nameAr = "بازلت", nameEn = "Basalt",
            aliases = listOf("basalt", "حجر بركاني أسود"),
            category = RockCategory.IGNEOUS,
            hardness = "5-6",
            colors = listOf("أسود", "رمادي داكن"),
            luster = "ناعم إلى خشن دقيق الحبيبات",
            keyFeature = "أسود كثيف دقيق النسيج، قد يحوي فجوات (كوّات)",
            formation = "تصلّد سريع على السطح (بركاني انسيابي)",
            fieldId = "صخرة سوداء/رمادية داكنة ثقيلة، حبيبات لا تُرى بالعين، غالباً مسامية",
            uses = "بناء طرق، عزل حراري، أصناف أثرية"
        ),

        RockRecord(
            nameAr = "أبسيديان", nameEn = "Obsidian",
            aliases = listOf("obsidian", "الزجاج البركاني", "حجر البركان"),
            category = RockCategory.IGNEOUS,
            hardness = "5-5.5",
            colors = listOf("أسود لامع", "بني داكن", "مخطط أحياناً"),
            luster = "زجاجي مثالي",
            keyFeature = "زجاجي 100% — بلا بلورات، بريق مرآة",
            formation = "تبريد فوري للحمم عالية السيليكا",
            fieldId = "حواف حادة كالزجاج، كسر محاري، أسود لامع",
            uses = "أدوات تاريخية، جراحة دقيقة قديمة"
        ),

        RockRecord(
            nameAr = "ريوليت", nameEn = "Rhyolite",
            aliases = listOf("rhyolite"),
            category = RockCategory.IGNEOUS,
            hardness = "6-7",
            colors = listOf("وردي", "رمادي فاتح", "أبيض مصفر"),
            luster = "دقيق الحبيبات أو زجاجي جزئياً",
            keyFeature = "بلورات كبيرة معزولة داخل عجينة دقيقة (نسيج بورفيري)",
            formation = "بركاني حامضي (مقابل جرانيت على السطح)",
            fieldId = "فاتح اللون (وردي/رمادي)، أحياناً بلورات كبيرة في مصفوفة ناعمة",
            uses = "دراسة البراكين، بناء أحياناً"
        ),

        RockRecord(
            nameAr = "غابرو", nameEn = "Gabbro",
            aliases = listOf("gabbro"),
            category = RockCategory.IGNEOUS,
            hardness = "6-7",
            colors = listOf("أسود", "رمادي داكن"),
            luster = "حبيبي خشن، بلورات داكنة",
            keyFeature = "مثل الجرانيت لكن أسود — حبيبي خشن قاعدي",
            formation = "تصلّد بطيء في الأعماق (اقتحامي قاعدي)",
            fieldId = "صخرة داكنة حبيبية خشنة — حبيبات زبرجد + فلسبار أبيض",
            uses = "قواعد البناء، دراسة قشرة المحيطات"
        ),

        RockRecord(
            nameAr = "بيميس (خفاف)", nameEn = "Pumice",
            aliases = listOf("pumice", "حجر الخفاف", "حجر الخفف"),
            category = RockCategory.IGNEOUS,
            hardness = "6",
            colors = listOf("أبيض", "رمادي فاتح", "بيج"),
            luster = "مطفأ، مسامي جداً",
            keyFeature = "أخف من الماء — يطفو على سطحه",
            formation = "انفجارات بركانية (صخرة بركانية رغوية)",
            fieldId = "صخرة بيضاء خفيفة جداً ذات ثقوب، تطفو على الماء",
            uses = "مواد التجميل، الصنفرة، صناعة الخرسانة الخفيفة"
        ),

        RockRecord(
            nameAr = "ديوريت", nameEn = "Diorite",
            aliases = listOf("diorite"),
            category = RockCategory.IGNEOUS,
            hardness = "6-7",
            colors = listOf("رمادي مرقط", "أسود وأبيض"),
            luster = "حبيبي متوسط",
            keyFeature = "مرقط أبيض وأسود كالبقرة (بلاجيوكلاز + هورنبليند)",
            formation = "اقتحامي، مرحلة وسطى بين جرانيت وغابرو",
            fieldId = "رمادي مرقط بشكل واضح، حبيبات متوسطة، لا كوارتز كثير",
            uses = "نادر الاستخدام التجاري، زخارف"
        ),

        // ══════════════════ صخور رسوبية ════════════════════════════

        RockRecord(
            nameAr = "حجر الكلس (الحجر الجيري)", nameEn = "Limestone",
            aliases = listOf("كلسي", "limestone", "كربونات"),
            category = RockCategory.SEDIMENTARY,
            hardness = "3-4",
            colors = listOf("أبيض", "رمادي", "بيج", "أسود"),
            luster = "مطفأ، أحياناً بلوري",
            keyFeature = "يُفور مع HCl بقوة — الاختبار الأسرع في الميدان",
            formation = "أعماق البحار الدافئة الضحلة، رواسب كائنات بحرية",
            fieldId = "ضع قطرة HCl — فوران قوي فوري. قد تحوي أصداف أو حفريات",
            uses = "الأسمنت، الجير، البناء"
        ),

        RockRecord(
            nameAr = "الحجر الرملي", nameEn = "Sandstone",
            aliases = listOf("رملي", "sandstone"),
            category = RockCategory.SEDIMENTARY,
            hardness = "يتراوح 3-7 حسب الأسمنت",
            colors = listOf("أصفر", "أحمر", "بيج", "رمادي", "أبيض"),
            luster = "خشن الملمس (رملي)",
            keyFeature = "خشونة رملية واضحة باللمس والنظر",
            formation = "شواطئ، صحاري، أنهار قديمة",
            fieldId = "ملمس خشن كالورق الزجاجي، حبيبات رمل مرئية، غالباً ذو طبقات",
            uses = "بناء، حجارة الرحى، خزانات النفط"
        ),

        RockRecord(
            nameAr = "الشيل", nameEn = "Shale",
            aliases = listOf("صفحي", "طيني", "shale"),
            category = RockCategory.SEDIMENTARY,
            hardness = "1-2",
            colors = listOf("رمادي", "أسود", "بني", "أخضر"),
            luster = "مطفأ، أحياناً لامع",
            keyFeature = "يتشقق إلى صفائح رقيقة، ناعم جداً",
            formation = "قيعان البحيرات والبحار الهادئة، رواسب غرينية",
            fieldId = "يتقشر إلى طبقات رفيعة جداً، ناعم يُخدش بالظفر، رائحة طينية عند البلل",
            uses = "تصنيع الطوب، الأسمنت، خزانات النفط"
        ),

        RockRecord(
            nameAr = "الكونغلومرا", nameEn = "Conglomerate",
            aliases = listOf("حجر حوة", "conglomerate"),
            category = RockCategory.SEDIMENTARY,
            hardness = "متغير",
            colors = listOf("متنوع — حسب الحصى"),
            luster = "خشن مرئي",
            keyFeature = "حصى مستديرة مسمنتة داخل مصفوفة ناعمة",
            formation = "مجاري أنهار، شواطئ عالية الطاقة",
            fieldId = "حصى مستديرة (>2mm) ترى بالعين مضمّنة في أسمنت طيني أو رملي",
            uses = "دراسة البيئات القديمة، بناء أحياناً"
        ),

        RockRecord(
            nameAr = "الطباشير", nameEn = "Chalk",
            aliases = listOf("chalk"),
            category = RockCategory.SEDIMENTARY,
            hardness = "1-2",
            colors = listOf("أبيض", "أبيض مصفر"),
            luster = "مطفأ",
            keyFeature = "أبيض ناعم جداً يلطّخ الأصابع ويُفور مع HCl",
            formation = "أعماق البحار من بقايا كائنات مجهرية (كوكوليث)",
            fieldId = "أبيض ناعم يُخدش بالظفر، يلطّخ، HCl يفور",
            uses = "طباشير الكتابة، أسمنت"
        ),

        RockRecord(
            nameAr = "المارل", nameEn = "Marl",
            aliases = listOf("marl", "كلسي طيني"),
            category = RockCategory.SEDIMENTARY,
            hardness = "1-3",
            colors = listOf("رمادي", "بيج", "أخضر مائل"),
            luster = "مطفأ",
            keyFeature = "بين الشيل والكلس — يُفور ببطء مع HCl",
            formation = "بيئات بحرية ضحلة، بحيرات",
            fieldId = "طيني ناعم يُفور ببطء مع HCl — خليط طين وكلس",
            uses = "صناعة الأسمنت، الزراعة"
        ),

        RockRecord(
            nameAr = "الصوان", nameEn = "Chert",
            aliases = listOf("flint", "صوّان", "chert"),
            category = RockCategory.SEDIMENTARY,
            hardness = "7",
            colors = listOf("رمادي", "أسود", "بني"),
            luster = "شمعي إلى زجاجي",
            keyFeature = "صلابة عالية جداً في صخرة رسوبية — غير عادي",
            formation = "قيعان البحار، أسفل الكلس أحياناً",
            fieldId = "كتل أو طبقات صلبة جداً (تشرر بضرب الحجارة)، كسر محاري حاد",
            uses = "أدوات حجرية تاريخية، الصوّان للقدّاحات"
        ),

        RockRecord(
            nameAr = "الفحم", nameEn = "Coal",
            aliases = listOf("coal", "فحم حجري"),
            category = RockCategory.SEDIMENTARY,
            hardness = "1-2",
            colors = listOf("أسود"),
            luster = "لامع (أنثراسيت) إلى مطفأ (ليغنيت)",
            keyFeature = "أسود خفيف، يلطّخ باللون الأسود، قابل للاشتعال",
            formation = "تحلل غابات قديمة في بيئات مستنقعية",
            fieldId = "أسود اللون يلطّخ يدك، خفيف نسبياً، يشتعل",
            uses = "وقود، صناعة"
        ),

        RockRecord(
            nameAr = "الإيفابوريت (ملح صخري)", nameEn = "Rock Salt",
            aliases = listOf("ملح صخري", "halite rock", "evaporite"),
            category = RockCategory.SEDIMENTARY,
            hardness = "2-2.5",
            colors = listOf("أبيض", "وردي", "برتقالي"),
            luster = "زجاجي",
            keyFeature = "مذاق مالح، قابل للذوبان في الماء",
            formation = "تبخر بحار وبحيرات قديمة (مناطق جافة)",
            fieldId = "تذوّق قطعة صغيرة — ملوحة فورية + انفصام مكعبي",
            uses = "ملح الطعام، صناعة كيميائية، معالجة مياه"
        ),

        // ══════════════════ صخور تحولية ════════════════════════════

        RockRecord(
            nameAr = "رخام", nameEn = "Marble",
            aliases = listOf("marble"),
            category = RockCategory.METAMORPHIC,
            hardness = "3-4",
            colors = listOf("أبيض", "وردي", "رمادي", "مخطط"),
            luster = "بلوري متوسط، وجوه مستوية",
            keyFeature = "مثل الكلس لكن بلوري خشن — يُفور مع HCl",
            formation = "تحوّل الحجر الكلسي بالحرارة والضغط",
            fieldId = "بلوري أبيض أو مخطط، يُفور مع HCl، وجوه كالسيتية لامعة",
            uses = "بناء، نحت، زخارف"
        ),

        RockRecord(
            nameAr = "كوارتزيت", nameEn = "Quartzite",
            aliases = listOf("quartzite"),
            category = RockCategory.METAMORPHIC,
            hardness = "7",
            colors = listOf("أبيض", "وردي", "رمادي"),
            luster = "زجاجي وميض (بلوري كثيف)",
            keyFeature = "صلب جداً — يكسر عبر الحبيبات لا بينها",
            formation = "تحوّل الحجر الرملي بالحرارة والضغط",
            fieldId = "أبيض/رمادي متماسك جداً، الكسر يعبر الحبيبات (يشع) لا يتبع الحبيبات",
            uses = "بلاط، بناء طرق، خامة سيليكا"
        ),

        RockRecord(
            nameAr = "شيست", nameEn = "Schist",
            aliases = listOf("schist", "شيستوز"),
            category = RockCategory.METAMORPHIC,
            hardness = "متوسط 5-6",
            colors = listOf("فضي", "ذهبي", "رمادي", "أخضر"),
            luster = "لامع جداً (ميكا)",
            keyFeature = "ورقي الشكل، يتشقق إلى ألواح، لمعان ميكا قوي",
            formation = "تحوّل قوي للشيل أو الحجر الطيني",
            fieldId = "وجوه بريقة كالفضة (ميكا) مع بنية ورقية واضحة",
            uses = "دراسة المناطق التحولية العميقة"
        ),

        RockRecord(
            nameAr = "جنيس", nameEn = "Gneiss",
            aliases = listOf("gneiss"),
            category = RockCategory.METAMORPHIC,
            hardness = "6-7",
            colors = listOf("رمادي وأسود مخطط"),
            luster = "حبيبي مع خطوط واضحة",
            keyFeature = "خطوط متناوبة من معادن فاتحة وداكنة",
            formation = "تحوّل جوي بالغ الشدة لجرانيت أو صخور متوسطة",
            fieldId = "نسيج حبيبي خشن مع خطوط متوازية فاتحة/داكنة واضحة",
            uses = "بناء، زخارف"
        ),

        RockRecord(
            nameAr = "سليت (الأردواز)", nameEn = "Slate",
            aliases = listOf("slate", "أردواز"),
            category = RockCategory.METAMORPHIC,
            hardness = "2-4",
            colors = listOf("رمادي", "أسود", "أخضر", "أحمر"),
            luster = "مطفأ إلى لامع خفيف",
            keyFeature = "يتشقق إلى ألواح مستوية مثالية (انفصام في اتجاه واحد)",
            formation = "تحوّل منخفض الدرجة للشيل",
            fieldId = "يتشقق بنقرة خفيفة إلى ألواح مستوية، صوت رنّان عند الطرق",
            uses = "ألواح السقف، ألواح الكتابة القديمة"
        ),

        RockRecord(
            nameAr = "فيليت", nameEn = "Phyllite",
            aliases = listOf("phyllite"),
            category = RockCategory.METAMORPHIC,
            hardness = "3-4",
            colors = listOf("رمادي فضي", "أخضر فضي"),
            luster = "لامع فضي قوي",
            keyFeature = "بين السليت والشيست — ورقي ولامع أكثر من السليت",
            formation = "تحوّل متوسط للشيل",
            fieldId = "سطح فضي متموج اللمعان، أكثر بريقاً من الأردواز",
            uses = "دراسة تسلسل التحول"
        ),

        RockRecord(
            nameAr = "الأمفيبوليت", nameEn = "Amphibolite",
            aliases = listOf("amphibolite"),
            category = RockCategory.METAMORPHIC,
            hardness = "5-6",
            colors = listOf("أسود وأبيض", "رمادي داكن"),
            luster = "حبيبي متوسط إلى خشن",
            keyFeature = "هورنبليند أسود + بلاجيوكلاز أبيض — ثنائي اللون",
            formation = "تحوّل متوسط إلى شديد لصخور قاعدية أو غنية بالكالسيوم",
            fieldId = "أسود وأبيض حبيبي، لا خطوط واضحة، مظهر معدني",
            uses = "دراسة التحول، مواد بناء أحياناً"
        )

    ) // end of records list

    // ══════════════════ دوال البحث ════════════════════════════════

    /** بحث بالاسم (عربي أو إنجليزي أو اسم بديل) */
    fun findByName(query: String): RockRecord? {
        val q = query.trim().lowercase()
        return records.firstOrNull { r ->
            r.nameAr.lowercase().contains(q) ||
            r.nameEn.lowercase().contains(q)  ||
            r.aliases.any { it.lowercase().contains(q) }
        }
    }

    /** بحث بالخصائص: لون، صلابة، نوع، كلمة مفتاحية */
    fun searchByProperties(description: String): List<RockRecord> {
        val q = description.trim().lowercase()
        val tokens = q.split(" ", "،", ",").filter { it.length > 1 }

        return records.filter { r ->
            val blob = listOf(
                r.nameAr, r.nameEn,
                r.hardness,
                r.colors.joinToString(" "),
                r.luster, r.keyFeature,
                r.formation, r.fieldId, r.uses,
                r.category.name,
                when (r.category) {
                    RockCategory.MINERAL    -> "معدن"
                    RockCategory.IGNEOUS    -> "ناري بركاني"
                    RockCategory.SEDIMENTARY -> "رسوبي"
                    RockCategory.METAMORPHIC -> "تحولي"
                }
            ).joinToString(" ").lowercase()

            tokens.any { token -> blob.contains(token) }
        }.sortedByDescending { r ->
            // رتّب حسب عدد الكلمات المطابقة
            tokens.count { token ->
                val blob = listOf(r.nameAr, r.nameEn,
                    r.colors.joinToString(" "),
                    r.keyFeature, r.fieldId
                ).joinToString(" ").lowercase()
                blob.contains(token)
            }
        }
    }

    /** نص ملخص للنطق الصوتي */
    fun toSpeechSummary(r: RockRecord): String {
        val catAr = when (r.category) {
            RockCategory.MINERAL     -> "معدن"
            RockCategory.IGNEOUS     -> "صخرة نارية"
            RockCategory.SEDIMENTARY -> "صخرة رسوبية"
            RockCategory.METAMORPHIC -> "صخرة تحولية"
        }
        return "${r.nameAr} — $catAr. " +
               "الصلابة: ${r.hardness}. " +
               "اللون: ${r.colors.take(2).joinToString(" أو ")}. " +
               "كيف تعرفه: ${r.fieldId}."
    }

    /** نص تفصيلي كامل */
    fun toFullText(r: RockRecord): String = buildString {
        val catAr = when (r.category) {
            RockCategory.MINERAL     -> "معدن"
            RockCategory.IGNEOUS     -> "صخرة نارية"
            RockCategory.SEDIMENTARY -> "صخرة رسوبية"
            RockCategory.METAMORPHIC -> "صخرة تحولية"
        }
        appendLine("═══ ${r.nameAr} / ${r.nameEn} ═══")
        appendLine("النوع     : $catAr")
        appendLine("الصلابة  : ${r.hardness} (موس)")
        appendLine("اللون     : ${r.colors.joinToString(", ")}")
        appendLine("البريق   : ${r.luster}")
        appendLine("الميزة   : ${r.keyFeature}")
        appendLine("التكوين  : ${r.formation}")
        appendLine("الميدان  : ${r.fieldId}")
        appendLine("الاستخدامات: ${r.uses}")
    }
}
