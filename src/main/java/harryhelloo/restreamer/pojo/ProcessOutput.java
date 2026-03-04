package harryhelloo.restreamer.pojo;

import lombok.Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程输出
 * 
 * <p>封装外部进程的执行结果，包括标准输出、错误输出和退出码。</p>
 * 
 * <h2>数据结构：</h2>
 * <ul>
 *     <li>stdout: 标准输出内容列表</li>
 *     <li>stderr: 错误输出内容列表</li>
 *     <li>exitCode: 进程退出码（0 表示成功）</li>
 * </ul>
 * 
 * <h2>使用示例：</h2>
 * <pre>
 * ProcessOutput output = ProcessOutput.builder()
 *     .stdout(inputStream)
 *     .stderr(errorStream)
 *     .exitCode(0)
 *     .build();
 * </pre>
 * 
 * @author harryhelloo
 * @version 1.0
 */
@Data
public class ProcessOutput {
    
    /**
     * 标准输出内容列表
     */
    private List<String> stdout;
    
    /**
     * 错误输出内容列表
     */
    private List<String> stderr;
    
    /**
     * 进程退出码（0 表示成功）
     */
    private int exitCode;

    /**
     * 构造函数
     * 
     * @param builder 构建器对象
     */
    public ProcessOutput(Builder builder) {
        this.stdout = builder.stdout;
        this.stderr = builder.stderr;
        this.exitCode = builder.exitCode;
    }

    /**
     * 创建构建器
     * 
     * @return Builder 实例
     */
    static public Builder builder() {
        return new Builder();
    }

    /**
     * ProcessOutput 构建器
     */
    public static final class Builder {
        private List<String> stdout = new ArrayList<>();
        private List<String> stderr = new ArrayList<>();
        private int exitCode;

        /**
         * 私有构造函数
         */
        private Builder() {}

        /**
         * 从输入流读取标准输出
         * 
         * @param inputStream 标准输出流
         * @return Builder 实例
         * @throws IOException 读取失败时抛出
         */
        public Builder stdout(InputStream inputStream) throws IOException {
            this.stdout.clear();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                this.stdout.add(reader.readLine());
            }
            return this;
        }

        /**
         * 从输入流读取错误输出
         * 
         * @param inputStream 错误输出流
         * @return Builder 实例
         * @throws IOException 读取失败时抛出
         */
        public Builder stderr(InputStream inputStream) throws IOException {
            this.stderr.clear();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                this.stderr.add(reader.readLine());
            }
            return this;
        }

        /**
         * 设置退出码
         * 
         * @param exitCode 进程退出码
         * @return Builder 实例
         */
        public Builder exitCode(int exitCode) {
            this.exitCode = exitCode;
            return this;
        }

        public ProcessOutput build() {
            return new ProcessOutput(this);
        }
    }
}
