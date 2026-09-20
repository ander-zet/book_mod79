package com.example.customdiary.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class DiarySavedData extends SavedData {
    private static final String DATA_NAME = "custom_diary_data";

    private boolean giveAllActive = false;
    private final Map<UUID, Integer> playerProgression = new HashMap<>();
    private final Map<UUID, Integer> playerDropCounts = new HashMap<>();
    private final Set<UUID> cursedFirePlayers = new HashSet<>();
    private final List<PendingTask> pendingTasks = new ArrayList<>();

    public static class PendingTask {
        public UUID playerUUID;
        public long executeTick;
        public int step; // 1: +3s, 2: +6s, 3: +13s

        public PendingTask(UUID playerUUID, long executeTick, int step) {
            this.playerUUID = playerUUID;
            this.executeTick = executeTick;
            this.step = step;
        }
    }

    public static DiarySavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DiarySavedData::new, DiarySavedData::load, null),
                DATA_NAME
        );
    }

    public boolean isGiveAllActive() { return giveAllActive; }
    public void setGiveAllActive(boolean active) { this.giveAllActive = active; setDirty(); }

    public int getPlayerChapter(UUID uuid) { return playerProgression.getOrDefault(uuid, 0); }
    public void setPlayerChapter(UUID uuid, int chapter) { playerProgression.put(uuid, chapter); setDirty(); }

    public int getDropCount(UUID uuid) { return playerDropCounts.getOrDefault(uuid, 0); }
    public int incrementDropCount(UUID uuid) {
        int count = getDropCount(uuid) + 1;
        playerDropCounts.put(uuid, count);
        setDirty();
        return count;
    }

    public Set<UUID> getCursedFirePlayers() { return cursedFirePlayers; }
    public List<PendingTask> getPendingTasks() { return pendingTasks; }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("GiveAllActive", giveAllActive);

        CompoundTag progTag = new CompoundTag();
        playerProgression.forEach((uuid, ch) -> progTag.putInt(uuid.toString(), ch));
        tag.put("PlayerProgression", progTag);

        CompoundTag dropsTag = new CompoundTag();
        playerDropCounts.forEach((uuid, cnt) -> dropsTag.putInt(uuid.toString(), cnt));
        tag.put("PlayerDropCounts", dropsTag);

        ListTag fireList = new ListTag();
        for (UUID uuid : cursedFirePlayers) {
            CompoundTag fireTag = new CompoundTag();
            fireTag.putUUID("UUID", uuid);
            fireList.add(fireTag);
        }
        tag.put("CursedFirePlayers", fireList);

        return tag;
    }

    public static DiarySavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        DiarySavedData data = new DiarySavedData();
        data.giveAllActive = tag.getBoolean("GiveAllActive");

        if (tag.contains("PlayerProgression", Tag.TAG_COMPOUND)) {
            CompoundTag progTag = tag.getCompound("PlayerProgression");
            for (String key : progTag.getAllKeys()) {
                data.playerProgression.put(UUID.fromString(key), progTag.getInt(key));
            }
        }

        if (tag.contains("PlayerDropCounts", Tag.TAG_COMPOUND)) {
            CompoundTag dropsTag = tag.getCompound("PlayerDropCounts");
            for (String key : dropsTag.getAllKeys()) {
                data.playerDropCounts.put(UUID.fromString(key), dropsTag.getInt(key));
            }
        }

        if (tag.contains("CursedFirePlayers", Tag.TAG_LIST)) {
            ListTag fireList = tag.getList("CursedFirePlayers", Tag.TAG_COMPOUND);
            for (int i = 0; i < fireList.size(); i++) {
                data.cursedFirePlayers.add(fireList.getCompound(i).getUUID("UUID"));
            }
        }

        return data;
    }
}