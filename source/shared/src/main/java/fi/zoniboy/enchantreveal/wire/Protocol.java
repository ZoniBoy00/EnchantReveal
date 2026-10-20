package fi.zoniboy.enchantreveal.wire;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/** Small, versioned protocol shared by the Paper plugin and Fabric client. */
public final class Protocol {
    public static final String REQUEST = "enchantreveal:request";
    public static final String PREVIEW = "enchantreveal:preview";
    public static final int VERSION = 1;
    public static final int REQUEST_BYTES = 20;
    public static final int MAX_BYTES = 16384;

    public record Request(int containerId, int stateId, long nonce) {}
    public record Enchant(String key, int level) {
        public Enchant {
            if (key == null || key.length() > 128 || !key.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                    || level < 1 || level > 255) throw new IllegalArgumentException("Invalid enchantment");
        }
    }
    public record Offer(int requiredLevel, List<Enchant> enchantments) {
        public Offer {
            enchantments = List.copyOf(enchantments);
            if (requiredLevel < 0 || enchantments.size() > 32) throw new IllegalArgumentException("Invalid offer");
        }
    }
    public record Preview(Request request, List<Offer> offers) {
        public Preview {
            offers = List.copyOf(offers);
            if (request == null || offers.size() != 3) throw new IllegalArgumentException("Expected three offers");
        }
    }

    private Protocol() {}

    public static byte[] encodeRequest(Request request) {
        return write(out -> writeHeader(out, request));
    }

    public static Request decodeRequest(byte[] bytes) throws IOException {
        if (bytes.length != REQUEST_BYTES) throw new IOException("Invalid request length");
        return readHeader(new DataInputStream(new ByteArrayInputStream(bytes)));
    }

    public static byte[] encodePreview(Preview preview) {
        return write(out -> {
            writeHeader(out, preview.request());
            for (Offer offer : preview.offers()) {
                out.writeInt(offer.requiredLevel());
                out.writeInt(offer.enchantments().size());
                for (Enchant enchant : offer.enchantments()) {
                    out.writeUTF(enchant.key());
                    out.writeInt(enchant.level());
                }
            }
        });
    }

    public static Preview decodePreview(byte[] bytes) throws IOException {
        if (bytes.length > MAX_BYTES) throw new IOException("Preview too large");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        Request request = readHeader(in);
        List<Offer> offers = new ArrayList<>(3);
        try {
            for (int i = 0; i < 3; i++) {
                int cost = in.readInt();
                int count = in.readInt();
                if (count < 0 || count > 32) throw new IOException("Invalid enchantment count");
                List<Enchant> enchantments = new ArrayList<>(count);
                for (int j = 0; j < count; j++) enchantments.add(new Enchant(in.readUTF(), in.readInt()));
                offers.add(new Offer(cost, enchantments));
            }
            if (in.available() != 0) throw new IOException("Trailing preview bytes");
            return new Preview(request, offers);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid preview", e);
        }
    }

    private static Request readHeader(DataInputStream in) throws IOException {
        if (in.readInt() != VERSION) throw new IOException("Unsupported protocol");
        int containerId = in.readInt();
        int stateId = in.readInt();
        long nonce = in.readLong();
        if (containerId < 0 || stateId < 0) throw new IOException("Invalid menu state");
        return new Request(containerId, stateId, nonce);
    }

    private static void writeHeader(DataOutputStream out, Request request) throws IOException {
        out.writeInt(VERSION);
        out.writeInt(request.containerId());
        out.writeInt(request.stateId());
        out.writeLong(request.nonce());
    }

    @FunctionalInterface
    private interface Writer { void write(DataOutputStream out) throws IOException; }

    private static byte[] write(Writer writer) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            writer.write(new DataOutputStream(bytes));
            if (bytes.size() > MAX_BYTES) throw new IllegalArgumentException("Packet too large");
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
