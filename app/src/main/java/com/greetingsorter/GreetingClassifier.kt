package com.greetingsorter

/** The groups images are sorted into, in the order they are shown. */
enum class Category(val title: String, val emoji: String) {
    GOOD_MORNING("Good Morning", "🌅"),
    GOOD_AFTERNOON("Good Afternoon", "☀️"),
    GOOD_EVENING("Good Evening", "🌇"),
    GOOD_NIGHT("Good Night", "🌙"),
    BIRTHDAY("Birthday", "🎂"),
    ANNIVERSARY("Anniversary", "💐"),
    FESTIVAL("Festivals", "🪔"),
    OTHER_WISHES("Other Wishes", "🙏"),
    NOT_GREETING("Not a greeting", "🖼️");
}

/**
 * Decides which greeting group an image belongs to from the text OCR found on it.
 * Pure Kotlin so it can be unit tested without a phone.
 */
object GreetingClassifier {

    private class Rule(val category: Category, patterns: List<String>) {
        val regex = Regex(patterns.joinToString("|") { "(?:$it)" }, RegexOption.IGNORE_CASE)
    }

    // OCR on decorative fonts often swaps o/0 and drops spaces, so the patterns allow for that.
    private const val GOOD = "g[o0]{2}d"
    private const val S = "\\s*"

    // Checked in order: the first rule that matches wins.
    private val rules = listOf(
        Rule(
            Category.BIRTHDAY, listOf(
                "birth${S}day", "b'?day", "bday", "hbd", "many happy returns",
                "जन्म${S}दिन", "जन्मदिवस", "हैप्पी बर्थडे", "बर्थडे",
            )
        ),
        Rule(
            Category.ANNIVERSARY, listOf(
                "anniversary", "anniversery", "सालगिरह", "वर्षगांठ", "एनिवर्सरी",
            )
        ),
        Rule(
            Category.FESTIVAL, listOf(
                "d[iu]wal[ie]", "deepavali", "dipawali", "\\bholi\\b", "\\beid\\b", "ramadan", "ramzan",
                "christmas", "x-?mas", "new${S}year", "pongal", "sankranti", "sankranthi", "\\bonam\\b",
                "navratri", "navaratri", "dussehra", "dasara", "dussera", "vijaya${S}dashami",
                "durga${S}puja", "ganesh", "ganpati", "vinayaka", "raksha${S}bandhan", "rakhi",
                "janmashtami", "\\bugadi\\b", "gudi${S}padwa", "baisakhi", "vaisakhi", "lohri",
                "karva${S}chauth", "dhanteras", "bhai${S}dooj", "bhaubeej", "easter", "good${S}friday",
                "gurpurab", "guru${S}purab", "guru${S}nanak", "shivratri", "ram${S}navami",
                "hanuman${S}jayanti", "makar", "\\bbihu\\b", "chhath", "\\bteej\\b", "vishu",
                "independence${S}day", "republic${S}day", "mother'?s${S}day", "father'?s${S}day",
                "friendship${S}day", "teacher'?s${S}day", "women'?s${S}day", "valentine", "thanksgiving",
                "guru${S}purnima", "akshaya${S}tritiya", "buddha${S}purnima", "muharram", "bakrid",
                "दीपावली", "दिवाली", "होली", "ईद", "नव${S}वर्ष", "नया${S}साल", "नवरात्र", "दशहरा",
                "रक्षा${S}बंधन", "राखी", "जन्माष्टमी", "गणेश", "मकर${S}संक्रांति", "लोहड़ी", "करवा${S}चौथ",
                "धनतेरस", "भाई${S}दूज", "शिवरात्रि", "राम${S}नवमी", "छठ", "स्वतंत्रता${S}दिवस", "गणतंत्र${S}दिवस",
            )
        ),
        Rule(
            Category.GOOD_NIGHT, listOf(
                "$GOOD${S}n[i1l]ght", "$GOOD${S}nite", "sweet${S}dreams", "sleep${S}well", "\\bgn\\b",
                "शुभ${S}रात्रि", "शुभ${S}रात्री", "गुड${S}नाइट", "गुड${S}नाईट",
            )
        ),
        Rule(
            Category.GOOD_EVENING, listOf(
                "$GOOD${S}even[i1l]ng", "$GOOD${S}eve\\b", "happy${S}evening", "pleasant${S}evening",
                "शुभ${S}संध्या", "गुड${S}इवनिंग", "शुभ${S}शाम",
            )
        ),
        Rule(
            Category.GOOD_AFTERNOON, listOf(
                "$GOOD${S}after${S}n[o0]{2}n", "happy${S}afternoon",
                "शुभ${S}दोपहर", "गुड${S}आफ्टरनून",
            )
        ),
        Rule(
            Category.GOOD_MORNING, listOf(
                "$GOOD${S}m[o0]rn[i1l]ng", "$GOOD${S}m[o0]rn", "m[o0]rn[i1l]ng", "\\bgm\\b",
                "have${S}a${S}(?:nice|great|good|wonderful|blessed|beautiful|lovely)${S}day",
                "happy${S}(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday)",
                "(?:blessed|beautiful|wonderful|lovely)${S}(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday|day)",
                "सुप्रभात", "शुभ${S}प्रभात", "शुभ${S}सवेरा", "गुड${S}मॉर्निंग", "गुड${S}मोर्निंग",
            )
        ),
        Rule(
            Category.OTHER_WISHES, listOf(
                "wish", "congrat", "best${S}of${S}luck", "happy", "bless", "greetings", "get${S}well",
                "thank${S}you", "regards", "god${S}bless", "jai${S}shri", "jai${S}shree", "radhe",
                "शुभकामना", "शुभ${S}कामना", "बधाई", "मुबारक", "आशीर्वाद", "धन्यवाद", "जय${S}श्री",
                "शुभ", "हार्दिक",
            )
        ),
    )

    fun classify(text: String): Category {
        val normalized = text.replace(Regex("\\s+"), " ").trim()
        if (normalized.isEmpty()) return Category.NOT_GREETING
        return rules.firstOrNull { it.regex.containsMatchIn(normalized) }?.category
            ?: Category.NOT_GREETING
    }
}
