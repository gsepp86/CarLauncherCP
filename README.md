# Car Launcher CP

Launcher per autoradio Android: un'unica schermata "da plancia" con velocità GPS,
radio FM, musica Bluetooth, telefono, navigatore, app in evidenza e tema colore
selezionabile. Il tema predefinito è il rosso, ma l'accento dell'interfaccia si cambia
al volo. Testi molto grandi, pensati per essere letti mentre si guida.

![Schermata principale](screenshots/launcher.png)

## Funzioni

- **Velocità oraria** — tachimetro circolare con velocità GPS in km/h, velocità massima
  raggiunta e stato del fix (`GPS ATTIVO` / `CERCO GPS` / `GPS OFF`). A macchina ferma il
  valore resta esattamente **0**: i falsi 1-2 km/h del GPS vengono filtrati con una soglia
  di fermo e un'isteresi, e se il fix sparisce la velocità non resta congelata sull'ultimo
  valore.
- **Radio FM** — apre l'app radio del tuo autoradio: viene cercata automaticamente fra i
  pacchetti più diffusi, altrimenti la scegli tu e la scelta viene ricordata.
- **Musica Bluetooth** — apre il lettore musicale (app di sistema o quella che preferisci).
  Mentre un brano è in riproduzione il tasto mostra **barre animate** che si muovono.
- **Telefono** — apre il dialer per chiamare in vivavoce.
- **Navigatore** — apre l'app di navigazione scelta fra quelle installate sul sistema
  (Google Maps, Waze, Here, TomTom, Sygic, OsmAnd, Yandex, iGO e altre). Se non ne è
  stata scelta una viene rilevata automaticamente; la scelta resta memorizzata.
- **App in evidenza** — fino a 6 scorciatoie scelte da te, in una griglia 2×3
  (una riga in verticale). Tieni premuta una scorciatoia per sostituirla o rimuoverla.
- **Tema colore** — 11 accenti (Rosso, Corallo, Arancio, Ambra, Verde, Turchese, Blu,
  Indaco, Viola, Magenta, Ghiaccio). Il colore viene applicato a sfondo, tachimetro,
  icone e card, e resta memorizzato.
- **Layout adattivo** — orizzontale per la plancia (quattro tasti in griglia 2×2),
  verticale per l'uso su telefono.

Tieni premuto un tasto (Radio, Musica, Navigatore) per cambiare l'app collegata senza
passare dal pannello del tema.

![Tema colore](screenshots/tema-colore.png)

## Installazione

1. Scarica `CarLauncherCP-1.1.0.apk` dalla sezione **Releases**.
2. Copiala sul dispositivo (chiavetta USB, ADB, ecc.) e installala, abilitando
   l'installazione da origini sconosciute se richiesto.
3. Al primo avvio concede il permesso **Posizione** (serve solo per la velocità GPS).
   Se lo neghi, il launcher funziona lo stesso e il tachimetro mostra `--`.
4. Per usarlo come home: **Impostazioni → App → App predefinite → Home → Car Launcher CP**.
   L'activity dichiara già il category `HOME`, quindi compare fra i launcher disponibili.

L'APK nella release è firmato con la chiave di debug del progetto: va bene per il
sideload sul proprio autoradio, non per la pubblicazione su Play Store. Per una
firma tua crea un keystore e un file `keystore.properties` nella radice del progetto
(entrambi esclusi da git):

```properties
storeFile=/percorso/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## Costruzione

Requisiti: JDK 17, Android SDK con platform 34 e build-tools 34.

```bash
./gradlew assembleDebug    # APK di debug
./gradlew assembleRelease  # APK di release
```

Gli APK finiscono in `app/build/outputs/apk/`.

## Anteprima del design

`preview/index.html` è una riproduzione statica dell'interfaccia (HTML/CSS, font
Rajdhani) utile per provare i colori senza installare l'app: si apre direttamente
nel browser, senza build. Non fa parte dell'APK.

## Struttura

```
app/src/main/java/com/carlaunchercp/
  MainActivity.java      schermata principale e layout
  SpeedGaugeView.java    tachimetro disegnato su Canvas
  Speedometer.java       velocità GPS filtrata (fermo, isteresi, fix scaduto)
  TileView.java          card Radio / Musica / Telefono / Navigatore
  EqualizerBarsView.java barre animate durante la riproduzione
  ThemePanel.java        pannello tema + app collegate
  AppPickerDialog.java   elenco app con ricerca
  LauncherActions.java   intent per radio, musica, telefono, navigazione
  ThemePalette.java      palette degli accenti
  Prefs.java             preferenze
  Ui.java                helper di stile
  CarBackgroundView.java sfondo con bagliori dell'accento
```
