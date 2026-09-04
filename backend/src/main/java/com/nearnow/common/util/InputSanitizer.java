package com.nearnow.common.util;

/**
 * Strips control characters (including the null byte, 0x00) from
 * client-supplied text before it ever reaches a database write.
 *
 * Why this exists: Postgres' UTF-8 text columns reject a raw 0x00 byte
 * outright ("invalid byte sequence for encoding UTF8: 0x00"), which
 * surfaced as a 500 error on registration. Whatever client-side bug
 * put that byte there (a buggy autofill plugin, a text-encoding edge
 * case, a stray control character from an input method) isn't ours to
 * fix from here — but a backend that can be crash-looped by one rogue
 * byte in a name field is a bug on OUR side regardless of the client.
 * This is the "never trust client input" defense, applied literally.
 */
public final class InputSanitizer {
    private InputSanitizer() {}

    /**
     * Removes all C0/C1 control characters (0x00-0x1F, 0x7F-0x9F) except
     * plain spaces, then trims. Safe to apply to any free-text field
     * (names, emails, addresses) — none of those legitimately contain
     * control characters.
     */
    public static String clean(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            boolean isControlChar = (c <= 0x1F) || (c >= 0x7F && c <= 0x9F);
            if (!isControlChar) sb.append(c);
        }
        return sb.toString().trim();
    }
}
