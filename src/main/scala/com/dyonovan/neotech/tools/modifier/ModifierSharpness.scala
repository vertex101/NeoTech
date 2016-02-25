package com.dyonovan.neotech.tools.modifier

import net.minecraft.enchantment.{Enchantment, EnchantmentHelper}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import scala.collection.mutable.ArrayBuffer

/**
  * This file was created for NeoTech
  *
  * NeoTech is licensed under the
  * Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License:
  * http://creativecommons.org/licenses/by-nc-sa/4.0/
  *
  * @author Paul Davis <pauljoda>
  * @since 2/24/2016
  */
object ModifierSharpness extends Modifier("sharpness") {
    lazy val SHARPNESS = "Sharpness"

    def getSharpnessLevel(stack : ItemStack) : Int = {
        val tag = getModifierTagFromStack(stack)
        if(tag != null && tag.hasKey(SHARPNESS))
            return tag.getInteger(SHARPNESS)
        0
    }

    /**
      * Used to get the level for this modifier
      *
      * @param tag The tag that the level is stored on
      * @return The level
      */
    override def getLevel(tag : NBTTagCompound) = tag.getInteger(SHARPNESS)

    def writeToNBT(tag: NBTTagCompound, stack: ItemStack, count: Int): NBTTagCompound = {
        val list = EnchantmentHelper.getEnchantments(stack)
        list.put(Enchantment.sharpness.effectId, getSharpnessLevel(stack) + count)
        EnchantmentHelper.setEnchantments(list, stack)
        tag.setInteger(SHARPNESS, getSharpnessLevel(stack) + count)
        super.writeToNBT(tag, stack)
        tag
    }

    /**
      * Used to get the tool tip for this modifier
      *
      * @param stack The stack in
      * @return A list of tips
      */
    override def getToolTipForWriting(stack: ItemStack, tag : NBTTagCompound): ArrayBuffer[String] = new ArrayBuffer[String]() //Vanilla handles this
}