import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Annotates real fixture renders; does not synthesize missing block pixels. */
public class ContactSheet {
    private static int[] size(BufferedImage image, int maxWidth, int maxHeight) {
        double scale = Math.min(1, Math.min((double) maxWidth / image.getWidth(), (double) maxHeight / image.getHeight()));
        return new int[] { (int) Math.round(image.getWidth() * scale), (int) Math.round(image.getHeight() * scale) };
    }

    public static void main(String[] args) throws Exception {
        BufferedImage flat = ImageIO.read(new File(args[0]));
        BufferedImage surface = ImageIO.read(new File(args[1]));
        BufferedImage inset = ImageIO.read(new File(args[2]));
        int[] flatSize = size(flat, 560, 800), surfaceSize = size(surface, 900, 900), insetSize = size(inset, 440, 500);
        int insetY = 145 + flatSize[1];
        int height = Math.max(1260, Math.max(insetY + insetSize[1] + 20, 145 + surfaceSize[1] + 320));
        BufferedImage sheet = new BufferedImage(1540, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(22, 28, 36)); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("GTNH Web Map: saved block rendering fixture", 20, 32);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.drawString(args[4], 20, 57);
        g.drawString("157 baseline variants; tin examples; native model row; separate natural vending panel. See saved-state manifest.", 20, 80);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(flat, 20, 115, flatSize[0], flatSize[1], null);
        g.drawImage(surface, 610, 115, surfaceSize[0], surfaceSize[1], null);
        g.drawImage(inset, 20, insetY, insetSize[0], insetSize[1], null);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("Flat (source " + flat.getWidth() + "x" + flat.getHeight() + ")", 20, 105);
        g.drawString("Surface (source " + surface.getWidth() + "x" + surface.getHeight() + ")", 610, 105);
        g.drawString("Labelled detail (source " + inset.getWidth() + "x" + inset.getHeight() + ")", 20, insetY - 10);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        String[] notes = {
            "Original grid: 13 columns, 3-block spacing, Y=80.",
            "Grid origin (-660,80,855); render center (-642,80,876).",
            "Tin samples: X=-660..-633, Z=897.",
            "Native row: Z=906/909; walrus scale=1 in all four facings.",
            "Cold vending panel (-627,80,900) has one missing corner.",
            "Separate formed SOUTH-facing panel: (-615,80,900).",
            "Its active state is produced by normal source ticks, with no energy/items.",
            "",
            "Actual map PNGs, framed by the production projection matrix.",
            "Views preserve image aspect ratios; source pixels remain separately saved.",
            "Source/unit checks, saved-state proof, and map pixels are separate evidence.",
            "These views are not a Minecraft-client parity comparison.",
            "Large TESR effects and owner-cell overhang retain documented limits."
        };
        int y = Math.max(115 + surfaceSize[1] + 30, insetY + 20);
        for (String line : notes) { g.drawString(line, 480, y); y += 22; }
        g.dispose();
        ImageIO.write(sheet, "png", new File(args[3]));
    }
}
