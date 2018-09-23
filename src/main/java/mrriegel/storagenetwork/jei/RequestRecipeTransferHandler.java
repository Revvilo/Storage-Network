package mrriegel.storagenetwork.jei;

import java.util.List;
import java.util.Map;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mrriegel.storagenetwork.gui.IStorageContainer;
import mrriegel.storagenetwork.network.ClearRecipeMessage;
import mrriegel.storagenetwork.network.RecipeMessage;
import mrriegel.storagenetwork.registry.PacketRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class RequestRecipeTransferHandler<C extends Container & IStorageContainer> implements IRecipeTransferHandler<C> {

  Class<C> clazz;

  public RequestRecipeTransferHandler(Class<C> clazz) {
    this.clazz = clazz;
  }

  @Override
  public Class<C> getContainerClass() {
    return clazz;
  }

  public static NBTTagCompound recipeToTag(Container container, IRecipeLayout recipeLayout) {
    NBTTagCompound nbt = new NBTTagCompound();
    Map<Integer, ? extends IGuiIngredient<ItemStack>> inputs = recipeLayout.getItemStacks().getGuiIngredients();
    for (Slot slot : container.inventorySlots) {
      if (slot.inventory instanceof InventoryCrafting) {
        //for some reason it was looping like this  (int j = 1; j < 10; j++)
        IGuiIngredient<ItemStack> ingredient = inputs.get(slot.getSlotIndex() + 1);
        if (ingredient == null) {
          continue;
        }
        List<ItemStack> possibleItems = ingredient.getAllIngredients();
        if (possibleItems == null) {
          continue;
        }
        NBTTagList invList = new NBTTagList();
        for (int i = 0; i < possibleItems.size(); i++) {
          if (i >= 5) {
            break; // Max 5 possible items to avoid reaching max network packet size
          }
          if (!possibleItems.get(i).isEmpty()) {
            NBTTagCompound stackTag = new NBTTagCompound();
            possibleItems.get(i).writeToNBT(stackTag);
            invList.appendTag(stackTag);
          }
        }
        nbt.setTag("s" + (slot.getSlotIndex()), invList);
      }
    }
    return nbt;
  }

  @Override
  public IRecipeTransferError transferRecipe(Container container, IRecipeLayout recipeLayout, EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
    if (doTransfer) {
      PacketRegistry.INSTANCE.sendToServer(new ClearRecipeMessage());
      NBTTagCompound nbt = RequestRecipeTransferHandler.recipeToTag(container, recipeLayout);
      PacketRegistry.INSTANCE.sendToServer(new RecipeMessage(nbt));
    }
    return null;
  }
}
