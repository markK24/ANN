package dictionary;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class Dictionary<T1, T2> implements Serializable {
    private final T1[] lang1;
    private final T2[] lang2;
    private final int size;

    public Dictionary(T1[] lang1, T2[] lang2) {
        if (lang1.length != lang2.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        this.lang1 = lang1;
        this.lang2 = lang2;
        this.size = lang1.length;
    }

    public static <T> Dictionary<T, Integer> getIndexed(T[] lang) {
        Integer[] indexes = new Integer[lang.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }
        return new Dictionary<>(lang, indexes);
    }

    public static String[] getWordSet(String[][] texts) {
        LinkedList<String> wordList = new LinkedList<>();
        for (String[] text : texts) {
            Collections.addAll(wordList, text);
        }
        return getWordSet(wordList.toArray(new String[]{}));
    }

    public static String[] getWordSet(String[] text) {
        LinkedHashSet<String> wordSet = new LinkedHashSet<>();
        Collections.addAll(wordSet, text);
        return wordSet.toArray(new String[]{});
    }

    public T2 translateTo(T1 o) {
        for (T1 t1 : lang1) {
            if (t1.equals(o)) {
                return lang2[Arrays.asList(lang1).indexOf(t1)];
            }
        }
        throw new NoSuchElementException();
    }

    public T1 translateFrom(T2 o) {
        for (T2 t2 : lang2) {
            if (t2.equals(o)) {
                return lang1[Arrays.asList(lang2).indexOf(t2)];
            }
        }
        throw new NoSuchElementException();
    }

    public static void save(Dictionary dict, @NotNull File file) throws IOException {
        Files.deleteIfExists(file.toPath());
        ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file.getPath()));
        os.writeObject(dict);
        os.flush();
        os.close();
    }

    public static Dictionary load(@NotNull File file) throws IOException, ClassNotFoundException {
        return load(file, false);
    }

    public static Dictionary load(@NotNull File file, boolean clean) throws IOException, ClassNotFoundException {
        ObjectInputStream is = new ObjectInputStream(new FileInputStream(file.getPath()));
        Dictionary dict = (Dictionary) is.readObject();
        is.close();
        if (clean) {
            Files.delete(file.toPath());
        }
        return dict;
    }

    public int getSize() {
        return size;
    }
}
