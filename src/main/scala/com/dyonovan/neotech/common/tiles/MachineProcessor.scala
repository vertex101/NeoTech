package com.dyonovan.neotech.common.tiles

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * This file was created for NeoTech
  *
  * NeoTech is licensed under the
  * Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License:
  * http://creativecommons.org/licenses/by-nc-sa/4.0/
  *
  * @author Paul Davis <pauljoda>
  * @since 1/31/2016
  */
abstract class MachineProcessor extends AbstractMachine {

    var cookTime = 0
    var didWork : Boolean = false

    /**
      * Get the output of the recipe
      *
      * @param stack The input
      * @return The output
      */
    def getOutputForStack(stack: ItemStack): ItemStack

    /**
      * Used to actually cook the item. You should reset values here if need be
      */
    def cook() : Unit

    /**
      * Called when the tile has completed the cook process
      */
    def completeCook() : Unit

    /**
      * Used to tell if this tile is able to process
      *
      * @return True if you are able to process
      */
    def canProcess : Boolean

    /**
      * Used to get how long it takes to cook things, you should check for upgrades at this point
      *
      * @return The time it takes in ticks to cook the current item
      */
    def getCookTime : Int

    /**
      * Used to get how much energy to drain per tick, you should check for upgrades at this point
      *
      * @return How much energy to drain per tick
      */
    def getEnergyCostPerTick : Int

    /**
      * The initial size of the inventory
      *
      * @return
      */
    override def initialSize: Int

    /*******************************************************************************************************************
      ************************************************ Processor methods ***********************************************
      ******************************************************************************************************************/

    override def doWork() = {
        didWork = false

        /** We want to check if we are above the value needed before we actually start checking for cooking, this will ensure
         *  we don't run into issues with going one tick over */
        if(cookTime >= getCookTime) {
            completeCook()
            cookTime = 0
        }

        //Do Operations
        if (canProcess) {
            cook()
            didWork = true
        } else {
            cookTime = 0
        }

        if (didWork) {
            worldObj.markBlockForUpdate(pos)
        }
    }

    override def writeToNBT(tag: NBTTagCompound): Unit = {
        super.writeToNBT(tag)
        tag.setInteger("CookTime", cookTime)
    }

    override def readFromNBT(tag: NBTTagCompound): Unit = {
        super.readFromNBT(tag)
        cookTime = tag.getInteger("CookTime")
    }

    @SideOnly(Side.CLIENT)
    def getCookProgressScaled(scaleVal: Int): Int =
        ((cookTime * scaleVal) / Math.max(getCookTime, 0.001)).toInt

    /*******************************************************************************************************************
      ************************************************ Inventory methods ***********************************************
      ******************************************************************************************************************/

    /**
      * Used to get what slots you can use per face
      * @param side The face to check
      * @return An array of slots to interface with
      */
    override def getSlotsForFace(side: EnumFacing): Array[Int] = {
        side match {
            case EnumFacing.UP => getInputSlots
            case EnumFacing.DOWN => getOutputSlots
            case _ => getInputSlots ++ getOutputSlots
        }
    }

    /**
      * Returns true if automation can insert the given item in the given slot from the given side. Args: slot, item,
      * side
      */
    override def canInsertItem(slot: Int, itemStackIn: ItemStack, direction: EnumFacing): Boolean = {
        if (slot == 0 && getOutputForStack(itemStackIn) != null) {
            if (getStackInSlot(0) == null) return true
            if (getStackInSlot(0).isItemEqual(itemStackIn)) {
                if (getStackInSlot(0).getMaxStackSize >= getStackInSlot(0).stackSize + itemStackIn.stackSize)
                    return true
            }
        }
        false
    }

    /**
      * Returns true if automation can extract the given item in the given slot from the given side. Args: slot, item,
      * side
      */
    override def canExtractItem(index: Int, stack: ItemStack, direction: EnumFacing): Boolean = index == 1

    /**
      * Used to define if an item is valid for a slot
      *
      * @param slot The slot id
      * @param itemStackIn The stack to check
      * @return True if you can put this there
      */
    override def isItemValidForSlot(slot: Int, itemStackIn: ItemStack): Boolean =
        slot == 0 && getOutputForStack(itemStackIn) != null

    /**
      * Flag to let use know we are sided
      */
    override def hasCapability(capability: Capability[_], facing : EnumFacing) = true

    /**
      * Used to get the capability from the tile, since the InventorySide can't access these methods
      */
    override def getCapabilityFromTile[T](capability: Capability[T], facing: EnumFacing) : T
        = super[TileEntity].getCapability[T](capability, facing)

    /**
      * Gets the IItemHandler for the face, allows ISided interactions without needing to implement ISided
      *
      * @param capability What kind of capability
      * @param facing What side
      * @tparam T The type
      * @return The IItemHandler for that side
      */
    override def getCapability[T](capability: Capability[T], facing: EnumFacing) : T =
        super[InventorySided].getCapability[T](capability, facing)
}
