// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.datagen;

import com.lewho.chat.Advancements;
import com.lewho.chat.EntityChatData;
import com.lewho.i18n.CCText;
import com.lewho.utils.Randomizer;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

/**
 * Generates the English fallback language file.
 */
public class CreatureChatLangProvider extends FabricLanguageProvider {
    public CreatureChatLangProvider(FabricDataOutput output) {
        super(output, "creaturechat");
    }

    @Override
    public void generateTranslations(TranslationBuilder builder) {
        Map<String, String> en = new TreeMap<>();
        Stream.of(
                Randomizer.allErrorText(),
                Randomizer.allNoResponseText(),
                CCText.UI_TEXT.stream(),
                CCText.CONFIG_TEXT.stream(),
                EntityChatData.ERROR_MISC.stream(),
                EntityChatData.ERROR_SOLUTIONS.stream(),
                Advancements.allText()
        ).flatMap(s -> s).forEach(tr -> en.putIfAbsent(tr.key(), tr.en()));

        en.forEach(builder::add);
        LangSync.sync(en);
    }

    @Override
    public String getName() {
        return "CreatureChat Lang";
    }
}
