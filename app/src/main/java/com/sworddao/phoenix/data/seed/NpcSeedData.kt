package com.sworddao.phoenix.data.seed


import com.sworddao.phoenix.feature.npc.data.IdleAnimationState
import com.sworddao.phoenix.feature.npc.data.InteractionAvailability
import com.sworddao.phoenix.feature.npc.data.Npc
import com.sworddao.phoenix.feature.npc.data.NpcSchedule
import com.sworddao.phoenix.feature.npc.data.NpcScheduleEntry
import com.sworddao.phoenix.feature.npc.data.TimeOfDay

object NpcSeedData {

fun loadMockNpcs(): List<Npc> {
        return listOf(
            Npc(
                id = "grandma_mei",
                displayName = "Grandma Mei",
                occupation = "Retired Baker",
                personality = "Warm, patient, and funny. She treats everyone like family and loves sharing stories over fresh bread.",
                currentLocation = "Grandma Mei's Bakery",
                friendshipXp = 0,
                schedule = NpcSchedule(
                    entries = listOf(
                        NpcScheduleEntry(TimeOfDay.MORNING, "Grandma Mei's Bakery", "Baking morning bread and greeting early customers"),
                        NpcScheduleEntry(TimeOfDay.AFTERNOON, "Village Square", "Sitting on a bench, sharing stories with neighbors"),
                        NpcScheduleEntry(TimeOfDay.EVENING, "Grandma Mei's Bakery", "Closing up and sharing leftover treats"),
                        NpcScheduleEntry(TimeOfDay.NIGHT, "Home", "Resting after a long day")
                    )
                ),
                avatarEmoji = "\uD83D\uDC75",
                idleAnimationState = IdleAnimationState.WORKING,
                interactionAvailability = InteractionAvailability.AVAILABLE,
                vocabularyCategories = listOf("Greetings", "Family", "Food", "Daily Conversation"),
                dialogueReferences = listOf("bakery_intro", "bakery_food", "bakery_family"),
                shortDescription = "The warmest baker in Qingyuan Village. She makes the best mooncakes and steamed buns."
            ),
            Npc(
                id = "restaurant_owner_lin",
                displayName = "Restaurant Owner Lin",
                occupation = "Chef",
                personality = "Passionate about food and always eager to teach. He believes cooking is a language everyone understands.",
                currentLocation = "Restaurant",
                friendshipXp = 0,
                schedule = NpcSchedule(
                    entries = listOf(
                        NpcScheduleEntry(TimeOfDay.MORNING, "Restaurant", "Preparing ingredients for the day"),
                        NpcScheduleEntry(TimeOfDay.AFTERNOON, "Restaurant", "Serving lunch customers and chatting"),
                        NpcScheduleEntry(TimeOfDay.EVENING, "Restaurant", "Dinner rush and teaching cooking vocabulary"),
                        NpcScheduleEntry(TimeOfDay.NIGHT, "Home", "Resting and planning tomorrow's menu")
                    )
                ),
                avatarEmoji = "\uD83D\uDC68\u200D\uD83C\uDF73",
                idleAnimationState = IdleAnimationState.WORKING,
                interactionAvailability = InteractionAvailability.AVAILABLE,
                vocabularyCategories = listOf("Food", "Ordering", "Ingredients", "Cooking", "Payment", "Restaurant Etiquette"),
                dialogueReferences = listOf("restaurant_intro", "restaurant_menu", "restaurant_ordering"),
                shortDescription = "Owner of the best restaurant in the village. His hot pot is legendary."
            ),
            Npc(
                id = "taxi_driver_chen",
                displayName = "Taxi Driver Chen",
                occupation = "Taxi Driver",
                personality = "Friendly and talkative. He knows every corner of the village and loves sharing local gossip.",
                currentLocation = "Village Square",
                friendshipXp = 0,
                schedule = NpcSchedule(
                    entries = listOf(
                        NpcScheduleEntry(TimeOfDay.MORNING, "Village Square", "Waiting for morning passengers"),
                        NpcScheduleEntry(TimeOfDay.AFTERNOON, "Village Exit", "Taking passengers to nearby towns"),
                        NpcScheduleEntry(TimeOfDay.EVENING, "Village Square", "Evening shift and chatting with locals"),
                        NpcScheduleEntry(TimeOfDay.NIGHT, "Home", "Resting before tomorrow's early start")
                    )
                ),
                avatarEmoji = "\uD83D\uDE95",
                idleAnimationState = IdleAnimationState.SITTING,
                interactionAvailability = InteractionAvailability.AVAILABLE,
                vocabularyCategories = listOf("Directions", "Numbers", "Time", "Travel", "Weather"),
                dialogueReferences = listOf("taxi_intro", "taxi_directions", "taxi_numbers"),
                shortDescription = "The friendliest driver in Qingyuan. He can take you anywhere and teach you along the way."
            ),
            Npc(
                id = "university_student_wei",
                displayName = "University Student Wei",
                occupation = "University Student",
                personality = "Curious and energetic. She loves technology, music, and meeting new people from different cultures.",
                currentLocation = "Tea House",
                friendshipXp = 0,
                schedule = NpcSchedule(
                    entries = listOf(
                        NpcScheduleEntry(TimeOfDay.MORNING, "Tea House", "Studying and drinking morning tea"),
                        NpcScheduleEntry(TimeOfDay.AFTERNOON, "Village Square", "Meeting friends and exploring"),
                        NpcScheduleEntry(TimeOfDay.EVENING, "Tea House", "Evening study session and conversation practice"),
                        NpcScheduleEntry(TimeOfDay.NIGHT, "Home", "Studying and listening to music")
                    )
                ),
                avatarEmoji = "\uD83D\uDC69\u200D\uD83C\uDF93",
                idleAnimationState = IdleAnimationState.SITTING,
                interactionAvailability = InteractionAvailability.AVAILABLE,
                vocabularyCategories = listOf("Friends", "Technology", "Music", "Gaming", "Campus Life"),
                dialogueReferences = listOf("wei_intro", "wei_tech", "wei_music"),
                shortDescription = "A bright university student who loves learning about different cultures."
            )
        )
    }

}
