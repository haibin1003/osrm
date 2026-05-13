package com.osrm.application.h5.service;

import com.osrm.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CaptchaService {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int EXPIRE_SECONDS = 300; // 5 minutes
    private static final int MAX_ATTEMPTS_PER_MINUTE = 10;
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 4;

    // captchaKey -> CaptchaEntry
    private final Map<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();
    // ip -> (windowStartMs, count)
    private final Map<String, RateLimitEntry> rateLimitStore = new ConcurrentHashMap<>();

    public CaptchaResult generate(String clientIp) {
        checkRateLimit(clientIp);

        String code = generateCode();
        String key = UUID.randomUUID().toString();
        captchaStore.put(key, new CaptchaEntry(code, Instant.now().getEpochSecond()));

        String base64Image = renderImage(code);
        return new CaptchaResult(key, base64Image);
    }

    public void verify(String key, String input) {
        if (key == null || input == null || key.isBlank() || input.isBlank()) {
            throw new BizException("验证码无效或已过期");
        }
        CaptchaEntry entry = captchaStore.remove(key);
        if (entry == null) {
            throw new BizException("验证码无效或已过期");
        }
        long now = Instant.now().getEpochSecond();
        if (now - entry.timestamp > EXPIRE_SECONDS) {
            throw new BizException("验证码已过期，请重新获取");
        }
        if (!entry.code.equalsIgnoreCase(input.trim())) {
            throw new BizException("验证码错误");
        }
    }

    private void checkRateLimit(String clientIp) {
        if (clientIp == null) {
            clientIp = "unknown";
        }
        long now = System.currentTimeMillis();
        RateLimitEntry entry = rateLimitStore.compute(clientIp, (k, v) -> {
            if (v == null || now - v.windowStart > 60_000) {
                return new RateLimitEntry(now, new AtomicInteger(1));
            }
            v.count.incrementAndGet();
            return v;
        });
        if (entry.count.get() > MAX_ATTEMPTS_PER_MINUTE) {
            throw new BizException("请求过于频繁，请稍后再试");
        }
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CAPTCHA_CHARS.charAt(RANDOM.nextInt(CAPTCHA_CHARS.length())));
        }
        return sb.toString();
    }

    private String renderImage(String code) {
        int width = 100;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // background
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, width, height);

        // noise lines
        for (int i = 0; i < 8; i++) {
            g.setColor(new Color(RANDOM.nextInt(200), RANDOM.nextInt(200), RANDOM.nextInt(200)));
            int x1 = RANDOM.nextInt(width);
            int y1 = RANDOM.nextInt(height);
            int x2 = RANDOM.nextInt(width);
            int y2 = RANDOM.nextInt(height);
            g.drawLine(x1, y1, x2, y2);
        }

        // text
        g.setFont(new Font("Arial", Font.BOLD, 24));
        int charWidth = width / (CODE_LENGTH + 1);
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(30 + RANDOM.nextInt(80), 30 + RANDOM.nextInt(80), 30 + RANDOM.nextInt(80)));
            int x = (i + 1) * charWidth - 8;
            int y = 28 + RANDOM.nextInt(6) - 3;
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }

        // border noise
        for (int i = 0; i < 20; i++) {
            g.setColor(new Color(RANDOM.nextInt(255), RANDOM.nextInt(255), RANDOM.nextInt(255)));
            int x = RANDOM.nextInt(width);
            int y = RANDOM.nextInt(height);
            g.fillRect(x, y, 2, 2);
        }

        g.dispose();

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", os);
            byte[] bytes = os.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            logger.error("生成验证码图片失败", e);
            throw new BizException("验证码生成失败");
        }
    }

    private static class CaptchaEntry {
        final String code;
        final long timestamp;
        CaptchaEntry(String code, long timestamp) {
            this.code = code;
            this.timestamp = timestamp;
        }
    }

    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger count;
        RateLimitEntry(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }

    public record CaptchaResult(String key, String image) {}
}
