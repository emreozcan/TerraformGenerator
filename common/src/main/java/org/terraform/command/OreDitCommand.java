package org.terraform.command;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.terraform.command.contants.InvalidArgumentException;
import org.terraform.command.contants.TerraCommand;
import org.terraform.main.TerraformGeneratorPlugin;
import org.terraform.utils.version.OneOneSevenBlockHandler;

import java.util.HashMap;
import java.util.Stack;

public class OreDitCommand extends TerraCommand {

    public OreDitCommand(TerraformGeneratorPlugin plugin, String... aliases) {
        super(plugin, aliases);
    }

    @Override
    public String getDefaultDescription() {
        return "Calculates the percentages of each ore type within the chunk you're in";
    }

    @Override
    public boolean canConsoleExec() {
        return false;
    }

    @Override
    public boolean hasPermission(CommandSender sender) {

        return sender.isOp();
    }

    @Override
    public void execute(CommandSender sender, Stack<String> args)
            throws InvalidArgumentException {

    	Material[] auditMat = new Material[] {
    			Material.DIORITE,
    			Material.ANDESITE,
    			Material.GRANITE,
    			Material.GRAVEL,
    			Material.COAL_ORE,
    			Material.IRON_ORE,
    			Material.GOLD_ORE,
    			Material.DIAMOND_ORE,
    			Material.LAPIS_ORE,
    			Material.REDSTONE_ORE,
    			Material.EMERALD_ORE,
    			OneOneSevenBlockHandler.DRIPSTONE_BLOCK,
    			OneOneSevenBlockHandler.DEEPSLATE,
    			OneOneSevenBlockHandler.TUFF,
    			OneOneSevenBlockHandler.COPPER_ORE
    	};
        Player p = (Player) sender;
        Chunk c = p.getLocation().getChunk();
        HashMap<Material,Integer> ores = new HashMap<>();
    	for(Material audit:auditMat) {
        	ores.put(audit, 0);
        }
        p.sendMessage("Sampling surrounding chunk radius of 3...");
        
        for(int cx = -3; cx <= 3; cx++)
            for(int cz = -3; cz <= 3; cz++) {
            	Chunk target = p.getWorld().getChunkAt(c.getX()+cx, c.getZ()+cz);
                
                for(int x = 0; x < 16; x++) {
                	for(int z = 0; z < 16; z++) {
                		for(int y = (int) p.getLocation().getY(); y > 0; y--) {
                			Material mat = target.getBlock(x, y, z).getType();
                			for(Material audit:auditMat) {
                				if(mat == audit) {
                					ores.put(audit, ores.get(audit)+1);
                				}
                			}
                		}
                    }
                }
            }
        
        p.sendMessage("-----[Ore Count]-----");
        for(Material audit:auditMat) {
        	p.sendMessage(audit.toString() + " - " + ores.get(audit));
        }
        
    }
}
