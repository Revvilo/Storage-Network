package mrriegel.cworks.gui;

import mrriegel.cworks.init.ModItems;
import mrriegel.cworks.network.PacketHandler;
import mrriegel.cworks.network.StacksMessage;
import mrriegel.cworks.tile.TileMaster;
import mrriegel.cworks.tile.TileRequest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerDispenser;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.common.util.Constants;

public class ContainerRequest extends Container {
	public InventoryPlayer playerInv;
	public TileRequest tile;
	public InventoryCraftResult result;
	public InventoryCrafting craftMatrix = new InventoryCrafting(this, 3, 3);
	public InventoryRequest back;

	class RSlot extends Slot {

		public RSlot(IInventory inventoryIn, int index, int xPosition,
				int yPosition) {
			super(inventoryIn, index, xPosition, yPosition);
		}

		@Override
		public void onSlotChanged() {
			super.onSlotChanged();
			slotChanged();
		}

	}

	class OutputSlot extends SlotCrafting {

		public OutputSlot(EntityPlayer player,
				InventoryCrafting craftingInventory, IInventory p_i45790_3_,
				int slotIndex, int xPosition, int yPosition) {
			super(player, craftingInventory, p_i45790_3_, slotIndex, xPosition,
					yPosition);
		}

		@Override
		public void onSlotChanged() {
			super.onSlotChanged();
			slotChanged();
			onCraftMatrixChanged(null);
		}

	}

	public ContainerRequest(TileRequest tile, InventoryPlayer playerInv) {
		this.tile = tile;
		this.playerInv = playerInv;
		back = new InventoryRequest(tile);
		result = new InventoryCraftResult();
		NBTTagCompound nbt = new NBTTagCompound();
		tile.writeToNBT(nbt);
		NBTTagList invList = nbt.getTagList("matrix", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < invList.tagCount(); i++) {
			NBTTagCompound stackTag = invList.getCompoundTagAt(i);
			int slot = stackTag.getByte("Slot");
			craftMatrix.setInventorySlotContents(slot,
					ItemStack.loadItemStackFromNBT(stackTag));
		}
		if (!tile.getWorld().isRemote)
			PacketHandler.INSTANCE.sendTo(new StacksMessage(((TileMaster) tile
					.getWorld().getTileEntity(tile.getMaster())).getStacks()),
					(EntityPlayerMP) playerInv.player);

		int index = 0;
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 3; ++j) {
				this.addSlotToContainer(new RSlot(craftMatrix, index++,
						8 + j * 18, 110 + i * 18));
			}
		}
		index = 0;
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 2; ++j) {
				this.addSlotToContainer(new RSlot(back, index++, 134 + j * 18,
						110 + i * 18));
			}
		}
		this.addSlotToContainer(new OutputSlot(playerInv.player, craftMatrix,
				result, index++, 101, 128));
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9,
						8 + j * 18, 174 + i * 18));
			}
		}
		for (int i = 0; i < 9; ++i) {
			this.addSlotToContainer(new Slot(playerInv, i, 8 + i * 18, 232));
		}
		this.onCraftMatrixChanged(this.craftMatrix);

	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		if (!tile.getWorld().isRemote
				&& tile.getWorld().getTotalWorldTime() % 50 == 0) {
			PacketHandler.INSTANCE.sendTo(new StacksMessage(((TileMaster) tile
					.getWorld().getTileEntity(tile.getMaster())).getStacks()),
					(EntityPlayerMP) playerInv.player);
		}
	}

	public void onCraftMatrixChanged(IInventory inventoryIn) {
		this.result.setInventorySlotContents(0, CraftingManager.getInstance()
				.findMatchingRecipe(this.craftMatrix, tile.getWorld()));
	}

	public void slotChanged() {
//		onCraftMatrixChanged(null);
		NBTTagCompound nbt = new NBTTagCompound();
		tile.writeToNBT(nbt);
		NBTTagList invList = new NBTTagList();
		for (int i = 0; i < 6; i++) {
			if (back.getStackInSlot(i) != null) {
				NBTTagCompound stackTag = new NBTTagCompound();
				stackTag.setByte("Slot", (byte) i);
				back.getStackInSlot(i).writeToNBT(stackTag);
				invList.appendTag(stackTag);
			}
		}
		nbt.setTag("back", invList);
		invList = new NBTTagList();
		for (int i = 0; i < 9; i++) {
			if (craftMatrix.getStackInSlot(i) != null) {
				NBTTagCompound stackTag = new NBTTagCompound();
				stackTag.setByte("Slot", (byte) i);
				craftMatrix.getStackInSlot(i).writeToNBT(stackTag);
				invList.appendTag(stackTag);
			}
		}
		nbt.setTag("matrix", invList);
		tile.readFromNBT(nbt);

	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer playerIn, int slotIndex) {
		ItemStack itemstack = null;
		Slot slot = this.inventorySlots.get(slotIndex);

		if (slot != null && slot.getHasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();

			if (slotIndex <= 15) {
				if (!this.mergeItemStack(itemstack1, 15, 15 + 37, true)) {
					return null;
				}
				slot.onSlotChange(itemstack1, itemstack);
			} else {
				if (!this.mergeItemStack(itemstack1, 9, 15, false))
					return null;
			}
			if (itemstack1.stackSize == 0) {
				slot.putStack((ItemStack) null);
			} else {
				slot.onSlotChanged();
			}

			if (itemstack1.stackSize == itemstack.stackSize) {
				return null;
			}
			slot.onPickupFromSlot(playerIn, itemstack1);
		}

		return itemstack;
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return true;
	}

}