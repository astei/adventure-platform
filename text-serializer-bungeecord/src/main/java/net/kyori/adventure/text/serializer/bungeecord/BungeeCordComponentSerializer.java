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
package net.kyori.adventure.text.serializer.bungeecord;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

/**
 * A component serializer for BungeeCord's {@link BaseComponent}.
 */
public final class BungeeCordComponentSerializer implements ComponentSerializer<Component, Component, BaseComponent[]> {
  private static final boolean SUPPORTED = bind();
  private static final BungeeCordComponentSerializer MODERN = new BungeeCordComponentSerializer(GsonComponentSerializer.gson(), LegacyComponentSerializer.builder().hexColors().build());
  private static final BungeeCordComponentSerializer PRE_1_16 = new BungeeCordComponentSerializer(GsonComponentSerializer.colorDownsamplingGson(), LegacyComponentSerializer.legacy());

  /**
   * Gets whether the component serializer has native support.
   *
   * <p>Even if this is {@code false}, the serializer will still work.</p>
   *
   * @return if there is native support
   */
  public static boolean nativeSupport() {
    return SUPPORTED;
  }

  /**
   * Gets a component serializer.
   *
   * @return a component serializer
   */
  public static BungeeCordComponentSerializer get() {
    return MODERN;
  }

  /**
   * Gets a component serializer, with color downsampling.
   *
   * @return a component serializer
   */
  public static BungeeCordComponentSerializer legacy() {
    return PRE_1_16;
  }

  private final GsonComponentSerializer serializer;
  private final LegacyComponentSerializer legacySerializer;

  private BungeeCordComponentSerializer(final GsonComponentSerializer serializer, final LegacyComponentSerializer legacySerializer) {
    this.serializer = serializer;
    this.legacySerializer = legacySerializer;
  }

  private static boolean bind() {
    try {
      final Field gsonField = GsonInjections.field(net.md_5.bungee.chat.ComponentSerializer.class, "gson");
      return GsonInjections.injectGson((Gson) gsonField.get(null), builder -> {
        GsonComponentSerializer.gson().populator().apply(builder); // TODO: this might be unused?
        builder.registerTypeAdapterFactory(new SelfSerializable.AdapterFactory());
      });
    } catch(Exception ex) {
      return false;
    }
  }

  @Override
  public @NonNull Component deserialize(final @NonNull BaseComponent@NonNull[] input) {
    requireNonNull(input, "input");

    if(input.length == 1 && input[0] instanceof AdapterComponent) {
      return ((AdapterComponent) input[0]).component;
    } else {
      return this.serializer.deserialize(net.md_5.bungee.chat.ComponentSerializer.toString(input));
    }
  }

  @Override
  public @NonNull BaseComponent@NonNull[] serialize(final @NonNull Component component) {
    requireNonNull(component, "component");

    if(SUPPORTED) {
      return new BaseComponent[] {new AdapterComponent(component)};
    } else {
      return net.md_5.bungee.chat.ComponentSerializer.parse(this.serializer.serialize(component));
    }
  }

  class AdapterComponent extends BaseComponent implements SelfSerializable {
    private final Component component;
    private volatile String legacy;

    @SuppressWarnings("deprecation") // TODO: when/if bungee removes this, ???
    AdapterComponent(final Component component) {
      this.component = component;
    }

    @Override
    public String toLegacyText() {
      if (this.legacy == null) {
        this.legacy = legacySerializer.serialize(this.component);
      }
      return this.legacy;
    }

    @Override
    public @NonNull BaseComponent duplicate() {
      return this;
    }

    @Override
    public void write(final JsonWriter out) throws IOException {
      serializer.serializer().getAdapter(Component.class).write(out, this.component);
    }
  }
}
