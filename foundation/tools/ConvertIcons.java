/* Build-time adapter to Android's own SVG-to-vector converter. */
import com.android.ide.common.vectordrawable.Svg2Vector;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class ConvertIcons {
  public static void main(String[] args) throws Exception {
    java.util.logging.Logger.getLogger("com.android.utils.PositionXmlParser").setLevel(java.util.logging.Level.WARNING);
    Path source = Path.of(args[0]), target = Path.of(args[1]);
    Files.createDirectories(target);
    AtomicInteger success = new AtomicInteger(), skipped = new AtomicInteger();
    ExecutorService workers = Executors.newFixedThreadPool(4);
    try (var files = Files.list(source)) {
      files.filter(path -> path.toString().endsWith(".svg")).forEach(path -> workers.submit(() -> {
        String name = path.getFileName().toString().replace(".svg", ".xml");
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
          Svg2Vector.parseSvgToXml(path, output);
          String xml = output.toString(StandardCharsets.UTF_8);
          if (!xml.contains("<path") || !xml.contains("<vector")) { skipped.incrementAndGet(); return; }
          Files.writeString(target.resolve("dot_icon_" + name), xml);
          success.incrementAndGet();
        } catch (Exception failure) { skipped.incrementAndGet(); }
      }));
    }
    workers.shutdown();
    if (!workers.awaitTermination(10, TimeUnit.MINUTES)) throw new IOException("Icon conversion timed out");
    if (success.get() < 9000) throw new IOException("Unexpectedly incomplete icon conversion: " + success);
    System.out.println("Converted " + success + " licensed monochrome vectors; unsupported SVGs skipped: " + skipped);
  }
}
