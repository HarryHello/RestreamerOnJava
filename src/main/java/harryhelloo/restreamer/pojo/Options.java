package harryhelloo.restreamer.pojo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Options {
    private List<OptionPair> options;

    public void addOption(OptionPair singleOptionPair) {
        options.add(singleOptionPair);
    }

    public void addOption(String label, String value) {
        options.add(new OptionPair(label, value));
    }
}
