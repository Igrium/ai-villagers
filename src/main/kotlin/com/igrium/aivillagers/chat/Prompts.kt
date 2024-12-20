package com.igrium.aivillagers.chat

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.core.Role
import com.igrium.aivillagers.chat.PromptManager.ProfessionPrompt
import com.igrium.aivillagers.util.VillagerUtils
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
            content = applyTemplate(history, initialPrompt)
        )
    }

    /**
     * A map of the different prompts for all the different professions.
     */
    val professionPrompts: Map<VillagerProfession, ProfessionPrompt> = ConcurrentHashMap();

    var fallbackProfessionPrompt = ProfessionPrompt("You are unemployed.")

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
        val ent = chatHistory.entity!!;
        val villager = if (ent is VillagerEntity) ent else null

        val language = Language.getInstance()

        val st = ST(message)

        st.add("entity", language.get(chatHistory.entity.type.translationKey, "villager"))
        st.add("name", chatHistory.entity.displayName?.string)

        val prof = chatHistory.getProfession()
        st.add("profession", VillagerUtils.getProfessionName(prof))

        // Check if it's going to be here first to avoid recursion when calculating professionPrompt.
        if (message.contains("<professionPrompt>")) {
            st.add("professionPrompt", getProfessionPromptContent(chatHistory))
        }

        return st.render()
    }
}