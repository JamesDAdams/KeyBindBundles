package com.matyrobbrt.keybindbundles.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class CustomImageManager {
    private static final Map<String, ResourceLocation> LOADED_TEXTURES = new HashMap<>();
    private static Path storageDir;

    public static Path getStorageDir() {
        if (storageDir == null) {
            Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
            storageDir = gameDir.resolve("config").resolve("keybindbundles").resolve("custom_icons");
        }
        try {
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return storageDir;
    }

    public static ResourceLocation getOrLoadTexture(String filename) {
        if (filename == null || filename.isBlank()) return null;
        if (LOADED_TEXTURES.containsKey(filename)) {
            return LOADED_TEXTURES.get(filename);
        }

        Path file = getStorageDir().resolve(filename);
        if (!Files.exists(file)) {
            return null;
        }

        try (InputStream is = Files.newInputStream(file)) {
            NativeImage nativeImage = NativeImage.read(is);
            DynamicTexture dynTex = new DynamicTexture(nativeImage);
            String safePath = "custom_icon_" + filename.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("keybindbundles", safePath);
            Minecraft.getInstance().getTextureManager().register(rl, dynTex);
            LOADED_TEXTURES.put(filename, rl);
            return rl;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String registerImage(BufferedImage image, String formatHint) {
        if (image == null) return null;
        try {
            int w = image.getWidth();
            int h = image.getHeight();
            if (w <= 0 || h <= 0) return null;

            // Render crisp 16x16 icon keeping aspect ratio
            BufferedImage target = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = target.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            float scale = Math.min(16f / w, 16f / h);
            int dw = Math.max(1, Math.round(w * scale));
            int dh = Math.max(1, Math.round(h * scale));
            int dx = (16 - dw) / 2;
            int dy = (16 - dh) / 2;

            g2.drawImage(image, dx, dy, dw, dh, null);
            g2.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(target, "PNG", baos);
            byte[] bytes = baos.toByteArray();

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            String filename = "icon_" + sb.toString() + ".png";

            Path targetPath = getStorageDir().resolve(filename);
            Files.write(targetPath, bytes);

            // Register in Minecraft texture manager
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                NativeImage nativeImage = NativeImage.read(bais);
                DynamicTexture dynTex = new DynamicTexture(nativeImage);
                ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("keybindbundles", "custom_icon_" + filename.toLowerCase().replaceAll("[^a-z0-9_.-]", "_"));
                Minecraft.getInstance().getTextureManager().register(rl, dynTex);
                LOADED_TEXTURES.put(filename, rl);
            }

            return filename;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static BufferedImage loadFromBytesOrSvg(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;

        // 1. Check for SVG
        try {
            String str = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).trim();
            if (str.startsWith("<svg") || (str.startsWith("<?xml") && str.contains("<svg"))) {
                return renderSvgSimple(str);
            }
        } catch (Exception ignored) {
        }

        // 2. Try ImageIO
        try {
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
            if (bi != null) return bi;
        } catch (Exception ignored) {
        }

        // 3. Fallback to Minecraft NativeImage (stb_image)
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             NativeImage ni = NativeImage.read(bais)) {
            BufferedImage bi = new BufferedImage(ni.getWidth(), ni.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < ni.getHeight(); y++) {
                for (int x = 0; x < ni.getWidth(); x++) {
                    int abgr = ni.getPixelRGBA(x, y);
                    int r = abgr & 0xFF;
                    int g = (abgr >> 8) & 0xFF;
                    int b = (abgr >> 16) & 0xFF;
                    int a = (abgr >> 24) & 0xFF;
                    int argb = (a << 24) | (r << 16) | (g << 8) | b;
                    bi.setRGB(x, y, argb);
                }
            }
            return bi;
        } catch (Exception ignored) {
        }

        return null;
    }

    private static BufferedImage renderSvgSimple(String svgXml) {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(40, 44, 52, 220));
        g2.fillRoundRect(2, 2, 60, 60, 12, 12);
        g2.setColor(new Color(230, 160, 40));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(2, 2, 60, 60, 12, 12);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g2.drawString("SVG", 12, 38);
        g2.dispose();
        return img;
    }
}
