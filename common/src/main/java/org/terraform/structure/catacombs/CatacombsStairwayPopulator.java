package org.terraform.structure.catacombs;

import java.util.Random;

import org.bukkit.Material;
import org.terraform.coregen.populatordata.PopulatorDataAbstract;
import org.terraform.data.SimpleBlock;
import org.terraform.structure.room.CubeRoom;
import org.terraform.utils.SphereBuilder;

public class CatacombsStairwayPopulator extends CatacombsStandardPopulator {

	public CatacombsStairwayPopulator(Random rand, boolean forceSpawn, boolean unique) {
		super(rand, forceSpawn, unique);
	}
	

    @Override
    public void populate(PopulatorDataAbstract data, CubeRoom room) {
    	super.populate(data, room);
    	SimpleBlock center = room.getCenterSimpleBlock(data).getUp();
    	new SphereBuilder(new Random(), center.getDown(), Material.CAVE_AIR)
    	.setRadius(3f)
    	.setHardReplace(true)
    	.build();
    	
    	
    	super.spawnHangingChains(data, room);
    }

}
