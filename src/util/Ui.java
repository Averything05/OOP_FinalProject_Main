package util;

/**
 * Console message helpers. Each kind of message has its own tag so success,
 * error, information, warning and alert lines look different from each other.
 */
public final class Ui {

    private Ui() {
    }

    public static void success(String message) {
        System.out.println("[SUCCESS] " + message);
    }

    public static void error(String message) {
        System.out.println("[ERROR] " + message);
    }

    public static void info(String message) {
        System.out.println("[INFO] " + message);
    }

    public static void warning(String message) {
        System.out.println("[WARNING] " + message);
    }

    /** Used by the Observer classes for low-balance and large-transaction alerts. */
    public static void alert(String message) {
        System.out.println("[ALERT] " + message);
    }

    public static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    public static void line(int width) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < width; i++) {
            builder.append('-');
        }
        System.out.println(builder);
    }
}
