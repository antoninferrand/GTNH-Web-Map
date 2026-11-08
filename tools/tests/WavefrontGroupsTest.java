import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.dynmap.hdmap.renderer.WavefrontModel;
public class WavefrontGroupsTest {
    public static void main(String[] args)throws Exception {
        String obj="v 0 0 0\nv 1 0 0\nv 0 1 0\nvt 0 0\nvt 1 0\nvt 0 1\no one\nf 1/1 2/2 3/3\ng two shared\nf -3/1 -2/2 -1/3\n";
        byte[] bytes=obj.getBytes(StandardCharsets.UTF_8);
        WavefrontModel all=WavefrontModel.read(new ByteArrayInputStream(bytes));
        WavefrontModel one=WavefrontModel.read(new ByteArrayInputStream(bytes),Collections.singleton("one"));
        WavefrontModel two=WavefrontModel.read(new ByteArrayInputStream(bytes),Collections.singleton("shared"));
        if(all.triangleCount()!=2||one.triangleCount()!=1||two.triangleCount()!=1)throw new AssertionError("group selection");
        List<double[]> copy=one.triangles();copy.get(0)[0]=99;
        if(one.triangles().get(0)[0]!=0)throw new AssertionError("mutable internal triangle");
        try{copy.clear();throw new AssertionError("mutable list");}catch(UnsupportedOperationException expected){}
        System.out.println("OBJ object/group selection, shared groups, negative indices and immutable deep copies passed");
    }
}
