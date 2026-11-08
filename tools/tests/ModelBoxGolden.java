import java.lang.reflect.*;

/** Dumps actual Minecraft ModelBox positions/UVs without constructing a client or rendering through GL. */
public class ModelBoxGolden {
    public static void main(String[] args) throws Exception {
        Class<?> rendererType=Class.forName("net.minecraft.client.model.ModelRenderer");
        Class<?> boxType=Class.forName("net.minecraft.client.model.ModelBox");
        Class<?> unsafeType=Class.forName("sun.misc.Unsafe");
        Field singleton=unsafeType.getDeclaredField("theUnsafe");singleton.setAccessible(true);
        Object unsafe=singleton.get(null);
        Object renderer=unsafeType.getMethod("allocateInstance",Class.class).invoke(unsafe,rendererType);
        rendererType.getField("field_78801_a").setFloat(renderer,64);
        rendererType.getField("field_78799_b").setFloat(renderer,32);
        System.out.print("[");
        for(boolean mirror:new boolean[]{false,true}) {
            rendererType.getField("field_78809_i").setBoolean(renderer,mirror);
            Object box=boxType.getConstructor(rendererType,int.class,int.class,float.class,float.class,float.class,int.class,int.class,int.class,float.class)
                    .newInstance(renderer,7,9,1.25f,2f,-4f,5,7,3,0f);
            Field quadField=boxType.getDeclaredField("field_78254_i");quadField.setAccessible(true);
            Object[] quads=(Object[])quadField.get(box);
            if(mirror)System.out.print(",");System.out.print("[");boolean first=true;
            for(Object quad:quads) {
                Object[] vertices=(Object[])quad.getClass().getField("field_78239_a").get(quad);
                for(int[] triangle:new int[][]{{0,1,2},{0,2,3}}) {
                    if(!first)System.out.print(",");first=false;System.out.print("[");
                    boolean firstValue=true;
                    for(int index:triangle) {
                        Object vertex=vertices[index];Class<?> t=vertex.getClass();Object point=t.getField("field_78243_a").get(vertex);
                        double[] data={point.getClass().getField("field_72450_a").getDouble(point),
                                point.getClass().getField("field_72448_b").getDouble(point),
                                point.getClass().getField("field_72449_c").getDouble(point),
                                t.getField("field_78241_b").getFloat(vertex),1-t.getField("field_78242_c").getFloat(vertex)};
                        for(double value:data){if(!firstValue)System.out.print(",");firstValue=false;System.out.print(value);}
                    }
                    System.out.print("]");
                }
            }
            System.out.print("]");
        }
        System.out.println("]");
    }
}
