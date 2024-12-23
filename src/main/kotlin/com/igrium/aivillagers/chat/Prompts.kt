package com.igrium.aivillagers.chat

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.core.Role
import com.igrium.aivillagers.chat.PromptManager.ProfessionPrompt
import com.igrium.aivillagers.util.SimpleTemplate
import com.igrium.aivillagers.util.VillagerUtils
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.util.Language
import net.minecraft.village.VillagerProfession
import org.stringtemplate.v4.ST
import java.util.concurrent.ConcurrentHashMap

/**
 * It's honestly easier to keep track of all the prompt values in Kotlin.
 */
class Prompts {

    private fun ChatHistoryComponent.getProfession(): VillagerProfession {
        val ent = this.entity;
        return if (ent is VillagerEntity) ent.villagerData.profession else VillagerProfession.NONE
    }

    /**
     * The initial prompt used to initialize all villagers.
     */
    var initialPrompt: String = "";

    /**
     * Apply the initial prompt to a given villager.
     * @param history The villager's chat history component
     * @return A `ChatMessage` with the initial prompt.
     */
    fun getInitialPromptMsg(history: ChatHistoryComponent): ChatMessage {
        return ChatMessage(
            role = Role.System,
            content = calcInitialPrompt(history)
        )
    }

    fun calcInitialPrompt(history: ChatHistoryComponent): String {
        val ent = history.entity;
        val baby = if (ent is LivingEntity) ent.isBaby else false;
        return if (baby) applyTemplate(history, babyPrompt) else applyTemplate(history, initialPrompt);
    }

    /**
     * A map of the different prompts for all the different professions.
     */
    val professionPrompts: Map<VillagerProfession, ProfessionPrompt> = ConcurrentHashMap();

    var fallbackProfessionPrompt = ProfessionPrompt("You are unemployed.")

    var babyPrompt: String = "You are a kid.";

    @JvmOverloads
    fun getProfessionPromptContent(history: ChatHistoryComponent, switched: Boolean = false): String {
        val prompt = professionPrompts.getOrDefault(history.getProfession(), fallbackProfessionPrompt);
        val str = if (switched) prompt.switched else prompt.initial;
        return applyTemplate(history, str)
    }

    @JvmOverloads
    fun getProfessionPromptMsg(history: ChatHistoryComponent, switched: Boolean = false): ChatMessage {
        return ChatMessage(
            role = Role.User,
            content = getProfessionPromptContent(history, switched)
        )
    }

    var nameChangePrompt: String = ""

    fun getNameChangePromptMsg(history: ChatHistoryComponent): ChatMessage {
        return ChatMessage(
            role = Role.System,
            content = applyTemplate(history, nameChangePrompt)
        )
    }

    /**
     * Apply the values from an entity to a message template.
     * @param chatHistory The entity's chat history component.
     * @param message The template message.
     * @return The generated message.
     */
    fun applyTemplate(chatHistory: ChatHistoryComponent, message: String): String {

        val language = Language.getInstance()

        val st = SimpleTemplate()

        st.add("entity", { language.get(chatHistory.entity.type.translationKey, "villager") })
        st.add("name", { chatHistory.entity.displayName?.string })

        st.add("profession", { VillagerUtils.getProfessionName(chatHistory.getProfession()) })

        // Won't get called unless professionPrompt is there, so no recursion issues.
        st.add("professionPrompt", { getProfessionPromptContent(chatHistory) })


        return st.render(message)
    }
}