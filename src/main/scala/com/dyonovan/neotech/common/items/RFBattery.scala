package com.dyonovan.neotech.common.items

import com.dyonovan.neotech.NeoTech
import com.dyonovan.neotech.common.items.traits.ItemBattery
import com.dyonovan.neotech.lib.Reference
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * This file was created for NeoTech
  *
  * NeoTech is licensed under the
  * Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License:
  * http://creativecommons.org/licenses/by-nc-sa/4.0/
  *
  * @author Dyonovan
  * @since 2/17/2016
  */
class RFBattery(name: String, tier: Int) extends ItemBattery {

    setMaxStackSize(1)
    setCreativeTab(NeoTech.tabNeoTech)
    setMaxStackSize(maxStackSize)
    setUnlocalizedName(Reference.MOD_ID + ":" + name)

    val tierPower = getTierPower(tier)
    capacity = tierPower._1
    maxReceive = tierPower._2
    maxExtract = tierPower._2

    override def onUpdate(stack: ItemStack, worldIn: World, entity: Entity, itemSlot: Int, isSelected: Boolean): Unit = {

    }

    /**
      * Defines amount of power each tier holds
      *
      * @param t Battery Tier
      * @return Touple2(capacity, maxReceive)
      */
    def getTierPower(t: Int): (Int, Int) = {
        t match {
            case 1 => (25000, 2500)
            case 2 => (100000, 10000)
            case 3 => (1000000, 100000)
            case _ => (0, 0)
        }
    }

    @SideOnly(Side.CLIENT)
    override def addInformation(stack: ItemStack, player: EntityPlayer, list: java.util.List[String], boolean: Boolean): Unit = {
        list.add(getEnergyStored(stack) + "/" + getMaxEnergyStored(stack) + " RF")
    }
}
