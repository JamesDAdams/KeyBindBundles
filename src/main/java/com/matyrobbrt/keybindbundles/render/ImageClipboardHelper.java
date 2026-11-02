package com.matyrobbrt.keybindbundles.render;

import net.minecraft.client.Minecraft;

import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

public class ImageClipboardHelper {

    public static String tryPasteImage() {
        // 1. Try AWT System Clipboard
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);
            if (contents != null) {
                // A. Direct Image flavor (e.g. Snipping tool, copy image in browser/discord)
                if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    try {
                        Object data = contents.getTransferData(DataFlavor.imageFlavor);
                        if (data instanceof Image img) {
                            BufferedImage bimg = toBufferedImage(img);
                            if (bimg != null) {
                                String saved = CustomImageManager.registerImage(bimg, "png");
                                if (saved != null) return saved;
                            }
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }

                // B. File list flavor (e.g. copied file in Windows File Explorer)
                if (contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) contents.getTransferData(DataFlavor.javaFileListFlavor);
                        if (files != null && !files.isEmpty()) {
                            for (File f : files) {
                                if (isImageFile(f.getName())) {
                                    byte[] bytes = Files.readAllBytes(f.toPath());
                                    BufferedImage bimg = CustomImageManager.loadFromBytesOrSvg(bytes);
                                    if (bimg != null) {
                                        String saved = CustomImageManager.registerImage(bimg, getExtension(f.getName()));
                                        if (saved != null) return saved;
                                    }
                                }
                            }
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }

                // C. Stream / MIME image flavors
                for (DataFlavor flavor : contents.getTransferDataFlavors()) {
                    String sub = flavor.getSubType();
                    if (sub != null && (sub.equalsIgnoreCase("png") || sub.equalsIgnoreCase("jpeg")
                            || sub.equalsIgnoreCase("jpg") || sub.equalsIgnoreCase("gif") || sub.equalsIgnoreCase("bmp") || sub.equalsIgnoreCase("x-png"))) {
                        try {
                            Object data = contents.getTransferData(flavor);
                            if (data instanceof InputStream is) {
                                byte[] bytes = is.readAllBytes();
                                BufferedImage bimg = CustomImageManager.loadFromBytesOrSvg(bytes);
                                if (bimg != null) {
                                    String saved = CustomImageManager.registerImage(bimg, "png");
                                    if (saved != null) return saved;
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }

                // D. String flavor from AWT
                if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    try {
                        String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
                        String res = tryParseImageFromText(text);
                        if (res != null) return res;
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // 2. Fallback to Minecraft / GLFW Clipboard text
        try {
            String text = Minecraft.getInstance().keyboardHandler.getClipboard();
            return tryParseImageFromText(text);
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static String tryParseImageFromText(String text) {
        if (text == null || text.isBlank()) return null;
        text = text.trim();

        // Strip surrounding quotes
        if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
            text = text.substring(1, text.length() - 1).trim();
        }

        // 1. Data URI: data:image/png;base64,....
        if (text.startsWith("data:image/") && text.contains("base64,")) {
            try {
                int comma = text.indexOf(',');
                String base64 = text.substring(comma + 1).trim();
                byte[] bytes = Base64.getDecoder().decode(base64);
                BufferedImage bimg = CustomImageManager.loadFromBytesOrSvg(bytes);
                if (bimg != null) {
                    return CustomImageManager.registerImage(bimg, "png");
                }
            } catch (Throwable ignored) {
            }
        }

        // 2. SVG XML String
        if (text.startsWith("<svg") || (text.startsWith("<?xml") && text.contains("<svg"))) {
            try {
                BufferedImage bimg = CustomImageManager.loadFromBytesOrSvg(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                if (bimg != null) {
                    return CustomImageManager.registerImage(bimg, "svg");
                }
            } catch (Throwable ignored) {
            }
        }

        // 3. Local File Path
        try {
            Path path = Path.of(text);
            if (Files.exists(path) && !Files.isDirectory(path)) {
                byte[] bytes = Files.readAllBytes(path);
                BufferedImage bimg = CustomImageManager.loadFromBytesOrSvg(bytes);
                if (bimg != null) {
                    return CustomImageManager.registerImage(bimg, getExtension(text));
                }
            }
        } catch (Throwable ignored) {
        }

        // 4. Web URL (http:// or https://)
        if (text.startsWith("http://") || text.startsWith("https://")) {
            try {
                URI uri = URI.create(text);
                try (InputStream is = uri.toURL().openStream()) {
                    byte[] bytes = is.readAllBytes();
                    BufferedImage bimg = CustomImageManager.loadFromBytesOrSvg(bytes);
                    if (bimg != null) {
                        return CustomImageManager.registerImage(bimg, "png");
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static boolean isImageFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".svg") || lower.endsWith(".webp") || lower.endsWith(".bmp") || lower.endsWith(".gif");
    }

    private static String getExtension(String name) {
        int idx = name.lastIndexOf('.');
        return idx >= 0 ? name.substring(idx + 1).toLowerCase() : "png";
    }

    private static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage bi) {
            return bi;
        }

        // Use ImageIcon to block and guarantee full image loading
        ImageIcon icon = new ImageIcon(img);
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }

        BufferedImage bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bimg.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        return bimg;
    }
}
