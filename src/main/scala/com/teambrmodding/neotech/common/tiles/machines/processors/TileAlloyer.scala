package com.teambrmodding.neotech.common.tiles.machines.processors

import com.teambrmodding.neotech.client.gui.machines.processors.GuiAlloyer
import com.teambrmodding.neotech.collections.EnumInputOutputMode
import com.teambrmodding.neotech.common.container.machines.processors.ContainerAlloyer
import com.teambrmodding.neotech.common.tiles.MachineProcessor
import com.teambrmodding.neotech.managers.{MetalManager, RecipeManager}
import com.teambrmodding.neotech.registries.AlloyerRecipeHandler
import com.teambrmodding.neotech.utils.ClientUtils
import com.teambr.bookshelf.client.gui.{GuiColor, GuiTextFormat}
import com.teambr.bookshelf.common.tiles.traits.FluidHandler
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.text.translation.I18n
import net.minecraft.util.{EnumFacing, EnumParticleTypes}
import net.minecraft.world.World
import net.minecraftforge.fluids.{Fluid, FluidStack, FluidTank, IFluidHandler}

/**
  * This file was created for NeoTech
  *
  * NeoTech is licensed under the
  * Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License:
  * http://creativecommons.org/licenses/by-nc-sa/4.0/
  *
  * @author Paul Davis <pauljoda>
  * @since 2/20/2016
  */
class TileAlloyer extends MachineProcessor[(FluidStack, FluidStack), FluidStack] with FluidHandler {

    lazy val BASE_ENERGY_TICK = 100

    /**
      * The initial size of the inventory
      *
      * @return
      */
    override def initialSize: Int = 0

    /**
      * Add all modes you want, in order, here
      */
    def addValidModes() : Unit = {
        validModes += EnumInputOutputMode.INPUT_ALL
        validModes += EnumInputOutputMode.INPUT_PRIMARY
        validModes += EnumInputOutputMode.INPUT_SECONDARY
        validModes += EnumInputOutputMode.OUTPUT_ALL
        validModes += EnumInputOutputMode.ALL_MODES
    }

    /**
      * Used to get how much energy to drain per tick, you should check for upgrades at this point
      *
      * @return How much energy to drain per tick
      */
    override def getEnergyCostPerTick: Int =
        if(getUpgradeBoard != null && getUpgradeBoard.getProcessorCount > 0)
            BASE_ENERGY_TICK * getUpgradeBoard.getProcessorCount
        else
            BASE_ENERGY_TICK

    /**
      * Used to get how long it takes to cook things, you should check for upgrades at this point
      *
      * @return The time it takes in ticks to cook the current item
      */
    override def getCookTime : Int = {
        if(getUpgradeBoard != null && getUpgradeBoard.getProcessorCount > 0)
            1000 - (getUpgradeBoard.getProcessorCount * 112)
        else
            1000
    }

    /**
      * Used to tell if this tile is able to process
      *
      * @return True if you are able to process
      */
    override def canProcess: Boolean = {
        if (energyStorage.getEnergyStored >= getEnergyCostPerTick) {
            return tanks(INPUT_TANK_1).getFluid != null && tanks(INPUT_TANK_2) != null &&
                    RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer)
                            .isValidInput(tanks(INPUT_TANK_1).getFluid, tanks(INPUT_TANK_2).getFluid) &&
                    (if(tanks(OUTPUT_TANK).getFluid == null) true else RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer)
                            .getOutput(tanks(INPUT_TANK_1).getFluid, tanks(INPUT_TANK_2).getFluid).get.amount + tanks(OUTPUT_TANK).getFluidAmount <= tanks(OUTPUT_TANK).getCapacity)
        }
        failCoolDown = 40
        false
    }

    /**
      * Used to actually cook the item
      */
    override def cook(): Unit = cookTime += 1

    /**
      * Called when the tile has completed the cook process
      */
    override def completeCook(): Unit = {
        if(canProcess) { //Just to be safe
        val recipeTest = RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer).getRecipe((tanks(INPUT_TANK_1).getFluid, tanks(INPUT_TANK_2).getFluid))
            if(recipeTest.isDefined) {
                val recipe = recipeTest.get
                val output = recipe.getOutput((tanks(INPUT_TANK_1).getFluid, tanks(INPUT_TANK_2).getFluid))
                //Drain Inputs
                val drain1 = tanks(INPUT_TANK_1).drain(recipe.getFluidFromString(recipe.fluidOne).amount, false)
                val drain2 = tanks(INPUT_TANK_2).drain(recipe.getFluidFromString(recipe.fluidTwo).amount, false)

                if(drain1 != null && drain2 != null && drain1.amount > 0 && drain2.amount > 0) {
                    tanks(INPUT_TANK_1).drain(recipe.getFluidFromString(recipe.fluidOne).amount, true)
                    tanks(INPUT_TANK_2).drain(recipe.getFluidFromString(recipe.fluidTwo).amount, true)
                    tanks(OUTPUT_TANK).fill(output.get, true)
                }
            }
        }
    }

    /**
      * Get the output of the recipe
      *
      * @param input The input
      * @return The output
      */
    override def getOutput(input: (FluidStack, FluidStack)): FluidStack =
        if(RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer).getOutput(input).isDefined)
            RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer).getOutput(input).get
        else
            null

    /**
      * Get the output of the recipe (used in insert options)
      *
      * @param input The input
      * @return The output
      */
    override def getOutputForStack(input: ItemStack): ItemStack = null

    /*******************************************************************************************************************
      **************************************************  Tile Methods  ************************************************
      ******************************************************************************************************************/

    /**
      * This will try to take things from other inventories and put it into ours
      */
    override def tryInput() : Unit = {
        for(dir <- EnumFacing.values) {
            worldObj.getTileEntity(pos.offset(dir)) match {
                case otherTank : IFluidHandler =>

                    if(canInputFromSide(dir)) // Left Tank
                        if(otherTank.getTankInfo(dir.getOpposite) != null && otherTank.getTankInfo(dir.getOpposite).nonEmpty &&
                                otherTank.getTankInfo(dir.getOpposite)(0) != null && otherTank.getTankInfo(dir.getOpposite)(0).fluid != null &&
                                canFill(dir, otherTank.getTankInfo(dir.getOpposite)(0).fluid.getFluid)) {
                            val amount = tanks(INPUT_TANK_1).fill(otherTank.drain(dir.getOpposite, 1000, false), false)
                            if (amount > 0)
                                tanks(INPUT_TANK_1).fill(otherTank.drain(dir.getOpposite, amount, true), true)
                        }

                    if(canInputFromSide(dir, isPrimary = false)) // Right Tank
                        if(otherTank.getTankInfo(dir.getOpposite) != null && otherTank.getTankInfo(dir.getOpposite).nonEmpty &&
                                otherTank.getTankInfo(dir.getOpposite)(0) != null && otherTank.getTankInfo(dir.getOpposite)(0).fluid != null &&
                                canFill(dir, otherTank.getTankInfo(dir.getOpposite)(0).fluid.getFluid)) {
                            val amount = tanks(INPUT_TANK_2).fill(otherTank.drain(dir.getOpposite, 1000, false), false)
                            if (amount > 0)
                                tanks(INPUT_TANK_2).fill(otherTank.drain(dir.getOpposite, amount, true), true)
                        }

                case _ =>
            }
        }
    }

    /**
      * This will try to take things from other inventories and put it into ours
      */
    override def tryOutput() : Unit = {
        for(dir <- EnumFacing.values) {
            if(canOutputFromSide(dir)) {
                worldObj.getTileEntity(pos.offset(dir)) match {
                    case otherTank : IFluidHandler =>
                        val fluid = otherTank.getTankInfo(dir.getOpposite)(0).fluid
                        if((if(fluid == null) true else
                        if (tanks(OUTPUT_TANK).getFluid != null  && tanks(OUTPUT_TANK).drain(1000, false) != null)
                            otherTank.getTankInfo(dir.getOpposite)(0).fluid.getFluid == tanks(OUTPUT_TANK).getFluid.getFluid
                        else false)
                                && canDrain(dir.getOpposite, if(fluid != null) fluid.getFluid else null)) {
                            val amount = otherTank.fill(dir.getOpposite, drain(dir, 1000, doDrain = false), false)
                            if (amount > 0)
                                otherTank.fill(dir, drain(dir.getOpposite, amount, doDrain = true),  true)
                        }
                    case _ =>
                }
            }
        }
    }

    override def writeToNBT(tag : NBTTagCompound) : NBTTagCompound = {
        super[MachineProcessor].writeToNBT(tag)
        super[FluidHandler].writeToNBT(tag)
        tag
    }

    override def readFromNBT(tag : NBTTagCompound) : Unit = {
        super[MachineProcessor].readFromNBT(tag)
        super[FluidHandler].readFromNBT(tag)
    }

    /*******************************************************************************************************************
      ************************************************ Inventory methods ***********************************************
      ******************************************************************************************************************/

    /**
      * Used to get what slots are allowed to be input
      *
      * @return The slots to input from
      */
    override def getInputSlots(mode : EnumInputOutputMode) : Array[Int] = Array()

    /**
      * Used to get what slots are allowed to be output
      *
      * @return The slots to output from
      */
    override def getOutputSlots(mode : EnumInputOutputMode) : Array[Int] = Array()

    /*******************************************************************************************************************
      **************************************************** Fluid methods ***********************************************
      ******************************************************************************************************************/

    lazy val INPUT_TANK_1 = 0
    lazy val INPUT_TANK_2 = 1
    lazy val OUTPUT_TANK  = 2

    /**
      * Used to set up the tanks needed. You can insert any number of tanks
      */
    override def setupTanks(): Unit = {
        tanks += new FluidTank(10 * MetalManager.BLOCK_MB) // IN 1
        tanks += new FluidTank(10 * MetalManager.BLOCK_MB) // IN 2
        tanks += new FluidTank(10 * MetalManager.BLOCK_MB) // OUT
    }

    /**
      * Which tanks can input
      *
      * @return
      */
    override def getInputTanks: Array[Int] = Array(INPUT_TANK_1, INPUT_TANK_2)

    /**
      * Which tanks can output
      *
      * @return
      */
    override def getOutputTanks: Array[Int] = Array(OUTPUT_TANK)

    /**
      * Called when something happens to the tank, you should mark the block for update here if a tile
      */
    override def onTankChanged(tank: FluidTank): Unit =
        worldObj.notifyBlockUpdate(pos, worldObj.getBlockState(pos), worldObj.getBlockState(pos), 6)
    /**
      * Returns true if the given fluid can be inserted into the given direction.
      *
      * More formally, this should return true if fluid is able to enter from the given direction.
      */
    override def canFill(from: EnumFacing, fluid: Fluid): Boolean = {
        if(fluid == null) return false
        if(isDisabled(from)) return false
        if(tanks(INPUT_TANK_1).getFluid == null)
            return RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer).isValidSingle(new FluidStack(fluid, 1000))
        else if(tanks(INPUT_TANK_2).getFluid == null)
            return RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer).isValidSingle(new FluidStack(fluid, 1000))
        else {
            if(fluid == tanks(INPUT_TANK_1).getFluid.getFluid || fluid == tanks(INPUT_TANK_2).getFluid.getFluid)
                return true
            else
                return false
        }
        false
    }


    /*******************************************************************************************************************
      ***************************************************** Misc methods ***********************************************
      ******************************************************************************************************************/

    /**
      * Return the container for this tile
      *
      * @param ID Id, probably not needed but could be used for multiple guis
      * @param player The player that is opening the gui
      * @param world The world
      * @param x X Pos
      * @param y Y Pos
      * @param z Z Pos
      * @return The container to open
      */
    override def getServerGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef =
        new ContainerAlloyer(player.inventory, this)

    /**
      * Return the gui for this tile
      *
      * @param ID Id, probably not needed but could be used for multiple guis
      * @param player The player that is opening the gui
      * @param world The world
      * @param x X Pos
      * @param y Y Pos
      * @param z Z Pos
      * @return The gui to open
      */
    override def getClientGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef =
        new GuiAlloyer(player, this)

    override def getDescription : String = {
        "" +
                GuiColor.GREEN + GuiTextFormat.BOLD + GuiTextFormat.UNDERLINE + ClientUtils.translate("neotech.text.stats") + ":\n" +
                GuiColor.YELLOW + GuiTextFormat.BOLD + ClientUtils.translate("neotech.text.energyUsage") + ":\n" +
                GuiColor.WHITE + "  " + getEnergyCostPerTick + " RF/tick\n" +
                GuiColor.YELLOW + GuiTextFormat.BOLD + ClientUtils.translate("neotech.text.processTime") + ":\n" +
                GuiColor.WHITE + "  " + getCookTime + " ticks\n\n" +
                GuiColor.WHITE + I18n.translateToLocal("neotech.alloyer.desc") + "\n\n" +
                GuiColor.GREEN + GuiTextFormat.BOLD + GuiTextFormat.UNDERLINE + I18n.translateToLocal("neotech.text.upgrades") + ":\n" + GuiTextFormat.RESET +
                GuiColor.YELLOW + GuiTextFormat.BOLD + I18n.translateToLocal("neotech.text.processors") + ":\n" +
                GuiColor.WHITE + I18n.translateToLocal("neotech.electricCrucible.processorUpgrade.desc") + "\n\n" +
                GuiColor.YELLOW + GuiTextFormat.BOLD + I18n.translateToLocal("neotech.text.hardDrives") + ":\n" +
                GuiColor.WHITE + I18n.translateToLocal("neotech.electricFurnace.hardDriveUpgrade.desc") + "\n\n" +
                GuiColor.YELLOW + GuiTextFormat.BOLD + I18n.translateToLocal("neotech.text.control") + ":\n" +
                GuiColor.WHITE + I18n.translateToLocal("neotech.electricFurnace.controlUpgrade.desc") + "\n\n" +
                GuiColor.YELLOW + GuiTextFormat.BOLD + I18n.translateToLocal("neotech.text.expansion") + ":\n" +
                GuiColor.WHITE +  I18n.translateToLocal("neotech.electricFurnace.expansionUpgrade.desc")
    }

    /**
      * Used to output the redstone single from this structure
      *
      * Use a range from 0 - 16.
      *
      * 0 Usually means that there is nothing in the tile, so take that for lowest level. Like the generator has no energy while
      * 16 is usually the flip side of that. Output 16 when it is totally full and not less
      *
      * @return int range 0 - 16
      */
    override def getRedstoneOutput: Int = if(isActive) 16 else 0

    /**
      * Used to get what particles to spawn. This will be called when the tile is active
      */
    override def spawnActiveParticles(x: Double, y: Double, z: Double): Unit = {
        worldObj.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0, 0, 0)
        worldObj.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0, 0, 0)
        worldObj.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0, 0, 0)
    }
}