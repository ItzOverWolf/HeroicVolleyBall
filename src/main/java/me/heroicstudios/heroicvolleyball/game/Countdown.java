package me.heroicstudios.heroicvolleyball.game;

import me.heroicstudios.heroicvolleyball.HeroicVolleyBall;
import me.heroicstudios.heroicvolleyball.message.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class Countdown {

    private Countdown() {
    }

    public static void run(HeroicVolleyBall plugin, int seconds, String titleKey, String subtitleKey,
                           Runnable onFinish, String... placeholders) {
        if (seconds <= 0) {
            onFinish.run();
            return;
        }

        Messages messages = plugin.getMessages();
        for (int i = 0; i < seconds; i++) {
            int remaining = seconds - i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String[] all = new String[placeholders.length + 2];
                System.arraycopy(placeholders, 0, all, 0, placeholders.length);
                all[placeholders.length] = "seconds";
                all[placeholders.length + 1] = String.valueOf(remaining);
                Component title = messages.get(titleKey, all);
                Component subtitle = messages.get(subtitleKey, all);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.showTitle(Messages.noFade(title, subtitle));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }
            }, i * 20L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, onFinish, seconds * 20L);
    }
}
