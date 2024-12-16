package com.igrium.aivillagers;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

public class SpeechVCPlugin implements VoicechatPlugin {

    private static SpeechVCPlugin instance;

    public static SpeechVCPlugin getInstance() {
        return instance;
    }

    @Override
    public String getPluginId() {
        return "villager-ai";
    }

    private VoicechatApi api;
    private VoicechatServerApi serverApi;

    private SpeechAudioManager audioManager;
    
    @Override
    public void initialize(VoicechatApi api) {
        instance = this;
        this.api = api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onServerStopped);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        serverApi = event.getVoicechat();
        audioManager = new SpeechAudioManager(this);
    }

    private void onServerStopped(VoicechatServerStoppedEvent event) {
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
    }

    public VoicechatApi getApi() {
        return api;
    }

    public VoicechatServerApi getServerApi() {
        return serverApi;
    }

    public SpeechAudioManager getAudioManager() {
        return audioManager;
    }
}
