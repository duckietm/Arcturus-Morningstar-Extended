package com.eu.habbo.networking.gameserver.auth;

import com.eu.habbo.Emulator;
import com.eu.habbo.networking.gameserver.GameServerAttributes;
import com.google.gson.JsonArray;
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

    private static final String LOGIN_PATH           = "/api/auth/login";
    private static final String REGISTER_PATH        = "/api/auth/register";
    private static final String FORGOT_PATH          = "/api/auth/forgot-password";
    private static final String LOGOUT_PATH          = "/api/auth/logout";
    private static final String CHECK_EMAIL_PATH     = "/api/auth/check-email";
    private static final String CHECK_USERNAME_PATH  = "/api/auth/check-username";
    private static final String ROOM_TEMPLATES_PATH  = "/api/auth/room-templates";
    private static final String REMEMBER_PATH        = "/api/auth/remember";
    private static final String REFRESH_PATH         = "/api/auth/refresh";
    private static final String HEALTH_PATH          = "/api/health";

    private static final Pattern USERNAME_RE = Pattern.compile("^[A-Za-z0-9._-]{3,32}$");
    private static final Pattern EMAIL_RE = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern FIGURE_RE = Pattern.compile("^[A-Za-z0-9.\\-]{1,200}$");
    private static final SecureRandom RNG = new SecureRandom();
    private static final int MAX_BODY_BYTES = 8 * 1024;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest req)) {
            super.channelRead(ctx, msg);
            return;
        }

        String path = new QueryStringDecoder(req.uri()).path();

        if (!path.equals(LOGIN_PATH) && !path.equals(REGISTER_PATH)
                && !path.equals(FORGOT_PATH) && !path.equals(LOGOUT_PATH)
                && !path.equals(CHECK_EMAIL_PATH) && !path.equals(CHECK_USERNAME_PATH)
                && !path.equals(ROOM_TEMPLATES_PATH)
                && !path.equals(REMEMBER_PATH)
                && !path.equals(REFRESH_PATH)
                && !path.equals(HEALTH_PATH)) {
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

        if (path.equals(HEALTH_PATH)) {
            if (req.method() != HttpMethod.GET && req.method() != HttpMethod.HEAD) {
                sendJson(ctx, req, HttpResponseStatus.METHOD_NOT_ALLOWED, errorPayload("Use GET."));
                return;
            }
            JsonObject ok = new JsonObject();
            ok.addProperty("status", "ok");
            sendJson(ctx, req, HttpResponseStatus.OK, ok);
            return;
        }

        if (path.equals(ROOM_TEMPLATES_PATH)) {
            if (req.method() != HttpMethod.GET && req.method() != HttpMethod.HEAD) {
                sendJson(ctx, req, HttpResponseStatus.METHOD_NOT_ALLOWED, errorPayload("Use GET."));
                return;
            }
            handleRoomTemplates(ctx, req);
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

        if (req.content().readableBytes() > MAX_BODY_BYTES) {
            sendJson(ctx, req, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, errorPayload("Payload too large."));
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

        if (path.equals(CHECK_EMAIL_PATH)) {
            handleCheckEmail(ctx, req, body, ip);
            return;
        }
        if (path.equals(CHECK_USERNAME_PATH)) {
            handleCheckUsername(ctx, req, body, ip);
            return;
        }
        if (path.equals(REMEMBER_PATH)) {
            handleRemember(ctx, req, body, ip);
            return;
        }
        if (path.equals(REFRESH_PATH)) {
            handleRefresh(ctx, req, body, ip);
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

    private void handleCheckEmail(ChannelHandlerContext ctx, FullHttpRequest req, JsonObject body, String ip) {
        if (!AuthRateLimiter.tryProbe(ip)) {
            long secs = AuthRateLimiter.secondsUntilProbeReset(ip);
            sendJson(ctx, req, HttpResponseStatus.TOO_MANY_REQUESTS,
                    errorPayload("Too many requests. Try again in " + secs + "s."));
            return;
        }
        String email = readString(body, "email").trim();
        if (email.isEmpty() || email.length() > 254 || !EMAIL_RE.matcher(email).matches()) {
            sendJson(ctx, req, HttpResponseStatus.BAD_REQUEST, errorPayload("Invalid email address."));
            return;
        }

        Boolean cached = AvailabilityCache.lookupEmail(email);
        boolean taken;
        if (cached != null) {
            taken = !cached;
        } else {
            try (Connection conn = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT 1 FROM users WHERE mail = ? LIMIT 1")) {
                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery()) {
                    taken = rs.next();
                }
            } catch (Exception e) {
                LOGGER.error("check-email failed", e);
                sendJson(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorPayload("Server error."));
                return;
            }
            AvailabilityCache.storeEmail(email, !taken);
        }

        JsonObject res = new JsonObject();
        res.addProperty("available", !taken);
        if (taken) res.addProperty("error", "This email is already in use.");
        sendJson(ctx, req, HttpResponseStatus.OK, res);
    }

    private void handleCheckUsername(ChannelHandlerContext ctx, FullHttpRequest req, JsonObject body, String ip) {
        if (!AuthRateLimiter.tryProbe(ip)) {
            long secs = AuthRateLimiter.secondsUntilProbeReset(ip);
            sendJson(ctx, req, HttpResponseStatus.TOO_MANY_REQUESTS,
                    errorPayload("Too many requests. Try again in " + secs + "s."));
            return;
        }
        String username = readString(body, "username").trim();
        if (!USERNAME_RE.matcher(username).matches()) {
            sendJson(ctx, req, HttpResponseStatus.BAD_REQUEST,
                    errorPayload("Username must be 3-32 chars (letters, numbers, . _ -)."));
            return;
        }

        Boolean cached = AvailabilityCache.lookupUsername(username);
        boolean taken;
        if (cached != null) {
            taken = !cached;
        } else {
            try (Connection conn = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT 1 FROM users WHERE username = ? LIMIT 1")) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    taken = rs.next();
                }
            } catch (Exception e) {
                LOGGER.error("check-username failed", e);
                sendJson(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorPayload("Server error."));
                return;
            }
            AvailabilityCache.storeUsername(username, !taken);
        }

        JsonObject res = new JsonObject();
        res.addProperty("available", !taken);
        if (taken) res.addProperty("error", "This Habbo name is already taken.");
        sendJson(ctx, req, HttpResponseStatus.OK, res);
    }

    private void handleLogout(ChannelHandlerContext ctx, FullHttpRequest req, com.google.gson.JsonObject body) {
        String ssoTicket = readString(body, "ssoTicket");
        String rememberToken = readString(body, "rememberToken").trim();
        JsonObject ok = new JsonObject();
        ok.addProperty("message", "Logged out.");

        try (Connection conn = Emulator.getDatabase().getDataSource().getConnection()) {
            int userId = 0;

            if (ssoTicket != null && !ssoTicket.isEmpty()) {
                try (PreparedStatement lookup = conn.prepareStatement(
                        "SELECT id FROM users WHERE auth_ticket = ? LIMIT 1")) {
                    lookup.setString(1, ssoTicket);
                    try (ResultSet rs = lookup.executeQuery()) {
                        if (rs.next()) userId = rs.getInt("id");
                    }
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
            }

            if (!rememberToken.isEmpty()) {
                RememberJwtService.revokeFromToken(conn, rememberToken);
            }
        } catch (Exception e) {
            LOGGER.error("Logout cleanup failed", e);
        }

        sendJson(ctx, req, HttpResponseStatus.OK, ok);
    }

    private void handleRemember(ChannelHandlerContext ctx, FullHttpRequest req, JsonObject body, String ip) {
        String jwt = readString(body, "rememberToken").trim();
        if (jwt.isEmpty()) {
            sendJson(ctx, req, HttpResponseStatus.BAD_REQUEST, errorPayload("Missing rememberToken."));
            return;
        }

        try (Connection conn = Emulator.getDatabase().getDataSource().getConnection()) {
            RememberJwtService.RotationResult rot = RememberJwtService.rotate(conn, jwt, ip);
            if (rot == null) {
                sendJson(ctx, req, HttpResponseStatus.UNAUTHORIZED, errorPayload("Remember token invalid or expired."));
                return;
            }

            String ssoTicket = mintSsoTicket();
            try (PreparedStatement upd = conn.prepareStatement(
                    "UPDATE users SET auth_ticket = ?, ip_current = ? WHERE id = ? LIMIT 1")) {
                upd.setString(1, ssoTicket);
                upd.setString(2, ip == null ? "" : ip);
                upd.setInt(3, rot.userId);
                upd.executeUpdate();
            }

            JsonObject ok = new JsonObject();
            ok.addProperty("ssoTicket", ssoTicket);
            ok.addProperty("username", rot.username);
            ok.addProperty("rememberToken", rot.jwt);
            ok.addProperty("expiresAt", rot.expiresAt);
            sendJson(ctx, req, HttpResponseStatus.OK, ok);
        } catch (Exception e) {
            LOGGER.error("Remember login failed", e);
            sendJson(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorPayload("Server error."));
        }
    }

    private void handleRefresh(ChannelHandlerContext ctx, FullHttpRequest req, JsonObject body, String ip) {
        String jwt = readString(body, "rememberToken").trim();
        if (jwt.isEmpty()) {
            sendJson(ctx, req, HttpResponseStatus.BAD_REQUEST, errorPayload("Missing rememberToken."));
            return;
        }

        try (Connection conn = Emulator.getDatabase().getDataSource().getConnection()) {
            RememberJwtService.RotationResult rot = RememberJwtService.rotate(conn, jwt, ip);
            if (rot == null) {
                sendJson(ctx, req, HttpResponseStatus.UNAUTHORIZED, errorPayload("Remember token invalid or expired."));
                return;
            }
            JsonObject ok = new JsonObject();
            ok.addProperty("rememberToken", rot.jwt);
            ok.addProperty("expiresAt", rot.expiresAt);
            sendJson(ctx, req, HttpResponseStatus.OK, ok);
        } catch (Exception e) {
            LOGGER.error("Refresh failed", e);
            sendJson(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorPayload("Server error."));
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, FullHttpRequest req, JsonObject body, String ip) {
        String username = readString(body, "username").trim();
        String password = readString(body, "password");
        boolean rememberMe = readBoolean(body, "remember", false);

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

                String rememberToken = null;
                if (rememberMe) {
                    try {
                        RememberJwtService.RotationResult issued = RememberJwtService.issueForNewFamily(
                                conn, userId, rs.getString("username"), ip);
                        rememberToken = issued.jwt;
                    } catch (SQLException e) {
                        LOGGER.error("Failed to issue remember-me JWT for userId=" + userId, e);
                    }
                }

                AuthRateLimiter.recordSuccess(ip);

                JsonObject ok = new JsonObject();
                ok.addProperty("ssoTicket", ssoTicket);
                ok.addProperty("username", rs.getString("username"));
                if (rememberToken != null) ok.addProperty("rememberToken", rememberToken);
                sendJson(ctx, req, HttpResponseStatus.OK, ok);
            }
        } catch (Exception e) {
            LOGGER.error("Login query failed for username=" + username, e);
            sendJson(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorPayload("Server error."));
        }
    }

    private void handleRegister(ChannelHandlerContext ctx, FullHttpRequest req, JsonObject body, String ip) {
        if (!Emulator.getConfig().getBoolean("login.register.enabled", true)) {
            sendJson(ctx, req, HttpResponseStatus.FORBIDDEN, errorPayload("Registration is closed."));
            return;
        }

        String username = readString(body, "username").trim();
        String email    = readString(body, "email").trim();
        String password = readString(body, "password");
        String figure   = readString(body, "figure").trim();
        String gender   = readString(body, "gender").trim().toUpperCase();
        int templateId  = readInt(body, "templateId", 0);

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

            String finalLook = (figure.isEmpty() || !FIGURE_RE.matcher(figure).matches()) ? defaultLook : figure;
            String finalGender = (gender.equals("M") || gender.equals("F")) ? gender : "M";

            int startingCredits  = Math.max(0, Emulator.getConfig().getInt("new_user_credits", 0));
            int startingDuckets  = Math.max(0, Emulator.getConfig().getInt("new_user_duckets", 0));
            int startingDiamonds = Math.max(0, Emulator.getConfig().getInt("new_user_diamonds", 0));

            int newUserId = 0;
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO users (username, password, mail, account_created, " +
                            "ip_register, ip_current, last_online, last_login, motto, look, gender, " +
                            "credits, `rank`, home_room, machine_id, auth_ticket, online) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, '', '', '0')",
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
                ins.setString(10, finalLook);
                ins.setString(11, finalGender);
                ins.setInt(12, startingCredits);
                ins.executeUpdate();
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    if (keys.next()) newUserId = keys.getInt(1);
                }
            }

            if (newUserId > 0 && (startingDuckets > 0 || startingDiamonds > 0)) {
                seedUserCurrencies(conn, newUserId, startingDuckets, startingDiamonds);
            }

            LOGGER.info("[auth/register] user created id={} username='{}' templateId={} credits={} duckets={} diamonds={}",
                    newUserId, username, templateId, startingCredits, startingDuckets, startingDiamonds);

            if (newUserId > 0 && templateId > 0) {
                cloneTemplateForUser(conn, templateId, newUserId, username);
            } else if (templateId > 0) {
                LOGGER.warn("[auth/register] skipping template clone: user insert did not return an id (username='{}')", username);
            }

            AvailabilityCache.invalidateEmail(email);
            AvailabilityCache.invalidateUsername(username);

            JsonObject ok = new JsonObject();
            ok.addProperty("message", "Welcome aboard, " + username + "! Your account is ready — log in below with the password you just chose.");
            sendJson(ctx, req, HttpResponseStatus.OK, ok);
        } catch (Exception e) {
            LOGGER.error("Register query failed for username=" + username, e);
            sendJson(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorPayload("Server error."));
        }
    }

    private static void materializeCustomLayout(Connection conn, int templateId, int newRoomId) {
        String overrideModel = "0";
        String heightmap = "";
        int doorX = 0, doorY = 0, doorDir = 2;
        try (PreparedStatement sel = conn.prepareStatement(
                "SELECT override_model, heightmap, door_x, door_y, door_dir " +
                        "FROM room_templates WHERE template_id = ? LIMIT 1")) {
            sel.setInt(1, templateId);
            try (ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    overrideModel = rs.getString("override_model");
                    heightmap = rs.getString("heightmap");
                    doorX = rs.getInt("door_x");
                    doorY = rs.getInt("door_y");
                    doorDir = rs.getInt("door_dir");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("[auth/register] reading template layout failed templateId=" + templateId, e);
            return;
        }

        if (!"1".equals(overrideModel) || heightmap == null || heightmap.isEmpty()) {
            return;
        }

        String customName = "custom_" + newRoomId;

        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO room_models_custom (id, name, door_x, door_y, door_dir, heightmap) " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE name = VALUES(name), door_x = VALUES(door_x), " +
                        "door_y = VALUES(door_y), door_dir = VALUES(door_dir), heightmap = VALUES(heightmap)")) {
            ins.setInt(1, newRoomId);
            ins.setString(2, customName);
            ins.setInt(3, doorX);
            ins.setInt(4, doorY);
            ins.setInt(5, doorDir);
            ins.setString(6, heightmap);
            ins.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[auth/register] room_models_custom insert failed roomId=" + newRoomId, e);
            return;
        }

        try (PreparedStatement upd = conn.prepareStatement(
                "UPDATE rooms SET model = ? WHERE id = ? LIMIT 1")) {
            upd.setString(1, customName);
            upd.setInt(2, newRoomId);
            upd.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[auth/register] rooms.model rename failed roomId=" + newRoomId, e);
        }

        LOGGER.info("[auth/register] materialized custom layout '{}' for roomId={}", customName, newRoomId);
    }

    private static void seedUserCurrencies(Connection conn, int userId, int duckets, int diamonds) {
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO users_currency (user_id, type, amount) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE amount = VALUES(amount)")) {
            if (duckets > 0) {
                ins.setInt(1, userId);
                ins.setInt(2, 0);
                ins.setInt(3, duckets);
                ins.addBatch();
            }
            if (diamonds > 0) {
                ins.setInt(1, userId);
                ins.setInt(2, 5);
                ins.setInt(3, diamonds);
                ins.addBatch();
            }
            ins.executeBatch();
        } catch (SQLException e) {
            LOGGER.error("[auth/register] seeding users_currency failed userId=" + userId
                    + " duckets=" + duckets + " diamonds=" + diamonds, e);
        }
    }

    private void handleRoomTemplates(ChannelHandlerContext ctx, FullHttpRequest req) {
        JsonArray templates = new JsonArray();
        try (Connection conn = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT template_id, title, description, thumbnail " +
                             "FROM room_templates WHERE enabled = '1' " +
                             "ORDER BY sort_order ASC, template_id ASC")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject t = new JsonObject();
                    t.addProperty("templateId", rs.getInt("template_id"));
                    t.addProperty("title", rs.getString("title"));
                    t.addProperty("description", rs.getString("description"));
                    t.addProperty("thumbnail", rs.getString("thumbnail"));
                    templates.add(t);
                }
            }
        } catch (Exception e) {
            LOGGER.error("room-templates list failed", e);
            sendJson(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorPayload("Server error."));
            return;
        }
        JsonObject res = new JsonObject();
        res.add("templates", templates);
        sendJson(ctx, req, HttpResponseStatus.OK, res);
    }

    private static void cloneTemplateForUser(Connection conn, int templateId, int userId, String userName) {
        LOGGER.info("[auth/register] cloning template id={} for user id={} name='{}'", templateId, userId, userName);

        try (PreparedStatement check = conn.prepareStatement(
                "SELECT 1 FROM room_templates WHERE template_id = ? AND enabled = '1' LIMIT 1")) {
            check.setInt(1, templateId);
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) {
                    LOGGER.warn("[auth/register] unknown/disabled room template id={} for user id={}", templateId, userId);
                    return;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("[auth/register] template lookup failed for templateId=" + templateId, e);
            return;
        }

        int newRoomId = 0;
        int roomsInserted = 0;
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO rooms (owner_id, owner_name, name, description, model, password, state, " +
                        "users_max, category, paper_floor, paper_wall, paper_landscape, thickness_wall, " +
                        "thickness_floor, moodlight_data, override_model, trade_mode) " +
                        "(SELECT ?, ?, name, room_description, model, password, state, " +
                        "users_max, category, paper_floor, paper_wall, paper_landscape, thickness_wall, " +
                        "thickness_floor, moodlight_data, override_model, trade_mode " +
                        "FROM room_templates WHERE template_id = ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ins.setInt(1, userId);
            ins.setString(2, userName);
            ins.setInt(3, templateId);
            roomsInserted = ins.executeUpdate();
            try (ResultSet keys = ins.getGeneratedKeys()) {
                if (keys.next()) newRoomId = keys.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error("[auth/register] clone rooms failed templateId=" + templateId + " userId=" + userId, e);
            return;
        }

        LOGGER.info("[auth/register] rooms insert: rowsAffected={} newRoomId={}", roomsInserted, newRoomId);

        if (newRoomId <= 0) {
            LOGGER.warn("[auth/register] clone aborted - no roomId returned (templateId={}, userId={})", templateId, userId);
            return;
        }

        materializeCustomLayout(conn, templateId, newRoomId);

        int itemsInserted = 0;
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO items (user_id, room_id, item_id, wall_pos, x, y, z, rot, " +
                        "extra_data, wired_data, limited_data, guild_id) " +
                        "(SELECT ?, ?, item_id, wall_pos, x, y, z, rot, extra_data, wired_data, '0:0', 0 " +
                        "FROM room_templates_items WHERE template_id = ?)")) {
            ins.setInt(1, userId);
            ins.setInt(2, newRoomId);
            ins.setInt(3, templateId);
            itemsInserted = ins.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[auth/register] clone items failed templateId=" + templateId
                    + " roomId=" + newRoomId + " userId=" + userId, e);
        }

        LOGGER.info("[auth/register] items insert: rowsAffected={} roomId={}", itemsInserted, newRoomId);

        try (PreparedStatement upd = conn.prepareStatement(
                "UPDATE users SET home_room = ? WHERE id = ? LIMIT 1")) {
            upd.setInt(1, newRoomId);
            upd.setInt(2, userId);
            int rows = upd.executeUpdate();
            LOGGER.info("[auth/register] home_room update: rowsAffected={} userId={} roomId={}", rows, userId, newRoomId);
        } catch (SQLException e) {
            LOGGER.error("[auth/register] setting home_room failed userId=" + userId + " roomId=" + newRoomId, e);
        }
    }

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

    private static int readInt(JsonObject obj, String key, int defaultValue) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return defaultValue;
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static boolean readBoolean(JsonObject obj, String key, boolean defaultValue) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return defaultValue;
        try {
            com.google.gson.JsonElement el = obj.get(key);
            if (el.getAsJsonPrimitive().isBoolean()) return el.getAsBoolean();
            String s = el.getAsString();
            return "1".equals(s) || "true".equalsIgnoreCase(s);
        } catch (Exception e) {
            return defaultValue;
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
        response.headers().set("Access-Control-Allow-Methods", "GET, HEAD, POST, OPTIONS");
        response.headers().set("Access-Control-Allow-Headers", "Content-Type, X-Requested-With");
    }

    private static boolean isKeepAlive(FullHttpRequest req) {
        String connection = req.headers().get(HttpHeaderNames.CONNECTION);
        return connection == null || !"close".equalsIgnoreCase(connection);
    }
}
