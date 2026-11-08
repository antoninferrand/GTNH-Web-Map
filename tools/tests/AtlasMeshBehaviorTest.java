import java.util.*;
import org.dynmap.hdmap.renderer.AtlasMesh;

public class AtlasMeshBehaviorTest {
    static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
    static double area(double[] p) {
        double x=(p[6]-p[1])*(p[12]-p[2])-(p[7]-p[2])*(p[11]-p[1]);
        double y=(p[7]-p[2])*(p[10]-p[0])-(p[5]-p[0])*(p[12]-p[2]);
        double z=(p[5]-p[0])*(p[11]-p[1])-(p[6]-p[1])*(p[10]-p[0]);
        return .5*Math.sqrt(x*x+y*y+z*z);
    }
    static void validate(double[] source, int columns, int rows) {
        List<AtlasMesh.Triangle> result=AtlasMesh.split(source,columns,rows);
        double total=0;
        for(AtlasMesh.Triangle child:result) {
            total+=area(child.vertices);
            check(child.texture>=0 && child.texture<columns*rows,"Cell index outside atlas");
            for(int i=0;i<15;i+=5) {
                check(child.vertices[i+3]>=0 && child.vertices[i+3]<=1,"U outside cell");
                check(child.vertices[i+4]>=0 && child.vertices[i+4]<=1,"V outside cell");
            }
        }
        check(Math.abs(total-area(source))<1e-8,"Clipping changed surface area: "+total);
    }
    public static void main(String[] args) {
        double[] squareHalf={0,0,0,0,0, 1,0,0,1,0, 0,1,0,0,1};
        validate(squareHalf,8,8);
        for(int grid:new int[]{1,2,3,8,16}) {
            Random random=new Random(413);
            for(int i=0;i<80;i++) {
                double[] p=squareHalf.clone();
                for(int v=0;v<15;v+=5) { p[v+3]=random.nextDouble()*3-1; p[v+4]=random.nextDouble()*3-1; }
                validate(p,grid,grid);
            }
        }
        double[] upper={0,0,0,.1,.9, 1,0,0,.2,.9, 0,1,0,.1,.8};
        check(AtlasMesh.split(upper,2,2).get(0).texture==0,"Bottom-origin UV must map upper-left to PNG row0");
        double[] lower=upper.clone();for(int i=4;i<15;i+=5)lower[i]-=.5;
        check(AtlasMesh.split(lower,2,2).get(0).texture==2,"Lower-left must select PNG row1");
        double[] constantUV={0,0,0,.5,.5, 1,0,0,.5,.5, 0,1,0,.5,.5};
        validate(constantUV,8,8);
        check(AtlasMesh.split(constantUV,8,8).size()==1,"Constant UV must not duplicate boundary faces");
        double[] bad=squareHalf.clone();bad[0]=Double.NaN;
        try { AtlasMesh.split(bad,8,8); throw new AssertionError("Accepted NaN"); } catch(IllegalArgumentException expected) {}
        System.out.println("Atlas clipping preserves area and UV/cell identity for400 randomized triangles plus boundary cases");
    }
}
