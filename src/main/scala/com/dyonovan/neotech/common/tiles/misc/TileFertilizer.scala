package com.dyonovan.neotech.common.tiles.misc

import java.util

import com.google.common.collect.Lists
import com.teambr.bookshelf.api.waila.Waila
import com.teambr.bookshelf.client.gui.GuiColor
import com.teambr.bookshelf.common.tiles.traits.{Inventory, UpdatingTile}
import mcp.mobius.waila.api.ITaggedList
import mcp.mobius.waila.api.ITaggedList.ITipList
import net.minecraft.block.IGrowable
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockPos


/**
 * This file was created for NeoTech
 *
 * NeoTech is licensed under the
 * Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License:
 * http://creativecommons.org/licenses/by-nc-sa/4.0/
 *
 * @author Dyonovan
 * @since August 22, 2015
 */
class TileFertilizer extends TileEntity with Inventory with UpdatingTile with Waila {

    var corner1: BlockPos = _
    var corner2: BlockPos = _
    var list: util.ArrayList[BlockPos] = _

    override def onServerTick(): Unit = {
        if (corner1 == null) {
            corner2 = pos.north(3).west(3)
            corner1 = pos.south(3).east(3).down(2)
            list = Lists.newArrayList(BlockPos.getAllInBox(corner1, corner2)).asInstanceOf[util.ArrayList[BlockPos]]
        }
        if (worldObj.rand.nextInt(40) == 0) {
            val plantPOS = list.get(worldObj.rand.nextInt(list.size()))
            if (plantPOS != pos) {
                worldObj.getBlockState(plantPOS).getBlock match {
                    case plant: IGrowable =>
                        val state = worldObj.getBlockState(plantPOS)
                        if (plant.canGrow(worldObj, plantPOS, state, worldObj.isRemote)) {
                            if (isBoneMeal._1) {
                                plant.grow(worldObj, worldObj.rand, plantPOS, state)
                                if (worldObj.rand.nextInt(100) <= 34)
                                    decrStackSize(isBoneMeal._2, 1)
                            } else if (worldObj.rand.nextInt(100) <= 34)
                                plant.grow(worldObj, worldObj.rand, plantPOS, state)
                        }
                    case _ =>
                }
            }
        }
    }

    private def isBoneMeal: (Boolean, Int) = {
        for (i <- 0 until getSizeInventory()) {
            if (getStackInSlot(i) != null && getStackInSlot(i).stackSize > 0) return (true, i)
        }
        (false, -1)
    }

    override def writeToNBT(tag: NBTTagCompound): Unit = {
        super[TileEntity].writeToNBT(tag)
        super[Inventory].writeToNBT(tag)
    }

    override def readFromNBT(tag: NBTTagCompound): Unit = {
        super[TileEntity].readFromNBT(tag)
        super[Inventory].readFromNBT(tag)
    }

    override def markDirty(): Unit = {
        super[TileEntity].markDirty()
        super[Inventory].markDirty()
    }

    override var inventoryName: String = _

    override def hasCustomName(): Boolean = false

    override def initialSize: Int = 4

    override def returnWailaBody(tipList: ITaggedList.ITipList): ITipList = {
        var count = 0
        for (i <- 0 until getSizeInventory()) {
            if (getStackInSlot(i) != null)
                count += getStackInSlot(i).stackSize
            tipList.add(GuiColor.WHITE + "BoneMeal: " + count)
        }
        tipList
    }
}
