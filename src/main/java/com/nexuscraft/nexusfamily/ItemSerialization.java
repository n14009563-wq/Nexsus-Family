package com.nexuscraft.nexusfamily;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Turns an ItemStack[] (an inventory's contents) into a byte[] and back, so it can be
 * stored directly in a PersistentDataContainer. Standard Bukkit serialization idiom --
 * relies only on long-stable Bukkit API (BukkitObjectOutputStream/InputStream), nothing
 * version-specific.
 */
public final class ItemSerialization {

    private ItemSerialization() {
    }

    public static byte[] toBytes(ItemStack[] items) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOut = new BukkitObjectOutputStream(byteOut);
            dataOut.writeInt(items.length);
            for (ItemStack item : items) {
                dataOut.writeObject(item);
            }
            dataOut.close();
            return byteOut.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize inventory contents", e);
        }
    }

    public static ItemStack[] fromBytes(byte[] bytes) {
        try {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream dataIn = new BukkitObjectInputStream(byteIn);
            int length = dataIn.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) dataIn.readObject();
            }
            dataIn.close();
            return items;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize inventory contents", e);
        }
    }
}
