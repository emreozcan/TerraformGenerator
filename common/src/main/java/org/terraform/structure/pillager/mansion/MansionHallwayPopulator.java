package org.terraform.structure.pillager.mansion;

import java.util.Random;

import org.bukkit.Material;
import org.terraform.coregen.PopulatorDataAbstract;
import org.terraform.structure.room.CubeRoom;

public class MansionHallwayPopulator extends MansionRoomPopulator {

	public MansionHallwayPopulator(CubeRoom room) {
		super(room);
	}

	@Override
	public void decorateRoom(PopulatorDataAbstract data, Random random) {
		int[] lowerBounds = this.getRoom().getLowerCorner();
		int[] upperBounds = this.getRoom().getUpperCorner();
		for(int x = lowerBounds[0]; x <= upperBounds[0]; x++)
			for(int z = lowerBounds[1]; z <= upperBounds[1]; z++)
				data.setType(x,this.getRoom().getY(), z, Material.BROWN_WOOL);
	
	}

}
