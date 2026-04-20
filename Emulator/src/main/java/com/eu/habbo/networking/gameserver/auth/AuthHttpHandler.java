package com.eu.habbo.networking.gameserver.auth;

import com.eu.habbo.Emulator;
import com.eu.habbo.networking.gameserver.GameServerAttributes;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.*;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;

public class AuthHttpHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthHttpHandler.class);

    private static final String LOGIN_PATH    = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String FORGOT_PATH   = "/api/auth/forgot-password";
    private static final String LOGOUT_PATH   = "/api/auth/logout";

    private static final Pattern USERNAME_RE = Pattern.compile("^[A-Za-z0-9._-]{3,32}$");
    private static final Pattern EMAIL_RE = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final SecureRandom RNG = new SecureRandom();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest req)) {
            super.channelRead(ctx, msg);
            return;
        }

        String path = new QueryStringDecoder(req.uri()).path();

        if (!path.equals(LOGIN_PATH) && !path.equals(REGISTER_PATH)
                && !path.equals(FORGOT_PATH) && !path.equals(LOGOUT_PATH)) {
            super.channelRead(ctx, msg);
            return;
        }

        try {
            handle(ctx, req, path);
        } finally {
            ReferenceCountUtil.release(req);
        }
    }

    private void handle(ChannelHandlerContext ctx, FullHttpRequest req, String path) {
        if (req.method() == HttpMethod.OPTIONS) {
            sendCors(ctx, req);
            return;
        }

        if (req.method() != HttpMethod.POST) {
            sendJson(ctx, req, HttpResponseStatus.METHOD_NOT_ALLOWED, errorPayload("Use POST."));
            return;
        }

        String ip = resolveClientIp(ctx, req);

        if (AuthRateLimiter.isLocked(ip)) {
            long secs = AuthRateLimiter.secondsUntilUnlock(ip);
            sendJson(ctx, req, HttpResponseStatus.TOO_MANY_REQUESTS,
                    errorPayload("Too many attempts. Try again in " + secs + "s."));
            return;
        }

        JsonObject body;
        try {
            String text = req.content().toString(StandardCharsets.UTF_8);
            body = text.isEmpty() ? new JsonObject() : JsonParser.parseString(text).getAsJsonObject();
        } catch (Exception e) {
            sendJson(ctx, req, HttpResponseStatus.BAD_REQUEST, errorPayload("Invalid JSON body."));
            return;
        }

        if (path.equals(LOGOUT_PATH)) {
            handleLogout(ctx, req, body);
            return;
        }

        String turnstileToken = readString(body, "turnstileToken");
        if (!TurnstileVerifier.verify(turnstileToken, ip)) {
            AuthRateLimiter.recordFailure(ip);
            sendJson(ctx, req, HttpResponseStatus.FORBIDDEN, errorPayload("Security check failed."));
            return;
        }

        switch (path) {
            case LOGIN_PATH    -> handleLogin(ctx, req, body, ip);
            case REGISTER_PATH -> handleRegister(ctx, req, body, ip);
            case FORGOT_PATH   -> handleForgot(ctx, req, body, ip);
        }
    }

    /* ─── Logout ────────────────────────────────────────────────────────── */

    private void handleLogout(ChannelHandlerContext ctx, FullHttpRequest req, com.google.gson.JsonObject body) {
        String ssoTicket = readString(body, "ssoTicket");
        JsonObject ok = new JsonObject();
        ok.addProperty("message", "Logged out.");

        if (ssoTicket == null || ssoTicket.isEmpty()) {
            sendJson(ctx, req, HttpResponseStatus.OK, ok);
            return;
        }

        try (Connection conn = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement lookup = conn.prepareStatement(
                     "SELECT id FROM users WHERE auth_ticket = ? LIMIT 1")) {
            lookup.setString(1, ssoTicket);
            int userId = 0;
            try (ResultSet rs = lookup.executeQuery()) {
                if (rs.next()) userId = rs.getInt("id");
            }

            if (userId > 0) {
                try (PreparedStatement clear = conn.prepareStatement(
                        "UPDATE users SET auth_ticket = '', online = '0' WHERE id = ? LIMIT 1")) {
                    clear.setInt(1, userId);
                    clear.executeUpdate();
                }

                if (Emulator.getGameServer() != null
                        && Emulator.getGameServer().getGameClientManager() != null) {
                    com.eu.habbo.habbohotel.users.Habbo habbo =
                            Emulator.getGameServer().getGameClientManager().getHabbo(userId);
                    if (habbo != null && habbo.getClient() != null) {
                        Emulator.getGameServer().getGameClientManager().disposeClient(habbo.getClient());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Logout cleanup failed for ticket", e);
        }

        sendJson(ctx, req, HttpResponseStatus.OK, ok);
    }

    /* ─── Login ─────────────────────────────────────────────────────────── */

    private void handleLogin(ChannelHandlerContext ctx, FullHttpRequest req, JsonObject body, String ip) {
        String username = readString(body, "username").trim();
        String password = readString(body, "password");

        if (username.isEmpty() || password.isEmpty()) {
            sendJson(ctx, req, HttpResponseStatus.BAD_REQUEST, errorPayload("Missing credentials."));
            return;
        }

        try (Connection conn = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, username, password FROM users WHERE username = ? LIMIT 1")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    LOGGER.info("[auth/login] user not found username='{}' ip={}", username, ip);
                    AuthRateLimiter.recordFailure(ip);
                    sendJson(ctx, req, HttpResponseStatus.UNAUTHORIZED,
                            errorPayload("Invalid Habbo name or password."));
                    return;
                }

                int userId = rs.getInt("id");
                String stored = rs.getString("password");
                String storedPreview = stored == null
                        ? "<null>"
                        : (stored.isEmpty() ? "<empty>" : stored.substring(0, Math.min(7, stored.length())) + "…(" + stored.length() + " chars)");

                if (stored == null || stored.isEmpty() || !checkPassword(password, stored)) {
                    LOGGER.info("[auth/login] password mismatch for user id={} username='{}' stored='{}'",
                            userId, username, storedPreview);
                    AuthRateLimiter.recordFailure(ip);
                    sendJson(ctx, req, HttpResponseStatus.UNAUTHORIZED,
                            errorPayload("Invalid Habbo name or password."));
                    return;
                }

                String ssoTicket = mintSsoTicket();

                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE users SET auth_ticket = ?, ip_current = ? WHERE id = ? LIMIT 1")) {
                    upd.setString(1, ssoTicket);
                    upd.setString(2, ip == null ? "" : ip);
                    upd.setInt(3, userId);
                    upd.executeUpdate();
                }

                AuthRateLimiter.recordSuccess(ip);

                JsonObject ok = new JsonObject();
                ok.addProperty("ssoTicket", ssoTicket);
                ok.addProperty("username", rs.getString("username"));
                sendJson(ctx, req, HttpResponseStatus.OK, ok);
            }
        } catch (Exception e) {
            LOGGER.error("Login query failed for username=" + username, e);
            sendJson(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorPayload("Server error."));
        }
    }

    /* ─── Register ──────────────────────────────────────────────────────── */

    private void handleRegister(ChannelHandlerContext ctx, FullHttpRequest req, JsonObject body, String ip) {
        if (!Emulator.getConfig().getBoolean("login.register.enabled", true)) {
            sendJson(ctx, req, HttpResponseStatus.FORBIDDEN, errorPayload("Registration is closed."));
            return;
        }

        String username = readString(body, "username").trim();
        String email    = readString(body, "email").trim();
        String password = readString(body, "password");

        if (!USERNAME_RE.matcher(username).matches()) {
            sendJson(ctx, req, HttpResponseStatus.BAD_REQUEST,
                    errorPayload("Username must be 3-32 chars (letters, numbers, . _ -)."));
            return;
        }
        if (!EMAIL_RE.matcher(email).matches()) {
            sendJson(ctx, req, HttpResponseStatus.BAD_REQUEST, errorPayload("Invalid email address."));
            return;
        }
        if (password.length() < 8) {
            sendJson(ctx, req, HttpResponseStatus.BAD_REQUEST,
                    errorPayload("Password must be at least 8 characters."));
            return;
        }

        try (Connection conn = Emulator.getDatabase().getDataSource().getConnection()) {
            int maxPerIp = Emulator.getConfig().getInt("register.max_per_ip", 5);
            if (maxPerIp > 0 && ip != null && !ip.isEmpty()) {
                try (PreparedStatement quota = conn.prepareStatement(
                        "SELECT COUNT(*) FROM users WHERE ip_register = ?")) {
                    quota.setString(1, ip);
                    try (ResultSet rs = quota.executeQuery()) {
                        if (rs.next() && rs.getInt(1) >= maxPerIp) {
                            sendJson(ctx, req, HttpResponseStatus.TOO_MANY_REQUESTS,
                                    errorPayload("This IP has reached the maximum of "
                                            + maxPerIp + " registered accounts."));
                            return;
                        }
                    }
                }
            }

            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT username, mail FROM users WHERE username = ? OR mail = ? LIMIT 1")) {
                check.setString(1, username);
                check.setString(2, email);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        String existingUser = rs.getString("username");
                        String existingMail = rs.getString("mail");
                        boolean userTaken = existingUser != null && existingUser.equalsIgnoreCase(username);
                        boolean mailTaken = existingMail != null && existingMail.equalsIgnoreCase(email);
                        String message;
                        if (userTaken && mailTaken)      message = "That Habbo name and email are already in use.";
                        else if (userTaken)              message = "That Habbo name is already in use.";
                        else                             message = "That email address is already in use.";
                        sendJson(ctx, req, HttpResponseStatus.CONFLICT, errorPayload(message));
                        return;
                    }
                }
            }

            String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
            String defaultLook = Emulator.getConfig().getValue("register.default.look",
                    "hr-100-7.hd-180-1.ch-210-66.lg-270-82.sh-290-80");
            String defaultMotto = Emulator.getConfig().getValue("register.default.motto", "I love Habbo!");
            int now = Emulator.getIntUnixTimestamp();

            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO users (username, password, mail, account_created, " +
                            "ip_register, ip_current, last_online, last_login, motto, look, gender, " +
                            "credits, `rank`, home_room, machine_id, auth_ticket, online) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'M', 0, 1, 0, '', '', '0')",
                    Statement.RETURN_GENERATED_KEYS)) {
                ins.setString(1, username);
                ins.setString(2, hashed);
                ins.setString(3, email);
                ins.setInt(4, now);
                ins.setString(5, ip == null ? "" : ip);
                ins.setString(6, ip == null ? "" : ip);
                ins.setInt(7, now);
                ins.setInt(8, now);
                ins.setString(9, defaultMotto);
                ins.setString(10, defaultLook);
                ins.executeUpdate();
            }

            JsonObject ok = new JsonObject();
            ok.addProperty("message", "Welcome aboard, " + username + "! Your account is ready — log in below with the password you just chose.");
            sendJson(ctx, req, HttpResponseStatus.OK, ok);
        } catch (Exception e) {
            LOGGER.error("Register query failed for username=" + username, e);
            sendJson(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorPayload("Server error."));
        }
    }

    /* ─── Forgot password ───────────────────────────────────────────────── */

    private void handleForgot(ChannelHandlerContext ctx, FullHttpRequest req, JsonObject body, String ip) {
        String email = readString(body, "email").trim();

        if (!EMAIL_RE.matcher(email).matches()) {
            sendJson(ctx, req, HttpResponseStatus.BAD_REQUEST, errorPayload("Invalid email address."));
            return;
        }

        JsonObject ok = new JsonObject();
        ok.addProperty("message", "Email sent! If an account matches that address you'll find a reset link in your inbox shortly (check spam if it doesn't show up within a minute).");

        try (Connection conn = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, username FROM users WHERE mail = ? LIMIT 1")) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    String username = rs.getString("username");
                    String token = mintResetToken();
                    long expiresAt = Instant.now().getEpochSecond() + 60L * 60L; // 1h

                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO password_resets (user_id, token, expires_at, created_ip) " +
                                    "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                                    "token = VALUES(token), expires_at = VALUES(expires_at), created_ip = VALUES(created_ip)")) {
                        ins.setInt(1, userId);
                        ins.setString(2, token);
                        ins.setTimestamp(3, Timestamp.from(Instant.ofEpochSecond(expiresAt)));
                        ins.setString(4, ip == null ? "" : ip);
                        ins.executeUpdate();
                    }

                    String resetUrlBase = Emulator.getConfig().getValue("password.reset.url",
                            "http://localhost/reset-password");
                    String fullUrl = resetUrlBase + (resetUrlBase.contains("?") ? "&" : "?") + "token=" + token;
                    String subject = "Reset your Habbo password";
                    String message = "Hi " + username + ",\n\n" +
                            "Someone (hopefully you) requested a password reset for your Habbo account.\n" +
                            "Click the link below within the next hour to choose a new password:\n\n" +
                            fullUrl + "\n\n" +
                            "If you didn't request this you can safely ignore this email.";

                    Emulator.getThreading().getService().submit((Runnable) () -> SmtpMailService.send(email, subject, message));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Forgot-password query failed for email=" + email, e);
        }

        sendJson(ctx, req, HttpResponseStatus.OK, ok);
    }

    /* ─── Helpers ───────────────────────────────────────────────────────── */

    private static boolean checkPassword(String plain, String stored) {
        String compatible = stored.startsWith("$2y$") ? "$2a$" + stored.substring(4) : stored;
        try {
            return BCrypt.checkpw(plain, compatible);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String mintSsoTicket() {
        byte[] buf = new byte[32];
        RNG.nextBytes(buf);
        return "nitro-" + Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String mintResetToken() {
        byte[] buf = new byte[32];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String readString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String resolveClientIp(ChannelHandlerContext ctx, FullHttpRequest req) {
        String ipHeader = Emulator.getConfig() != null
                ? Emulator.getConfig().getValue("ws.ip.header", "")
                : "";
        if (!ipHeader.isEmpty() && req.headers().contains(ipHeader)) {
            String hv = req.headers().get(ipHeader);
            if (hv != null && !hv.isEmpty()) {
                int comma = hv.indexOf(',');
                return (comma > 0 ? hv.substring(0, comma) : hv).trim();
            }
        }
        if (ctx.channel().attr(GameServerAttributes.WS_IP).get() != null) {
            return ctx.channel().attr(GameServerAttributes.WS_IP).get();
        }
        if (ctx.channel().remoteAddress() instanceof InetSocketAddress addr) {
            return addr.getAddress().getHostAddress();
        }
        return "";
    }

    private static JsonObject errorPayload(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", message);
        return obj;
    }

    private static void sendJson(ChannelHandlerContext ctx, FullHttpRequest req,
                                 HttpResponseStatus status, JsonObject body) {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(bytes));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        applyCors(req, response);
        boolean keepAlive = isKeepAlive(req);
        if (keepAlive) response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        var future = ctx.writeAndFlush(response);
        if (!keepAlive) future.addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendCors(ChannelHandlerContext ctx, FullHttpRequest req) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
        applyCors(req, response);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void applyCors(FullHttpRequest req, FullHttpResponse response) {
        String origin = req.headers().get(HttpHeaderNames.ORIGIN);
        if (origin != null && !origin.isEmpty()) {
            response.headers().set("Access-Control-Allow-Origin", origin);
            response.headers().set("Vary", "Origin");
            response.headers().set("Access-Control-Allow-Credentials", "true");
        }
        response.headers().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.headers().set("Access-Control-Allow-Headers", "Content-Type, X-Requested-With");
    }

    private static boolean isKeepAlive(FullHttpRequest req) {
        String connection = req.headers().get(HttpHeaderNames.CONNECTION);
        return connection == null || !"close".equalsIgnoreCase(connection);
    }
}
