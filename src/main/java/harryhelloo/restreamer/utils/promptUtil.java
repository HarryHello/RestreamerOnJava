package harryhelloo.restreamer.utils;

import harryhelloo.restreamer.pojo.Settings;

import java.text.MessageFormat;

public class promptUtil {
    public static String translatePrompt(String text, String sourceLang, String targetLang) {
        Settings settings = Settings.get();
        return MessageFormat.format(
            "Please translate the follow text from {0} to {1} and answer only result: {2}",
            sourceLang, targetLang, text
        );
    }
}
