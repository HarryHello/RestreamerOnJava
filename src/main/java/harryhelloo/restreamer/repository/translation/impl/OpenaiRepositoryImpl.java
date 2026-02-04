package harryhelloo.restreamer.repository.translation.impl;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import harryhelloo.restreamer.exception.OpenaiException;
import harryhelloo.restreamer.pojo.Options;
import harryhelloo.restreamer.repository.translation.OpenaiRepository;

import java.text.MessageFormat;

public class OpenaiRepositoryImpl implements OpenaiRepository {
    @Override
    public String translate(String text,
                            String apiKey,
                            String baseUrl,
                            String model,
                            String sourceLang,
                            String targetLang) {
        OpenAIClient client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();

        String userMessage = MessageFormat.format(
            "Please translate the follow text from {0} to {1} and answer only result: {2}",
            sourceLang, targetLang, text
        );
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
            .addUserMessage(userMessage)
            .model(model)
            .build();

        try {
            ChatCompletion chatCompletion = client.chat().completions().create(params);
            return chatCompletion.toString();
        } catch (Exception e) {
            throw new OpenaiException("Failed to translate with OpenAI", e);
        }
    }

    @Override
    public Options getModels(String apiKey, String baseUrl) {
        OpenAIClient client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();
        return null;
    }
}
