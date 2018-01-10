package tehnut.resourceful.crops.compat.enderio;

import crazypants.enderio.machine.farm.FarmNotification;
import crazypants.enderio.machine.farm.TileFarmStation;
import crazypants.enderio.machine.farm.farmers.HarvestResult;
import crazypants.enderio.machine.farm.farmers.IHarvestResult;
import crazypants.enderio.machine.farm.farmers.PlantableFarmer;
import crazypants.util.Prep;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.event.ForgeEventFactory;
import tehnut.resourceful.crops.block.tile.TileSeedContainer;
import tehnut.resourceful.crops.core.data.Seed;
import tehnut.resourceful.crops.block.BlockResourcefulCrop;
import tehnut.resourceful.crops.item.ItemResourceful;
import tehnut.resourceful.crops.item.ItemResourcefulSeed;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class ResourcefulFarmer extends PlantableFarmer {

    @Override
    public boolean canPlant(ItemStack stack) {
        return super.canPlant(stack) && ( stack.getItem() instanceof ItemResourcefulSeed );
    }

    @Override
    public boolean canHarvest(TileFarmStation farm, BlockPos bc, Block block, IBlockState state) {
        if ( !( block instanceof BlockResourcefulCrop ) )
            return false;
        return !( (IGrowable) block ).canGrow(farm.getWorld(), bc, state, true);
    }

    @Override
    public boolean prepareBlock(TileFarmStation farm, BlockPos bc, Block block, IBlockState meta) {
        if ( block == null )
            return false;

        int slot = farm.getSupplySlotForCoord(bc);
        ItemStack seedStack = farm.getSeedTypeInSuppliesFor(slot);
        if ( Prep.isInvalid(seedStack) ) {
            if ( !farm.isSlotLocked(slot) ) {
                farm.setNotification(FarmNotification.NO_SEEDS);
            }
            return false;
        }

        if ( !(seedStack.getItem() instanceof ItemResourcefulSeed) )
            return false;

        IPlantable plantable = (IPlantable) seedStack.getItem();

        farm.tillBlock(bc);
        return plantFromInventory(farm, bc, plantable);
    }

    @Override
    protected boolean canPlant(World worldObj, BlockPos bc, IPlantable plantable) {
        IBlockState target = plantable.getPlant(null, new BlockPos(0,0,0));
        BlockPos groundPos = bc.down();
        IBlockState groundBS = worldObj.getBlockState(groundPos);
        Block ground = groundBS.getBlock();
        if ( target != null && target.getBlock().canPlaceBlockAt(worldObj, bc)
          && ground.canSustainPlant(groundBS, worldObj, groundPos, EnumFacing.UP, plantable) ) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean plantFromInventory(TileFarmStation farm, BlockPos bc, IPlantable plantable) {
        World worldObj = farm.getWorld();
        if ( canPlant(worldObj, bc, plantable) ) {
            ItemStack seedStack = farm.takeSeedFromSupplies(bc);
            if( Prep.isValid(seedStack) ) {
                return plant(farm, worldObj, bc, seedStack, plantable);
            }
        }
        return false;
    }

    protected boolean plant(TileFarmStation farm, World worldObj, BlockPos bc, ItemStack seedStack, IPlantable plantable) {
        if ( seedStack == null || !( seedStack.getItem() instanceof ItemResourcefulSeed ) )
            return false;
        Seed seed = ( (ItemResourceful) seedStack.getItem() ).getSeed(seedStack);
        if ( seed == null )
            return false;
        worldObj.setBlockState(bc, Blocks.AIR.getDefaultState(), 1 | 2);
        IBlockState target = plantable.getPlant(null, bc);
        worldObj.setBlockState(bc, target, 1 | 2);
        worldObj.setTileEntity(bc, new TileSeedContainer(seed.getRegistryName()));
        farm.actionPerformed(false);
        return true;
    }

    // Copied directly from PlantableFarmer. Only one line is changed to use the stack-sensitive plant(...) method.
    @Override
    public IHarvestResult harvestBlock(TileFarmStation farm, BlockPos bc, Block block, IBlockState meta) {
        if( !canHarvest(farm, bc, block, meta) ) {
            return null;
        }
        if( !farm.hasHoe() ) {
            farm.setNotification(FarmNotification.NO_HOE);
            return null;
        }

        World worldObj = farm.getWorld();
        List<EntityItem> result = new ArrayList<EntityItem>();
        final EntityPlayerMP fakePlayer = farm.getFakePlayer();
        final int fortune = farm.getMaxLootingValue();

        ItemStack removedPlantable = Prep.getEmpty();

        List<ItemStack> drops = block.getDrops(worldObj, bc, meta, fortune);
        float chance = ForgeEventFactory.fireBlockHarvesting(drops, worldObj, bc, meta, fortune, 1.0F, false, fakePlayer);
        farm.damageHoe(1, bc);
        farm.actionPerformed(false);
        if( drops != null ) {
            for ( ItemStack stack : drops ) {
                if ( Prep.isValid(stack) && stack.stackSize > 0 && worldObj.rand.nextFloat() <= chance ) {
                    if ( Prep.isInvalid(removedPlantable) && isPlantableForBlock(stack, block) ) {
                        removedPlantable = stack.copy();
                        removedPlantable.stackSize = 1;
                        stack.stackSize--;
                        if ( stack.stackSize > 0 ) {
                            result.add(new EntityItem(worldObj, bc.getX() + 0.5, bc.getY() + 0.5, bc.getZ() + 0.5, stack.copy()));
                        }
                    } else {
                        result.add(new EntityItem(worldObj, bc.getX() + 0.5, bc.getY() + 0.5, bc.getZ() + 0.5, stack.copy()));
                    }
                }
            }
        }

        ItemStack[] inv = fakePlayer.inventory.mainInventory;
        for ( int slot = 0; slot < inv.length; slot++ ) {
            ItemStack stack = inv[slot];
            if ( Prep.isValid(stack) ) {
                inv[slot] = Prep.getEmpty();
                result.add(new EntityItem(worldObj, bc.getX() + 0.5, bc.getY() + 1, bc.getZ() + 0.5, stack));
            }
        }

        if( Prep.isValid(removedPlantable) ) {
            if( !plant(farm, worldObj, bc, removedPlantable, (IPlantable) removedPlantable.getItem()) ) { // Modified to use stack-sensitive plant(...) method.
                result.add(new EntityItem(worldObj, bc.getX() + 0.5, bc.getY() + 0.5, bc.getZ() + 0.5, removedPlantable.copy()));
                worldObj.setBlockState(bc, Blocks.AIR.getDefaultState(), 1 | 2);
            }
        } else {
            worldObj.setBlockState(bc, Blocks.AIR.getDefaultState(), 1 | 2);
        }

        return new HarvestResult(result, bc);
    }

    // Copied directly from PlantableFarmer because it's private.
    private boolean isPlantableForBlock(ItemStack stack, Block block) {
        if( !( stack.getItem() instanceof IPlantable ) ) {
            return false;
        }
        IPlantable plantable = (IPlantable) stack.getItem();
        IBlockState b = plantable.getPlant(null, new BlockPos(0, 0, 0));
        return b != null && b.getBlock() == block;
    }
}
