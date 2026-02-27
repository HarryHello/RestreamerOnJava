package harryhelloo.restreamer.utils;

import java.util.HashMap;
import java.util.Map;

public class LangUtil {
    private static final Map<String, String> SOURCE_LANG_MAPPER = new HashMap<>();

    static {
        SOURCE_LANG_MAPPER.put("en-US", "en");
        SOURCE_LANG_MAPPER.put("en-GB", "en");
        SOURCE_LANG_MAPPER.put("fr-FR", "fr");
        SOURCE_LANG_MAPPER.put("zh-HK", "zh");
    }
}
