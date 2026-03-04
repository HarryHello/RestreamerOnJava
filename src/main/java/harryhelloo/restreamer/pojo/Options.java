package harryhelloo.restreamer.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 选项列表
 * 
 * <p>用于存储下拉选择框的选项列表，通常用于翻译模型选择等场景。</p>
 * 
 * <h2>数据结构：</h2>
 * <pre>
 * {
 *   "options": [
 *     { "label": "模型名称", "value": "模型 ID" }
 *   ]
 * }
 * </pre>
 * 
 * @author harryhelloo
 * @version 1.0
 * @see OptionPair
 */
@Data
@Builder
@AllArgsConstructor
public class Options {
    
    /**
     * 选项列表
     */
    private List<OptionPair> options;

    /**
     * 默认构造函数，初始化选项列表
     */
    public Options() {
        this.options = new ArrayList<>();
    }

    /**
     * 添加单个选项
     * 
     * @param singleOptionPair 选项对
     */
    public void addOption(OptionPair singleOptionPair) {
        options.add(singleOptionPair);
    }

    /**
     * 添加选项
     * 
     * @param label 选项标签（显示文本）
     * @param value 选项值（实际值）
     */
    public void addOption(String label, Object value) {
        options.add(new OptionPair(label, value));
    }

    /**
     * 转换为 Map 格式
     * 
     * <p>便于前端使用，转换为包含 options 键的 Map 对象。</p>
     * 
     * @return Map 格式的选项数据
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("options", options);
        return map;
    }
}
