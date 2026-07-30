// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static registry of all official Minecraft locale codes and their self-names (display names
 * in the language's own script). Used server-side to convert a stored locale code such as
 * {@code ru_ru} into the human-readable string {@code "Русский (Россия)"} that is embedded
 * in the LLM character-generation and chat prompts.
 * <p>
 * The map is ordered alphabetically by locale code. Unknown codes fall back to the raw
 * code string so the prompt still receives something meaningful.
 */
public final class MinecraftLanguages {

    private MinecraftLanguages() {}

    /** All known Minecraft locale codes, sorted alphabetically. */
    public static final List<String> ALL_CODES;

    /** Locale code → in-language display name. */
    private static final Map<String, String> DISPLAY_NAMES;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("af_za",    "Afrikaans");
        m.put("ar_sa",    "العربية (العربية السعودية)");
        m.put("az_az",    "Azərbaycanca");
        m.put("ba_ru",    "Башҡортса");
        m.put("bar",      "Boarisch");
        m.put("be_by",    "Беларуская (Беларусь)");
        m.put("bg_bg",    "Български (България)");
        m.put("br_fr",    "Brezhoneg");
        m.put("brb",      "Braobans");
        m.put("bs_ba",    "Bosanski (Bosna i Hercegovina)");
        m.put("ca_es",    "Català (Espanya)");
        m.put("cs_cz",    "Čeština (Česko)");
        m.put("cy_gb",    "Cymraeg (Y Deyrnas Unedig)");
        m.put("da_dk",    "Dansk (Danmark)");
        m.put("de_at",    "Österreichisches Deutsch");
        m.put("de_ch",    "Schwiizerdütsch");
        m.put("de_de",    "Deutsch (Deutschland)");
        m.put("el_gr",    "Ελληνικά (Ελλάδα)");
        m.put("en_au",    "English (Australia)");
        m.put("en_ca",    "English (Canada)");
        m.put("en_gb",    "English (United Kingdom)");
        m.put("en_nz",    "English (New Zealand)");
        m.put("en_pt",    "Pirate Speak");
        m.put("en_ud",    "ʇxǝʇ uʍop-ǝpısdn");
        m.put("en_us",    "English (US)");
        m.put("enp",      "Anglish");
        m.put("enws",     "Shakespearean English");
        m.put("eo_uy",    "Esperanto");
        m.put("es_ar",    "Español (Argentina)");
        m.put("es_cl",    "Español (Chile)");
        m.put("es_ec",    "Español (Ecuador)");
        m.put("es_es",    "Español (España)");
        m.put("es_mx",    "Español (México)");
        m.put("es_py",    "Español (Paraguay)");
        m.put("es_uy",    "Español (Uruguay)");
        m.put("es_ve",    "Español (Venezuela)");
        m.put("et_ee",    "Eesti (Eesti)");
        m.put("eu_es",    "Euskara (Espainia)");
        m.put("fa_ir",    "فارسی (ایران)");
        m.put("fi_fi",    "Suomi (Suomi)");
        m.put("fil_ph",   "Filipino (Pilipinas)");
        m.put("fr_ca",    "Français (Canada)");
        m.put("fr_fr",    "Français (France)");
        m.put("fra_de",   "Fränggisch");
        m.put("fur_it",   "Furlan (Italie)");
        m.put("fy_nl",    "Frysk (Nederlân)");
        m.put("ga_ie",    "Gaeilge (Éire)");
        m.put("gd_gb",    "Gàidhlig (An Rìoghachd Aonaichte)");
        m.put("gl_es",    "Galego (España)");
        m.put("gt_mx",    "Kaqchikel");
        m.put("gu_in",    "ગુજરાતી (ભારત)");
        m.put("haw_us",   "ʻŌlelo Hawaiʻi");
        m.put("he_il",    "עברית (ישראל)");
        m.put("hi_in",    "हिन्दी (भारत)");
        m.put("hr_hr",    "Hrvatski (Hrvatska)");
        m.put("hu_hu",    "Magyar (Magyarország)");
        m.put("hy_am",    "Հայերեն (Հայաստան)");
        m.put("id_id",    "Bahasa Indonesia (Indonesia)");
        m.put("ig_ng",    "Igbo");
        m.put("io_en",    "Ido");
        m.put("is_is",    "Íslenska (Ísland)");
        m.put("it_it",    "Italiano (Italia)");
        m.put("ja_jp",    "日本語 (日本)");
        m.put("jbo_en",   "Lojban");
        m.put("ka_ge",    "ქართული (საქართველო)");
        m.put("kk_kz",    "Қазақша (Қазақстан)");
        m.put("kn_in",    "ಕನ್ನಡ (ಭಾರತ)");
        m.put("ko_kr",    "한국어 (대한민국)");
        m.put("ksh",      "Kölsch/Ripoarisch");
        m.put("ky_kg",    "Кыргызча");
        m.put("la_la",    "Latina");
        m.put("lb_lu",    "Lëtzebuergesch (Lëtzebuerg)");
        m.put("li_li",    "Limburgisch");
        m.put("lmo",      "Lombard");
        m.put("lo_la",    "ລາວ");
        m.put("lt_lt",    "Lietuvių (Lietuva)");
        m.put("lv_lv",    "Latviešu (Latvija)");
        m.put("lzh",      "文言");
        m.put("mk_mk",    "Македонски (Македонија)");
        m.put("mn_mn",    "Монгол");
        m.put("ms_my",    "Bahasa Melayu (Malaysia)");
        m.put("mt_mt",    "Malti (Malta)");
        m.put("nah",      "Mēxihkatlahtōlli");
        m.put("nds_de",   "Plattdüütsch");
        m.put("nl_be",    "Vlaams");
        m.put("nl_nl",    "Nederlands (Nederland)");
        m.put("nn_no",    "Norsk Nynorsk (Noreg)");
        m.put("no_no",    "Norsk Bokmål (Norge)");
        m.put("oc_fr",    "Occitan (França)");
        m.put("ovd",      "Övdalian");
        m.put("pl_pl",    "Polski (Polska)");
        m.put("pt_br",    "Português (Brasil)");
        m.put("pt_pt",    "Português (Portugal)");
        m.put("qya_aa",   "Quenya");
        m.put("ro_ro",    "Română (România)");
        m.put("rpr",      "Русский (Дореформенная орфография)");
        m.put("ru_ru",    "Русский (Россия)");
        m.put("ry_ua",    "Руснацькый");
        m.put("sah_sah",  "Саха тыла");
        m.put("se_no",    "Davvisámegiella");
        m.put("sk_sk",    "Slovenčina (Slovensko)");
        m.put("sl_si",    "Slovenščina (Slovenija)");
        m.put("so_so",    "Soomaali");
        m.put("sq_al",    "Shqip (Shqipëri)");
        m.put("sr_cs",    "Srpski (Latinica)");
        m.put("sr_sp",    "Српски (Ћирилица)");
        m.put("sv_se",    "Svenska (Sverige)");
        m.put("sw_ke",    "Kiswahili");
        m.put("ta_in",    "தமிழ் (இந்தியா)");
        m.put("th_th",    "ภาษาไทย (ประเทศไทย)");
        m.put("tl_ph",    "Wikang Tagalog (Pilipinas)");
        m.put("tlh_aa",   "tlhIngan Hol");
        m.put("tok",      "Toki Pona");
        m.put("tr_tr",    "Türkçe (Türkiye)");
        m.put("tt_ru",    "Татарча (Россия)");
        m.put("uk_ua",    "Українська (Україна)");
        m.put("val_es",   "Valencià (Espanya)");
        m.put("vec_it",   "Vèneto");
        m.put("vi_vn",    "Tiếng Việt (Việt Nam)");
        m.put("xh_za",    "isiXhosa");
        m.put("yi_de",    "ייִדיש");
        m.put("yo_ng",    "Yorùbá");
        m.put("zh_cn",    "简体中文 (中国大陆)");
        m.put("zh_tw",    "繁體中文 (台灣)");
        m.put("zlm_arab", "Melayu (Jawi)");
        m.put("zu_za",    "isiZulu");
        DISPLAY_NAMES = Collections.unmodifiableMap(m);
        List<String> codes = new ArrayList<>(m.keySet());
        Collections.sort(codes);
        ALL_CODES = Collections.unmodifiableList(codes);
    }

    /**
     * Returns the in-language display name for the given locale code, e.g.
     * {@code displayName("ru_ru")} → {@code "Русский (Россия)"}. Falls back to the
     * raw code if the locale is not in the table.
     */
    public static String displayName(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return DISPLAY_NAMES.getOrDefault(code.trim().toLowerCase(), code.trim());
    }

    /**
     * Returns a short human-readable description suitable for display in the
     * {@code /creaturechat setup show} output.
     *
     * @param code locale code, or {@code ""} / {@code "auto"} for automatic
     */
    public static String describe(String code) {
        if (code == null || code.isBlank() || code.equalsIgnoreCase("auto")) {
            return "auto (client locale)";
        }
        String name = displayName(code);
        return code.toLowerCase() + " (" + name + ")";
    }
}
