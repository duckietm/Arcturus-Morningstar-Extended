package com.eu.habbo.networking.gameserver.chat;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.networking.gameserver.GameServerAttributes;
import com.eu.habbo.networking.gameserver.auth.AccessTokenService;
import com.eu.habbo.networking.gameserver.auth.AuthRateLimiter;
import com.eu.habbo.networking.gameserver.auth.CorsOriginGate;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Voice messages for the room chat.
 *
 * POST {base}            body = the recorded clip (audio/webm, audio/ogg or audio/mp4), header X-Voice-Duration = seconds.
 *                        Needs the player's bearer token (same token as the badge API). Answers {id, url, duration, seconds}.
 * GET  {base}/{id}       streams the clip (public: the token travels inside the chat message).
 *
 * Clips live under `chat.voice.path` (default data/chat-voice) and are pruned after `chat.voice.keep.hours`
 * (default 48), never more than chat.voice.max.storage.mb (default 512 MB) in total. The base path is served both as /api/chat/voice and, because the gateway only forwards a fixed
 * set of /api prefixes to the emulator, as /api/badges/voice.
 */
public class ChatVoiceHttpHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatVoiceHttpHandler.class);

    public static final String[] BASE_PATHS = {"/api/chat/voice", "/api/badges/voice"};

    private static final int MAX_BODY_BYTES = 400 * 1024;
    private static final int MAX_SECONDS_DEFAULT = 20;
    private static final int UPLOADS_PER_MINUTE = 6;
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9]{16,40}$");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private static final Map<Integer, long[]> UPLOAD_WINDOWS = new ConcurrentHashMap<>();
    private static volatile long lastPrune = 0L;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest req)) {
            super.channelRead(ctx, msg);
            return;
        }

        String path = new QueryStringDecoder(req.uri()).path();
        String base = matchBase(path);
        if (base == null) {
            super.channelRead(ctx, msg);
            return;
        }

        try {
            handle(ctx, req, path, base);
        } finally {
            ReferenceCountUtil.release(req);
        }
    }

    private static String matchBase(String path) {
        for (String base : BASE_PATHS) {
            if (path.equals(base) || path.startsWith(base + "/")) return base;
        }
        return null;
    }

    private void handle(ChannelHandlerContext ctx, FullHttpRequest req, String path, String base) {
        if (req.method() == HttpMethod.OPTIONS) {
            sendCors(ctx, req);
            return;
        }

        if (!isEnabled()) {
            sendJson(ctx, req, HttpResponseStatus.NOT_FOUND, error("Voice messages are disabled."));
            return;
        }

        String trailing = path.length() > base.length() ? path.substring(base.length() + 1) : "";

        try {
            if (trailing.isEmpty()) {
                if (req.method() == HttpMethod.POST) {
                    handleUpload(ctx, req, base);
                    return;
                }
                sendJson(ctx, req, HttpResponseStatus.METHOD_NOT_ALLOWED, error("Use POST."));
                return;
            }

            if (req.method() == HttpMethod.GET || req.method() == HttpMethod.HEAD) {
                handleDownload(ctx, req, trailing);
                return;
            }
            sendJson(ctx, req, HttpResponseStatus.METHOD_NOT_ALLOWED, error("Use GET."));
        } catch (Exception e) {
            LOGGER.error("[chat/voice] unexpected error path=" + path, e);
            sendJson(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR, error("Server error."));
        }
    }

    private void handleUpload(ChannelHandlerContext ctx, FullHttpRequest req, String base) throws IOException {
        int userId = authenticate(req);
        if (userId == 0) {
            sendJson(ctx, req, HttpResponseStatus.UNAUTHORIZED, error("Authentication required."));
            return;
        }

        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(userId);
        if (habbo == null || habbo.getHabboInfo().getCurrentRoom() == null) {
            sendJson(ctx, req, HttpResponseStatus.CONFLICT, error("You must be in a room to send a voice message.", "must_be_in_room"));
            return;
        }

        if (!habbo.getHabboStats().allowTalk()) {
            sendJson(ctx, req, HttpResponseStatus.FORBIDDEN, error("You are muted.", "muted"));
            return;
        }

        if (!allowUpload(userId)) {
            sendJson(ctx, req, HttpResponseStatus.TOO_MANY_REQUESTS, error("Too many voice messages, wait a moment.", "rate_limited"));
            return;
        }

        int size = req.content().readableBytes();
        if (size <= 0) {
            sendJson(ctx, req, HttpResponseStatus.BAD_REQUEST, error("Empty recording.", "empty"));
            return;
        }
        if (size > MAX_BODY_BYTES) {
            sendJson(ctx, req, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, error("Recording too large.", "too_large"));
            return;
        }

        byte[] bytes = new byte[size];
        req.content().getBytes(req.content().readerIndex(), bytes);

        // The declared Content-Type is only a hint: the container is identified from the bytes themselves, and
        // anything that is not one of the four accepted audio containers is refused before it touches the disk.
        String declared = String.valueOf(req.headers().get(HttpHeaderNames.CONTENT_TYPE, "")).toLowerCase(Locale.ROOT);
        String extension = sniffAudioContainer(bytes);
        if (extension == null || (extensionFor(declared) != null && !extensionFor(declared).equals(extension))) {
            sendJson(ctx, req, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE, error("Unsupported audio format.", "invalid_audio"));
            return;
        }

        Path dir = storageDir();
        Files.createDirectories(dir);
        if (!hasStorageRoom(dir, size)) {
            sendJson(ctx, req, HttpResponseStatus.INSUFFICIENT_STORAGE, error("Voice storage is full, try again later.", "storage_full"));
            return;
        }

        int maxSeconds = maxSeconds();
        int seconds;
        try {
            seconds = Integer.parseInt(String.valueOf(req.headers().get("X-Voice-Duration", "0")).trim());
        } catch (NumberFormatException e) {
            seconds = 0;
        }
        if (seconds < 1) seconds = 1;
        if (seconds > maxSeconds) {
            sendJson(ctx, req, HttpResponseStatus.BAD_REQUEST, error("Recording longer than " + maxSeconds + " seconds.", "too_long"));
            return;
        }

        String id = newId();
        Path target = dir.resolve(id + "." + extension);
        Path temp = dir.resolve(id + ".part");

        Files.write(temp, bytes);
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

        pruneOld(dir);

        JsonObject ok = new JsonObject();
        ok.addProperty("id", id);
        ok.addProperty("url", base + "/" + id);
        ok.addProperty("seconds", seconds);
        ok.addProperty("bytes", size);
        ok.addProperty("token", "[voice:" + id + ":" + seconds + "]");
        sendJson(ctx, req, HttpResponseStatus.CREATED, ok);

        LOGGER.debug("[chat/voice] {} uploaded {} ({} bytes, {}s)", habbo.getHabboInfo().getUsername(), id, size, seconds);
    }

    private void handleDownload(ChannelHandlerContext ctx, FullHttpRequest req, String id) throws IOException {
        int dot = id.indexOf('.');
        if (dot >= 0) id = id.substring(0, dot);

        if (!ID_PATTERN.matcher(id).matches()) {
            sendJson(ctx, req, HttpResponseStatus.BAD_REQUEST, error("Invalid id."));
            return;
        }

        String ip = resolveClientIp(ctx, req);
        if (!AuthRateLimiter.tryProbe(ip)) {
            sendJson(ctx, req, HttpResponseStatus.TOO_MANY_REQUESTS, error("Too many requests."));
            return;
        }

        Path dir = storageDir();
        pruneOld(dir);
        Path file = null;
        String type = null;
        for (String extension : new String[]{"webm", "ogg", "m4a", "mp3"}) {
            Path candidate = dir.resolve(id + "." + extension);
            if (Files.isRegularFile(candidate)) {
                file = candidate;
                type = mimeFor(extension);
                break;
            }
        }

        if (file == null) {
            sendJson(ctx, req, HttpResponseStatus.NOT_FOUND, error("Voice message not found or expired.", "not_found"));
            return;
        }

        byte[] bytes = Files.readAllBytes(file);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                req.method() == HttpMethod.HEAD ? Unpooled.EMPTY_BUFFER : Unpooled.wrappedBuffer(bytes));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, type);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "public, max-age=86400, immutable");
        response.headers().set(HttpHeaderNames.ACCEPT_RANGES, "none");
        // Never let a browser reinterpret a clip as anything but audio, and never offer it as a download.
        response.headers().set("X-Content-Type-Options", "nosniff");
        response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, "inline");
        response.headers().set("Content-Security-Policy", "default-src 'none'; sandbox");
        applyCors(req, response);
        boolean keepAlive = isKeepAlive(req);
        if (keepAlive) response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        var future = ctx.writeAndFlush(response);
        if (!keepAlive) future.addListener(ChannelFutureListener.CLOSE);
    }

    // ---------------------------------------------------------------- policy

    private static boolean isEnabled() {
        return Emulator.getConfig() == null || Emulator.getConfig().getBoolean("chat.voice.enabled", true);
    }

    private static int maxSeconds() {
        int value = Emulator.getConfig() == null ? MAX_SECONDS_DEFAULT : Emulator.getConfig().getInt("chat.voice.max.seconds", MAX_SECONDS_DEFAULT);
        return Math.max(1, Math.min(60, value));
    }

    private static Path storageDir() {
        String configured = Emulator.getConfig() == null ? "" : Emulator.getConfig().getValue("chat.voice.path", "");
        return Paths.get(configured == null || configured.isBlank() ? "data/chat-voice" : configured).toAbsolutePath();
    }

    /** A sliding minute per player: at most UPLOADS_PER_MINUTE clips. */
    private static boolean allowUpload(int userId) {
        long now = System.currentTimeMillis();
        long[] window = UPLOAD_WINDOWS.computeIfAbsent(userId, ignored -> new long[UPLOADS_PER_MINUTE]);
        synchronized (window) {
            int oldest = 0;
            for (int i = 1; i < window.length; i++) if (window[i] < window[oldest]) oldest = i;
            if (now - window[oldest] < 60_000L) return false;
            window[oldest] = now;
            return true;
        }
    }

    /**
     * Identifies the audio container from its first bytes; null for anything else (images, scripts, archives…).
     * WebM/Matroska = EBML header, Ogg = "OggS", MP4/M4A = "ftyp" box at offset 4, MP3 = ID3 tag or a frame sync.
     */
    static String sniffAudioContainer(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;

        if ((bytes[0] & 0xFF) == 0x1A && (bytes[1] & 0xFF) == 0x45 && (bytes[2] & 0xFF) == 0xDF && (bytes[3] & 0xFF) == 0xA3) return "webm";
        if (bytes[0] == 'O' && bytes[1] == 'g' && bytes[2] == 'g' && bytes[3] == 'S') return "ogg";
        if (bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p') return "m4a";
        if (bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3') return "mp3";
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xE0) == 0xE0 && (bytes[1] & 0x06) != 0) return "mp3";

        return null;
    }

    /** Total size of the clip folder must stay under chat.voice.max.storage.mb (default 512). */
    private static boolean hasStorageRoom(Path dir, int incoming) {
        int maxMb = Emulator.getConfig() == null ? 512 : Emulator.getConfig().getInt("chat.voice.max.storage.mb", 512);
        long limit = Math.max(16L, maxMb) * 1024L * 1024L;

        try (var stream = Files.list(dir)) {
            long total = stream.filter(Files::isRegularFile).mapToLong(file -> {
                try {
                    return Files.size(file);
                } catch (IOException e) {
                    return 0L;
                }
            }).sum();

            if (total + incoming <= limit) return true;

            // over the cap: drop the oldest quarter and check once more
            lastPrune = 0L;
            pruneOld(dir);
            return total / 4 * 3 + incoming <= limit;
        } catch (IOException e) {
            return false;
        }
    }

    /** Deletes clips older than chat.voice.keep.hours (default 48) plus stale uploads; runs at most once every 10 minutes. */
    private static void pruneOld(Path dir) {
        long now = System.currentTimeMillis();
        if (now - lastPrune < 600_000L) return;
        lastPrune = now;

        int keepHours = Emulator.getConfig() == null ? 48 : Emulator.getConfig().getInt("chat.voice.keep.hours", 48);
        long cutoff = now - Math.max(1, keepHours) * 3_600_000L;
        long partCutoff = now - 600_000L;

        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                try {
                    long modified = Files.getLastModifiedTime(file).toMillis();
                    boolean stalePart = file.getFileName().toString().endsWith(".part") && modified < partCutoff;

                    if (modified < cutoff || stalePart) Files.deleteIfExists(file);
                } catch (IOException ignored) {
                    // best effort
                }
            });
        } catch (IOException e) {
            LOGGER.debug("[chat/voice] prune failed", e);
        }
    }

    private static String newId() {
        StringBuilder builder = new StringBuilder(24);
        for (int i = 0; i < 24; i++) builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        return builder.toString();
    }

    private static String extensionFor(String contentType) {
        if (contentType.startsWith("audio/webm") || contentType.startsWith("video/webm")) return "webm";
        if (contentType.startsWith("audio/ogg")) return "ogg";
        if (contentType.startsWith("audio/mp4") || contentType.startsWith("audio/aac") || contentType.startsWith("audio/x-m4a")) return "m4a";
        if (contentType.startsWith("audio/mpeg")) return "mp3";
        return null;
    }

    private static String mimeFor(String extension) {
        return switch (extension) {
            case "ogg" -> "audio/ogg";
            case "m4a" -> "audio/mp4";
            case "mp3" -> "audio/mpeg";
            default -> "audio/webm";
        };
    }

    // ---------------------------------------------------------------- http plumbing (same shape as BadgeHttpHandler)

    private static int authenticate(FullHttpRequest req) {
        String header = req.headers().get(HttpHeaderNames.AUTHORIZATION);
        if (header == null || header.isEmpty()) return 0;
        String token = header.startsWith("Bearer ") ? header.substring(7).trim() : header.trim();
        return AccessTokenService.verify(token);
    }

    private static JsonObject error(String message) {
        return error(message, null);
    }

    private static JsonObject error(String message, String code) {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", message);
        if (code != null) obj.addProperty("code", code);
        return obj;
    }

    private static void sendJson(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status, JsonObject body) {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(bytes));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        applyCors(req, response);
        boolean keepAlive = isKeepAlive(req);
        if (keepAlive) response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        var future = ctx.writeAndFlush(response);
        if (!keepAlive) future.addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendCors(ChannelHandlerContext ctx, FullHttpRequest req) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
        applyCors(req, response);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void applyCors(FullHttpRequest req, FullHttpResponse response) {
        response.headers().set("Vary", "Origin");
        String origin = req.headers().get(HttpHeaderNames.ORIGIN);
        if (origin != null && !origin.isEmpty() && CorsOriginGate.isAllowed(req)) {
            response.headers().set("Access-Control-Allow-Origin", origin);
            response.headers().set("Access-Control-Allow-Credentials", "true");
            response.headers().set("Access-Control-Allow-Methods", "GET, HEAD, POST, OPTIONS");
            response.headers().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, X-Voice-Duration");
        }
    }

    private static boolean isKeepAlive(FullHttpRequest req) {
        String connection = req.headers().get(HttpHeaderNames.CONNECTION);
        if (connection != null && connection.equalsIgnoreCase("close")) return false;
        if (connection != null && connection.equalsIgnoreCase("keep-alive")) return true;
        return req.protocolVersion().isKeepAliveDefault();
    }

    private static String resolveClientIp(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (ctx.channel().attr(GameServerAttributes.WS_IP).get() != null) {
            return ctx.channel().attr(GameServerAttributes.WS_IP).get();
        }
        if (ctx.channel().remoteAddress() instanceof InetSocketAddress addr) {
            return addr.getAddress().getHostAddress();
        }
        return "";
    }
}
