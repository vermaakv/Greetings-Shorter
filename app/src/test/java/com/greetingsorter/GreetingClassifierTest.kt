package com.greetingsorter

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingClassifierTest {

    private fun check(expected: Category, vararg texts: String) {
        texts.forEach { assertEquals("\"$it\"", expected, GreetingClassifier.classify(it)) }
    }

    @Test fun morning() = check(
        Category.GOOD_MORNING,
        "Good Morning", "GOOD\nMORNING\nHave a nice day", "G00d M0rning", "goodmorning friends",
        "Happy Sunday", "Have a blessed day", "सुप्रभात", "शुभ प्रभात", "Morning! Stay blessed",
    )

    @Test fun afternoon() = check(Category.GOOD_AFTERNOON, "Good Afternoon", "good after noon")

    @Test fun evening() = check(Category.GOOD_EVENING, "Good Evening", "GOOD EVENING\nfriends", "शुभ संध्या")

    @Test fun night() = check(Category.GOOD_NIGHT, "Good Night", "GOOD\nNIGHT\nSweet dreams", "gud nite".replace("gud", "good"), "शुभ रात्रि")

    @Test fun birthday() = check(
        Category.BIRTHDAY,
        "Happy Birthday!", "Happy B'day dear", "Wishing you a very happy birth day", "जन्मदिन की हार्दिक शुभकामनाएं",
    )

    @Test fun anniversary() = check(Category.ANNIVERSARY, "Happy Anniversary", "Happy Wedding Anniversary")

    @Test fun festival() = check(
        Category.FESTIVAL,
        "Happy Diwali", "Happy Deepavali", "Happy Holi", "Eid Mubarak", "Merry Christmas",
        "Happy New Year 2027", "Happy Pongal", "Ganesh Chaturthi", "Happy Raksha Bandhan",
        "दीपावली की शुभकामनाएं", "होली की हार्दिक शुभकामनाएं",
    )

    @Test fun otherWishes() = check(
        Category.OTHER_WISHES,
        "Congratulations!", "Best wishes", "God bless you", "Get well soon", "हार्दिक बधाई",
    )

    @Test fun notGreeting() = check(
        Category.NOT_GREETING,
        "", "   ", "Invoice #2231 Total 450", "Meeting at 5pm in room 3", "Scheme holidays list",
    )
}
