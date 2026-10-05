package com.crimsonmoon.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

/**
 * Загружает и размещает NBT-структуру напрямую в мир, БЕЗ использования
 * ванильного {@link net.minecraft.structure.StructureTemplateManager}.
 *
 * Зачем: ванильный менеджер разбирает файл структуры целиком в дерево Java-
 * объектов ({@code NbtCompound}/{@code NbtList}/...) и держит его в памяти
 * ПОСТОЯННО (кэш, который никогда не очищается за время жизни сервера). Для
 * маленьких структур это незаметно, но для структуры на несколько миллионов
 * блок-записей (как арена, ~4.4 млн позиций) это означает гигабайты heap,
 * зависшие навсегда после первого же захода в измерение — и рано или поздно
 * OutOfMemoryError вылетает уже на чём-то совершенно постороннем (рендер
 * обычного моба, как в присланном крэш-репорте), просто потому что памяти
 * больше ни на что не хватает.
 *
 * Этот загрузчик читает файл вручную, тег за тегом, и СРАЗУ применяет каждый
 * блок к миру, не накапливая список блоков в памяти. Дополнительная память
 * сверх обычной работы игры — по сути постоянная (палитра блоков, которая
 * маленькая, и один текущий блок за раз), независимо от размера структуры.
 *
 * Файл читается за два прохода (структура арены хранит "blocks" раньше
 * "palette", а без палитры блок-индексы расшифровать нельзя):
 *  1) находим "size" и "palette" (всё остальное, включая огромный "blocks", быстро пропускается);
 *  2) зная палитру — читаем "blocks" и сразу расставляем блоки в мире.
 *
 * Известное ограничение: NBT блок-сущностей (содержимое сундуков, текст
 * табличек, настройки спаунеров и т.п.) и NBT-сущности, встроенные в
 * структуру (мобы, предметы на подставках и т.п.), этим загрузчиком
 * намеренно НЕ восстанавливаются — сами блоки/рельеф расставляются, но
 * содержимое контейнеров/текст останется пустым. Для арены-рельефа это
 * обычно не критично; если нужно — можно добавить отдельно.
 */
final class ArenaStructureLoader {

    private static final int TAG_END = 0;
    private static final int TAG_BYTE = 1;
    private static final int TAG_SHORT = 2;
    private static final int TAG_INT = 3;
    private static final int TAG_LONG = 4;
    private static final int TAG_FLOAT = 5;
    private static final int TAG_DOUBLE = 6;
    private static final int TAG_BYTE_ARRAY = 7;
    private static final int TAG_STRING = 8;
    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;
    private static final int TAG_INT_ARRAY = 11;
    private static final int TAG_LONG_ARRAY = 12;

    private ArenaStructureLoader() {}

    /** Читает ресурс мода целиком в память как есть (это только сжатые байты, ~12МБ — не проблема). */
    static byte[] loadResourceBytes(String classpathPath) throws IOException {
        try (InputStream in = ArenaStructureLoader.class.getResourceAsStream(classpathPath)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        }
    }

    /**
     * Разбирает структуру и расставляет её блоки в {@code world}, начиная с
     * позиции, которую вычисляет {@code originFn} из размера структуры (узнаём
     * размер только после первого прохода, поэтому он передаётся функцией, а
     * не значением).
     */
    static Vec3i streamPlace(byte[] compressedNbt, ServerWorld world, Function<Vec3i, BlockPos> originFn) throws IOException {
        Vec3i[] sizeHolder = new Vec3i[1];
        BlockState[][] paletteHolder = new BlockState[1][];

        try (DataInputStream in = openStream(compressedNbt)) {
            readTopLevel(in, (key, type, dis) -> {
                if (key.equals("size")) {
                    sizeHolder[0] = readSize(dis, type);
                } else if (key.equals("palette")) {
                    paletteHolder[0] = readPalette(dis, type);
                } else {
                    skip(dis, type);
                }
            });
        }

        Vec3i size = sizeHolder[0];
        BlockState[] palette = paletteHolder[0];
        if (size == null || palette == null) {
            throw new IOException("В файле структуры отсутствует 'size' или 'palette'");
        }

        BlockPos origin = originFn.apply(size);

        try (DataInputStream in = openStream(compressedNbt)) {
            readTopLevel(in, (key, type, dis) -> {
                if (key.equals("blocks")) {
                    streamBlocks(dis, type, palette, world, origin);
                } else {
                    skip(dis, type);
                }
            });
        }

        return size;
    }

    private static DataInputStream openStream(byte[] compressedNbt) throws IOException {
        return new DataInputStream(new BufferedInputStream(
            new GZIPInputStream(new ByteArrayInputStream(compressedNbt)), 1 << 20));
    }

    @FunctionalInterface
    private interface EntryHandler {
        void handle(String key, int type, DataInputStream in) throws IOException;
    }

    private static void readTopLevel(DataInputStream in, EntryHandler handler) throws IOException {
        int rootType = in.readByte();
        if (rootType != TAG_COMPOUND) {
            throw new IOException("Некорректный формат структуры: ожидался TAG_Compound в корне");
        }
        in.readUTF(); // имя корневого тега, обычно пустое

        while (true) {
            int type = in.readByte();
            if (type == TAG_END) {
                return;
            }
            String key = in.readUTF();
            handler.handle(key, type, in);
        }
    }

    private static Vec3i readSize(DataInputStream in, int type) throws IOException {
        if (type != TAG_LIST) {
            skip(in, type);
            return new Vec3i(0, 0, 0);
        }
        int elemType = in.readByte();
        int len = in.readInt();
        int x = 0, y = 0, z = 0;
        for (int i = 0; i < len; i++) {
            int v = readPrimitiveInt(in, elemType);
            if (i == 0) x = v;
            else if (i == 1) y = v;
            else if (i == 2) z = v;
        }
        return new Vec3i(x, y, z);
    }

    private static int readPrimitiveInt(DataInputStream in, int type) throws IOException {
        switch (type) {
            case TAG_BYTE: return in.readByte();
            case TAG_SHORT: return in.readShort();
            case TAG_INT: return in.readInt();
            default:
                skip(in, type);
                return 0;
        }
    }

    private static BlockState[] readPalette(DataInputStream in, int type) throws IOException {
        if (type != TAG_LIST) {
            skip(in, type);
            return new BlockState[0];
        }
        int elemType = in.readByte();
        int len = in.readInt();
        BlockState[] result = new BlockState[len];
        for (int i = 0; i < len; i++) {
            if (elemType != TAG_COMPOUND) {
                skip(in, elemType);
                result[i] = net.minecraft.block.Blocks.AIR.getDefaultState();
                continue;
            }
            NbtCompound entry = readCompoundBody(in);
            result[i] = resolveBlockState(entry);
        }
        return result;
    }

    private static BlockState resolveBlockState(NbtCompound entry) {
        String name = entry.getString("Name");
        if (name.isEmpty()) {
            return net.minecraft.block.Blocks.AIR.getDefaultState();
        }
        Identifier id = new Identifier(name);
        Block block = Registries.BLOCK.get(id);
        BlockState state = block.getDefaultState();

        if (entry.contains("Properties", NbtElement.COMPOUND_TYPE)) {
            NbtCompound props = entry.getCompound("Properties");
            StateManager<Block, BlockState> stateManager = block.getStateManager();
            for (String propName : props.getKeys()) {
                Property<?> property = stateManager.getProperty(propName);
                if (property != null) {
                    state = applyProperty(state, property, props.getString(propName));
                }
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.parse(value);
        return parsed.map(v -> state.with(property, v)).orElse(state);
    }

    /** Читает "blocks" и СРАЗУ ставит каждый непустой блок в мир — ничего не накапливает. */
    private static void streamBlocks(DataInputStream in, int type, BlockState[] palette,
                                      ServerWorld world, BlockPos origin) throws IOException {
        if (type != TAG_LIST) {
            skip(in, type);
            return;
        }
        int elemType = in.readByte();
        int len = in.readInt();

        BlockPos.Mutable target = new BlockPos.Mutable();

        for (int i = 0; i < len; i++) {
            if (elemType != TAG_COMPOUND) {
                skip(in, elemType);
                continue;
            }

            int posX = 0, posY = 0, posZ = 0;
            boolean hasPos = false;
            int stateIndex = -1;

            while (true) {
                int t = in.readByte();
                if (t == TAG_END) {
                    break;
                }
                String key = in.readUTF();
                if (key.equals("pos") && t == TAG_LIST) {
                    int pElemType = in.readByte();
                    int pLen = in.readInt();
                    int[] vals = new int[3];
                    for (int p = 0; p < pLen; p++) {
                        int v = readPrimitiveInt(in, pElemType);
                        if (p < 3) vals[p] = v;
                    }
                    posX = vals[0]; posY = vals[1]; posZ = vals[2];
                    hasPos = true;
                } else if (key.equals("state") && t == TAG_INT) {
                    stateIndex = in.readInt();
                } else {
                    // включая необязательный "nbt" блок-сущности — намеренно пропускаем
                    skip(in, t);
                }
            }

            if (!hasPos || stateIndex < 0 || stateIndex >= palette.length) {
                continue;
            }
            BlockState state = palette[stateIndex];
            if (state.isAir() || state.isOf(net.minecraft.block.Blocks.STRUCTURE_VOID)) {
                continue; // измерение и так пустое — воздух/structure_void ставить незачем
            }

            target.set(origin.getX() + posX, origin.getY() + posY, origin.getZ() + posZ);
            world.setBlockState(target, state, Block.NOTIFY_ALL);
        }
    }

    // ==================== generic NBT: skip / read-small-compound ====================

    private static void skip(DataInputStream in, int type) throws IOException {
        switch (type) {
            case TAG_END: return;
            case TAG_BYTE: in.skipBytes(1); return;
            case TAG_SHORT: in.skipBytes(2); return;
            case TAG_INT: in.skipBytes(4); return;
            case TAG_LONG: in.skipBytes(8); return;
            case TAG_FLOAT: in.skipBytes(4); return;
            case TAG_DOUBLE: in.skipBytes(8); return;
            case TAG_BYTE_ARRAY: {
                int len = in.readInt();
                in.skipBytes(len);
                return;
            }
            case TAG_STRING:
                in.readUTF();
                return;
            case TAG_LIST: {
                int elemType = in.readByte();
                int len = in.readInt();
                for (int i = 0; i < len; i++) skip(in, elemType);
                return;
            }
            case TAG_COMPOUND:
                while (true) {
                    int t = in.readByte();
                    if (t == TAG_END) return;
                    in.readUTF();
                    skip(in, t);
                }
            case TAG_INT_ARRAY: {
                int len = in.readInt();
                in.skipBytes(len * 4);
                return;
            }
            case TAG_LONG_ARRAY: {
                int len = in.readInt();
                in.skipBytes(len * 8);
                return;
            }
            default:
                throw new IOException("Неизвестный тип NBT-тега: " + type);
        }
    }

    /** Полноценный разбор компаунда в обычные NbtElement — используем только для МАЛЕНЬКИХ структур (палитра). */
    private static NbtCompound readCompoundBody(DataInputStream in) throws IOException {
        NbtCompound compound = new NbtCompound();
        while (true) {
            int type = in.readByte();
            if (type == TAG_END) {
                return compound;
            }
            String name = in.readUTF();
            compound.put(name, readElement(in, type));
        }
    }

    private static NbtElement readElement(DataInputStream in, int type) throws IOException {
        switch (type) {
            case TAG_BYTE: return NbtByte.of(in.readByte());
            case TAG_SHORT: return NbtShort.of(in.readShort());
            case TAG_INT: return NbtInt.of(in.readInt());
            case TAG_LONG: return NbtLong.of(in.readLong());
            case TAG_FLOAT: return NbtFloat.of(in.readFloat());
            case TAG_DOUBLE: return NbtDouble.of(in.readDouble());
            case TAG_BYTE_ARRAY: {
                int len = in.readInt();
                byte[] arr = new byte[len];
                in.readFully(arr);
                return new NbtByteArray(arr);
            }
            case TAG_STRING:
                return NbtString.of(in.readUTF());
            case TAG_LIST: {
                int elemType = in.readByte();
                int len = in.readInt();
                NbtList list = new NbtList();
                for (int i = 0; i < len; i++) {
                    list.add(readElement(in, elemType));
                }
                return list;
            }
            case TAG_COMPOUND:
                return readCompoundBody(in);
            case TAG_INT_ARRAY: {
                int len = in.readInt();
                int[] arr = new int[len];
                for (int i = 0; i < len; i++) arr[i] = in.readInt();
                return new NbtIntArray(arr);
            }
            case TAG_LONG_ARRAY: {
                int len = in.readInt();
                long[] arr = new long[len];
                for (int i = 0; i < len; i++) arr[i] = in.readLong();
                return new NbtLongArray(arr);
            }
            default:
                throw new IOException("Неизвестный тип NBT-тега: " + type);
        }
    }
}
