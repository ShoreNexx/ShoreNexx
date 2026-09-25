import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class BannerGenerator {

    // ----------------------------------------------------------------------
    // info panel content
    // ----------------------------------------------------------------------
    static final String WINDOW_TITLE = "profile.sh --live";

    static final String LEFT_LABEL = "PROFILE.IMG";
    static final String LEFT_META = "300x340 / 1-BIT";
    static final String LEFT_FOOTER_TEMPLATE = "PTS %d \u00b7 SHORENEXX";

    static final String RIGHT_LABEL = "SYSTEM.INFO";
    static final String RIGHT_STATUS = "LIVE";
    static final String RIGHT_HANDLE = "shorenexx@github";
    static final String[][] ROWS = {
            {"whoami", "Sahil Mhatre"},
            {"OS", "Pop os 24.04 LTS, Windows 11"},
            {"", ""},
            {"", ""},
            {"Core.Lang", "Java \u00b7 JavaScript"},
            {"Core.Backend", "JDBC \u00b7 Hibernate"},
            {"Core.Web", "HTML \u00b7 CSS \u00b7 React \u00b7 Tailwind"},
            {"Core.DB", "PostgreSQL \u00b7 OracleSQL"},
            {"Core.Real", "English \u00b7 Marathi \u00b7 Hindi"},
            {"", ""},
            {"", ""},
            {"Focus.Current", "Full-stack Development \u00b7 DevOps"},
            {"Focus.Learning", "Backend \u00b7 Cloud Architecture"},
            {"", ""},
            {"", ""},
            {"Grid.GitHub", "ShoreNexx"},
    };
    static final String RIGHT_FOOTER_LEFT = "ALL SYSTEMS NOMINAL";
    static final String RIGHT_FOOTER_RIGHT = "UTC+5:30 \u00b7 SAHIL.NODE";

    // ----------------------------------------------------------------------
    // Palette (dark theme, purple/lavender accents like the reference image)
    // ----------------------------------------------------------------------
    static final String BG = "#0b0e14";
    static final String PANEL_BG = "#0d1117";
    static final String BORDER = "#1f2733";
    static final String ACCENT_CYAN = "#4dd8e6";
    static final String ACCENT_RED = "#ff5f56";
    static final String ACCENT_YELLOW = "#ffbd2e";
    static final String ACCENT_GREEN = "#27c93f";
    static final String TEXT_DIM = "#5b6472";
    static final String TEXT_MAIN = "#c9d1d9";
    static final String TEXT_BRIGHT = "#e8eaf0";
    static final String DOT_LEADER = "#2a3140";
    // dark -> light purples
    static final String[] DOT_COLORS = {"#9b8cf2", "#b7a9ff", "#7c6fd6", "#e6e0ff", "#5f52b8"};

    static final String FONT_STACK =
            "ui-monospace, SFMono-Regular, 'JetBrains Mono', Consolas, 'Courier New', monospace";

    static final int W = 1160;
    static final int H = 606;
    static final int MARGIN = 24;
    static final int TITLEBAR_H = 40;
    static final int LEFT_W = 400;

    record Dot(double x, double y, double r, String color) {}

    public static void main(String[] args) throws IOException {
        String imagePath = null;
        String outputPath = "assets/banner-dark.svg";
        long seed = 7L;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--image" -> imagePath = args[++i];
                case "--output" -> outputPath = args[++i];
                case "--seed" -> seed = Long.parseLong(args[++i]);
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(1);
                }
            }
        }

        if (imagePath == null) {
            System.err.println("Usage: java BannerGenerator --image <path> [--output <path>] [--seed <n>]");
            System.exit(1);
            return;
        }

        File imgFile = new File(imagePath);
        if (!imgFile.exists()) {
            System.err.println("Image not found: " + imagePath);
            System.exit(1);
            return;
        }

        String svg = buildSvg(imgFile, seed);

        File outFile = new File(outputPath);
        if (outFile.getParentFile() != null) {
            outFile.getParentFile().mkdirs();
        }
        try (PrintWriter pw = new PrintWriter(outFile, "UTF-8")) {
            pw.print(svg);
        }
        System.out.println("Wrote " + outputPath);
    }

    // ------------------------------------------------------------------
    // Stipple / halftone dot generation from the source photo
    // ------------------------------------------------------------------
    static List<Dot> stipplePoints(File imageFile, int targetW, int targetH, int maxDots, Random rnd)
            throws IOException {
        BufferedImage original = ImageIO.read(imageFile);
        if (original == null) {
            throw new IOException("Could not decode image: " + imageFile);
        }

        int gridW = 90;
        int gridH = Math.max(1, Math.round(90f * original.getHeight() / original.getWidth()));

        // Downscale to a small grayscale working grid.
        BufferedImage gray = new BufferedImage(gridW, gridH, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = gray.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(original, 0, 0, gridW, gridH, null);
        g2.dispose();

        double cellW = (double) targetW / gridW;
        double cellH = (double) targetH / gridH;

        List<Dot> points = new ArrayList<>();

        for (int gy = 0; gy < gridH; gy++) {
            for (int gx = 0; gx < gridW; gx++) {
                int rgb = gray.getRGB(gx, gy) & 0xFF; // grayscale, so R=G=B
                double brightness = rgb / 255.0;      // 0 = black, 1 = white
                double darkness = 1.0 - brightness;

                if (darkness < 0.06) continue;

                int nDots = (int) Math.round(Math.pow(darkness, 1.4) * 6);
                if (nDots == 0 && rnd.nextDouble() < darkness * 0.15) {
                    nDots = 1;
                }

                double cx0 = gx * cellW;
                double cy0 = gy * cellH;

                for (int k = 0; k < nDots; k++) {
                    double jx = rnd.nextDouble() * cellW;
                    double jy = rnd.nextDouble() * cellH;
                    double x = round2(cx0 + jx);
                    double y = round2(cy0 + jy);
                    double r = round2(0.5 + rnd.nextDouble() * 1.1);

                    String color;
                    if (darkness > 0.75) {
                        color = DOT_COLORS[rnd.nextInt(2)]; // indices 0-1
                    } else if (darkness > 0.4) {
                        color = DOT_COLORS[1 + rnd.nextInt(3)]; // indices 1-3
                    } else {
                        color = DOT_COLORS[3 + rnd.nextInt(2)]; // indices 3-4
                    }

                    points.add(new Dot(x, y, r, color));
                }
            }
        }

        if (points.size() > maxDots) {
            Collections.shuffle(points, rnd);
            points = points.subList(0, maxDots);
        }
        return points;
    }

    static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ------------------------------------------------------------------
    // SVG assembly
    // ------------------------------------------------------------------
    static String buildSvg(File imageFile, long seed) throws IOException {
        Random rnd = new Random(seed);

        int rightW = W - MARGIN * 3 - LEFT_W;
        int panelTop = MARGIN * 2 + TITLEBAR_H;
        int panelH = H - panelTop - MARGIN;

        List<Dot> dots = stipplePoints(imageFile, LEFT_W - 40, panelH - 90, 18000, rnd);

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
                .append(W).append(" ").append(H).append("\" font-family=\"").append(FONT_STACK).append("\">\n");

        // background
        svg.append(String.format(
                "<rect x=\"0\" y=\"0\" width=\"%d\" height=\"%d\" rx=\"14\" fill=\"%s\"/>%n", W, H, BG));
        svg.append(String.format(
                "<rect x=\"0.5\" y=\"0.5\" width=\"%d\" height=\"%d\" rx=\"14\" fill=\"none\" stroke=\"%s\"/>%n",
                W - 1, H - 1, BORDER));

        // title bar
        svg.append(circle(MARGIN + 14, MARGIN + 20, 6, ACCENT_RED));
        svg.append(circle(MARGIN + 38, MARGIN + 20, 6, ACCENT_YELLOW));
        svg.append(circle(MARGIN + 62, MARGIN + 20, 6, ACCENT_GREEN));
        svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" fill=\"%s\" font-size=\"13\">%s</text>%n",
                W / 2, MARGIN + 25, TEXT_DIM, esc(WINDOW_TITLE)));
        svg.append(String.format(
                "<line x1=\"0\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"%s\"/>%n",
                MARGIN * 2 + TITLEBAR_H - 16, W, MARGIN * 2 + TITLEBAR_H - 16, BORDER));

        // ===== LEFT PANEL =====
        int lx = MARGIN, ly = panelTop;
        svg.append(String.format(
                "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"8\" fill=\"%s\" stroke=\"%s\"/>%n",
                lx, ly, LEFT_W, panelH, PANEL_BG, BORDER));
        svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" fill=\"%s\" font-size=\"13\" letter-spacing=\"1\">%s</text>%n",
                lx + 16, ly + 28, ACCENT_CYAN, esc(LEFT_LABEL)));
        svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" text-anchor=\"end\" fill=\"%s\" font-size=\"11\">%s</text>%n",
                lx + LEFT_W - 16, ly + 28, TEXT_DIM, esc(LEFT_META)));
        svg.append(line(lx + 16, ly + 40, lx + LEFT_W - 16, ly + 40, BORDER, null));

        int artX = lx + 20, artY = ly + 52;
        int artW = LEFT_W - 40, artH = panelH - 92;
        int bl = 14;
        int[][] corners = {{artX, artY, 1, 1}, {artX + artW, artY, -1, 1},
                {artX, artY + artH, 1, -1}, {artX + artW, artY + artH, -1, -1}};
        for (int[] c : corners) {
            int cx = c[0], cy = c[1], dx = c[2], dy = c[3];
            svg.append(String.format(
                    "<path d=\"M%d %d L%d %d L%d %d\" stroke=\"%s\" stroke-width=\"1.5\" fill=\"none\" opacity=\"0.7\"/>%n",
                    cx, cy + dy * bl, cx, cy, cx + dx * bl, cy, ACCENT_CYAN));
        }

        svg.append(String.format("<g transform=\"translate(%d,%d)\">%n", artX + 20, artY + 20));
        for (Dot d : dots) {
            svg.append(String.format("<circle cx=\"%s\" cy=\"%s\" r=\"%s\" fill=\"%s\"/>%n",
                    d.x(), d.y(), d.r(), d.color()));
        }
        svg.append("</g>\n");

        String footerLeftTxt = String.format(LEFT_FOOTER_TEMPLATE, dots.size());
        svg.append(line(lx + 16, ly + panelH - 28, lx + LEFT_W - 16, ly + panelH - 28, BORDER, null));
        svg.append(String.format("<text x=\"%d\" y=\"%d\" fill=\"%s\" font-size=\"11\">%s</text>%n",
                lx + 16, ly + panelH - 10, TEXT_DIM, esc(footerLeftTxt)));

        // ===== RIGHT PANEL =====
        int rx = lx + LEFT_W + MARGIN, ry = panelTop;
        svg.append(String.format(
                "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"8\" fill=\"%s\" stroke=\"%s\"/>%n",
                rx, ry, rightW, panelH, PANEL_BG, BORDER));
        svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" fill=\"%s\" font-size=\"13\" letter-spacing=\"1\">%s</text>%n",
                rx + 16, ry + 28, ACCENT_CYAN, esc(RIGHT_LABEL)));

        double pillW = 14 + RIGHT_HANDLE.length() * 7.5;
        double pillX = rx + rightW - 16 - pillW;
        svg.append(circle((int) (pillX - 70), ry + 24, 4, ACCENT_RED));
        svg.append(String.format("<text x=\"%s\" y=\"%d\" fill=\"%s\" font-size=\"11\">%s</text>%n",
                pillX - 62, ry + 28, ACCENT_RED, esc(RIGHT_STATUS)));
        svg.append(String.format(
                "<rect x=\"%s\" y=\"%d\" width=\"%s\" height=\"20\" rx=\"10\" fill=\"none\" stroke=\"%s\" opacity=\"0.6\"/>%n",
                pillX, ry + 14, pillW, ACCENT_CYAN));
        svg.append(String.format(
                "<text x=\"%s\" y=\"%d\" text-anchor=\"middle\" fill=\"%s\" font-size=\"11\">%s</text>%n",
                pillX + pillW / 2, ry + 28, ACCENT_CYAN, esc(RIGHT_HANDLE)));

        svg.append(line(rx + 16, ry + 40, rx + rightW - 16, ry + 40, BORDER, null));

        double rowY = ry + 66;
        double rowH = Math.min((panelH - 66 - 40) / (double) Math.max(ROWS.length, 1), 26);
        int labelX = rx + 16;
        int valueRightX = rx + rightW - 16;

        for (String[] row : ROWS) {
             if (row[0].isEmpty()) {
                rowY += 12;
                continue;
            }
            String label = row[0], value = row[1];
            svg.append(String.format("<text x=\"%d\" y=\"%s\" fill=\"%s\" font-size=\"13\">%s</text>%n",
                    labelX, fmt(rowY), TEXT_MAIN, esc(label)));
            svg.append(String.format(
                    "<text x=\"%d\" y=\"%s\" text-anchor=\"end\" fill=\"%s\" font-size=\"13\">%s</text>%n",
                    valueRightX, fmt(rowY), TEXT_BRIGHT, esc(value)));

            double labelEnd = labelX + label.length() * 7.3 + 6;
            double valueStart = valueRightX - value.length() * 7.3 - 6;
            if (valueStart > labelEnd) {
                svg.append(String.format(
                        "<line x1=\"%s\" y1=\"%s\" x2=\"%s\" y2=\"%s\" stroke=\"%s\" stroke-width=\"1\" stroke-dasharray=\"1,3\"/>%n",
                        fmt(labelEnd), fmt(rowY - 4), fmt(valueStart), fmt(rowY - 4), DOT_LEADER));
            }
            rowY += rowH;
        }

        svg.append(line(rx + 16, ry + panelH - 28, rx + rightW - 16, ry + panelH - 28, BORDER, null));
        svg.append(circle(rx + 22, ry + panelH - 14, 3, ACCENT_GREEN));
        svg.append(String.format("<text x=\"%d\" y=\"%d\" fill=\"%s\" font-size=\"11\">%s</text>%n",
                rx + 32, ry + panelH - 10, ACCENT_GREEN, esc(RIGHT_FOOTER_LEFT)));
        svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" text-anchor=\"end\" fill=\"%s\" font-size=\"11\">%s</text>%n",
                rx + rightW - 16, ry + panelH - 10, TEXT_DIM, esc(RIGHT_FOOTER_RIGHT)));

        svg.append("</svg>");
        return svg.toString();
    }

    static String circle(double cx, double cy, double r, String fill) {
        return String.format("<circle cx=\"%s\" cy=\"%s\" r=\"%s\" fill=\"%s\"/>%n", fmt(cx), fmt(cy), fmt(r), fill);
    }

    static String line(double x1, double y1, double x2, double y2, String stroke, String extra) {
        return String.format("<line x1=\"%s\" y1=\"%s\" x2=\"%s\" y2=\"%s\" stroke=\"%s\"/>%n",
                fmt(x1), fmt(y1), fmt(x2), fmt(y2), stroke);
    }

    static String fmt(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.valueOf(Math.round(v * 100.0) / 100.0);
    }
}
