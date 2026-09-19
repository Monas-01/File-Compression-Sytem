import java.util.*;
import java.io.*;

public class HuffCompression {
    private static StringBuilder sb = new StringBuilder();
    private static Map<Byte, String> huffmap = new HashMap<>();

    // Tracks how many zero-bits were padded onto the last byte
    private static int paddingBits = 0;

    public static void compress(String src, String dst) {
        try {
            // Reset static state for each compression call
            sb = new StringBuilder();
            huffmap = new HashMap<>();
            paddingBits = 0;

            FileInputStream inStream = new FileInputStream(src);
            byte[] b = inStream.readAllBytes();
            byte[] huffmanBytes = createZip(b);
            OutputStream outStream = new FileOutputStream(dst);
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
            objectOutStream.writeObject(huffmanBytes);
            objectOutStream.writeObject(huffmap);
            objectOutStream.writeInt(paddingBits);   // save padding count
            inStream.close();
            objectOutStream.close();
            outStream.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static byte[] createZip(byte[] bytes) {
        if (bytes.length == 0) return new byte[0];
        MinPriorityQueue<ByteNode> nodes = getByteNodes(bytes);
        ByteNode root = createHuffmanTree(nodes);
        Map<Byte, String> huffmanCodes = getHuffCodes(root);
        byte[] huffmanCodeBytes = zipBytesWithCodes(bytes, huffmanCodes);
        return huffmanCodeBytes;
    }

    private static MinPriorityQueue<ByteNode> getByteNodes(byte[] bytes) {
        MinPriorityQueue<ByteNode> nodes = new MinPriorityQueue<ByteNode>();
        Map<Byte, Integer> tempMap = new HashMap<>();
        for (byte b : bytes) {
            Integer value = tempMap.get(b);
            if (value == null)
                tempMap.put(b, 1);
            else
                tempMap.put(b, value + 1);
        }
        for (Map.Entry<Byte, Integer> entry : tempMap.entrySet())
            nodes.add(new ByteNode(entry.getKey(), entry.getValue()));
        return nodes;
    }

    private static ByteNode createHuffmanTree(MinPriorityQueue<ByteNode> nodes) {
        while (nodes.len() > 1) {
            ByteNode left = nodes.poll();
            ByteNode right = nodes.poll();
            ByteNode parent = new ByteNode(null, left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;
            nodes.add(parent);
        }
        return nodes.poll();
    }

    private static Map<Byte, String> getHuffCodes(ByteNode root) {
        if (root == null) return huffmap;
        if (root.data != null) {          // only one distinct byte: the root is a leaf
            huffmap.put(root.data, "0");
            return huffmap;
        }
        getHuffCodes(root.left, "0", sb);
        getHuffCodes(root.right, "1", sb);
        return huffmap;
    }

    private static void getHuffCodes(ByteNode node, String code, StringBuilder sb1) {
        StringBuilder sb2 = new StringBuilder(sb1);
        sb2.append(code);
        if (node != null) {
            if (node.data == null) {
                getHuffCodes(node.left, "0", sb2);
                getHuffCodes(node.right, "1", sb2);
            } else
                huffmap.put(node.data, sb2.toString());
        }
    }


    private static byte[] zipBytesWithCodes(byte[] bytes, Map<Byte, String> huffCodes) {
        StringBuilder strBuilder = new StringBuilder();
        for (byte b : bytes)
            strBuilder.append(huffCodes.get(b));

        // Pad on the RIGHT so the last byte is left-aligned
        int totalBits = strBuilder.length();
        paddingBits = (8 - (totalBits % 8)) % 8;
        for (int p = 0; p < paddingBits; p++)
            strBuilder.append('0');

        byte[] huffCodeBytes = new byte[strBuilder.length() / 8];
        for (int i = 0, idx = 0; i < strBuilder.length(); i += 8, idx++)
            huffCodeBytes[idx] = (byte) Integer.parseInt(strBuilder.substring(i, i + 8), 2);
        return huffCodeBytes;
    }

    public static void decompress(String src, String dst) {
        try {
            FileInputStream inStream = new FileInputStream(src);
            ObjectInputStream objectInStream = new ObjectInputStream(inStream);
            byte[] huffmanBytes = (byte[]) objectInStream.readObject();
            Map<Byte, String> huffmanCodes = (Map<Byte, String>) objectInStream.readObject();
            int padding = objectInStream.readInt();   // read padding count
            byte[] bytes = decomp(huffmanCodes, huffmanBytes, padding);
            OutputStream outStream = new FileOutputStream(dst);
            outStream.write(bytes);
            inStream.close();
            objectInStream.close();
            outStream.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static byte[] decomp(Map<Byte, String> huffmanCodes,
                                byte[] huffmanBytes, int padding) {
        // Convert all bytes to their 8-bit binary string
        StringBuilder sb1 = new StringBuilder();
        for (int i = 0; i < huffmanBytes.length; i++) {
            byte b = huffmanBytes[i];
            // Always force 8-bit representation for every byte
            int val = b & 0xFF;                      // treat as unsigned
            String bits = String.format("%8s",
                    Integer.toBinaryString(val)).replace(' ', '0');
            sb1.append(bits);
        }

        // Strip the padding zeros from the end of the last byte
        if (padding > 0)
            sb1.delete(sb1.length() - padding, sb1.length());

        // Build reverse map: code -> byte
        Map<String, Byte> map = new HashMap<>();
        for (Map.Entry<Byte, String> entry : huffmanCodes.entrySet())
            map.put(entry.getValue(), entry.getKey());

        // Decode bit-by-bit
        List<Byte> list = new ArrayList<>();
        for (int i = 0; i < sb1.length();) {
            int count = 1;
            Byte found = null;
            while (found == null) {
                if (i + count > sb1.length()) break;   // safety: never overrun
                String key = sb1.substring(i, i + count);
                found = map.get(key);
                if (found == null) count++;
            }
            if (found == null) break;                   // reached trimmed end cleanly
            list.add(found);
            i += count;
        }

        byte[] result = new byte[list.size()];
        for (int i = 0; i < result.length; i++)
            result[i] = list.get(i);
        return result;
    }

    // Kept for backward compatibility (unused internally now)
    private static String convertbyteInBit(boolean flag, byte b) {
        int byte0 = b;
        if (flag) byte0 |= 256;
        String str0 = Integer.toBinaryString(byte0);
        if (flag || byte0 < 0)
            return str0.substring(str0.length() - 8);
        else return str0;
    }
}