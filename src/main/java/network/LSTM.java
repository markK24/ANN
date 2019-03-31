package network;

import dictionary.Dictionary;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.RmsProp;
import tools.data.types.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

public class LSTM {
    private MultiLayerNetwork net;

    public LSTM build(MultiLayerConfiguration config) {
        net = new MultiLayerNetwork(config);
        net.init();
        return this;
    }

    public static MultiLayerConfiguration DefaultTextGenLSTMConfiguration(int dictionarySize, int hiddenLayerWidth) {
        return new NeuralNetConfiguration.Builder()
                .seed(new Random().nextInt())
                .biasInit(0)
                .miniBatch(false)
                .updater(new RmsProp(0.001))
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(
                        new org.deeplearning4j.nn.conf.layers.LSTM.Builder()
                                .nIn(dictionarySize)
                                .nOut(hiddenLayerWidth)
                                .activation(Activation.TANH)
                                .build()
                )
                .layer(
                        new RnnOutputLayer.Builder()
                                .nIn(hiddenLayerWidth)
                                .nOut(dictionarySize)
                                .activation(Activation.SOFTMAX)
                                .build()
                )
                .backpropType(BackpropType.TruncatedBPTT)
                .tBPTTLength(150)
                .build();
    }

    public void train(INDArray input, INDArray expected, int epochs, boolean logEpochs) {
        for (int i = 0; i < epochs; i++) {
            if (logEpochs) {
                System.out.println("Epoch " + (i + 1));
            }
            net.fit(input, expected);
        }
    }

    public void train(DataSet dataSet, int epochs, boolean logEpochs) {
        for (int i = 0; i < epochs; i++) {
            if (logEpochs) {
                System.out.println("Epoch " + (i + 1));
            }
            net.fit(dataSet);
        }
    }

    public void train(int epochs, boolean logEpochs, Dictionary<String, Integer> dictionary, Text... texts) {
        int seriesLength = Text.maxLength(texts, Text.PunctuationPolicy.EXCLUDE, true);
        INDArray input = Nd4j.zeros(texts.length, dictionary.getSize(), seriesLength);
        INDArray expected = Nd4j.zeros(texts.length, dictionary.getSize(), seriesLength);
        for (int i = 0; i < texts.length; i++) {
            String[] words = texts[i].toWords(Text.PunctuationPolicy.EXCLUDE, true);
            int j;
            for (j = 0; j < words.length; j++) {
                input.putScalar(new int[]{i, dictionary.translateTo(words[j]), j}, 1);
                expected.putScalar(new int[]{i, (int) dictionary.translateTo(j == words.length - 1 ? texts[i].getTextEndString() : words[j + 1]), j}, 1);
            }
            for (j = words.length; j < seriesLength; j++) {
                input.putScalar(new int[]{i, (int) dictionary.translateTo(texts[i].getTextEndString()), j}, 1);
                expected.putScalar(new int[]{i, (int) dictionary.translateTo(texts[i].getTextEndString()), j}, 1);
            }
        }
        this.train(input, expected, epochs, logEpochs);
    }

    public void save(File file) throws IOException {
        Files.createFile(file.toPath());
        ModelSerializer.writeModel(net, file, true);
    }

    public LSTM load(File file) throws IOException {
        net = ModelSerializer.restoreMultiLayerNetwork(file, true);
        return this;
    }

    public void reset() {
        net.rnnClearPreviousState();
    }

    public INDArray output(INDArray input) {
        return net.rnnTimeStep(input);
    }


}