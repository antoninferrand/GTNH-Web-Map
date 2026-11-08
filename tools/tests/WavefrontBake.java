import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.json.simple.*;
import org.dynmap.hdmap.renderer.WavefrontModel;
/** Offline import using the same strict parser used by the production renderer. */
public class WavefrontBake {
    public static void main(String[] args)throws Exception {
        WavefrontModel model=WavefrontModel.read(Files.newInputStream(Paths.get(args[0])),args[1].equals("*")?null:Collections.singleton(args[1]));
        JSONArray triangles=new JSONArray();
        for(double[] triangle:model.triangles()) {
            JSONArray values=new JSONArray();
            for(int i=0;i<15;i++)values.add(triangle[i]+(i%5<3?Double.parseDouble(args[2+i%5]):0));
            triangles.add(values);
        }
        JSONObject part=new JSONObject();part.put("name",args[1]);part.put("triangles",triangles);
        System.out.println(part.toJSONString());
    }
}
