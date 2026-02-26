package harryhelloo.restreamer.repository.translation;

import harryhelloo.restreamer.pojo.Options;

public interface OllamaRepository {
    String translate(String text, String sourceLang, String targetLang);

    Options getModels();
}
