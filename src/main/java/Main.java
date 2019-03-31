import dictionary.Dictionary;
import network.LSTM;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.indexaccum.IMax;
import org.nd4j.linalg.factory.Nd4j;
import tools.data.DataCollector;
import tools.data.types.Text;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner r = new Scanner(System.in);
        String x;

        System.out.print("Количество примеров для обучения: ");
        DataCollector collector = new DataCollector(r.nextInt());
        collector.collect();
        String[][] textStrings = new String[collector.getSamples().length][];
        Text[] texts = collector.getSamples();
        for (int i = 0; i < textStrings.length; i++) {
            textStrings[i] = collector.getSamples()[i].toWords(Text.PunctuationPolicy.EXCLUDE, true);
        }
        Dictionary<String, Integer> dictionary = Dictionary.getIndexed(Dictionary.getWordSet(textStrings));
        System.out.println();

        System.out.println("Выберите вариант:");
        System.out.println("\t1 - Создать модель");
        System.out.println("\t2 - Загрузить модель");
        do {
            x = r.next();
        } while (!x.equals("1") && !x.equals("2"));
        LSTM network = new LSTM();
        switch (Integer.parseInt(x)) {
            case 1:
                network = new LSTM().build(LSTM.DefaultTextGenLSTMConfiguration(dictionary.getSize(), 100));
                System.out.println();

                System.out.println("Training...");
                network.train(200, true, dictionary, texts);
                System.out.println();
                break;
            case 2:
                System.out.println("Путь к файлу с моделью: ");
                try {
                    network = new LSTM().load(new File(r.nextLine()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }

        System.out.print("Введите первое слово: (STOP для прерывания): ");
        x = r.next();
        while (!x.equals("STOP")) {
            network.reset();
            ArrayList<String> words = new ArrayList<>();
            words.add(x);
            while (!x.equals(texts[0].getTextEndString())) {
                INDArray input = Nd4j.zeros(1, dictionary.getSize());
                input.putScalar(new int[]{1, (int) dictionary.translateTo(x)}, 1);
                INDArray output = network.output(input);
                x = String.valueOf(dictionary.translateFrom(Nd4j.getExecutioner().exec(new IMax(output), 1).getInt(0)));
                words.add(x);
            }
            System.out.println(new Text((String[]) words.toArray()).toString());
            System.out.println();
            System.out.print("Введите первое слово: (STOP для прерывания): ");
            x = r.next();
        }
        System.out.println();

        System.out.print("Сохранить модель? {Y/n] ");
        x = r.next();
        if (x.toLowerCase().equals("y")) {
            System.out.println("Путь к файлу: ");
            try {
                r.nextLine();
                x = r.nextLine();
                network.save(new File(x));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
