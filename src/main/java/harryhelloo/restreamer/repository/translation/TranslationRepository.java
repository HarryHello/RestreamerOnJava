package harryhelloo.restreamer.repository.translation;

import com.openai.core.http.AsyncStreamResponse;
import harryhelloo.restreamer.pojo.Options;

public interface TranslationRepository {
    Options initModels();
    
    AsyncStreamResponse<String> translate(String text, String sourceLang, String targetLang);
}