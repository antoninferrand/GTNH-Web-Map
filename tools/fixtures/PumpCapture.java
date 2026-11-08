package capture;
import java.util.*;import org.json.simple.*;
/** CPU boundary for the unmodified source TESR, including installed ModelBellows. */
public class Capture {
 public static String texture;static double[] matrix=identity();static Deque<double[]> stack=new ArrayDeque<>();
 static JSONArray boxes=new JSONArray(),triangles=new JSONArray();static List<double[]> pending=new ArrayList<>();static int mode;
 static double[] identity(){double[] m=new double[16];for(int i=0;i<4;i++)m[i*4+i]=1;return m;}
 static void multiply(double[] next){double[] out=new double[16];for(int i=0;i<4;i++)for(int j=0;j<4;j++)for(int k=0;k<4;k++)out[i*4+j]+=matrix[i*4+k]*next[k*4+j];matrix=out;}
 public static void push(){stack.push(matrix.clone());}public static void pop(){matrix=stack.pop();}
 public static void translate(double x,double y,double z){double[] m=identity();m[3]=x;m[7]=y;m[11]=z;multiply(m);}
 public static void scale(double x,double y,double z){double[] m=identity();m[0]=x;m[5]=y;m[10]=z;multiply(m);}
 public static void rotate(double angle,double x,double y,double z){double len=Math.sqrt(x*x+y*y+z*z);x/=len;y/=len;z/=len;double c=Math.cos(Math.toRadians(angle)),s=Math.sin(Math.toRadians(angle)),t=1-c;double[] m=identity();
  m[0]=t*x*x+c;m[1]=t*x*y-s*z;m[2]=t*x*z+s*y;m[4]=t*x*y+s*z;m[5]=t*y*y+c;m[6]=t*y*z-s*x;m[8]=t*x*z-s*y;m[9]=t*y*z+s*x;m[10]=t*z*z+c;multiply(m);}
 static JSONArray array(double[] values){JSONArray a=new JSONArray();for(double value:values)a.add(value);return a;}
 static JSONArray array(float[] values){double[] a=new double[values.length];for(int i=0;i<a.length;i++)a[i]=values[i];return array(a);}
 static JSONArray array(int[] values){double[] a=new double[values.length];for(int i=0;i<a.length;i++)a[i]=values[i];return array(a);}
 public static void box(float[] box,int[] uv,int[] atlas,float[] pivot,float[] rotation,boolean mirror,float scale){JSONObject data=new JSONObject();data.put("box",array(box));data.put("uv",array(uv));data.put("atlas",array(atlas));data.put("pivot",array(pivot));data.put("rotation",array(rotation));data.put("mirror",mirror);data.put("scale",scale);data.put("matrix",array(matrix));data.put("texture",texture);boxes.add(data);}
 public static void begin(int value){mode=value;pending.clear();}
 public static void vertex(double x,double y,double z,double u,double v){double[] point={x,y,z,1},out=new double[5];for(int i=0;i<3;i++)for(int j=0;j<4;j++)out[i]+=matrix[i*4+j]*point[j];out[3]=u;out[4]=1-v;pending.add(out);}
 static void triangle(int a,int b,int c){double[] values=new double[15];int i=0;for(int index:new int[]{a,b,c})for(double v:pending.get(index))values[i++]=v;JSONObject data=new JSONObject();data.put("texture",texture);data.put("vertices",array(values));triangles.add(data);}
 public static void end(){if(mode==4){if(pending.size()!=3)throw new AssertionError("triangle source");triangle(0,1,2);}else if(mode==7){if(pending.size()!=4)throw new AssertionError("quad source");triangle(0,1,2);triangle(0,2,3);}else throw new AssertionError("unsupported mode");}
 public static void main(String[] args){new witchinggadgets.client.render.TileRenderEssentiaPump().renderTileEntityAt(new witchinggadgets.common.blocks.tiles.TileEntityEssentiaPump(),0,0,0,0);JSONObject result=new JSONObject();result.put("triangles",triangles);result.put("boxes",boxes);System.out.println(result.toJSONString());}
}
