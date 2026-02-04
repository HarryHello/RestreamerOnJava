package harryhelloo.restreamer.pojo;

import lombok.Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessOutput {
    private List<String> stdout;
    private List<String> stderr;
    private int exitCode;

    public ProcessOutput(Builder builder) {
        this.stdout = builder.stdout;
        this.stderr = builder.stderr;
        this.exitCode = builder.exitCode;
    }

    static public Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<String> stdout = new ArrayList<>();
        private List<String> stderr = new ArrayList<>();
        private int exitCode;

        private Builder() {}

        public Builder stdout(InputStream inputStream) throws IOException {
            this.stdout.clear();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                this.stdout.add(reader.readLine());
            }
            return this;
        }

        public Builder stderr(InputStream inputStream) throws IOException {
            this.stderr.clear();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                this.stderr.add(reader.readLine());
            }
            return this;
        }

        public Builder exitCode(int exitCode) {
            this.exitCode = exitCode;
            return this;
        }

        public ProcessOutput build() {
            return new ProcessOutput(this);
        }
    }
}
