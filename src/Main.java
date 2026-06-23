import java.util.*;
import java.io.*;

/**
 * ╔══════════════════════════════════════════════════════════╗
 *   FILE COMPRESSION SYSTEM — Huffman Encoding
 *   Interactive Console Interface
 * ╚══════════════════════════════════════════════════════════╝
 */
public class Main {

    // ── ANSI styling ────────────────────────────────────────────────────────
    static final String RESET   = "\u001B[0m";
    static final String BOLD    = "\u001B[1m";
    static final String DIM     = "\u001B[2m";
    static final String GREEN   = "\u001B[32m";
    static final String CYAN    = "\u001B[36m";
    static final String YELLOW  = "\u001B[33m";
    static final String MAGENTA = "\u001B[35m";
    static final String RED     = "\u001B[31m";
    static final String BLUE    = "\u001B[34m";

    static final Scanner sc = new Scanner(System.in);

    // ── Session state ────────────────────────────────────────────────────────
    static String         lastText     = null;
    static HuffmanEncoder lastEncoder  = null;
    static boolean        isCompressed = false;

    // ════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        clearScreen();
        printBanner();

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = prompt("Enter choice").trim();
            clearScreen();
            switch (choice) {
                case "1" -> menuTextAnalysis();
                case "2" -> menuHuffmanEncoder();
                case "3" -> menuFileCompression();
                case "4" -> menuTreeVisualiser();
                case "5" -> menuAbout();
                case "0" -> running = false;
                default  -> warn("Invalid option. Please enter 0-5.");
            }
        }

        printBox(GREEN, "Thank you for using Huffman File Compressor!", "Goodbye  ♦");
    }

    // ════════════════════════════════════════════════════════════════════════
    // MENU 1 — Text / Message Analysis
    // ════════════════════════════════════════════════════════════════════════
    static void menuTextAnalysis() {
        header("TEXT & FREQUENCY ANALYSIS");

        String text = inputText("Enter text to analyse");
        if (text == null) return;

        Message msg = new Message(text);

        sectionTitle("Basic Statistics");
        row("Total characters",    String.valueOf(msg.getTotalFrequency()));
        row("Distinct characters", String.valueOf(msg.getCharacters().length));
        row("Uncompressed size",   msg.getSize() + " bits  (" + msg.getSize()/8 + " bytes)");

        sectionTitle("Character Frequency Table");
        int[]     freqs  = msg.getFrequencies();
        char[]    chars  = msg.getCharacters();
        int       total  = msg.getTotalFrequency();

        // Sort by frequency descending
        Character[] sorted = new Character[chars.length];
        for (int i = 0; i < chars.length; i++) sorted[i] = chars[i];
        Arrays.sort(sorted, (a, b) -> freqs[b] - freqs[a]);

        tableHeader("Char", "Freq", "Ratio", "Binary (ASCII)", "Bar");
        for (char c : sorted) {
            String display = (c == ' ') ? "SPACE" : (c == '\n') ? "\\n" : String.valueOf(c);
            String binary  = msg.convertBinary((int) c);
            double ratio   = (freqs[c] * 100.0) / total;
            String bar     = CYAN + "█".repeat(Math.min((int)(ratio / 2), 25)) + RESET;
            tableRow(display, String.valueOf(freqs[c]), String.format("%.1f%%", ratio), binary, bar);
        }

        sectionTitle("Binary Representation (first 64 bits)");
        String fullBin = msg.binaryCode();
        String preview = fullBin.length() > 64 ? fullBin.substring(0, 64) + "..." : fullBin;
        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < preview.length(); i++) {
            if (i > 0 && i % 8 == 0) grouped.append(' ');
            grouped.append(preview.charAt(i));
        }
        System.out.println("   " + DIM + grouped + RESET);
        System.out.println("   " + DIM + "Total bits: " + fullBin.length() + RESET);

        lastText     = text;
        lastEncoder  = null;
        isCompressed = false;

        pauseForUser();
    }

    // ════════════════════════════════════════════════════════════════════════
    // MENU 2 — Huffman Encoder
    // ════════════════════════════════════════════════════════════════════════
    static void menuHuffmanEncoder() {
        header("HUFFMAN ENCODER");

        String text;
        if (lastText != null) {
            System.out.println(CYAN + "  Previously entered text is available." + RESET);
            String use = prompt("  Use it? [y/n]").trim().toLowerCase();
            text = use.equals("y") ? lastText : inputText("Enter new text");
        } else {
            text = inputText("Enter text to encode");
        }
        if (text == null) return;

        lastText     = text;
        lastEncoder  = new HuffmanEncoder(text);
        isCompressed = false;

        loading("Building Huffman tree");
        lastEncoder.compress();
        isCompressed = true;
        ok("Encoding complete!");

        boolean back = false;
        while (!back) {
            System.out.println();
            menuOption("1", "Show character codes (Huffman table)");
            menuOption("2", "Show compressed bit-stream preview");
            menuOption("3", "Compression statistics");
            menuOption("0", "Back");
            System.out.println();

            switch (prompt("Choice").trim()) {
                case "1" -> showHuffmanTable();
                case "2" -> showBitStream();
                case "3" -> showCompressionStats();
                case "0" -> back = true;
                default  -> warn("Invalid option.");
            }
        }
    }

    static void showHuffmanTable() {
        sectionTitle("Huffman Code Table");
        tableHeader("Char", "Freq", "Code", "Code Len", "vs 8-bit");
        CharNode trav = lastEncoder.get_charset().getLink();
        while (trav != null) {
            String display   = (trav.ch == ' ') ? "SPACE" : String.valueOf(trav.ch);
            int    saving    = 8 - trav.bit_size.length();
            String savingStr = saving > 0
                    ? GREEN + "-" + saving + " bits" + RESET
                    : saving == 0 ? "same" : RED + "+" + Math.abs(saving) + " bits" + RESET;
            tableRow(display,
                    String.valueOf(trav.frequency),
                    YELLOW + trav.bit_size + RESET,
                    String.valueOf(trav.bit_size.length()),
                    savingStr);
            trav = trav.next;
        }
        pauseForUser();
    }

    static void showBitStream() {
        sectionTitle("Compressed Bit-Stream Preview");
        String[] codes = lastEncoder.compressedBinaryCode();
        int totalBits  = 0;
        for (String c : codes) totalBits += c.length();
        System.out.println("   " + DIM + "Total bits: " + totalBits + "  (~" + (totalBits/8) + " bytes)" + RESET);
        System.out.println();

        String[] palette = {CYAN, YELLOW, MAGENTA, GREEN, BLUE};
        int bitPos = 0, charIdx = 0;
        StringBuilder line = new StringBuilder("   ");
        for (String code : codes) {
            if (bitPos > 320) { // preview cap
                System.out.println(line);
                System.out.println("   " + DIM + "... (stream continues)" + RESET);
                break;
            }
            line.append(palette[charIdx % palette.length]).append(code).append(RESET).append(' ');
            bitPos += code.length();
            charIdx++;
            if (bitPos % 64 < code.length() && bitPos > 0) {
                System.out.println(line);
                line = new StringBuilder("   ");
            }
        }
        if (bitPos <= 320) System.out.println(line);
        pauseForUser();
    }

    static void showCompressionStats() {
        sectionTitle("Compression Statistics");

        int originalBits = new Message(lastText).getSize();

        // Raw bit stream (just encoded data, no table overhead)
        String[] codes = lastEncoder.compressedBinaryCode();
        int rawBits = 0;
        for (String c : codes) rawBits += c.length();
        double rawRatio = ((originalBits - rawBits) / (double) originalBits) * 100;

        // With table overhead (realistic stored size)
        int    totalWithOverhead = lastEncoder.getSizeOfSequence();
        double overheadRatio     = lastEncoder.howMuchCompressed();

        System.out.println("  " + DIM + "Original (uncompressed ASCII)" + RESET);
        row("  Original size", originalBits + " bits  (" + originalBits / 8 + " bytes)");

        System.out.println();
        System.out.println("  " + DIM + "Encoded bit-stream only  (no table stored)" + RESET);
        row("  Encoded bits",  rawBits + " bits");
        String rawCol = rawRatio >= 0 ? GREEN : RED;
        row("  Stream saving", rawCol + String.format("%.2f%%", rawRatio) + RESET);

        System.out.println();
        System.out.println("  " + DIM + "With code table overhead  (realistic stored size)" + RESET);
        row("  Total size",   totalWithOverhead + " bits  (" + totalWithOverhead / 8 + " bytes)");
        String totCol = overheadRatio >= 0 ? GREEN : YELLOW;
        row("  Net saving",   totCol + String.format("%.2f%%", overheadRatio) + RESET
                + DIM + "  (overhead dominates on short texts)" + RESET);

        // Visual bar for raw stream saving
        System.out.println();
        int filled = (int) Math.max(0, rawRatio / 2);
        int empty  = Math.max(0, 50 - filled);
        System.out.print("   Bit-stream saving  [");
        System.out.print(GREEN + "\u2588".repeat(filled) + RESET);
        System.out.print(DIM   + "\u2591".repeat(empty)  + RESET);
        System.out.printf("]  %.1f%%%n", rawRatio);

        System.out.println();
        System.out.println("  " + DIM
                + "Note: Huffman saves space on longer texts with skewed character frequencies."
                + RESET);

        pauseForUser();
    }

    // ════════════════════════════════════════════════════════════════════════
    // MENU 3 — File Compression
    // ════════════════════════════════════════════════════════════════════════
    static void menuFileCompression() {
        header("FILE COMPRESSION  (Byte-Level Huffman)");

        boolean back = false;
        while (!back) {
            menuOption("1", "Compress a file");
            menuOption("2", "Decompress a file");
            menuOption("0", "Back");
            System.out.println();

            switch (prompt("Choice").trim()) {
                case "1" -> compressFile();
                case "2" -> decompressFile();
                case "0" -> back = true;
                default  -> warn("Invalid option.");
            }
        }
    }

    static void compressFile() {
        sectionTitle("Compress File");
        String src = normalizePath(prompt("  Source file path").trim());
        if (!new File(src).exists()) { warn("File not found: " + src); return; }
        String dst = normalizePath(prompt("  Output file path (e.g. output.huff)").trim());

        loading("Compressing");
        long before = System.currentTimeMillis();
        HuffCompression.compress(src, dst);
        long ms = System.currentTimeMillis() - before;

        File dstFile = new File(dst);
        if (!dstFile.exists()) { warn("Compression failed. Check paths."); return; }

        long origSize = new File(src).length();
        long compSize = dstFile.length();
        double ratio  = (1.0 - (double) compSize / origSize) * 100.0;

        ok("Done in " + ms + " ms");
        System.out.println();
        row("Original size",   origSize + " bytes");
        row("Compressed size", compSize + " bytes");
        row("Space saved",     GREEN + String.format("%.2f%%", ratio) + RESET);
        row("Output saved to", dst);
        pauseForUser();
    }

    static void decompressFile() {
        sectionTitle("Decompress File");
        String src = normalizePath(prompt("  Compressed file path (.huff)").trim());
        if (!new File(src).exists()) { warn("File not found: " + src); return; }
        String dst = normalizePath(prompt("  Restore to path (e.g. restored.txt)").trim());

        loading("Decompressing");
        long before = System.currentTimeMillis();
        HuffCompression.decompress(src, dst);
        long ms = System.currentTimeMillis() - before;

        File dstFile = new File(dst);
        if (!dstFile.exists()) { warn("Decompression failed. Check paths."); return; }

        ok("Done in " + ms + " ms");
        row("Restored file", dst);
        row("Restored size", dstFile.length() + " bytes");
        pauseForUser();
    }

    // ════════════════════════════════════════════════════════════════════════
    // MENU 4 — Tree Visualiser
    // ════════════════════════════════════════════════════════════════════════
    static void menuTreeVisualiser() {
        header("HUFFMAN TREE VISUALISER");

        String text;
        if (lastText != null) {
            System.out.println(CYAN + "  Previously entered text is available." + RESET);
            String use = prompt("  Use it? [y/n]").trim().toLowerCase();
            text = use.equals("y") ? lastText : inputText("Enter text");
        } else {
            text = inputText("Enter text to build tree from");
        }
        if (text == null) return;

        Message     msg  = new Message(text);
        HuffmanTree tree = new HuffmanTree(msg);

        boolean back = false;
        while (!back) {
            System.out.println();
            menuOption("1", "In-Order traversal");
            menuOption("2", "Pre-Order traversal");
            menuOption("3", "Post-Order traversal");
            menuOption("4", "Level-Order traversal");
            menuOption("5", "ASCII tree diagram");
            menuOption("0", "Back");
            System.out.println();

            switch (prompt("Choice").trim()) {
                case "1" -> { sectionTitle("In-Order (L → Root → R)");
                    System.out.print("   "); tree.inOrder();    pauseForUser(); }
                case "2" -> { sectionTitle("Pre-Order (Root → L → R)");
                    System.out.print("   "); tree.preOrder();   pauseForUser(); }
                case "3" -> { sectionTitle("Post-Order (L → R → Root)");
                    System.out.print("   "); tree.postOrder();  pauseForUser(); }
                case "4" -> { sectionTitle("Level-Order (BFS)");
                    System.out.print("   "); tree.levelOrder(); pauseForUser(); }
                case "5" -> { sectionTitle("ASCII Tree Diagram");
                    System.out.println(DIM + "   [leaf: char | f=freq]   (internal: f=freq)" + RESET);
                    System.out.println();
                    printAsciiTree(tree.getRoot(), "", true);
                    pauseForUser(); }
                case "0" -> back = true;
                default  -> warn("Invalid option.");
            }
        }
    }

    static void printAsciiTree(Node node, String prefix, boolean isLeft) {
        if (node == null) return;
        boolean isLeaf = node.isLeaf();
        String  branch = isLeft ? "├── " : "└── ";
        String  label  = isLeaf
                ? YELLOW + "[" + displayChar(node.character) + " | f=" + node.frequency + "]" + RESET
                : CYAN   + "(f=" + node.frequency + ")" + RESET;
        System.out.println("   " + prefix + branch + label);
        String childPrefix = prefix + (isLeft ? "│   " : "    ");
        if (!isLeaf) {
            printAsciiTree(node.left,  childPrefix, true);
            printAsciiTree(node.right, childPrefix, false);
        }
    }

    static String displayChar(char c) {
        return switch (c) {
            case ' '  -> "SPC";
            case '\n' -> "\\n";
            case '\0' -> "NIL";
            default   -> String.valueOf(c);
        };
    }

    // ════════════════════════════════════════════════════════════════════════
    // MENU 5 — About
    // ════════════════════════════════════════════════════════════════════════
    static void menuAbout() {
        header("ABOUT THIS PROJECT");

        System.out.println(BOLD + "  Huffman File Compression System" + RESET);
        System.out.println(DIM  + "  DSA Final Semester Project\n"   + RESET);

        sectionTitle("How Huffman Encoding Works");
        System.out.println("  1. " + CYAN + "Frequency Analysis " + RESET + " — count how often each character appears.");
        System.out.println("  2. " + CYAN + "Min-Priority Queue " + RESET + " — insert (char, freq) nodes into min-heap.");
        System.out.println("  3. " + CYAN + "Tree Construction  " + RESET + " — merge the two smallest nodes repeatedly.");
        System.out.println("  4. " + CYAN + "Code Assignment    " + RESET + " — left edge = 0, right edge = 1.");
        System.out.println("  5. " + CYAN + "Encoding           " + RESET + " — replace every char with its variable-length code.");
        System.out.println("  6. " + CYAN + "Decoding           " + RESET + " — walk the tree bit-by-bit to recover original.");

        sectionTitle("Data Structures Used");
        dsRow("MinPriorityQueue", "Custom min-heap — core of tree construction");
        dsRow("SinglyLinkedList", "Backing store for QueueLL");
        dsRow("QueueLL",          "BFS queue for level-order traversal");
        dsRow("CharLinkedList",   "Stores char-to-Huffman-code mapping");
        dsRow("Node / ByteNode",  "Tree nodes (char-level and byte-level)");
        dsRow("Message",          "Wraps input; builds frequency table");
        dsRow("HuffmanTree",      "Constructs and exposes the Huffman tree");
        dsRow("HuffmanEncoder",   "Walks tree; produces compressed codes");
        dsRow("HuffCompression",  "Byte-level file compress / decompress");

        sectionTitle("Complexity");
        dsRow("Tree build",   "O(n log n)  where n = distinct characters");
        dsRow("Encoding",     "O(m)        where m = total characters in text");
        dsRow("Decoding",     "O(m log n)  tree traversal per output character");

        pauseForUser();
    }

    // ════════════════════════════════════════════════════════════════════════
    // UI HELPERS
    // ════════════════════════════════════════════════════════════════════════

    static void printBanner() {
        System.out.println(CYAN + BOLD);
        System.out.println("  ██╗  ██╗██╗   ██╗███████╗███████╗███╗   ███╗ █████╗ ███╗  ██╗");
        System.out.println("  ██║  ██║██║   ██║██╔════╝██╔════╝████╗ ████║██╔══██╗████╗ ██║");
        System.out.println("  ███████║██║   ██║█████╗  █████╗  ██╔████╔██║███████║██╔██╗██║");
        System.out.println("  ██╔══██║██║   ██║██╔══╝  ██╔══╝  ██║╚██╔╝██║██╔══██║██║╚████║");
        System.out.println("  ██║  ██║╚██████╔╝██║     ██║     ██║ ╚═╝ ██║██║  ██║██║ ╚███║");
        System.out.println("  ╚═╝  ╚═╝ ╚═════╝ ╚═╝     ╚═╝     ╚═╝     ╚═╝╚═╝  ╚═╝╚═╝  ╚══╝");
        System.out.println(RESET);
        System.out.println(YELLOW + "          FILE COMPRESSION SYSTEM  •  DSA Final Project" + RESET);
        System.out.println(DIM    + "          Huffman Encoding / Decoding Engine\n"           + RESET);
    }

    static void printMainMenu() {
        System.out.println(BOLD + CYAN + "  ┌─────────────────────────────────────────────┐" + RESET);
        System.out.println(BOLD + CYAN + "  │                 MAIN  MENU                  │" + RESET);
        System.out.println(BOLD + CYAN + "  └─────────────────────────────────────────────┘" + RESET);
        menuOption("1", "Text & Frequency Analysis");
        menuOption("2", "Huffman Encoder  (text -> binary codes)");
        menuOption("3", "File Compression  (compress / decompress files)");
        menuOption("4", "Huffman Tree Visualiser  (traversals + diagram)");
        menuOption("5", "About this project");
        menuOption("0", "Exit");
        System.out.println();
    }

    static void menuOption(String key, String label) {
        System.out.println("  " + YELLOW + BOLD + "[" + key + "]" + RESET + "  " + label);
    }

    static void header(String title) {
        clearScreen();
        printBanner();
        int pad = Math.max(0, (54 - title.length()) / 2);
        String line = "=".repeat(56);
        System.out.println(CYAN + BOLD + "  +" + line + "+");
        System.out.printf ("  |%" + (pad + title.length()) + "s%" + Math.max(0, 56 - pad - title.length()) + "s|%n", title, "");
        System.out.println("  +" + line + "+" + RESET + "\n");
    }

    static void sectionTitle(String t) {
        System.out.println("\n" + MAGENTA + BOLD + "  > " + t + RESET);
        System.out.println(DIM + "  " + "-".repeat(50) + RESET);
    }

    static void row(String key, String value) {
        System.out.printf("  %-24s %s%s%s%n", key, BOLD, value, RESET);
    }

    static void dsRow(String name, String desc) {
        System.out.printf("  " + YELLOW + "%-22s" + RESET + " %s%n", name, desc);
    }

    static void tableHeader(String... cols) {
        System.out.println();
        StringBuilder sb = new StringBuilder("  ");
        for (String c : cols) sb.append(String.format(BOLD + "%-16s" + RESET, c));
        System.out.println(sb);
        System.out.println(DIM + "  " + "-".repeat(cols.length * 16) + RESET);
    }

    static void tableRow(String... cols) {
        StringBuilder sb = new StringBuilder("  ");
        for (String c : cols) {
            String plain = c.replaceAll("\u001B\\[[;\\d]*m", "");
            int pad = Math.max(0, 16 - plain.length());
            sb.append(c).append(" ".repeat(pad));
        }
        System.out.println(sb);
    }

    static void printBox(String colour, String... lines) {
        int w = Arrays.stream(lines).mapToInt(String::length).max().orElse(20) + 4;
        String border = "=".repeat(w);
        System.out.println("\n" + colour + BOLD + "  +" + border + "+");
        for (String l : lines)
            System.out.printf("  |  %-" + (w - 2) + "s  |%n", l);
        System.out.println("  +" + border + "+" + RESET + "\n");
    }

    static String prompt(String label) {
        System.out.print("\n  " + CYAN + "> " + label + ": " + RESET);
        return sc.nextLine();
    }

    static String inputText(String label) {
        System.out.println("\n  " + CYAN + "> " + label + RESET);
        System.out.println(DIM + "    (press Enter twice when done)" + RESET);
        StringBuilder sb = new StringBuilder();
        String line;
        while (!(line = sc.nextLine()).isEmpty())
            sb.append(sb.length() > 0 ? "\n" : "").append(line);
        String text = sb.toString().trim();
        if (text.isEmpty()) { warn("No text entered."); return null; }
        return text;
    }

    static void loading(String msg) {
        System.out.print("\n  " + DIM + "  " + msg + "..." + RESET);
        System.out.flush();
        try { Thread.sleep(400); } catch (InterruptedException ignored) {}
        System.out.println("  " + GREEN + "Done!" + RESET);
    }

    static void ok(String msg) {
        System.out.println("  " + GREEN + BOLD + "[OK]  " + msg + RESET);
    }

    static void warn(String msg) {
        System.out.println("\n  " + RED + BOLD + "[!]  " + msg + RESET);
        pauseForUser();
    }

    static void pauseForUser() {
        System.out.print("\n  " + DIM + "Press Enter to continue..." + RESET);
        sc.nextLine();
        clearScreen();
        printBanner();
    }

    /** Handles Windows backslash paths and strips surrounding quotes. */
    static String normalizePath(String path) {
        // Remove surrounding quotes (e.g. if user dragged a file into terminal)
        if ((path.startsWith("\"") && path.endsWith("\"")) ||
                (path.startsWith("'")  && path.endsWith("'")))
            path = path.substring(1, path.length() - 1);
        // Normalize backslashes to forward slashes — Java File accepts both on Windows
        return path.replace('\\', '/');
    }

    static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}