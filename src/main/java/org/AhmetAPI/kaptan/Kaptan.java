package org.AhmetAPI.kaptan;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Özelleştirilebilir teleportasyon ve spawn yönetimi eklentisi.
 * İki dil desteği (TR/EN) ve reload özelliği içerir.
 * Oyuncu verileri kayitlar.ahmetapi dosyasına kaydedilir.
 * Mesajlar messages.ahmetapi dosyasından çekilir.
 *
 * @author xahmets_ (AhmetAPI)
 * @version 0.1
 */
public class Kaptan extends JavaPlugin implements Listener {

    // Oyuncu verileri için haritalar
    private final Map<Player, Boolean> teleporting = new HashMap<>();  // Işınlanma durumu
    private final Map<Player, Location> lastLocation = new HashMap<>();  // Son konumlar
    private final Map<Player, Location> firstSpawn = new HashMap<>();  // İlk spawn konumları
    private Location spawnLocation;  // Ana spawn noktası
    private final Random random = new Random();  // Rastgele sayı üretimi

    // Veri ve mesaj dosyaları
    private File dataFile;  // Oyuncu verileri dosyası
    private YamlConfiguration dataConfig;  // Veri dosyasının yapılandırması
    private File messageFile;  // Mesaj dosyası
    private YamlConfiguration messageConfig;  // Mesaj dosyasının yapılandırması

    // Dil ayarı
    private String language;

    /**
     * Eklenti başlatıldığında çalışır.
     */
    @Override
    public void onEnable() {
        // Config dosyasını yükle
        saveDefaultConfig();
        language = getConfig().getString("language", "TR");  // Varsayılan dil: Türkçe

        // Mesaj dosyasını yükle
        setupMessageFile();

        // Olay dinleyicilerini kaydet
        getServer().getPluginManager().registerEvents(this, this);

        // Spawn konumunu yükle
        loadSpawnLocation();

        // Veri dosyasını oluştur ve yükle
        setupDataFile();
        if (dataConfig == null) {
            getLogger().severe("dataConfig başlatılamadı! Veri dosyası yüklenemedi.");
        } else {
            loadPlayerData();
        }

        getLogger().info("Kaptan eklentisi etkinleştirildi! v0.1 - xahmets_");
    }

    /**
     * Eklenti kapatıldığında çalışır.
     */
    @Override
    public void onDisable() {
        savePlayerData();  // Verileri kaydet
        getLogger().info("Kaptan eklentisi devre dışı bırakıldı!");
    }

    /**
     * Komutları işler (iki dil desteğiyle).
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("only-players"));
            return true;
        }

        Player player = (Player) sender;
        String commandName = command.getName().toLowerCase();

        // /spawn veya /merkez komutu
        if (commandName.equals("spawn") || commandName.equals("merkez")) {
            if (!getConfig().getBoolean("spawn.enabled", true)) {
                sendActionBar(player, getMessage("feature-disabled").replace("%feature%", "Spawn"));
                return true;
            }
            if (spawnLocation == null) {
                sendActionBar(player, getMessage("spawn-not-set"));
                return true;
            }
            List<String> lastLocWorlds = getConfig().getStringList("spawn.last-location-worlds");
            if (lastLocWorlds.contains(player.getWorld().getName())) {
                lastLocation.put(player, player.getLocation());
                savePlayerLastLocation(player, player.getLocation());
            }
            startTeleport(player, spawnLocation, getMessage("spawn-success"));
            return true;
        }

        // /dunya veya /rtp komutu
        if (commandName.equals("dunya") || commandName.equals("rtp")) {
            if (!getConfig().getBoolean("dunya.enabled", true)) {
                sendActionBar(player, getMessage("feature-disabled").replace("%feature%", "Dunya"));
                return true;
            }
            List<String> allowedWorlds = getConfig().getStringList("dunya.allowed-worlds");
            if (!allowedWorlds.contains(player.getWorld().getName())) {
                sendActionBar(player, getMessage("dunya-not-allowed")
                        .replace("%world%", player.getWorld().getName()));
                return true;
            }

            if (firstSpawn.containsKey(player)) {
                startTeleport(player, lastLocation.get(player), getMessage("dunya-last-location"));
            } else {
                World world = getServer().getWorld(getConfig().getString("dunya.target-world"));
                if (world == null) {
                    sendActionBar(player, getMessage("invalid-world"));
                    return true;
                }

                Location randomLoc = findSafeRandomLocation(world);
                if (randomLoc == null) {
                    sendActionBar(player, getMessage("no-safe-location"));
                    return true;
                }

                startTeleport(player, randomLoc, getMessage("dunya-random-success"));
                firstSpawn.put(player, randomLoc);
                lastLocation.put(player, randomLoc);
                savePlayerRTPData(player, randomLoc);
                savePlayerFirstSpawn(player, randomLoc);
            }
            return true;
        }

        // /setspawn veya /spawnayarla komutu
        if (commandName.equals("setspawn") || commandName.equals("spawnayarla")) {
            if (!player.hasPermission(getConfig().getString("permissions.setspawn"))) {
                sendActionBar(player, getMessage("no-permission"));
                return true;
            }
            spawnLocation = player.getLocation();
            setSpawnLocation(spawnLocation);
            String message = getMessage("setspawn-success")
                    .replace("%world%", spawnLocation.getWorld().getName())
                    .replace("%x%", String.valueOf(spawnLocation.getBlockX()))
                    .replace("%y%", String.valueOf(spawnLocation.getBlockY()))
                    .replace("%z%", String.valueOf(spawnLocation.getBlockZ()));
            sendActionBar(player, message);
            savePlayerSpawnData(player, spawnLocation);
            getLogger().info("Spawn ayarlandı: " + spawnLocation.toString());
            return true;
        }

        // /kaptan reload komutu
        if (commandName.equals("kaptan")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("kaptan.reload")) {
                    sendActionBar(player, getMessage("no-permission"));
                    return true;
                }
                reloadConfig();  // Config dosyasını yeniden yükle
                language = getConfig().getString("language", "TR");
                setupMessageFile();  // Mesaj dosyasını yeniden yükle
                loadSpawnLocation();  // Spawn konumunu yeniden yükle
                setupDataFile();  // Veri dosyasını yeniden yükle
                loadPlayerData();
                sendActionBar(player, getMessage("reload-success"));
                getLogger().info("Config, mesajlar ve veriler yeniden yüklendi!");
                return true;
            }
        }

        return false;
    }

    /**
     * Oyuncuyu teleport eder (VIP'ler için anında, diğerleri için geri sayımlı).
     */
    private void startTeleport(Player player, Location location, String message) {
        String vipPerm = getConfig().getString("permissions.vip");
        if (player.hasPermission(vipPerm) && getConfig().getBoolean("teleport.vip-instant")) {
            teleportWithChunkLoad(player, location, message);
            return;
        }

        int countdown = getConfig().getInt("teleport.countdown");
        teleporting.put(player, true);

        new BukkitRunnable() {
            int timeLeft = countdown;

            @Override
            public void run() {
                if (!teleporting.containsKey(player) || !teleporting.get(player)) {
                    cancel();
                    return;
                }

                if (timeLeft > 0) {
                    player.sendTitle(ChatColor.translateAlternateColorCodes('&', getMessage("teleport-title")
                                    .replace("%time%", String.valueOf(timeLeft))),
                            ChatColor.translateAlternateColorCodes('&', getMessage("teleport-subtitle")), 0, 20, 0);
                    timeLeft--;
                } else {
                    if (teleporting.containsKey(player)) {
                        teleportWithChunkLoad(player, location, message);
                        teleporting.remove(player);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    /**
     * Chunk yüklenmesini sağlayarak oyuncuyu teleport eder.
     */
    private void teleportWithChunkLoad(Player player, Location location, String message) {
        Chunk chunk = location.getChunk();
        if (!chunk.isLoaded()) {
            chunk.load();
        }
        player.teleport(location);
        sendActionBar(player, "&a✔ " + message);
        player.playSound(location, Sound.valueOf(getConfig().getString("teleport.sound", "ENTITY_PLAYER_LEVELUP")), 1, 2);
    }

    /**
     * Güvenli bir rastgele konum bulur.
     */
    private Location findSafeRandomLocation(World world) {
        int minX = getConfig().getInt("dunya.random-coordinates.min-x");
        int maxX = getConfig().getInt("dunya.random-coordinates.max-x");
        int minZ = getConfig().getInt("dunya.random-coordinates.min-z");
        int maxZ = getConfig().getInt("dunya.random-coordinates.max-z");
        int attempts = getConfig().getInt("dunya.safety.attempts");

        for (int i = 0; i < attempts; i++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;
            int y = world.getHighestBlockYAt(x, z) + 1;

            Location loc = new Location(world, x, y, z);
            Block feet = loc.getBlock();
            Block head = feet.getRelative(0, 1, 0);
            Block ground = feet.getRelative(0, -1, 0);

            if (feet.getType().isAir() && head.getType().isAir() && ground.getType().isSolid()
                    && !ground.getType().toString().contains("WATER") && !ground.getType().toString().contains("LAVA")) {
                return loc;
            }
        }
        return null;
    }

    // Oyuncu hareket ettiğinde teleport iptali
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (teleporting.containsKey(player)) {
            teleporting.remove(player);
            sendActionBar(player, getMessage("teleport-cancel-move"));
            player.sendTitle(" ", " ", 0, 0, 0);
        }
    }

    // Oyuncu PvP'ye girerse teleport iptali
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (teleporting.containsKey(attacker)) {
                teleporting.remove(attacker);
                sendActionBar(attacker, getMessage("teleport-cancel-pvp"));
                attacker.sendTitle(" ", " ", 0, 0, 0);
            }
        }
    }

    // Oyuncu yeniden doğduğunda
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (firstSpawn.containsKey(player)) {
            Location firstLoc = firstSpawn.get(player);
            World world = firstLoc.getWorld();
            int radius = getConfig().getInt("respawn.radius");

            int x = (int) (firstLoc.getX() + (random.nextInt(radius * 2 + 1) - radius));
            int z = (int) (firstLoc.getZ() + (random.nextInt(radius * 2 + 1) - radius));
            int y = world.getHighestBlockYAt(x, z) + 1;

            Location respawnLoc = new Location(world, x, y, z);
            Block feet = respawnLoc.getBlock();
            Block head = feet.getRelative(0, 1, 0);
            Block ground = feet.getRelative(0, -1, 0);

            if (feet.getType().isAir() && head.getType().isAir() && ground.getType().isSolid()) {
                event.setRespawnLocation(respawnLoc);
                sendActionBar(player, getMessage("respawn-success"));
            } else {
                Location safeLoc = findSafeRandomLocation(world);
                if (safeLoc != null) {
                    event.setRespawnLocation(safeLoc);
                    sendActionBar(player, getMessage("respawn-success"));
                } else {
                    sendActionBar(player, getMessage("no-safe-respawn"));
                }
            }
        } else {
            sendActionBar(player, getMessage("no-first-spawn"));
        }
    }

    /**
     * Oyuncu RTP verilerini kaydeder.
     */
    private void savePlayerRTPData(Player player, Location rtpLocation) {
        if (dataConfig == null) {
            setupDataFile();
            if (dataConfig == null) return;
        }
        UUID uuid = player.getUniqueId();
        String path = uuid.toString() + ".rtp";
        dataConfig.set(path + ".x", rtpLocation.getX());
        dataConfig.set(path + ".y", rtpLocation.getY());
        dataConfig.set(path + ".z", rtpLocation.getZ());
        dataConfig.set(path + ".world", rtpLocation.getWorld().getName());
        saveDataFile();
    }

    /**
     * Oyuncu ilk spawn verilerini kaydeder.
     */
    private void savePlayerFirstSpawn(Player player, Location firstSpawnLocation) {
        if (dataConfig == null) {
            setupDataFile();
            if (dataConfig == null) return;
        }
        UUID uuid = player.getUniqueId();
        String path = uuid.toString() + ".first-spawn";
        dataConfig.set(path + ".x", firstSpawnLocation.getX());
        dataConfig.set(path + ".y", firstSpawnLocation.getY());
        dataConfig.set(path + ".z", firstSpawnLocation.getZ());
        dataConfig.set(path + ".world", firstSpawnLocation.getWorld().getName());
        saveDataFile();
    }

    /**
     * Oyuncu son konum verilerini kaydeder.
     */
    private void savePlayerLastLocation(Player player, Location lastLoc) {
        if (dataConfig == null) {
            setupDataFile();
            if (dataConfig == null) return;
        }
        UUID uuid = player.getUniqueId();
        String path = uuid.toString() + ".last-location";
        dataConfig.set(path + ".x", lastLoc.getX());
        dataConfig.set(path + ".y", lastLoc.getY());
        dataConfig.set(path + ".z", lastLoc.getZ());
        dataConfig.set(path + ".world", lastLoc.getWorld().getName());
        saveDataFile();
    }

    /**
     * Oyuncu spawn verilerini kaydeder.
     */
    private void savePlayerSpawnData(Player player, Location spawnLoc) {
        if (dataConfig == null) {
            setupDataFile();
            if (dataConfig == null) return;
        }
        UUID uuid = player.getUniqueId();
        String path = uuid.toString() + ".spawn";
        dataConfig.set(path + ".x", spawnLoc.getX());
        dataConfig.set(path + ".y", spawnLoc.getY());
        dataConfig.set(path + ".z", spawnLoc.getZ());
        dataConfig.set(path + ".world", spawnLoc.getWorld().getName());
        saveDataFile();
    }

    /**
     * Tüm oyuncu verilerini kaydeder.
     */
    private void savePlayerData() {
        if (dataConfig == null) {
            setupDataFile();
            if (dataConfig == null) return;
        }
        for (Player player : getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            dataConfig.set(uuid.toString() + ".name", player.getName());
            if (firstSpawn.containsKey(player)) {
                savePlayerFirstSpawn(player, firstSpawn.get(player));
            }
            if (lastLocation.containsKey(player)) {
                savePlayerLastLocation(player, lastLocation.get(player));
            }
        }
        saveDataFile();
    }

    /**
     * Oyuncu verilerini yükler.
     */
    private void loadPlayerData() {
        if (dataConfig == null) {
            setupDataFile();
            if (dataConfig == null) return;
        }
        for (Player player : getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            String path = uuid.toString();
            if (dataConfig.contains(path + ".first-spawn")) {
                double x = dataConfig.getDouble(path + ".first-spawn.x");
                double y = dataConfig.getDouble(path + ".first-spawn.y");
                double z = dataConfig.getDouble(path + ".first-spawn.z");
                String worldName = dataConfig.getString(path + ".first-spawn.world");
                World world = getServer().getWorld(worldName);
                if (world != null) {
                    firstSpawn.put(player, new Location(world, x, y, z));
                }
            }
            if (dataConfig.contains(path + ".last-location")) {
                double x = dataConfig.getDouble(path + ".last-location.x");
                double y = dataConfig.getDouble(path + ".last-location.y");
                double z = dataConfig.getDouble(path + ".last-location.z");
                String worldName = dataConfig.getString(path + ".last-location.world");
                World world = getServer().getWorld(worldName);
                if (world != null) {
                    lastLocation.put(player, new Location(world, x, y, z));
                }
            }
        }
    }

    /**
     * Veri dosyasını kaydeder.
     */
    private void saveDataFile() {
        if (dataConfig == null) return;
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("kayitlar.ahmetapi dosyasına kaydedilemedi: " + e.getMessage());
        }
    }

    /**
     * Veri dosyasını oluşturur ve yükler.
     */
    private void setupDataFile() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        dataFile = new File(getDataFolder(), "kayitlar.ahmetapi");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                getLogger().info("kayitlar.ahmetapi dosyası oluşturuldu.");
            } catch (IOException e) {
                getLogger().severe("kayitlar.ahmetapi dosyası oluşturulamadı: " + e.getMessage());
                return;
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    /**
     * Mesaj dosyasını oluşturur ve yükler.
     */
    private void setupMessageFile() {
        messageFile = new File(getDataFolder(), "messages.ahmetapi");
        if (!messageFile.exists()) {
            saveResource("messages.ahmetapi", false);  // Varsayılan dosyayı kopyala
        }
        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
    }

    /**
     * Mesajı dil ayarına göre çeker.
     */
    private String getMessage(String key) {
        if (messageConfig == null) {
            setupMessageFile();
            if (messageConfig == null) return "&cMesaj dosyası yüklenemedi!";
        }
        String path = language + "." + key;
        String message = messageConfig.getString(path);
        if (message == null) {
            getLogger().warning("messages.ahmetapi'de '" + path + "' bulunamadı! Varsayılan mesaj kullanılıyor.");
            return "&cHata: " + key + " bulunamadı!";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Spawn konumunu yükler.
     */
    private void loadSpawnLocation() {
        if (getConfig().isSet("spawn.location")) {
            spawnLocation = (Location) getConfig().get("spawn.location");
            getLogger().info("Spawn yüklendi: " + (spawnLocation != null ? spawnLocation.toString() : "null"));
        } else {
            getLogger().info("Spawn noktası config'de tanımlı değil.");
        }
    }

    /**
     * Spawn konumunu kaydeder.
     */
    private void setSpawnLocation(Location location) {
        spawnLocation = location;
        getConfig().set("spawn.location", location);
        saveConfig();
        getLogger().info("Spawn kaydedildi: " + location.toString());
    }

    /**
     * Oyuncuya action bar mesajı gönderir.
     */
    private void sendActionBar(Player player, String message) {
        player.sendActionBar(ChatColor.translateAlternateColorCodes('&', message));
    }
}