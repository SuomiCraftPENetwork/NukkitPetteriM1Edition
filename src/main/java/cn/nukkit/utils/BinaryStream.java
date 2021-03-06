package cn.nukkit.utils;

import cn.nukkit.entity.Attribute;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.item.*;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.GameRules;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.Vector3f;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.StringTag;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.network.protocol.types.EntityLink;
import it.unimi.dsi.fastutil.io.FastByteArrayInputStream;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public class BinaryStream {

    public int offset;
    private byte[] buffer;
    private int count;

    private static final int MAX_ARRAY_SIZE = 2147483639;

    public BinaryStream() {
        this.buffer = new byte[32];
        this.offset = 0;
        this.count = 0;
    }

    public BinaryStream(byte[] buffer) {
        this(buffer, 0);
    }

    public BinaryStream(byte[] buffer, int offset) {
        this.buffer = buffer;
        this.offset = offset;
        this.count = buffer.length;
    }

    public BinaryStream reset() {
        this.offset = 0;
        this.count = 0;
        return this;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
        this.count = buffer == null ? -1 : buffer.length;
    }

    public void setBuffer(byte[] buffer, int offset) {
        this.setBuffer(buffer);
        this.setOffset(offset);
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public byte[] getBuffer() {
        return Arrays.copyOf(buffer, count);
    }

    public int getCount() {
        return count;
    }

    public byte[] get() {
        return this.get(this.count - this.offset);
    }

    public byte[] get(int len) {
        if (len < 0) {
            this.offset = this.count - 1;
            return new byte[0];
        }
        len = Math.min(len, this.count - this.offset);
        this.offset += len;
        return Arrays.copyOfRange(this.buffer, this.offset - len, this.offset);
    }

    public void put(byte[] bytes) {
        if (bytes == null) {
            return;
        }

        this.ensureCapacity(this.count + bytes.length);

        System.arraycopy(bytes, 0, this.buffer, this.count, bytes.length);
        this.count += bytes.length;
    }

    public long getLong() {
        return Binary.readLong(this.get(8));
    }

    public void putLong(long l) {
        this.put(Binary.writeLong(l));
    }

    public int getInt() {
        return Binary.readInt(this.get(4));
    }

    public void putInt(int i) {
        this.put(Binary.writeInt(i));
    }

    public long getLLong() {
        return Binary.readLLong(this.get(8));
    }

    public void putLLong(long l) {
        this.put(Binary.writeLLong(l));
    }

    public int getLInt() {
        return Binary.readLInt(this.get(4));
    }

    public void putLInt(int i) {
        this.put(Binary.writeLInt(i));
    }

    public int getShort() {
        return Binary.readShort(this.get(2));
    }

    public void putShort(int s) {
        this.put(Binary.writeShort(s));
    }

    public int getLShort() {
        return Binary.readLShort(this.get(2));
    }

    public void putLShort(int s) {
        this.put(Binary.writeLShort(s));
    }

    public float getFloat() {
        return getFloat(-1);
    }

    public float getFloat(int accuracy) {
        return Binary.readFloat(this.get(4), accuracy);
    }

    public void putFloat(float v) {
        this.put(Binary.writeFloat(v));
    }

    public float getLFloat() {
        return getLFloat(-1);
    }

    public float getLFloat(int accuracy) {
        return Binary.readLFloat(this.get(4), accuracy);
    }

    public void putLFloat(float v) {
        this.put(Binary.writeLFloat(v));
    }

    public int getTriad() {
        return Binary.readTriad(this.get(3));
    }

    public void putTriad(int triad) {
        this.put(Binary.writeTriad(triad));
    }

    public int getLTriad() {
        return Binary.readLTriad(this.get(3));
    }

    public void putLTriad(int triad) {
        this.put(Binary.writeLTriad(triad));
    }

    public boolean getBoolean() {
        return this.getByte() == 0x01;
    }

    public void putBoolean(boolean bool) {
        this.putByte((byte) (bool ? 1 : 0));
    }

    public int getByte() {
        return this.buffer[this.offset++] & 0xff;
    }

    public void putByte(byte b) {
        this.put(new byte[]{b});
    }

    /**
     * Reads a list of Attributes from the stream.
     *
     * @return Attribute[]
     */
    public Attribute[] getAttributeList() throws Exception {
        List<Attribute> list = new ArrayList<>();
        long count = this.getUnsignedVarInt();

        for (int i = 0; i < count; ++i) {
            String name = this.getString();
            Attribute attr = Attribute.getAttributeByName(name);
            if (attr != null) {
                attr.setMinValue(this.getLFloat());
                attr.setValue(this.getLFloat());
                attr.setMaxValue(this.getLFloat());
                list.add(attr);
            } else {
                throw new Exception("Unknown attribute type \"" + name + '"');
            }
        }

        return list.toArray(new Attribute[0]);
    }

    /**
     * Writes a list of Attributes to the packet buffer using the standard format.
     */
    public void putAttributeList(Attribute[] attributes) {
        this.putUnsignedVarInt(attributes.length);
        for (Attribute attribute : attributes) {
            this.putString(attribute.getName());
            this.putLFloat(attribute.getMinValue());
            this.putLFloat(attribute.getValue());
            this.putLFloat(attribute.getMaxValue());
        }
    }

    public void putUUID(UUID uuid) {
        this.put(Binary.writeUUID(uuid));
    }

    public UUID getUUID() {
        return Binary.readUUID(this.get(16));
    }

    public void putSkin(Skin skin) {
        this.putSkin(ProtocolInfo.CURRENT_PROTOCOL, skin);
    }

    public void putSkin(int protocol, Skin skin) {
        this.putString(skin.getSkinId());

        if (protocol < ProtocolInfo.v1_13_0) {
            if (skin.isPersona()) { // Hack: Replace persona skins with steve skins for < 1.13 players to avoid invisible skins
                this.putByteArray(Base64.getDecoder().decode(Skin.STEVE_SKIN));
                if (protocol >= ProtocolInfo.v1_2_13) {
                    this.putByteArray(skin.getCapeData().data);
                }
                this.putString("geometry.humanoid.custom");
                this.putString(Skin.STEVE_GEOMETRY);
            } else {
                this.putByteArray(skin.getSkinData().data);
                if (protocol >= ProtocolInfo.v1_2_13) {
                    this.putByteArray(skin.getCapeData().data);
                }
                this.putString(skin.isLegacySlim ? "geometry.humanoid.customSlim" : "geometry.humanoid.custom");
                this.putString(skin.getGeometryData());
            }
        } else {
            if (protocol >= ProtocolInfo.v1_16_210) {
                this.putString(skin.getPlayFabId());
            }
            this.putString(skin.getSkinResourcePatch());
            this.putImage(skin.getSkinData());

            List<SkinAnimation> animations = skin.getAnimations();
            this.putLInt(animations.size());
            for (SkinAnimation animation : animations) {
                this.putImage(animation.image);
                this.putLInt(animation.type);
                this.putLFloat(animation.frames);
                if (protocol >= ProtocolInfo.v1_16_100) {
                    this.putLInt(animation.expression);
                }
            }

            this.putImage(skin.getCapeData());
            this.putString(skin.getGeometryData());
            this.putString(skin.getAnimationData());
            this.putBoolean(skin.isPremium());
            this.putBoolean(skin.isPersona());
            this.putBoolean(skin.isCapeOnClassic());
            this.putString(skin.getCapeId());
            this.putString(skin.getFullSkinId());
            if (protocol >= ProtocolInfo.v1_14_60) {
                this.putString(skin.getArmSize());
                this.putString(skin.getSkinColor());

                List<PersonaPiece> pieces = skin.getPersonaPieces();
                this.putLInt(pieces.size());
                for (PersonaPiece piece : pieces) {
                    this.putString(piece.id);
                    this.putString(piece.type);
                    this.putString(piece.packId);
                    this.putBoolean(piece.isDefault);
                    this.putString(piece.productId);
                }

                List<PersonaPieceTint> tints = skin.getTintColors();
                this.putLInt(tints.size());
                for (PersonaPieceTint tint : tints) {
                    this.putString(tint.pieceType);
                    List<String> colors = tint.colors;
                    this.putLInt(colors.size());
                    for (String color : colors) {
                        this.putString(color);
                    }
                }
            }
        }
    }

    public void putImage(SerializedImage image) {
        this.putLInt(image.width);
        this.putLInt(image.height);
        this.putByteArray(image.data);
    }

    public SerializedImage getImage() {
        int width = this.getLInt();
        int height = this.getLInt();
        byte[] data = this.getByteArray();
        return new SerializedImage(width, height, data);
    }

    public Skin getSkin() {
        return getSkin(ProtocolInfo.CURRENT_PROTOCOL);
    }

    public Skin getSkin(int protocol) { // Can be used only with protocol >= 388
        Skin skin = new Skin();
        skin.setSkinId(this.getString());
        if (protocol >= ProtocolInfo.v1_16_210) {
            skin.setPlayFabId(this.getString());
        }
        skin.setSkinResourcePatch(this.getString());
        skin.setSkinData(this.getImage());

        int animationCount = this.getLInt();
        for (int i = 0; i < Math.min(animationCount, 1024); i++) {
            SerializedImage image = this.getImage();
            int type = this.getLInt();
            float frames = this.getLFloat();
            int expression = protocol >= ProtocolInfo.v1_16_100 ? this.getLInt() : 0;
            skin.getAnimations().add(new SkinAnimation(image, type, frames, expression));
        }

        skin.setCapeData(this.getImage());
        skin.setGeometryData(this.getString());
        skin.setAnimationData(this.getString());
        skin.setPremium(this.getBoolean());
        skin.setPersona(this.getBoolean());
        skin.setCapeOnClassic(this.getBoolean());
        skin.setCapeId(this.getString());
        this.getString(); // TODO: Full skin id
        if (protocol >= ProtocolInfo.v1_14_60) {
            skin.setArmSize(this.getString());
            skin.setSkinColor(this.getString());

            int piecesLength = this.getLInt();
            for (int i = 0; i < Math.min(piecesLength, 1024); i++) {
                String pieceId = this.getString();
                String pieceType = this.getString();
                String packId = this.getString();
                boolean isDefault = this.getBoolean();
                String productId = this.getString();
                skin.getPersonaPieces().add(new PersonaPiece(pieceId, pieceType, packId, isDefault, productId));
            }

            int tintsLength = this.getLInt();
            for (int i = 0; i < Math.min(tintsLength, 1024); i++) {
                String pieceType = this.getString();
                List<String> colors = new ArrayList<>();
                int colorsLength = this.getLInt();
                for (int i2 = 0; i2 < Math.min(colorsLength, 1024); i2++) {
                    colors.add(this.getString());
                }
                skin.getTintColors().add(new PersonaPieceTint(pieceType, colors));
            }
        }
        return skin;
    }

    public Item getSlot() {
        return this.getSlot(ProtocolInfo.CURRENT_PROTOCOL);
    }

    public Item getSlot(int protocolId) {
        int id = this.getVarInt();
        if (id == 0) {
            return Item.get(0, 0, 0);
        }

        boolean hasData = false;
        int fullId = -1;
        if (protocolId >= ProtocolInfo.v1_16_100) {
            fullId = RuntimeItems.getRuntimeMapping(protocolId).getLegacyFullId(id);
            hasData = RuntimeItems.hasData(fullId);
            id = RuntimeItems.getId(fullId);
        }

        int auxValue = this.getVarInt();
        int data = auxValue >> 8;
        if (data == Short.MAX_VALUE) {
            data = -1;
        }
        // Swap data to network data
        if (hasData) {
            data = RuntimeItems.getData(fullId);
        }
        int cnt = auxValue & 0xff;

        int nbtLen = this.getLShort();
        byte[] nbt = new byte[0];
        if (nbtLen < Short.MAX_VALUE) {
            nbt = this.get(nbtLen);
        } else if (nbtLen == 65535) {
            int nbtTagCount = (int) getUnsignedVarInt();
            int offset = this.offset;
            FastByteArrayInputStream stream = new FastByteArrayInputStream(get());
            for (int i = 0; i < nbtTagCount; i++) {
                try {
                    // TODO: 05/02/2019 This hack is necessary because we keep the raw NBT tag. Try to remove it.
                    CompoundTag tag = NBTIO.read(stream, ByteOrder.LITTLE_ENDIAN, true);
                    // Hack for tool damage
                    if (tag.contains("Damage")) {
                        data = tag.getInt("Damage");
                        tag.remove("Damage");
                    }
                    if (tag.contains("__DamageConflict__")) {
                        tag.put("Damage", tag.removeAndGet("__DamageConflict__"));
                    }
                    if (tag.getAllTags().size() > 0) {
                        nbt = NBTIO.write(tag, ByteOrder.LITTLE_ENDIAN, false);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            setOffset(offset + (int) stream.position());
        }

        String[] canPlaceOn = new String[this.getVarInt()];
        for (int i = 0; i < canPlaceOn.length; ++i) {
            canPlaceOn[i] = this.getString();
        }

        String[] canDestroy = new String[this.getVarInt()];
        for (int i = 0; i < canDestroy.length; ++i) {
            canDestroy[i] = this.getString();
        }

        Item item = Item.get(
                id, data, cnt, nbt
        );

        if (canDestroy.length > 0 || canPlaceOn.length > 0) {
            CompoundTag namedTag = item.getNamedTag();
            if (namedTag == null) {
                namedTag = new CompoundTag();
            }

            if (canDestroy.length > 0) {
                ListTag<StringTag> listTag = new ListTag<>("CanDestroy");
                for (String blockName : canDestroy) {
                    listTag.add(new StringTag("", blockName));
                }
                namedTag.put("CanDestroy", listTag);
            }

            if (canPlaceOn.length > 0) {
                ListTag<StringTag> listTag = new ListTag<>("CanPlaceOn");
                for (String blockName : canPlaceOn) {
                    listTag.add(new StringTag("", blockName));
                }
                namedTag.put("CanPlaceOn", listTag);
            }

            item.setNamedTag(namedTag);
        }

        if (item.getId() == ItemID.SHIELD && protocolId >= ProtocolInfo.v1_12_0) {
            this.getVarLong();
        }

        return item;
    }

    public void putSlot(Item item) {
        this.putSlot(ProtocolInfo.CURRENT_PROTOCOL, item);
    }

    public void putSlot(int protocolId, Item item) {
        if (item == null || item.getId() == 0) {
            this.putVarInt(0);
            return;
        }

        int networkId = item.getId();
        boolean clearData = false;
        if (protocolId >= ProtocolInfo.v1_16_100) {
            int networkFullId = RuntimeItems.getRuntimeMapping(protocolId).getNetworkFullId(item);
            clearData = RuntimeItems.hasData(networkFullId);
            networkId = RuntimeItems.getNetworkId(networkFullId);
        }

        // Multiversion: Replace unsupported items
        // TODO: Send the original item data in nbt and read it from there in getSlot, replace netherite items with diamond items for < 1.16
        if (protocolId < ProtocolInfo.v1_14_0 && (networkId == Item.HONEYCOMB || (networkId == Item.SUSPICIOUS_STEW && protocolId < ProtocolInfo.v1_13_0))) {
            networkId = Item.INFO_UPDATE;
        } else if (protocolId < ProtocolInfo.v1_16_0 && networkId >= Item.LODESTONECOMPASS) {
            networkId = Item.INFO_UPDATE;
        }

        this.putVarInt(networkId);

        int auxValue;
        boolean isDurable = item instanceof ItemDurable;

        if (protocolId >= ProtocolInfo.v1_12_0) {
            auxValue = item.getCount();
            if (!isDurable) {
                int meta;
                if (protocolId < ProtocolInfo.v1_16_100) {
                    meta = item.hasMeta() ? item.getDamage() : -1;
                } else {
                    meta = clearData ? 0 : item.hasMeta() ? item.getDamage() : -1;
                }
                auxValue |= ((meta & 0x7fff) << 8);
            }
        } else {
            auxValue = (((item.hasMeta() ? item.getDamage() : -1) & 0x7fff) << 8) | item.getCount();
        }

        this.putVarInt(auxValue);

        if (item.hasCompoundTag() || (isDurable && protocolId >= ProtocolInfo.v1_12_0)) {
            if (protocolId < ProtocolInfo.v1_12_0) {
                byte[] nbt = item.getCompoundTag();
                this.putLShort(nbt.length);
                this.put(nbt);
            } else {
                try {
                    // Hack for tool damage
                    byte[] nbt = item.getCompoundTag();
                    CompoundTag tag;
                    if (nbt == null || nbt.length == 0) {
                        tag = new CompoundTag();
                    } else {
                        tag = NBTIO.read(nbt, ByteOrder.LITTLE_ENDIAN, false);
                    }
                    if (tag.contains("Damage")) {
                        tag.put("__DamageConflict__", tag.removeAndGet("Damage"));
                    }
                    if (isDurable) {
                        tag.putInt("Damage", item.getDamage());
                    }

                    this.putLShort(0xffff);
                    this.putByte((byte) 1);
                    this.put(NBTIO.write(tag, ByteOrder.LITTLE_ENDIAN, true));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            this.putLShort(0);
        }
        List<String> canPlaceOn = extractStringList(item, "CanPlaceOn");
        List<String> canDestroy = extractStringList(item, "CanDestroy");
        this.putVarInt(canPlaceOn.size());
        for (String block : canPlaceOn) {
            this.putString(block);
        }
        this.putVarInt(canDestroy.size());
        for (String block : canDestroy) {
            this.putString(block);
        }

        if (item.getId() == ItemID.SHIELD && protocolId >= ProtocolInfo.v1_12_0) {
            this.putVarLong(0); //"blocking tick" (ffs mojang)
        }
    }

    public Item getRecipeIngredient(int protocolId) {
        int networkId = this.getVarInt();
        if (networkId == 0) {
            return Item.get(0, 0, 0);
        }

        int id = networkId;
        if (protocolId >= ProtocolInfo.v1_16_100) {
            int legacyFullId = RuntimeItems.getRuntimeMapping(protocolId).getLegacyFullId(id);
            id = RuntimeItems.getId(legacyFullId);
        }

        int damage = this.getVarInt();
        if (damage == 0x7fff) {
            damage = -1;
        }

        int count = this.getVarInt();
        return Item.get(id, damage, count);
    }

    public void putRecipeIngredient(int protocolId, Item ingredient) {
        if (ingredient == null || ingredient.getId() == 0) {
            this.putVarInt(0);
            return;
        }

        int networkId = ingredient.getId();
        int damage = ingredient.hasMeta() ? ingredient.getDamage() : 0x7fff;

        if (protocolId >= ProtocolInfo.v1_16_100) {
            int networkFullId = RuntimeItems.getRuntimeMapping(protocolId).getNetworkFullId(ingredient);
            networkId = RuntimeItems.getNetworkId(networkFullId);
            if (RuntimeItems.hasData(networkFullId)) {
                damage = 0;
            }
        }

        this.putVarInt(networkId);
        this.putVarInt(damage);
        this.putVarInt(ingredient.getCount());
    }

    private static List<String> extractStringList(Item item, String tagName) {
        CompoundTag namedTag = item.getNamedTag();
        if (namedTag == null) {
            return Collections.emptyList();
        }

        ListTag<StringTag> listTag = namedTag.getList(tagName, StringTag.class);
        if (listTag == null) {
            return Collections.emptyList();
        }

        int size = listTag.size();
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            StringTag stringTag = listTag.get(i);
            if (stringTag != null) {
                values.add(stringTag.data);
            }
        }

        return values;
    }

    public byte[] getByteArray() {
        return this.get((int) this.getUnsignedVarInt());
    }

    public void putByteArray(byte[] b) {
        this.putUnsignedVarInt(b.length);
        this.put(b);
    }

    public String getString() {
        return new String(this.getByteArray(), StandardCharsets.UTF_8);
    }

    public void putString(String string) {
        byte[] b = string.getBytes(StandardCharsets.UTF_8);
        this.putByteArray(b);
    }

    public long getUnsignedVarInt() {
        return VarInt.readUnsignedVarInt(this);
    }

    public void putUnsignedVarInt(long v) {
        VarInt.writeUnsignedVarInt(this, v);
    }

    public int getVarInt() {
        return VarInt.readVarInt(this);
    }

    public void putVarInt(int v) {
        VarInt.writeVarInt(this, v);
    }

    public long getVarLong() {
        return VarInt.readVarLong(this);
    }

    public void putVarLong(long v) {
        VarInt.writeVarLong(this, v);
    }

    public long getUnsignedVarLong() {
        return VarInt.readUnsignedVarLong(this);
    }

    public void putUnsignedVarLong(long v) {
        VarInt.writeUnsignedVarLong(this, v);
    }

    public BlockVector3 getBlockVector3() {
        return new BlockVector3(this.getVarInt(), (int) this.getUnsignedVarInt(), this.getVarInt());
    }

    public BlockVector3 getSignedBlockPosition() {
        return new BlockVector3(getVarInt(), getVarInt(), getVarInt());
    }

    public void putSignedBlockPosition(BlockVector3 v) {
        putVarInt(v.x);
        putVarInt(v.y);
        putVarInt(v.z);
    }

    public void putBlockVector3(BlockVector3 v) {
        this.putBlockVector3(v.x, v.y, v.z);
    }

    public void putBlockVector3(int x, int y, int z) {
        this.putVarInt(x);
        this.putUnsignedVarInt(y);
        this.putVarInt(z);
    }

    public Vector3f getVector3f() {
        return new Vector3f(this.getLFloat(4), this.getLFloat(4), this.getLFloat(4));
    }

    public void putVector3f(Vector3f v) {
        this.putVector3f(v.x, v.y, v.z);
    }

    public void putVector3f(float x, float y, float z) {
        this.putLFloat(x);
        this.putLFloat(y);
        this.putLFloat(z);
    }

    public void putGameRules(GameRules gameRules) {
        Map<GameRule, GameRules.Value> rules = gameRules.getGameRules();
        this.putUnsignedVarInt(rules.size());
        rules.forEach((gameRule, value) -> {
            putString(gameRule.getName().toLowerCase());
            value.write(this);
        });
    }

    /**
     * Reads and returns an EntityUniqueID
     *
     * @return int
     */
    public long getEntityUniqueId() {
        return this.getVarLong();
    }

    /**
     * Writes an EntityUniqueID
     */
    public void putEntityUniqueId(long eid) {
        this.putVarLong(eid);
    }

    /**
     * Reads and returns an EntityRuntimeID
     */
    public long getEntityRuntimeId() {
        return this.getUnsignedVarLong();
    }

    /**
     * Writes an EntityUniqueID
     */
    public void putEntityRuntimeId(long eid) {
        this.putUnsignedVarLong(eid);
    }

    public BlockFace getBlockFace() {
        return BlockFace.fromIndex(this.getVarInt());
    }

    public void putBlockFace(BlockFace face) {
        this.putVarInt(face.getIndex());
    }

    public void putEntityLink(int protocol, EntityLink link) {
        putEntityUniqueId(link.fromEntityUniquieId);
        putEntityUniqueId(link.toEntityUniquieId);
        putByte(link.type);
        putBoolean(link.immediate);
        if (protocol >= 407) {
            putBoolean(link.riderInitiated);
        }
    }

    public EntityLink getEntityLink() {
        return new EntityLink(
                getEntityUniqueId(),
                getEntityUniqueId(),
                (byte) getByte(),
                getBoolean(),
                getBoolean() //1.16+
        );
    }

    public boolean feof() {
        return this.offset < 0 || this.offset >= this.buffer.length;
    }

    private void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - buffer.length > 0) {
            grow(minCapacity);
        }
    }

    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buffer.length;
        int newCapacity = oldCapacity << 1;

        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }

        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            newCapacity = hugeCapacity(minCapacity);
        }
        this.buffer = Arrays.copyOf(buffer, newCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) { // overflow
            throw new OutOfMemoryError();
        }
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }
}