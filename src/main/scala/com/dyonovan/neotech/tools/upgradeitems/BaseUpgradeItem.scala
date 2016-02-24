package com.dyonovan.neotech.tools.upgradeitems

import com.dyonovan.neotech.NeoTech
import com.dyonovan.neotech.lib.Reference
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item

/**
  * This file was created for NeoTech
  *
  * NeoTech is licensed under the
  * Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License:
  * http://creativecommons.org/licenses/by-nc-sa/4.0/
  *
  * @author Dyonovan
  * @since 2/23/2016
  */
class BaseUpgradeItem(name: String, stackSize: Int,
                      tab : CreativeTabs = NeoTech.tabTools) extends Item {

    setMaxStackSize(stackSize)
    if(tab != null)
        setCreativeTab(tab)
    setUnlocalizedName(Reference.MOD_ID + ":" + name)

    def getUpgradeName: String = name
}
