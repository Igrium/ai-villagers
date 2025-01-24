package com.igrium.aivillagers.subsystems;

import com.igrium.aivillagers.SpeechVCPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Responsible for listening to what players are saying and calling the other
 * subsystems.
 */
public interface ListeningSubsystem extends Subsystem {
    public void enableListening(Entity villager);
    public void disableListening(Entity villager);

    /**
     * Called on the voicechat thread when a voice chat packet is received on the server.
     *
     * @param plugin The AI villagers simple voice chat plugin.
     * @param player Player it's coming from.
     * @param data   16-bit, 48khz PCM voice data. Empty if this packet marks the end of the data.
     */
    void onMicPacket(SpeechVCPlugin plugin, ServerPlayerEntity player, short[] data);
}
