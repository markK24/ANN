import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.http.conn.HttpHostConnectException;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FicSearch {
    public static void main(String[] args) {
        Scanner r = new Scanner(System.in);
        System.out.print("Id начала поиска: ");
        int id = r.nextInt() - 1;
        System.out.print("Id начала индексации: ");
        int i = r.nextInt();
        System.out.print("Кол-во фанфиков: ");
        int count = r.nextInt() + i;
        i--;
        System.out.print("Загрузить cookies? [y/n] ");
        String cookies = "";
        if (r.next().equals("y")) {
            try {
                cookies = new String(Files.readAllBytes(new File("cookies.conf").toPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.print("Cookies аккаунта для скачивания 18+ фанфиков: ");
            cookies = r.next();
            System.out.print("Сохранить cookies? [y/n] ");
            if (r.next().equals("y")) {
                File conf = new File("cookies.conf");
                PrintWriter writer = null;
                try {
                    writer = new PrintWriter(conf);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                assert writer != null;
                writer.print(cookies);
                writer.flush();
                writer.close();
            }
        }
        System.out.print("Фильтр по жанру: ");
        String jenre = r.next();
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
                    System.out.println("id = " + id);
                    continue;
                }

                r = new Scanner(new File("trainingData/buffer.txt"));
                String x = "";
                while (!x.startsWith("Жанр")) {
                    x = r.nextLine();
                }
                System.out.println("id = " + id + "; " + x);
                if (!x.contains(jenre)) {
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
                while (!x.startsWith("\r\n\r\n\r\n\r\n\r\n") && r.hasNext()) {
                    r.skip("[^\r\n]*");
                    x = r.next();
                }
                r.useDelimiter("\\s");
                StringBuilder s = new StringBuilder();
                x = "";
                while (r.hasNextLine() && !(x.startsWith("Страница фанфика"))) {
                    s.append(x).append("\n");
                    x = r.nextLine();
                }
                r.close();
                if (split(s.deleteCharAt(s.length() - 1).toString()).length < 5000) {
                    i++;
                    Files.deleteIfExists(new File(String.format("trainingData/sample%d.txt", i)).toPath());
                    System.gc();
                    PrintWriter writer = new PrintWriter(new File(String.format("trainingData/sample%d.txt", i)));
                    for (String line :
                            s.toString().split("\n")) {
                        writer.println(line);
                    }
                    writer.flush();
                    writer.close();
                }
                Files.delete(new File("trainingData/buffer.txt").toPath());
            } catch (HttpHostConnectException | IllegalStateException e) {
                id--;
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
