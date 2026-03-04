package harryhelloo.restreamer.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 选项对
 *
 * <p>表示一个选项的标签 - 值对，用于下拉选择框等 UI 组件。</p>
 *
 * <h2>数据结构：</h2>
 * <pre>
 * {
 *   "label": "显示文本",
 *   "value": "实际值"
 * }
 * </pre>
 *
 * @author harryhelloo
 * @version 1.0
 * @see Options
 */
@Data
@Builder
@AllArgsConstructor
public class OptionPair {

    /**
     * 选项标签（显示文本）
     */
    private String label;
    
    /**
     * 选项值（实际存储的值）
     */
    private Object value;

}
