import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class XOR {
    public static void main(String[] args) {
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                .seed(123).optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).activation(Activation.SIGMOID).updater(new Nesterovs(0.9)).weightInit(WeightInit.NORMAL)
                .list()
                .layer(new DenseLayer.Builder().nIn(2).nOut(3).build())
                .layer(new DenseLayer.Builder().nIn(3).nOut(2).hasBias(true).build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).nIn(2).nOut(2).build())
                .pretrain(false).backprop(true)
                .build();

        MultiLayerNetwork MLP = new MultiLayerNetwork(config);
        MLP.init();
        MLP.setListeners(new ScoreIterationListener(1));
        DataSet set = new DataSet(
                Nd4j.create(new double[][] {{0.0, 0.0}, {0.0, 1.0}, {1.0, 0.0}, {1.0, 1.0}}),
                Nd4j.create(new double[][] {{1.0, 0.0}, {0.0, 1.0}, {0.0, 1.0}, {1.0, 0.0}})
        );
        for (int i = 0; i < 10000; i++) {
            System.out.println("i = " + i);
            MLP.fit(set);
        }

        INDArray output = MLP.output(set.getFeatures());

        Evaluation evaluation = new Evaluation(2);
        evaluation.eval(set.getLabels(), output);
        System.out.println(("Accuracy: " + evaluation.accuracy()));
        System.out.println(("Precision: " + evaluation.precision()));
        System.out.println(("Recall: " + evaluation.recall()));
        System.out.println((evaluation.confusionToString()));
        System.out.println(evaluation.stats());
    }
}
