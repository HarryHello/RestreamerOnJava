package harryhelloo.restreamer.repository.translation.impl;

import com.deepl.api.AuthorizationException;
import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLException;
import com.deepl.api.Usage;
import com.openai.core.http.AsyncStreamResponse;
import harryhelloo.restreamer.exception.DeeplException;
import harryhelloo.restreamer.exception.EmptyConfigException;
import harryhelloo.restreamer.pojo.Options;
import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.repository.translation.DeeplRepository;

public class DeeplRepositoryImpl implements DeeplRepository {
    DeepLClient client;

    private void buildClient() {
        String authKey = Settings.get().getDeeplAuthKey();
        if (authKey == null) {
            throw new EmptyConfigException("DeepL AuthKey is empty");
        }
    }

    @Override
    public Options initModels() {
        try {
            Usage usage = client.getUsage();
        } catch (AuthorizationException e) {
            throw new RuntimeException(e);
        } catch (DeepLException e) {
            throw new DeeplException(e);
        } catch (InterruptedException e) {
            throw new DeeplException("DeepL test was interrupted!", e);
        }
        return null;
    }

    @Override
    public AsyncStreamResponse<String> translate(String text, String sourceLang, String targetLang) {
        return null;
    }

}
