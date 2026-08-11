package com.xztx127.sj;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final JavaPlugin plugin;
    private File configFile;
    private File xxszFile;
    private FileConfiguration config;
    private FileConfiguration xxszConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // 创建配置目录
        File configDir = new File(plugin.getDataFolder().getParentFile(), "xztx127-sj");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        // 加载 config.yaml
        configFile = new File(configDir, "config.yaml");
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // 加载 xxsz.yaml
        xxszFile = new File(configDir, "xxsz.yaml");
        if (!xxszFile.exists()) {
            createDefaultXxszConfig();
        }
        xxszConfig = YamlConfiguration.loadConfiguration(xxszFile);
    }

    private void createDefaultConfig() {
        try {
            configFile.createNewFile();
            config = YamlConfiguration.loadConfiguration(configFile);
            
            // 显示/隐藏设置
            config.set("sidebar.enabled", true);
            config.set("display.server-ip", true);
            config.set("display.website", true);
            config.set("display.real-time", true);
            config.set("display.game-time", true);
            config.set("display.day-count", true);
            config.set("display.player-count", true);
            config.set("display.username", true);
            config.set("display.login-type", true);
            config.set("display.announcement", true);
            config.set("display.ping", true);

            config.save(configFile);
            plugin.getLogger().info("已创建默认 config.yaml 配置文件");
        } catch (IOException e) {
            plugin.getLogger().severe("无法创建 config.yaml: " + e.getMessage());
        }
    }

    private void createDefaultXxszConfig() {
        try {
            xxszFile.createNewFile();
            xxszConfig = YamlConfiguration.loadConfiguration(xxszFile);
            
            // 服务器设置
            xxszConfig.set("server.ip", "请在此填写服务器地址");
            xxszConfig.set("server.website", "请在此填写服务器官网");
            
            // 公告设置
            xxszConfig.set("announcement", "欢迎加入本服务器！祝您游戏愉快！");
            
            // 初始天数
            xxszConfig.set("initial-day", 1);

            xxszConfig.save(xxszFile);
            plugin.getLogger().info("已创建默认 xxsz.yaml 配置文件");
        } catch (IOException e) {
            plugin.getLogger().severe("无法创建 xxsz.yaml: " + e.getMessage());
        }
    }

    // Sidebar 开关
    public boolean isSidebarEnabled() {
        return config.getBoolean("sidebar.enabled", true);
    }

    // 显示项开关
    public boolean isShowServerIP() {
        return config.getBoolean("display.server-ip", true);
    }

    public boolean isShowWebsite() {
        return config.getBoolean("display.website", true);
    }

    public boolean isShowRealTime() {
        return config.getBoolean("display.real-time", true);
    }

    public boolean isShowGameTime() {
        return config.getBoolean("display.game-time", true);
    }

    public boolean isShowDayCount() {
        return config.getBoolean("display.day-count", true);
    }

    public boolean isShowPlayerCount() {
        return config.getBoolean("display.player-count", true);
    }

    public boolean isShowUsername() {
        return config.getBoolean("display.username", true);
    }

    public boolean isShowLoginType() {
        return config.getBoolean("display.login-type", true);
    }

    public boolean isShowAnnouncement() {
        return config.getBoolean("display.announcement", true);
    }

    public boolean isShowPing() {
        return config.getBoolean("display.ping", true);
    }

    // 获取配置值
    public String getServerIP() {
        return xxszConfig.getString("server.ip", "未设置服务器地址");
    }

    public String getWebsite() {
        return xxszConfig.getString("server.website", "未设置官网");
    }

    public String getAnnouncement() {
        return xxszConfig.getString("announcement", "");
    }

    public int getInitialDay() {
        return xxszConfig.getInt("initial-day", 1);
    }

    // 保存当前天数（可选功能）
    public void saveCurrentDay(int day) {
        xxszConfig.set("current-day", day);
        try {
            xxszConfig.save(xxszFile);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存当前天数：" + e.getMessage());
        }
    }

    // 重载配置
    public void reloadConfigs() {
        config = YamlConfiguration.loadConfiguration(configFile);
        xxszConfig = YamlConfiguration.loadConfiguration(xxszFile);
    }
}
