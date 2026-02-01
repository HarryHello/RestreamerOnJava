package harryhelloo.restreamer.repository.translation;

public interface OllamaRepository {
    String translate(String text, String host, String port, String model, String sourceLang, String targetLang);
}
