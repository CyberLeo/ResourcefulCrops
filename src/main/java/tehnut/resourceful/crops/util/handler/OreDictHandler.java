package tehnut.resourceful.crops.util.handler;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import tehnut.resourceful.crops.ConfigHandler;
import tehnut.resourceful.crops.api.base.Seed;
import tehnut.resourceful.crops.api.registry.SeedRegistry;
import tehnut.resourceful.crops.block.BlockROre;
import tehnut.resourceful.crops.item.ItemPouch;
import tehnut.resourceful.crops.item.ItemSeed;
import tehnut.resourceful.crops.item.ItemShard;
import tehnut.resourceful.crops.registry.BlockRegistry;
import tehnut.resourceful.crops.registry.ItemRegistry;

public class OreDictHandler {

    public static void load() {
        registerSeeds();
        registerShards();
        if (ConfigHandler.enableSeedPouches)
            registerPouches();

        OreDictionary.registerOre("oreGaianite", new ItemStack(BlockRegistry.getBlock(BlockROre.class), 1, 0));
        OreDictionary.registerOre("oreGaianite", new ItemStack(BlockRegistry.getBlock(BlockROre.class), 1, 1));
    }

    private static void registerSeeds() {
        for (Seed seed : SeedRegistry.getSeedList())
            OreDictionary.registerOre("rcropSeed" + seed.getName().replace(" ", ""), new ItemStack(ItemRegistry.getItem(ItemSeed.class), 1, SeedRegistry.getIndexOf(seed)));
    }

    private static void registerShards() {
        for (Seed seed : SeedRegistry.getSeedList())
            OreDictionary.registerOre("rcropShard" + seed.getName().replace(" ", ""), new ItemStack(ItemRegistry.getItem(ItemShard.class), 1, SeedRegistry.getIndexOf(seed)));
    }

    private static void registerPouches() {
        for (Seed seed : SeedRegistry.getSeedList())
            OreDictionary.registerOre("rcropPouch" + seed.getName().replace(" ", ""), new ItemStack(ItemRegistry.getItem(ItemPouch.class), 1, SeedRegistry.getIndexOf(seed)));
    }
}
