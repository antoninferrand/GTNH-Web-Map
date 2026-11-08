import java.lang.reflect.*;
import java.util.*;
import org.dynmap.hdmap.TexturePack;
import org.dynmap.hdmap.renderer.InventoryMimicRenderer;
import org.dynmap.renderer.*;
import org.dynmap.utils.PatchDefinitionFactory;

public class InventoryMimicBehaviorTest {
    static class Renderer extends InventoryMimicRenderer {
        Object item;
        @Override protected int blockForItem(Object id) { item=id; return Integer.valueOf(123).equals(id)?100:0; }
    }
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static Map<String,Object> item(int slot,Object id,int damage) {
        Map<String,Object> result=new HashMap<>();result.put("Slot",(byte)slot);result.put("id",id);result.put("Damage",(short)damage);return result;
    }
    public static void main(String[] args) throws Exception {
        Method init=TexturePack.HDTextureMap.class.getDeclaredMethod("initializeTable");init.setAccessible(true);init.invoke(null);
        new TexturePack.HDTextureMap(Collections.singletonList(100),1<<7,new int[]{1000,1001,1002,1003,1004,1005},null,
                TexturePack.BlockTransparency.OPAQUE,false,0x123456,null,null,true).addToTable();
        new TexturePack.HDTextureMap(Collections.singletonList(42),0xffff,new int[]{900,901,902,903,904,905},null,
                TexturePack.BlockTransparency.TRANSPARENT,false,0,null,null,true).addToTable();
        Renderer renderer=new Renderer();renderer.initializeRenderer(new PatchDefinitionFactory(),42,0xffff,new HashMap<String,String>());
        final Object[] inventory={Arrays.asList(item(1,321,0),item(0,123,7))};
        MapDataContext ctx=(MapDataContext)Proxy.newProxyInstance(MapDataContext.class.getClassLoader(),new Class<?>[]{MapDataContext.class},
                (proxy,method,values)->{
                    switch(method.getName()){
                        case "getBlockTileEntityField":return inventory[0];
                        case "getBlockTypeID":return 42;
                        case "getBlockData":return 0;
                        default:throw new AssertionError("Unexpected world dependency "+method.getName());
                    }
                });
        CustomRendererData data=renderer.getRenderData(ctx);
        check(data.getCustomMesh().length==6,"Mimic must retain six standard cube faces");
        int[] expected={1,4,0,3,2,5};
        for(int i=0;i<6;i++) {
            int patch=data.getCustomMesh()[i].getTextureIndex();
            check(patch==expected[i],"Cube face uses wrong BlockStep mapping");
            check(data.getCustomTextureMapper().getTextureLayersForPatchId(patch)[0]==1000+expected[i],"Inventory metadata face texture mismatch");
        }
        check(data.getCustomColorMultiplier(0,0).getColorMultiplier(ctx)==0x123456,"Copied block tint omitted");
        inventory[0]=Arrays.asList(item(1,123,7));
        check(renderer.getRenderData(ctx).getCustomTextureMapper().getTextureLayersForPatchId(0)[0]==900,"Non-slot0 inventory must not choose a block");
        inventory[0]=Arrays.asList(item(0,999,7));
        check(renderer.getRenderData(ctx).getCustomTextureMapper().getTextureLayersForPatchId(0)[0]==900,"Non-block item must retain default sprite");
        inventory[0]=Arrays.asList(item(0,123,25));
        check(renderer.getRenderData(ctx).getCustomTextureMapper().getTextureLayersForPatchId(0)[0]==900,"Invalid block metadata must not alias another variant");
        inventory[0]=null;
        check(renderer.getRenderData(ctx).getCustomTextureMapper().getTextureLayersForPatchId(0)[0]==900,"Empty inventory must retain default sprite");
        System.out.println("Mimic slot selection, metadata, six face mappings, tint and invalid/empty inventory passed");
    }
}
