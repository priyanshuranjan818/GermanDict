package com.learnwithhaxx.app;

import java.util.Arrays;
import java.util.List;

public class Conjugator {

    public static class Conjugation {
        public String ich, du, erSieEs, wir, ihr, sieSie;

        public Conjugation(String ich, String du, String erSieEs, String wir, String ihr, String sieSie) {
            this.ich = ich;
            this.du = du;
            this.erSieEs = erSieEs;
            this.wir = wir;
            this.ihr = ihr;
            this.sieSie = sieSie;
        }
    }

    private static final List<String> SEPARABLE_PREFIXES = Arrays.asList(
            "ab", "an", "auf", "aus", "bei", "ein", "mit", "nach", "her", "hin", "vor", "zu", "weg"
    );

    public static Conjugation conjugate(String input) {
        if (input == null || input.isEmpty()) return null;

        String raw = input.toLowerCase().trim();
        boolean isReflexive = false;
        String prefix = "";
        String baseVerb = raw;

        // 1. Handle Reflexive (sich ...)
        if (raw.startsWith("sich ")) {
            isReflexive = true;
            baseVerb = raw.substring(5).trim();
        }

        // 2. Handle Separable Prefixes
        for (String p : SEPARABLE_PREFIXES) {
            if (baseVerb.startsWith(p) && baseVerb.length() > p.length() + 1) {
                // Basic check to ensure we don't split words like "antworten" (an-tworten is wrong)
                // This is a simplification; a full dictionary is better but this covers most.
                if (baseVerb.endsWith("en") || baseVerb.endsWith("n")) {
                    prefix = p;
                    baseVerb = baseVerb.substring(p.length());
                    break;
                }
            }
        }

        Conjugation conj = getBaseConjugation(baseVerb);

        // 3. Apply prefix and reflexive pronouns
        return applyModifiers(conj, prefix, isReflexive);
    }

    private static Conjugation getBaseConjugation(String verb) {
        // --- Core Irregulars ---
        if (verb.equals("sein")) return new Conjugation("bin", "bist", "ist", "sind", "seid", "sind");
        if (verb.equals("haben")) return new Conjugation("habe", "hast", "hat", "haben", "habt", "haben");
        if (verb.equals("werden")) return new Conjugation("werde", "wirst", "wird", "werden", "werdet", "werden");
        if (verb.equals("wissen")) return new Conjugation("weiß", "weißt", "weiß", "wissen", "wisst", "wissen");

        // --- Modal Verbs (Pattern 4) ---
        if (verb.equals("können")) return new Conjugation("kann", "kannst", "kann", "können", "könnt", "können");
        if (verb.equals("müssen")) return new Conjugation("muss", "musst", "muss", "müssen", "müsst", "müssen");
        if (verb.equals("wollen")) return new Conjugation("will", "willst", "will", "wollen", "wollt", "wollen");
        if (verb.equals("sollen")) return new Conjugation("soll", "sollst", "soll", "sollen", "sollt", "sollen");
        if (verb.equals("dürfen")) return new Conjugation("darf", "darfst", "darf", "dürfen", "dürft", "dürfen");
        if (verb.equals("mögen")) return new Conjugation("mag", "magst", "mag", "mögen", "mögt", "mögen");

        // --- Stem Extraction ---
        String stem = verb;
        if (verb.endsWith("en")) stem = verb.substring(0, verb.length() - 2);
        else if (verb.endsWith("n")) stem = verb.substring(0, verb.length() - 1);

        String ich = stem + "e";
        String du = stem + "st";
        String er = stem + "t";
        String wir = verb; // Usually same as infinitive
        String ihr = stem + "t";
        String sie = verb;

        // --- Strong Verbs Stem Changes (Pattern 2) ---
        String duStem = stem;
        String erStem = stem;

        // e -> ie (sehen, lesen)
        if (verb.equals("sehen")) { duStem = "sieh"; erStem = "sieh"; }
        else if (verb.equals("lesen")) { duStem = "lies"; erStem = "lies"; }
        else if (verb.equals("empfehlen")) { duStem = "empfiehl"; erStem = "empfiehl"; }
        else if (verb.equals("stehlen")) { duStem = "stiehl"; erStem = "stiehl"; }
        // e -> i (geben, helfen, sprechen, essen, nehmen)
        else if (verb.equals("geben")) { duStem = "gib"; erStem = "gib"; }
        else if (verb.equals("helfen")) { duStem = "hilf"; erStem = "hilf"; }
        else if (verb.equals("sprechen")) { duStem = "sprich"; erStem = "sprich"; }
        else if (verb.equals("essen")) { duStem = "iss"; erStem = "iss"; }
        else if (verb.equals("nehmen")) { duStem = "nimm"; erStem = "nimm"; }
        else if (verb.equals("treffen")) { duStem = "triff"; erStem = "triff"; }
        // a -> ä (fahren, schlafen, waschen, tragen)
        else if (verb.equals("fahren")) { duStem = "fähr"; erStem = "fähr"; }
        else if (verb.equals("schlafen")) { duStem = "schläf"; erStem = "schläf"; }
        else if (verb.equals("waschen")) { duStem = "wäsch"; erStem = "wäsch"; }
        else if (verb.equals("tragen")) { duStem = "träg"; erStem = "träg"; }
        else if (verb.equals("lassen")) { duStem = "läss"; erStem = "läss"; }
        // au -> äu (laufen)
        else if (verb.equals("laufen")) { duStem = "läuf"; erStem = "läuf"; }
        else if (verb.equals("saufen")) { duStem = "säuf"; erStem = "säuf"; }

        du = duStem + "st";
        er = erStem + "t";

        // --- Special Stem Ending Rules ---
        // Stems in -t, -d
        if (stem.endsWith("t") || stem.endsWith("d")) {
            // Exceptions for strong verbs that already changed stem
            if (duStem.equals(stem)) du = stem + "est";
            if (erStem.equals(stem)) er = stem + "et";
            ihr = stem + "et";
        }
        // Stems in -s, -ß, -z, -x (drop 's' in -st)
        if (duStem.endsWith("s") || duStem.endsWith("ß") || duStem.endsWith("z") || duStem.endsWith("x")) {
            du = duStem + "t";
        }

        // Special case for -eln (handeln -> ich handle)
        if (verb.endsWith("eln")) {
            String elnBase = verb.substring(0, verb.length() - 3);
            ich = elnBase + "le";
            wir = verb;
            sie = verb;
        }

        return new Conjugation(ich, du, er, wir, ihr, sie);
    }

    private static Conjugation applyModifiers(Conjugation c, String prefix, boolean isReflexive) {
        if (c == null) return null;

        String suf = prefix.isEmpty() ? "" : " " + prefix;
        
        if (isReflexive) {
            return new Conjugation(
                c.ich + suf + " mich",
                c.du + suf + " dich",
                c.erSieEs + suf + " sich",
                c.wir + suf + " uns",
                c.ihr + suf + " euch",
                c.sieSie + suf + " sich"
            );
        } else if (!prefix.isEmpty()) {
            return new Conjugation(
                c.ich + suf, c.du + suf, c.erSieEs + suf,
                c.wir + suf, c.ihr + suf, c.sieSie + suf
            );
        }
        
        return c;
    }
}
