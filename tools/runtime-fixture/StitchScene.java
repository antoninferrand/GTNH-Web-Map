import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import javax.imageio.ImageIO;
import org.dynmap.ConfigurationNode;
import org.dynmap.DynmapCore;
import org.dynmap.hdmap.IsoHDPerspective;
import org.dynmap.utils.Matrix3D;
import org.dynmap.utils.Vector3D;

/** Stitches real renderer output using the production projection matrix. */
public class StitchScene {
    public static void main(String[] args) throws Exception {
        // input tile directory, output PNG, azimuth, inclination, scale, x0,z0,x1,z1
        ConfigurationNode config = new ConfigurationNode();
        config.put("name", "fixture");
        config.put("azimuth", Double.valueOf(args[2]));
        config.put("inclination", Double.valueOf(args[3]));
        config.put("scale", Integer.valueOf(args[4]));
        IsoHDPerspective perspective = new IsoHDPerspective(new DynmapCore(), config);
        Field field = IsoHDPerspective.class.getDeclaredField("world_to_map");
        field.setAccessible(true);
        Matrix3D matrix = (Matrix3D) field.get(perspective);
        double minX = Double.POSITIVE_INFINITY, minY = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX;
        for (int x : new int[] { Integer.parseInt(args[5]), Integer.parseInt(args[7]) }) {
            for (int z : new int[] { Integer.parseInt(args[6]), Integer.parseInt(args[8]) }) {
                for (int y : new int[] { 78, 86 }) {
                    Vector3D world = new Vector3D(x, y, z), map = new Vector3D();
                    matrix.transform(world, map);
                    minX = Math.min(minX, map.x); maxX = Math.max(maxX, map.x);
                    minY = Math.min(minY, map.y); maxY = Math.max(maxY, map.y);
                }
            }
        }
        int left = (int) Math.floor(minX) - 16, top = (int) Math.ceil(maxY) + 16;
        int width = (int) Math.ceil(maxX) - left + 16, height = top - (int) Math.floor(minY) + 16;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        int count = 0;
        for (int tx = Math.floorDiv(left, 128); tx <= Math.floorDiv(left + width, 128); tx++) {
            for (int ty = Math.floorDiv(top - height, 128); ty <= Math.floorDiv(top, 128); ty++) {
                File tile = new File(args[0], (tx >> 5) + "_" + (ty >> 5) + "/" + tx + "_" + ty + ".png");
                if (tile.isFile()) {
                    graphics.drawImage(ImageIO.read(tile), tx * 128 - left, top - (ty + 1) * 128, null);
                    count++;
                }
            }
        }
        graphics.dispose();
        ImageIO.write(image, "png", new File(args[1]));
        System.out.println(count + " source tiles stitched into " + width + "x" + height + " " + args[1]);
    }
}
