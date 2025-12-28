package ai.ozzu.api.utils;

import org.springframework.data.util.Pair;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

public final class CursorHelper {

    private CursorHelper(){}
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    public static String encode(OffsetDateTime time, UUID id) {
        String raw = time.toString() + "|" + id.toString();
        return ENCODER.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Pair<OffsetDateTime, UUID> decode(String cursor) {
        if (cursor == null) return null;
        String decoded = new String(DECODER.decode(cursor), StandardCharsets.UTF_8);
        String[] parts = decoded.split("\\|", 2);
        return Pair.of(OffsetDateTime.parse(parts[0]), UUID.fromString(parts[1]));
    }
}
