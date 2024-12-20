package com.igrium.aivillagers.util;

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.village.VillagerProfession;

public final class VillagerUtils {
    public static String getProfessionName(VillagerProfession profession) {
        Identifier id = Registries.VILLAGER_PROFESSION.getId(profession);
        // Get the path as fallback so minecraft doesn't get put in the name
        return Language.getInstance().get(id.toTranslationKey(), id.getPath());
    }
}
