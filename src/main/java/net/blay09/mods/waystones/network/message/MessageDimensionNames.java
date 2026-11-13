package net.blay09.mods.waystones.network.message;

import net.blay09.mods.waystones.Waystones;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class MessageDimensionNames implements IMessage {

    public MessageDimensionNames() {}

    int amount;
    Integer[] ids;
    String[] names;

    public MessageDimensionNames(boolean isserver) {
        // forge builds this array out of the live dimension set: it sizes an Integer[] from keySet().size()
        // and then fills it with keySet().toArray(). if a dimension registers or unregisters between those two
        // calls, the array comes back with null holes at the end. unboxing one of those used to throw straight
        // through the login event and kill the whole handshake, which is a spectacularly unhelpful way to
        // report a registry race somewhere else in the pack.
        Integer[] staticDimensionIds = DimensionManager.getStaticDimensionIDs();

        int validCount = 0;
        for (Integer dimId : staticDimensionIds) {
            if (dimId != null) {
                validCount++;
            }
        }

        this.amount = 0;
        this.ids = new Integer[validCount];
        this.names = new String[validCount];

        for (Integer dimId : staticDimensionIds) {
            if (dimId == null) {
                Waystones.LOG.warn(
                    "DimensionManager.getStaticDimensionIDs() returned a null id, skipping it. "
                        + "a dimension was registered or removed while this packet was being built.");
                continue;
            }
            String name;
            try {
                WorldServer world = DimensionManager.getWorld(dimId);
                WorldProvider provider = world != null ? world.provider : DimensionManager.createProviderFor(dimId);
                name = provider != null ? provider.getDimensionName() : null;
            } catch (Throwable t) {
                // a single broken provider must not take the connection with it
                Waystones.LOG.warn("Could not resolve the name for dimension {}, skipping it", dimId, t);
                continue;
            }
            if (name == null || name.isEmpty()) {
                name = "Dimension " + dimId;
            }
            this.ids[this.amount] = dimId;
            this.names[this.amount] = name;
            this.amount++;
        }

        if (this.amount != this.ids.length) {
            // provider lookups failed for some ids, so trim the tail instead of shipping nulls
            Integer[] trimmedIds = new Integer[this.amount];
            String[] trimmedNames = new String[this.amount];
            System.arraycopy(this.ids, 0, trimmedIds, 0, this.amount);
            System.arraycopy(this.names, 0, trimmedNames, 0, this.amount);
            this.ids = trimmedIds;
            this.names = trimmedNames;
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.amount = buf.readInt();
        this.ids = new Integer[this.amount];
        this.names = new String[this.amount];
        for (int i = 0; i < this.amount; i++) {
            this.ids[i] = buf.readInt();
            this.names[i] = ByteBufUtils.readUTF8String(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.amount);
        for (int i = 0; i < this.amount; i++) {
            buf.writeInt(this.ids[i]);
            ByteBufUtils.writeUTF8String(buf, this.names[i]);
        }
    }

    public int getAmount() {
        return amount;
    }

    public Integer[] getIds() {
        return ids;
    }

    public String[] getNames() {
        return names;
    }
}
