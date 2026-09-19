import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class HuffCompressionTest {
    @Test
    void roundTripRestoresOriginal(@TempDir Path dir) throws Exception {
        Path src = dir.resolve("in.txt");
        Path zip = dir.resolve("out.huff");
        Path out = dir.resolve("restored.txt");
        Files.writeString(src, "hello huffman hello world");

        HuffCompression.compress(src.toString(), zip.toString());
        HuffCompression.decompress(zip.toString(), out.toString());

        assertArrayEquals(Files.readAllBytes(src), Files.readAllBytes(out));
    }
    @Test
    void singleDistinctByteRoundTrip(@TempDir Path dir) throws Exception {
        Path src = dir.resolve("in.txt");
        Path zip = dir.resolve("out.huff");
        Path out = dir.resolve("restored.txt");
        Files.writeString(src, "aaaaaaaa");

        HuffCompression.compress(src.toString(), zip.toString());
        HuffCompression.decompress(zip.toString(), out.toString());

        assertArrayEquals(Files.readAllBytes(src), Files.readAllBytes(out));
    }

    @Test
    void emptyFileRoundTrip(@TempDir Path dir) throws Exception {
        Path src = dir.resolve("in.txt");
        Path zip = dir.resolve("out.huff");
        Path out = dir.resolve("restored.txt");
        Files.write(src, new byte[0]);

        HuffCompression.compress(src.toString(), zip.toString());
        HuffCompression.decompress(zip.toString(), out.toString());

        assertArrayEquals(Files.readAllBytes(src), Files.readAllBytes(out));
    }
}

