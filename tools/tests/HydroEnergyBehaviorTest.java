package org.dynmap.modsupport;
import java.util.*;import java.lang.reflect.Proxy;
import net.minecraft.nbt.NBTTagCompound;
import org.dynmap.hdmap.renderer.HydroEnergyWaterRenderer;
import org.dynmap.renderer.*;import org.dynmap.utils.*;
public class HydroEnergyBehaviorTest {
 static class Fixture extends HydroEnergyWaterRenderer {
  float level;Fixture(){initializeBlockKinds();}
  protected void initializeBlockKinds(){opaque[2]=true;}
  protected Float waterLevel(MapDataContext context){return level;}
 }
 static MapDataContext context(final int y){return (MapDataContext)Proxy.newProxyInstance(MapDataContext.class.getClassLoader(),new Class<?>[]{MapDataContext.class},(p,m,v)->{
  if(m.getName().equals("getY"))return y;
  if(m.getName().equals("getBlockTypeID"))return 4;
  if(m.getName().equals("getBlockTypeIDAt"))return 0;
  if(m.getName().equals("getPatchFactory"))return new PatchDefinitionFactory();
  throw new AssertionError(m.getName());});}
 public static void main(String[] args) {
  NBTTagCompound tag=new NBTTagCompound();tag.setFloat("walv",64.5f);tag.setInteger("dimI",0);
  tag.setInteger("limW",-20);tag.setInteger("limN",-20);tag.setInteger("limE",20);tag.setInteger("limS",20);tag.setInteger("limU",80);
  HydroEnergySnapshots.Dam dam=new HydroEnergySnapshots.Dam(tag,"overworld");Map<Integer,HydroEnergySnapshots.Dam> input=new HashMap<>();input.put(4,dam);
  HydroEnergySnapshots.publish(input,null);input.clear();
  if(HydroEnergySnapshots.get(4,"overworld")!=dam||HydroEnergySnapshots.get(4,"DIM-1")!=null||HydroEnergySnapshots.get(4,null)!=null)throw new AssertionError("World identity/snapshot isolation");
  Fixture renderer=new Fixture();renderer.level=64.5f;RenderPatch[] partial=renderer.getRenderPatchList(context(64));
  if(partial.length!=12)throw new AssertionError("Fractional dam surface missing");
  double maximum=0;for(RenderPatch patch:partial){PatchDefinition p=(PatchDefinition)patch;maximum=Math.max(maximum,Math.max(p.y0,Math.max(p.yu,p.yv)));}
  if(Math.abs(maximum-(.5-.0010000000474974513))>1e-8)throw new AssertionError("Wrong dam height");
  if(renderer.getRenderPatchList(context(65)).length!=0)throw new AssertionError("Water rendered above dam level");
  renderer.level=66;if(renderer.getRenderPatchList(context(64)).length!=10)throw new AssertionError("Submerged cell has false top");
  System.out.println("HydroEnergy immutable world-scoped snapshot, fractional surface, dry and submerged cell tests passed");
 }
}
