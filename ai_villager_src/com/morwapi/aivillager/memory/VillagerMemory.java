/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2487
 *  net.minecraft.class_2520
 */
package com.morwapi.aivillager.memory;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_2487;
import net.minecraft.class_2520;

public class VillagerMemory {
    private final Map<String, String> memories = new HashMap<String, String>();

    public void set(String key, String value) {
        this.memories.put(key, value);
    }

    public String get(String key) {
        return this.memories.get(key);
    }

    public void remove(String key) {
        this.memories.remove(key);
    }

    public Map<String, String> getAll() {
        return new HashMap<String, String>(this.memories);
    }

    public void clear() {
        this.memories.clear();
    }

    public void writeNbt(class_2487 nbt) {
        class_2487 memoryNbt = new class_2487();
        for (Map.Entry<String, String> entry : this.memories.entrySet()) {
            memoryNbt.method_10582(entry.getKey(), entry.getValue());
        }
        nbt.method_10566("AIVillagerMemory", (class_2520)memoryNbt);
    }

    public void readNbt(class_2487 nbt) {
        if (nbt.method_10545("AIVillagerMemory")) {
            class_2487 memoryNbt = nbt.method_10562("AIVillagerMemory");
            for (String key : memoryNbt.method_10541()) {
                this.memories.put(key, memoryNbt.method_10558(key));
            }
        }
    }
}

