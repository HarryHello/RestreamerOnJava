package harryhelloo.restreamer.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class Options {
    // 生成 label - value对 的类，用于前端 selection
    private List<OptionPair> options;

    public Options() {
        this.options = new ArrayList<>();
    }

    public void addOption(OptionPair singleOptionPair) {
        options.add(singleOptionPair);
    }

    public void addOption(String label, String value) {
        options.add(new OptionPair(label, value));
    }
}
