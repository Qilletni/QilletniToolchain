package dev.qilletni.toolchain.logging;

import picocli.CommandLine.Help.Ansi;

/**
 * Utility class for managing terminal color support detection and configuration.
 * Respects NO_COLOR environment variable and provides runtime color enable/disable.
 */
public class ColorSupport {
    private static Boolean colorEnabled = null;
    private static Ansi ansiInstance = null;

    /**
     * Determines if colors should be enabled based on:
     * - NO_COLOR environment variable
     * - Terminal TTY detection
     * - Explicit user configuration
     */
    public static boolean isColorEnabled() {
        if (colorEnabled != null) {
            return colorEnabled;
        }

        // Check NO_COLOR environment variable (https://no-color.org/)
        String noColor = System.getenv("NO_COLOR");
        if (noColor != null && !noColor.isEmpty()) {
            colorEnabled = false;
            return false;
        }

        // Check CI environment
        String ci = System.getenv("CI");
        if ("true".equalsIgnoreCase(ci)) {
            colorEnabled = false;
            return false;
        }

        // Check if stdout is a TTY
        if (System.console() == null || !isTerminal()) {
            colorEnabled = false;
            return false;
        }

        // Default to enabled if terminal supports it
        colorEnabled = true;
        return true;
    }

    /**
     * Check if stdout is connected to a terminal.
     */
    private static boolean isTerminal() {
        var term = System.getenv("TERM");
        return term != null && !term.isEmpty() && !"dumb".equals(term);
    }

    /**
     * Explicitly enable or disable colors.
     */
    public static void setColorEnabled(boolean enabled) {
        colorEnabled = enabled;
        ansiInstance = null; // Reset Ansi instance
    }

    /**
     * Get the Ansi instance for color formatting.
     * Returns ON if colors are explicitly enabled, AUTO for auto-detection, OFF if disabled.
     */
    public static Ansi getAnsi() {
        if (ansiInstance == null) {
            // If explicitly set, use ON or OFF directly
            if (colorEnabled != null) {
                ansiInstance = colorEnabled ? Ansi.ON : Ansi.OFF;
            } else {
                // Otherwise, use AUTO for automatic detection
                ansiInstance = Ansi.AUTO;
            }
        }
        return ansiInstance;
    }

    /**
     * Format a string with color if colors are enabled.
     * Uses Picocli's color markup: @|color text|@
     */
    public static String colorize(String markup) {
        return getAnsi().string(markup);
    }

    /**
     * Apply green color to text (for success messages).
     */
    public static String green(String text) {
        return colorize("@|green " + text + "|@");
    }

    /**
     * Apply red color to text (for error messages).
     */
    public static String red(String text) {
        return colorize("@|red " + text + "|@");
    }

    /**
     * Apply yellow color to text (for warnings).
     */
    public static String yellow(String text) {
        return colorize("@|yellow " + text + "|@");
    }

    /**
     * Apply cyan color to text (for info/actions).
     */
    public static String cyan(String text) {
        return colorize("@|cyan " + text + "|@");
    }

    /**
     * Apply bold formatting to text.
     */
    public static String bold(String text) {
        return colorize("@|bold " + text + "|@");
    }

    /**
     * Apply dim/faint formatting to text.
     */
    public static String dim(String text) {
        return colorize("@|faint " + text + "|@");
    }

    /**
     * Apply bright/bold white to text (for headers).
     */
    public static String brightWhite(String text) {
        return colorize("@|bold,white " + text + "|@");
    }

    /**
     * Apply bright yellow to text (for emphasis).
     */
    public static String brightYellow(String text) {
        return colorize("@|bold,yellow " + text + "|@");
    }
}
