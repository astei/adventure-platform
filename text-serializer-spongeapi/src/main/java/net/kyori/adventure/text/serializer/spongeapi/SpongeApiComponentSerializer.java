/*
 * This file is part of adventure-platform, licensed under the MIT License.
 *
 * Copyright (c) 2018-2020 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.kyori.adventure.text.serializer.spongeapi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.MinecraftVersion;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import static java.util.Objects.requireNonNull;

/**
 * A component serializer for Sponge API's {@link Text}.
 */
public final class SpongeApiComponentSerializer implements ComponentSerializer<Component, Component, Text> {
    private static final SpongeApiComponentSerializer INSTANCE = new SpongeApiComponentSerializer();
    private static final MinecraftVersion VERSION = Sponge.getPlatform().getMinecraftVersion();

    /**
     * Gets a component serializer for the current {@link Platform#getMinecraftVersion()}.
     *
     * @return a component serializer
     */
    public static SpongeApiComponentSerializer get() {
        return of(VERSION);
    }

    /**
     * Gets a component serializer for a specific {@link MinecraftVersion}.
     *
     * @param version a minecraft version
     * @return a component serializer
     */
    public static SpongeApiComponentSerializer of(final @NonNull MinecraftVersion version) {
        return INSTANCE;
    }

    private SpongeApiComponentSerializer() {}

    @NonNull
    @Override
    public Component deserialize(@NonNull Text input) {
        return GsonComponentSerializer.gson().deserialize(TextSerializers.JSON.serialize(requireNonNull(input, "text")));
    }

    @NonNull
    @Override
    public Text serialize(@NonNull Component component) {
        return TextSerializers.JSON.deserialize(GsonComponentSerializer.colorDownsamplingGson().serialize(requireNonNull(component, "component")));
    }
}
