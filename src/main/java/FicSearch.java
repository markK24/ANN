import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FicSearch {
    public static void main(String[] args) {
        Scanner r = new Scanner(System.in);
        int i = 0;
        System.out.print("Id начала поиска: ");
        int id = r.nextInt() - 1;
        System.out.print("Кол-во фанфиков: ");
        int count = r.nextInt();
        System.out.print("Cookies аккаунта для скачивания 18+ фанфиков: ");
        String cookies = r.next();
        r.close();
        while (i < count) {
            id++;
            ArchiveInputStream archive = null;
            OutputStream file = null;
            try {
                InputStream zip = new FileInputStream(new Downloader().download(new URL(String.format("http://fanfics.me/download.php?fic=%d&format=txt", id)),new File("trainingData/buffer.zip"), cookies));
                archive = new ArchiveStreamFactory().createArchiveInputStream("zip", zip);
                file = new FileOutputStream(new File("trainingData/buffer.txt"));
                archive.getNextEntry();
                IOUtils.copy(archive, file);
                archive.close();
                file.close();
                Files.deleteIfExists(new File("trainingData/buffer.zip").toPath());

                if (Files.size(new File("trainingData/buffer.txt").toPath()) == 0) {
                    Files.deleteIfExists(new File("trainingData/buffer.txt").toPath());
                    continue;
                }

                r = new Scanner(new File("trainingData/buffer.txt"));
                String x = "";
                while (!x.startsWith("Жанр")) {
                    x = r.nextLine();
                }
                if (!x.contains("PWP")) {
                    r.close();
                    Files.deleteIfExists(new File("trainingData/buffer.txt").toPath());
                    continue;
                }
                while (!x.startsWith("Статус")) {
                    x = r.nextLine();
                }
                if (!x.contains("Закончен")) {
                    r.close();
                    Files.deleteIfExists(new File("trainingData/buffer.txt").toPath());
                    continue;
                }
                r.useDelimiter("[^\r\n]");
                while (!x.equals("\r\n\r\n\r\n\r\n\r\n")) {
                    r.skip("[^\r\n]*");
                    x = r.next();
                }
                r.useDelimiter("\\s");
                StringBuilder s = new StringBuilder();
                while (r.hasNextLine()) {
                    s.append(r.nextLine()).append(" ");
                }
                r.close();
                if (split(s.deleteCharAt(s.length() - 1).toString()).length < 5000) {
                    i++;
                    Files.deleteIfExists(new File(String.format("trainingData/sample%d.txt", i)).toPath());
                    System.gc();
                    File f  = new File(String.format("trainingData/sample%d.txt", i));
                    Files.copy(new File("trainingData/buffer.txt").toPath(), f.toPath());
                }
                Files.delete(new File("trainingData/buffer.txt").toPath());
            } catch (IOException | ArchiveException e) {
                e.printStackTrace();
                try {
                    assert archive != null;
                    archive.close();
                    assert file != null;
                    file.close();
                } catch (IOException e1) {
                    e1.initCause(e);
                    e1.printStackTrace();
                }
            }

            System.gc();
        }
    }

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
}
