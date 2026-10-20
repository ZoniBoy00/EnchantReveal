import fi.zoniboy.enchantreveal.wire.Protocol;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public final class ProtocolTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void rejected(byte[] bytes) {
        try { Protocol.decodePreview(bytes); throw new AssertionError("Malformed preview accepted"); }
        catch (IOException expected) { }
    }
    public static void main(String[] args) throws Exception {
        var request = new Protocol.Request(17, 4, 123456789L);
        var bytes = Protocol.encodeRequest(request);
        check(bytes.length == 20 && request.equals(Protocol.decodeRequest(bytes)), "Request round trip");
        var preview = new Protocol.Preview(request, List.of(
                new Protocol.Offer(5, List.of(new Protocol.Enchant("minecraft:efficiency", 1))),
                new Protocol.Offer(14, List.of()),
                new Protocol.Offer(30, List.of(new Protocol.Enchant("minecraft:silk_touch", 1),
                        new Protocol.Enchant("minecraft:efficiency", 4), new Protocol.Enchant("minecraft:unbreaking", 3)))));
        bytes = Protocol.encodePreview(preview);
        check(preview.equals(Protocol.decodePreview(bytes)), "All three offers and additional enchants survive transfer");
        for (int length = 0; length < bytes.length; length++) rejected(Arrays.copyOf(bytes, length));
        byte[] badVersion = bytes.clone(); badVersion[3] = 99; rejected(badVersion);
        byte[] badCount = bytes.clone(); Arrays.fill(badCount, 24, 28, (byte) 0xff); rejected(badCount);
        rejected(Arrays.copyOf(bytes, bytes.length + 1));
        rejected(new byte[Protocol.MAX_BYTES + 1]);
        try { Protocol.decodeRequest(new byte[21]); throw new AssertionError("Long request accepted"); }
        catch (IOException expected) { }
        try { new Protocol.Enchant("invalid", 1); throw new AssertionError("Invalid key accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new Protocol.Enchant("minecraft:efficiency", 0); throw new AssertionError("Invalid level accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("PASS: wire round trips, multi-enchant preservation and malformed/truncated/oversized packets");
    }
}
