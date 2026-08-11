package com.xztx127.sj;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.firework.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SJPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private ConfigManager configManager;
    private Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private int currentDay = 1;
    private boolean hasNotifiedToday = false;
    private long lastNotifiedDay = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("sjreload").setExecutor(this);
        
        // 初始化天数
        currentDay = configManager.getInitialDay();
        
        // 启动计时器
        startGameTimeChecker();
        startSidebarUpdater();
        
        getLogger().info("SJ插件已启用！新的一天提示系统运行中。");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("sjreload")) {
            if (!sender.hasPermission("sj.reload")) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                return true;
            }
            
            configManager.reloadConfigs();
            currentDay = configManager.getInitialDay();
            sender.sendMessage(ChatColor.GREEN + "SJ插件配置已重载！");
            getLogger().info("配置已由 " + sender.getName() + " 重载");
            return true;
        }
        return false;
    }

    @Override
    public void onDisable() {
        // 保存当前天数
        configManager.saveCurrentDay(currentDay);
        getLogger().info("SJ插件已禁用。");
    }

    private void startGameTimeChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getWorlds().isEmpty()) return;
                
                // 获取主世界时间
                long worldTime = Bukkit.getWorlds().get(0).getTime();
                long currentDayTime = worldTime % 24000;
                
                // 7:00 AM = 7000 ticks
                if (currentDayTime >= 7000 && currentDayTime < 7020) {
                    // 检查是否是同一天内首次触发
                    long currentDayCount = worldTime / 24000;
                    if (currentDayCount != lastNotifiedDay) {
                        lastNotifiedDay = currentDayCount;
                        currentDay = configManager.getInitialDay() + (int)currentDayCount;
                        triggerMorningNotification();
                    }
                } else if (currentDayTime < 100) {
                    // 午夜重置标记
                    hasNotifiedToday = false;
                }
            }
        }.runTaskTimer(this, 20L, 20L); // 每秒检查一次
    }

    private void triggerMorningNotification() {
        if (hasNotifiedToday) return;
        hasNotifiedToday = true;

        String message = ChatColor.GOLD + "" + ChatColor.BOLD + "新的一天开始了！今天是第 " + currentDay + " 天";
        
        // 向所有玩家发送标题
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(ChatColor.GOLD + "新的一天开始了!", 
                           ChatColor.YELLOW + "今天是第 " + currentDay + " 天", 
                           20, 60, 20);
            
            // 在玩家位置生成烟花
            spawnFirework(player.getLocation());
        }

        getLogger().info("新的一天开始了！今天是第 " + currentDay + " 天");
    }

    private void spawnFirework(Location location) {
        Location fireworkLoc = location.clone().add(0, 2, 0);
        Firework firework = location.getWorld().spawn(fireworkLoc, Firework.class);
        
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(1);
        
        // 随机颜色
        Color color1 = Color.fromRGB(new Random().nextInt(256), new Random().nextInt(256), new Random().nextInt(256));
        Color color2 = Color.fromRGB(new Random().nextInt(256), new Random().nextInt(256), new Random().nextInt(256));
        
        FireworkEffect effect = FireworkEffect.builder()
            .with(FireworkEffect.Type.BALL_LARGE)
            .withColor(color1, color2)
            .withFade(Color.WHITE)
            .trail(true)
            .flicker(false)
            .build();
        
        meta.addEffect(effect);
        firework.setFireworkMeta(meta);
        
        // 设置不造成伤害
        firework.setSilent(true);
        
        // 自动移除
        new BukkitRunnable() {
            @Override
            public void run() {
                if (firework != null && !firework.isDead()) {
                    firework.remove();
                }
            }
        }.runTaskLater(this, 60L);
    }

    private void startSidebarUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllSidebars();
            }
        }.runTaskTimer(this, 20L, 20L); // 每秒更新一次
    }

    private void updateAllSidebars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateSidebar(player);
        }
    }

    private void updateSidebar(Player player) {
        if (!configManager.isSidebarEnabled()) return;

        Scoreboard scoreboard = playerScoreboards.computeIfAbsent(player.getUniqueId(), k -> {
            Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(sb);
            return sb;
        });

        // 如果记分板被重置，重新获取
        if (scoreboard.getObjective("sj_sidebar") == null) {
            Objective objective = scoreboard.registerNewObjective("sj_sidebar", "dummy", ChatColor.GOLD + "" + ChatColor.BOLD + "服务器信息");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        Objective objective = scoreboard.getObjective("sj_sidebar");
        if (objective == null) return;

        // 清除旧的分数
        for (String entry : objective.getScoreboard().getEntries()) {
            objective.getScore(entry).resetScore();
        }

        // 构建显示内容
        List<String> lines = new ArrayList<>();
        
        if (configManager.isShowServerIP()) {
            lines.add(ChatColor.WHITE + "服务器地址: " + ChatColor.YELLOW + configManager.getServerIP());
        }
        
        if (configManager.isShowWebsite()) {
            lines.add(ChatColor.WHITE + "官网: " + ChatColor.AQUA + configManager.getWebsite());
        }
        
        if (configManager.isShowRealTime()) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            lines.add(ChatColor.WHITE + "现实时间: " + ChatColor.GREEN + now.format(formatter));
        }
        
        if (configManager.isShowGameTime()) {
            long worldTime = player.getWorld().getTime() % 24000;
            int hours = (int)(worldTime / 1000 + 6) % 24;
            int minutes = (int)((worldTime % 1000) * 60 / 1000);
            lines.add(ChatColor.WHITE + "游戏时间: " + ChatColor.BLUE + String.format("%02d:%02d", hours, minutes));
        }
        
        if (configManager.isShowDayCount()) {
            lines.add(ChatColor.WHITE + "天数: " + ChatColor.GOLD + currentDay);
        }
        
        if (configManager.isShowPlayerCount()) {
            lines.add(ChatColor.WHITE + "在线人数: " + ChatColor.RED + Bukkit.getOnlinePlayers().size());
        }
        
        if (configManager.isShowUsername()) {
            lines.add(ChatColor.WHITE + "用户名: " + ChatColor.LIGHT_PURPLE + player.getName());
        }
        
        if (configManager.isShowLoginType()) {
            String loginType = player.isOnlineMode() ? "正版" : "离线";
            lines.add(ChatColor.WHITE + "登录模式: " + (player.isOnlineMode() ? ChatColor.GREEN : ChatColor.RED) + loginType);
        }
        
        if (configManager.isShowAnnouncement()) {
            String announcement = configManager.getAnnouncement();
            if (!announcement.isEmpty()) {
                lines.add(ChatColor.WHITE + "公告: " + ChatColor.DARK_GREEN + announcement);
            }
        }
        
        if (configManager.isShowPing()) {
            int ping = player.getPing();
            String pingColor = ping < 50 ? ChatColor.GREEN : (ping < 100 ? ChatColor.YELLOW : ChatColor.RED);
            lines.add(ChatColor.WHITE + "延迟: " + pingColor + ping + "ms");
        }

        // 添加空行分隔
        Collections.reverse(lines);
        int score = 1;
        for (String line : lines) {
            objective.getScore(line).setScore(score++);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 延迟一点更新记分板
        new BukkitRunnable() {
            @Override
            public void run() {
                updateSidebar(player);
            }
        }.runTaskLater(this, 20L);
    }
}
