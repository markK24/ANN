package tools.data;

import tools.data.types.Text;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class DataCollector {
    private final int sampleCount;

    private final Text[] samples;

    public DataCollector(int sampleCount) {
        this.sampleCount = sampleCount;
        samples = new Text[sampleCount];
    }

    public void collect() {
        for (int i = 0; i < sampleCount; i++) {
            File sample = new File(String.format("trainingData/sample%d.txt", i + 1));
            if (sample.exists()) {
                try {
                    samples[i] = new Text(new String(Files.readAllBytes(sample.toPath()), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.err.println(String.format("Sample %d doesn't exist!", i));
            }
        }
    }

    public Text[] getSamples() {
        return samples;
    }
}
