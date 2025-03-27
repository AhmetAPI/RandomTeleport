# Kaptan - Minecraft Teleportasyon ve Spawn Yönetim Eklentisi

![Kaptan Logo](https://via.placeholder.com/150) <!-- Logo eklemek isterseniz URL'yi değiştirin -->

**Kaptan**, Minecraft sunucularınız için özelleştirilebilir bir teleportasyon ve spawn yönetim eklentisidir. Türkçe ve İngilizce dil desteği ile oyuncularınıza esnek bir deneyim sunar. RTP (rastgele ışınlanma), spawn yönetimi ve daha fazlasını kolayca yapılandırabilirsiniz.

## Özellikler

- **Çift Dil Desteği**: Komutlar ve mesajlar Türkçe (`/merkez`, `/spawnayarla`) ve İngilizce (`/spawn`, `/setspawn`) olarak kullanılabilir.
- **Rastgele Işınlanma (RTP)**: Oyuncuları güvenli bir rastgele konuma ışınlar (`/dunya` veya `/rtp`).
- **Spawn Yönetimi**: Spawn noktasını ayarlayın (`/setspawn` veya `/spawnayarla`) ve oyuncuları oraya ışınlayın (`/spawn` veya `/merkez`).
- **Reload Desteği**: `/kaptan reload` ile ayarları ve mesajları yeniden yükleyin.
- **Veri Kaydı**: Oyuncu verileri (RTP, spawn, son konum) `kayitlar.ahmetapi` dosyasına kaydedilir.
- **Özelleştirilebilir Mesajlar**: `messages.ahmetapi` dosyası ile tüm mesajları düzenleyin.
- **Başlık Bildirimleri**: Her komut çalıştığında "Rastgele Işınlanma" başlığı ve "by github.com/ahmetapi" alt başlığı gösterilir.
- **VIP Desteği**: VIP oyuncular için anında ışınlanma özelliği.
- **Güvenli Konum Bulma**: RTP sırasında lav, su veya tehlikeli alanlardan kaçınır.

## Kurulum

1. **JAR Dosyasını İndirin**:
   - [Releases](https://github.com/ahmetapi/Kaptan/releases) sayfasından en son sürümü indirin.

2. **Sunucuya Yükleyin**:
   - İndirdiğiniz `Kaptan-0.1-SNAPSHOT.jar` dosyasını sunucunuzun `plugins/` klasörüne kopyalayın.

3. **Sunucuyu Başlatın**:
   - Sunucuyu başlatın; `plugins/Kaptan/` klasörü otomatik olarak oluşacaktır.
   - `config.yml`, `messages.ahmetapi` ve `kayitlar.ahmetapi` dosyaları oluşturulur.

4. **Ayarları Yapılandırın**:
   - `config.yml`’de dil, teleportasyon ayarları ve diğer seçenekleri düzenleyin.
   - `messages.ahmetapi`’de mesajları özelleştirin.

## Kullanım

### Komutlar
| Komut                | Açıklama                           | İzin                  |
|----------------------|------------------------------------|-----------------------|
| `/spawn` veya `/merkez` | Oyuncuyu spawn noktasına ışınlar   | Varsayılan            |
| `/dunya` veya `/rtp` | Rastgele bir konuma ışınlar        | Varsayılan            |
| `/setspawn` veya `/spawnayarla` | Spawn noktasını ayarlar            | `kaptan.setspawn`     |
| `/kaptan reload`     | Ayarları ve mesajları yeniden yükler | `kaptan.reload`     |

### Örnek Kullanım
1. **Spawn Ayarlama**:

/setspawn
text
Çıktı: Başlık "Rastgele Işınlanma" ve sohbet mesajı "Spawn noktası ayarlandı: world (0, 65, 0)".

2. **Spawn’a Işınlanma**:

/merkez
text
Çıktı: Başlık "Rastgele Işınlanma" ve sohbet mesajı "Spawn noktasına başarıyla ışınlandın!".

3. **Rastgele Işınlanma**:

/rtp
text
Çıktı: Başlık "Rastgele Işınlanma" ve sohbet mesajı "Dünyada rastgele bir yere ışınlandın!".

## Yapılandırma

### config.yml
```yaml
language: "TR"  # TR veya EN
teleport:
countdown: 10
sound: "ENTITY_PLAYER_LEVELUP"
vip-instant: true
spawn:
enabled: true
last-location-worlds:
 - "world"
dunya:
enabled: true
allowed-worlds:
 - "spawn"
target-world: "world"
random-coordinates:
 min-x: -3000
 max-x: 3000
 min-z: -3000
 max-z: 3000
safety:
 attempts: 10
respawn:
radius: 10
permissions:
vip: "vip.use"
setspawn: "kaptan.setspawn"
messages.ahmetapi
yaml
TR:
  spawn-success: "&aSpawn noktasına başarıyla ışınlandın!"
  setspawn-success: "&aSpawn noktası ayarlandı: %world% (%x%, %y%, %z%)"
  dunya-random-success: "&aDünyada rastgele bir yere ışınlandın!"
EN:
  spawn-success: "&aSuccessfully teleported to spawn point!"
  setspawn-success: "&aSpawn point set: %world% (%x%, %y%, %z%)"
  dunya-random-success: "&aTeleported to a random location in the world!"
Gereksinimler

    Minecraft Sürümü: 1.20+
    Sunucu Türü: Paper/Spigot (Paper önerilir)
    Java: 17 veya üstü

Katkı Sağlama

    Bu depoyu forklayın.
    Değişikliklerinizi yapın ve pull request gönderin.
    Sorularınız için Issues sayfasını kullanın.

Lisans

Bu proje  ile lisanslanmıştır.
İletişim

    GitHub: ahmetapi
    E-posta: ahmetapi@example.com <!-- Kendi e-postanızı ekleyin -->

Kaptan ile sunucunuzu daha eğlenceli hale getirin!
