package harryhelloo.restreamer.repository.translation;

import harryhelloo.restreamer.pojo.Options;

public interface OpenaiRepository {
    String translate(String text, String apiKey, String baseUrl, String model, String sourceLang, String targetLang);
    Options getModels(String apiKey, String baseUrl);
}
