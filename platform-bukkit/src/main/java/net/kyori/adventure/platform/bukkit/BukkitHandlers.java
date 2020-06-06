/*
 * This file is part of text-extras, licensed under the MIT License.
 *
 * Copyright (c) 2018 KyoriPowered
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
package net.kyori.adventure.platform.bukkit;

import java.lang.reflect.Proxy;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BukkitHandlers {
  
  private BukkitHandlers() {}

  static class Chat implements Handler.Chat<CommandSender, String> {
    @Override
    public boolean isAvailable() {
      return true;
    }

    @Override
    public String initState(final Component component) {
      return LegacyComponentSerializer.legacy().serialize(component);
    }

    @Override
    public void send(@NonNull final CommandSender target, @NonNull final String message) {
      target.sendMessage(message);
    }
  }

  static class BossBar implements Handler.BossBar<Player> {
    static final boolean SUPPORTED = Crafty.hasClass("org.bukkit.boss.BossBar"); // Added MC 1.9
    static final BukkitBossBarListener LISTENERS = new BukkitBossBarListener();
    
    BossBar() {
      final Plugin fakePlugin = (Plugin) Proxy.newProxyInstance(BukkitPlatform.class.getClassLoader(), new Class<?>[] {Plugin.class}, (proxy, method, args) -> {
        switch(method.getName()) {
          case "isEnabled":
            return true;
          case "equals":
            return proxy == args[0];
          default:
            return null; // yeet
        }
      });
      final Listener holder = new Listener() {};

      // Remove players from boss bars
      Bukkit.getPluginManager().registerEvent(PlayerQuitEvent.class, holder, EventPriority.NORMAL, (listener, event) -> {
        LISTENERS.unsubscribeFromAll(((PlayerQuitEvent) event).getPlayer());
      }, fakePlugin, false);
      
    }

    @Override
    public boolean isAvailable() {
      return SUPPORTED;
    }

    @Override
    public void show(@NonNull final Player viewer, final net.kyori.adventure.bossbar.@NonNull BossBar bar) {
      LISTENERS.subscribe(viewer, bar);
    }

    @Override
    public void hide(@NonNull final Player viewer, final net.kyori.adventure.bossbar.@NonNull BossBar bar) {
      LISTENERS.unsubscribe(viewer, bar);
    }
  }
  
  static abstract class PlaySound implements Handler.PlaySound<Player> {
    static final boolean IS_AT_LEAST_113 = Crafty.hasClass("org.bukkit.NamespacedKey");

    @Override
    public void play(@NonNull final Player viewer, @NonNull final Sound sound) {
      play(viewer, sound, viewer.getLocation());
    }

    @Override
    public void play(@NonNull final Player viewer, @NonNull final Sound sound, final double x, final double y, final double z) {
      play(viewer, sound, new Location(viewer.getWorld(), x, y, z));
    }
    
    protected abstract void play(final @NonNull Player viewer, final @NonNull Sound sound, final @NonNull Location position);

    static @NonNull String name(final @Nullable Key name) {
      if(name == null) {
        return "";
      }

      if(IS_AT_LEAST_113) { // sound format changed to use identifiers
        return name.asString();
      } else {
        return name.value();
      }
    }
  }
  
  static class PlaySound_WithCategory extends PlaySound {
    static final boolean SOUND_CATEGORY_SUPPORTED = Crafty.hasMethod(Player.class, "stopSound", String.class, Crafty.findClass("org.bukkit.SoundCategory")); // Added MC 1.11

    @Override
    public boolean isAvailable() {
      return SOUND_CATEGORY_SUPPORTED;
    }

    @Override
    protected void play(final @NonNull Player viewer, final @NonNull Sound sound, final @NonNull Location position) {
      final String name = name(sound.name());
      final SoundCategory category = category(sound.source());
      viewer.playSound(position, name, category, sound.volume(), sound.pitch());
    }

    @Override
    public void stop(@NonNull final Player viewer, @NonNull final SoundStop stop) {
      final String soundName = name(stop.sound());
      final Sound.@Nullable Source source = stop.source();
      final SoundCategory category = source == null ? null : category(source);
      viewer.stopSound(soundName, category);
    }

    static SoundCategory category(final Sound.@NonNull Source source) {
      switch(source) {
        case MASTER: return SoundCategory.MASTER;
        case MUSIC: return SoundCategory.MUSIC;
        case RECORD: return SoundCategory.RECORDS;
        case WEATHER: return SoundCategory.WEATHER;
        case BLOCK: return SoundCategory.BLOCKS;
        case HOSTILE: return SoundCategory.HOSTILE;
        case NEUTRAL: return SoundCategory.NEUTRAL;
        case PLAYER: return SoundCategory.PLAYERS;
        case AMBIENT: return SoundCategory.AMBIENT;
        case VOICE: return SoundCategory.VOICE;
        default: throw new IllegalArgumentException("Unknown sound source " + source);
      }
    }
  }
  
  static class PlaySound_NoCategory extends PlaySound {
    static final boolean SOUND_STOP_SUPPORTED = Crafty.hasMethod(Player.class, "stopSound", String.class); // Added MC 1.9

    @Override
    public boolean isAvailable() {
      return true;
    }

    @Override
    protected void play(final @NonNull Player viewer, final @NonNull Sound sound, final @NonNull Location position) {
      viewer.playSound(position, name(sound.name()), sound.volume(), sound.pitch());
    }

    @Override
    public void stop(@NonNull final Player viewer, @NonNull final SoundStop sound) {
      if(!SOUND_STOP_SUPPORTED) {
        return;
      }
      viewer.stopSound(name(sound.sound()));
    }
  }
}
