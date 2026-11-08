import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Annotates real fixture renders; does not synthesize missing block pixels. */
public class ContactSheet {
    public static void main(String[] args) throws Exception {
        BufferedImage sheet = new BufferedImage(1400, 1220, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(22, 28, 36)); g.fillRect(0, 0, 1400, 1220);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("GTNH Web Map: saved cold-block fixture", 20, 32);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.drawString(args[4] + " | 157 baseline variants + 10 tin pipes + 2 vending machines", 20, 57);
        g.drawString("All 169 saved IDs verified; disabled, inactive, zero stored energy. North-facing. Stone pedestals below.", 20, 80);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(ImageIO.read(new File(args[0])), 20, 115, 528, 624, null);
        g.drawImage(ImageIO.read(new File(args[1])), 570, 115, 805, 749, null);
        g.drawImage(ImageIO.read(new File(args[2])), 20, 775, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("Flat: source 704x832, displayed at 75%", 20, 105);
        g.drawString("Surface: source 1073x998, displayed at 75%", 570, 105);
        g.drawString("Tin pipes 5589-5598: native pixels", 20, 763);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        String[] notes = {
            "13 columns; 3-block spacing; block Y=80.",
            "Grid origin (-660,80,855); center (-642,80,876).",
            "Pipes: X=-660..-633, Z=897.",
            "Vending casing panel: around (-627,80,900).",
            "",
            "These are actual map PNGs, projected with the production matrix.",
            "Small pipe cubes appear above the larger stone pedestals.",
            "Inactive machines may legitimately have dark front panels.",
            "The cold scene does not exercise active/state-specific overlays.",
            "Source/unit checks and saved-ID proof are separate evidence;",
            "this is not a Minecraft-client parity comparison.",
            "",
            "Initial nanochip minimal-NBT errors are preserved in the log;",
            "their saved [00] byte arrays await clean reload validation."
        };
        int y = 900;
        for (String line : notes) { g.drawString(line, 470, y); y += 22; }
        g.dispose();
        ImageIO.write(sheet, "png", new File(args[3]));
    }
}
