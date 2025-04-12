package c01;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

public class Futil {
  public static void processDir(String dirName, String resultFileName) {
    Charset cp1250 = Charset.forName("Cp1250");
    Charset utf8 = StandardCharsets.UTF_8;
    try (
        FileChannel outputChannel = FileChannel.open(Paths.get(resultFileName), StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);) {
      outputChannel.truncate(0);
      Files.walkFileTree(Paths.get(dirName), new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (!file.toString().endsWith(".txt"))
            return FileVisitResult.CONTINUE;
          FileChannel inputChannel = FileChannel.open(file, StandardOpenOption.READ);
          ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
          while (inputChannel.read(buffer) > 0) {
            buffer.flip();
            String line = cp1250.decode(buffer).toString();
            buffer.clear();

            buffer = utf8.encode(line);
            outputChannel.write(buffer);
            buffer.clear();
          }
          inputChannel.close();
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}