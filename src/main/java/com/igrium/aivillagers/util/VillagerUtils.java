package com.igrium.aivillagers.util;

import net.minecraft.registry.Registries;
import net.minecraft.util.Language;
import net.minecraft.village.VillagerProfession;

public final class VillagerUtils {
    public static String getProfessionName(VillagerProfession profession) {
        return Language.getInstance().get(Registries.VILLAGER_PROFESSION.getId(profession).toTranslationKey());
    }
}
