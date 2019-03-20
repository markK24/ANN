import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.indexaccum.IMax;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * This example trains a RNN. When trained we only have to put the first
 * character of LEARNSTRING to the RNN, and it will recite the following chars
 *
 * @author Peter Grossmann
 */
public class RNN {

    // define a sentence to learn.
    // Add a special character at the beginning so the RNN learns the complete string and ends with the marker.
    private static final String[][] learnstrings = getLearningText();

    private static String[][] getLearningText() {
        Scanner r = new Scanner(System.in);
        System.out.print("Количество примеров для обучения: ");
        int i = r.nextInt();
        r.close();
        String[] filelist = new String[i];
        for (int j = 0; j < i; j++) {
            filelist[j] = String.format("trainingData/sample%d.txt", j + 1);
        }
        String[][] result = new String[i][];
        for (int j = 0; j < i; j++) {
            try {
                result[j] = split(new String(Files.readAllBytes(new File(filelist[j]).toPath())));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-100);
            }
        }
        return result;
    }

    @NotNull
    private static String[] split(String s) {
        StringBuilder stack = new StringBuilder();
        List<String> result = new ArrayList<>();
        for (char c :
                s.toCharArray()) {
            String cs = String.valueOf(c);
            if (cs.matches("[a-zA-ZабвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧЪЫЬЭЮЯ]")) {
                stack.append(lowerKirill(c));
            } else {
                if (stack.length() != 0) {
                    result.add(stack.toString());
                    stack = new StringBuilder();
                }
                if (!cs.matches("\\s")) {
                    result.add(cs);
                }
            }
        }
        return result.toArray(new String[0]);
    }

    private static char lowerKirill(char c) {
        int index = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧЪЫЬЭЮЯ".indexOf(c);
        return index > 0 ? "абвгдеёжзийклмнопрстуфхцчшщъыьэюя".charAt(index) : c;
    }

    @Contract(pure = true)
    private static int maxLength(String[]... strings) {
        int mx = strings[0].length;
        for (String[] arr: strings) {
            if (arr.length > mx) {
                mx = arr.length;
            }
        }
        return mx;
    }

    @NotNull
    private static String[] concat(String[]... strings) {
        ArrayList<String> result = new ArrayList<>();
        for (String[] ss :
                strings) {
            Collections.addAll(result, ss);
        }
        return result.toArray(new String[0]);
    }

    // a list of all possible characters
    private static final List<String> LEARNSTRING_WORDS_LIST = new ArrayList<>();

    // RNN dimensions
    private static final int HIDDEN_LAYER_WIDTH = maxLength(learnstrings);
    private static final int HIDDEN_LAYER_CONT = 2;
    private static final Random rand = new Random(7894);

    public static void main(String[] args) {

        // create a dedicated list of possible chars in LEARNSTRING_WORDS_LIST
        LinkedHashSet<String> LEARNSTRING_STRINGS = new LinkedHashSet<>(Arrays.asList(concat(learnstrings)));
        LEARNSTRING_STRINGS.add("{endtext}");
        LEARNSTRING_WORDS_LIST.addAll(LEARNSTRING_STRINGS);
        LEARNSTRING_STRINGS = null;

        // some common parameters
        NeuralNetConfiguration.Builder builder = new NeuralNetConfiguration.Builder();
        builder.seed(3853);
        builder.biasInit(0);
        builder.miniBatch(false);
        builder.updater(new RmsProp(0.001));
        builder.weightInit(WeightInit.XAVIER);

        ListBuilder listBuilder = builder.list();

        // first difference, for rnns we need to use LSTM.Builder
        for (int i = 0; i < HIDDEN_LAYER_CONT; i++) {
            LSTM.Builder hiddenLayerBuilder = new LSTM.Builder();
            hiddenLayerBuilder.nIn(i == 0 ? LEARNSTRING_WORDS_LIST.size() : HIDDEN_LAYER_WIDTH);
            hiddenLayerBuilder.nOut(HIDDEN_LAYER_WIDTH);
            // adopted activation function from LSTMCharModellingExample
            // seems to work well with RNNs
            hiddenLayerBuilder.activation(Activation.TANH);
            listBuilder.layer(i, hiddenLayerBuilder.build());
        }

        // we need to use RnnOutputLayer for our RNN
        RnnOutputLayer.Builder outputLayerBuilder = new RnnOutputLayer.Builder(LossFunction.MCXENT);
        // softmax normalizes the output neurons, the sum of all outputs is 1
        // this is required for our sampleFromDistribution-function
        outputLayerBuilder.activation(Activation.SOFTMAX);
        outputLayerBuilder.nIn(HIDDEN_LAYER_WIDTH);
        outputLayerBuilder.nOut(LEARNSTRING_WORDS_LIST.size());
        listBuilder.layer(HIDDEN_LAYER_CONT, outputLayerBuilder.build());

        // finish builder
        listBuilder.pretrain(false);
        listBuilder.backprop(true);

        // create network
        MultiLayerConfiguration conf = listBuilder.build();
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(1));


        /*
         * CREATE OUR TRAINING DATA
         */
        // create input and output arrays: SAMPLE_INDEX, INPUT_NEURON,
        // SEQUENCE_POSITION
        INDArray input = Nd4j.zeros(learnstrings.length, LEARNSTRING_WORDS_LIST.size(), HIDDEN_LAYER_WIDTH);
        INDArray labels = Nd4j.zeros(learnstrings.length, LEARNSTRING_WORDS_LIST.size(), HIDDEN_LAYER_WIDTH);
        // loop through our sample-sentence
        for (int i = 0; i < learnstrings.length; i++) {
            String[] end = new String[HIDDEN_LAYER_WIDTH - learnstrings[i].length];
            Arrays.fill(end, "{endtext}");
            learnstrings[i] = concat(learnstrings[i], end);
            int samplePos = 0;
            for (String currentWord : learnstrings[i]) {
                // small hack: when currentChar is the last, take the first char as
                // nextChar - not really required. Added to this hack by adding a starter first character.
                String nextWord = samplePos < learnstrings[i].length - 1 ? learnstrings[i][(samplePos + 1)] : "{endtext}";
                // input neuron for current-char is 1 at "samplePos"
                input.putScalar(new int[]{i, LEARNSTRING_WORDS_LIST.indexOf(currentWord), samplePos}, 1);
                // output neuron for next-char is 1 at "samplePos"
                labels.putScalar(new int[]{i, LEARNSTRING_WORDS_LIST.indexOf(nextWord), samplePos}, 1);
                samplePos++;
            }
        }
        DataSet trainingData = new DataSet(input, labels);

        // some epochs
        int epoch = 1;
        for (; epoch < 400; epoch++) {
            System.out.println("Epoch " + epoch);

            // train the data
            net.fit(trainingData);
        }

        for (; epoch <= 500; epoch++) {

            System.out.println("Epoch " + epoch);

            // train the data
            net.fit(trainingData);

            // put the first character into the rrn as an initialisation
            for (String[] learnstring :
                    learnstrings) {
                // clear current stance from the last example
                net.rnnClearPreviousState();

                System.out.print(learnstring[0] + " ");
                INDArray testInit = Nd4j.zeros(1, LEARNSTRING_WORDS_LIST.size(), 1);
                testInit.putScalar(LEARNSTRING_WORDS_LIST.indexOf(learnstring[0]), 1);

                // run one step -> IMPORTANT: rnnTimeStep() must be called, not
                // output()
                // the output shows what the net thinks what should come next
                INDArray output = net.rnnTimeStep(testInit);

                // now the net should guess LEARNSTRING.length more characters
                for (String dummy : learnstring) {

                    // first process the last output of the network to a concrete
                    // neuron, the neuron with the highest output has the highest
                    // chance to get chosen
                    int sampledCharacterIdx = Nd4j.getExecutioner().exec(new IMax(output), 1).getInt(0);

                    // print the chosen output
                    System.out.print(LEARNSTRING_WORDS_LIST.get(sampledCharacterIdx));
                    System.out.print(" ");

                    // use the last output as input
                    INDArray nextInput = Nd4j.zeros(1, LEARNSTRING_WORDS_LIST.size(), 1);
                    nextInput.putScalar(sampledCharacterIdx, 1);
                    output = net.rnnTimeStep(nextInput);

                }
                System.out.print("\n");
            }
        }
        Scanner scanner = new Scanner(System.in);
        String in = scanner.next();
        while (!in.equals("STOP")) {

            net.rnnClearPreviousState();

            System.out.print(in + " ");
            INDArray testInit = Nd4j.zeros(1, LEARNSTRING_WORDS_LIST.size(), 1);
            testInit.putScalar(LEARNSTRING_WORDS_LIST.indexOf(in), 1);

            // run one step -> IMPORTANT: rnnTimeStep() must be called, not
            // output()
            // the output shows what the net thinks what should come next
            INDArray output = net.rnnTimeStep(testInit);

            // now the net should guess LEARNSTRING.length more characters
            for (int i = 0; i < HIDDEN_LAYER_WIDTH; i++) {

                // first process the last output of the network to a concrete
                // neuron, the neuron with the highest output has the highest
                // chance to get chosen
                int sampledCharacterIdx = Nd4j.getExecutioner().exec(new IMax(output), 1).getInt(0);

                // print the chosen output
                System.out.print(LEARNSTRING_WORDS_LIST.get(sampledCharacterIdx));
                System.out.print(" ");

                // use the last output as input
                INDArray nextInput = Nd4j.zeros(1, LEARNSTRING_WORDS_LIST.size(), 1);
                nextInput.putScalar(sampledCharacterIdx, 1);
                output = net.rnnTimeStep(nextInput);

            }
            System.out.print("\n");
            in = scanner.next();
        }

        System.out.print("\nСохранить модель? [y/n] ");
        if (scanner.next().equals("y")) {
            System.out.print("\nFilename: ");
            scanner.nextLine();
            String filename = scanner.nextLine();
            try {
                ModelSerializer.writeModel(net, new File(filename), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}