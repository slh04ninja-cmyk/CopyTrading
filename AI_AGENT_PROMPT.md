# AI Agent Prompt — CopyTrading Bot + Android App

## Overview

Ce document decrit l'architecture du projet CopyTrading pour un agent AI qui doit travailler sur ce codebase.

### Le Bot (tgm)
Bot Telegram de copy trading qui lit les signaux de 108 canaux Telegram et execute les ordres sur MetaTrader 5 (Exness). Le bot tourne sur un serveur Windows VPS.

### L'App Android (CopyTrading)
Application Kotlin qui se connecte a l'API REST du bot (bot_api.py) pour afficher le dashboard, les positions, les performances et les logs en temps reel.

---

## Connexion au Serveur Windows

- **IP** : `38.247.138.124`
- **Port API** : `8000`
- **User** : `Administrator`
- **Hostname** : `vps-mt5`
- **Dossier bot** : `C:\TradingBot\`
- **Pas de SSH** — acces uniquement via l'API REST ou RDP

### Authentification

Tous les appels API necessitent le header :
```
Authorization: Bearer <API_TOKEN>
```

Le token est dans le fichier `.env` du serveur (`C:\TradingBot\.env`), variable `API_TOKEN`.

### Endpoints API principaux

| Endpoint | Methode | Description |
|---|---|---|
| `/api/status` | GET | Etat du bot + MT5 |
| `/api/dashboard` | GET | P&L quotidien, balance, equity |
| `/api/positions` | GET | Positions ouvertes |
| `/api/trades` | GET | Historique deals (params: days, from_date, to_date) |
| `/api/config` | GET/PUT | Variables .env |
| `/api/config/raw` | GET/PUT | Contenu brut du .env |
| `/api/logs` | GET | Logs du bot |
| `/api/file` | POST | Upload un fichier sur le serveur |
| `/api/file/read` | GET | Lire un fichier du serveur |
| `/api/exec` | POST | Executer une commande shell |
| `/api/bot/start` | POST | Demarrer le bot (telegram listener) |
| `/api/bot/stop` | POST | Arreter le bot |
| `/api/restart` | POST | Redemarrer uvicorn (recharge bot_api.py) |
| `/api/positions/close-all` | POST | Fermer toutes les positions |

### Exemples de commandes

```bash
# Status du bot
curl -s -H "Authorization: Bearer <TOKEN>" http://38.247.138.124:8000/api/status

# Dashboard
curl -s -H "Authorization: Bearer <TOKEN>" http://38.247.138.124:8000/api/dashboard

# Positions ouvertes
curl -s -H "Authorization: Bearer <TOKEN>" http://38.247.138.124:8000/api/positions

# Historique trades (30 jours)
curl -s -H "Authorization: Bearer <TOKEN>" "http://38.247.138.124:8000/api/trades?days=30"

# Lire un fichier du serveur
curl -s -H "Authorization: Bearer <TOKEN>" "http://38.247.138.124:8000/api/file/read?path=bot_api.py"

# Executer une commande
curl -X POST -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" \
  -d '{"command": "tasklist /FI \"IMAGENAME eq python.exe\""}' \
  http://38.247.138.124:8000/api/exec

# Lire les logs
curl -s -H "Authorization: Bearer <TOKEN>" http://38.247.138.124:8000/api/logs
```

### Upload + Restart workflow

```bash
# 1. Upload le fichier
curl -X POST -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" \
  -d '{"path": "bot_api.py", "content": "<contenu du fichier>"}' \
  http://38.247.138.124:8000/api/file

# 2. Redemarrer uvicorn (pour bot_api.py)
curl -X POST -H "Authorization: Bearer <TOKEN>" http://38.247.138.124:8000/api/restart

# OU redemarrer le bot (pour telegram_listener)
curl -X POST -H "Authorization: Bearer <TOKEN>" http://38.247.138.124:8000/api/bot/stop
sleep 2
curl -X POST -H "Authorization: Bearer <TOKEN>" http://38.247.138.124:8000/api/bot/start
```

---

## Repos GitHub

### Cloner les repos

```bash
# Configurer le token GitHub (une seule fois)
git config --global credential.helper store
echo "https://slh04ninja-cmyk:<TOKEN>@github.com" > ~/.git-credentials

# Cloner tgm (bot)
git clone https://github.com/slh04ninja-cmyk/tgm.git /data/data/com.termux/files/home/tgm

# Cloner CopyTrading (app Android)
git clone https://github.com/slh04ninja-cmyk/CopyTrading.git /data/data/com.termux/files/home/CopyTrading

# Cloner CT (copie isolee)
git clone https://github.com/slh04ninja-cmyk/CT.git /data/data/com.termux/files/home/CT
```

### Token GitHub

- **Compte** : `slh04ninja-cmyk`
- **Token** : stocke dans la config git locale (credential store)
- **Usage** : `git push origin main` pour tous les repos
- **IMPORTANT** : ne jamais commit le token dans les fichiers

### tgm (Bot principal)
- **Repo** : `slh04ninja-cmyk/tgm` (prive)
- **Branche** : `main`
- **Fichiers cles** :
  - `telegram_listener_v17_1.py` — bot principal (~4200 lignes)
  - `signal_parser_v15.py` — parser de signaux
  - `bot_messages_v15.py` — messages/alertes Telegram
  - `bot_api.py` — API REST FastAPI (uvicorn)
- **Dossier local** : `/data/data/com.termux/files/home/tgm/`

### CopyTrading (App Android)
- **Repo** : `slh04ninja-cmyk/CopyTrading` (prive)
- **Branche** : `main`
- **Build** : GitHub Actions → APK
- **Dossier local** : `/data/data/com.termux/files/home/CopyTrading/`
- **Structure** :
  - `app/src/main/java/com/copytrading/` — code Kotlin
  - `app/src/main/res/layout/` — layouts XML
  - `app/src/main/res/values/colors.xml` — couleurs
  - `app/src/main/res/drawable/` — drawables XML
  - `.github/workflows/` — CI build APK

### CT (Copie isolee)
- **Repo** : `slh04ninja-cmyk/CT` (prive)
- **Usage** : test isole pour install.bat + wizard
- **App renommee** : CopyTrading2
- **Dossier serveur** : `C:\TradingBot2\`

---

## Architecture du Bot

### Fichiers du bot (4 fichiers)

1. **telegram_listener_v17_1.py** (~4200 lignes)
   - Boucle principale async (Telethon)
   - Reception des signaux depuis 108 canaux Telegram
   - Parsing → decision (accepter/refuser/fusionner)
   - Execution des ordres MK/L1/L2 sur MT5
   - Deleted message tracker (1 min interval)
   - Rapport quotidien PDF + XLSX
   - Logs unifies : `== CH{num}-{mode} | {action} | PE={entry} | PA={current} ==`

2. **signal_parser_v15.py**
   - Classe `SignalParser` — parse les messages texte en signaux structures
   - Log level DEBUG (silencieux)

3. **bot_messages_v15.py**
   - Fonctions de logging : `log_signal_detected()`, `log_refuse()`, etc.
   - Format unifie sans accents

4. **bot_api.py**
   - Serveur FastAPI (uvicorn, port 8000)
   - Endpoints REST pour l'app Android
   - Gestion MT5 (connexion, ordres, historique)
   - Endpoint `/api/restart` pour recharger le code

### Variables d'environnement (.env)

| Variable | Description | Defaut |
|---|---|---|
| `TRADING_START_HOUR` | Heure debut trading UTC | 5 |
| `TRADING_END_HOUR` | Heure fin trading UTC | 19 |
| `MAX_SL_USD` | Stop loss max en USD | 10.0 |
| `LIMIT_OFFSET_1` | Offset L1 en USD | 3.0 |
| `LIMIT_OFFSET_2` | Offset L2 en USD | 6.0 |
| `DAILY_PROFIT_LIMIT` | Limite P&L quotidien | 200.0 |
| `API_TOKEN` | Token auth API | (generer) |

### Format des commentaires MT5

`CH{canal}-{signal}-{ordre}`

- **Canal** : CH5, CH3, CH60, etc.
- **Signal** : ZN (Zone), PU (Purge), MP (Momentum), QA (Quick Alert), AL (Alert)
- **Ordre** : MK (Market), L1 (Limit 1), L2 (Limit 2)

Exemple : `CH5-ZN-MK` = Canal 5, signal Zone, ordre Market

### Channels.txt

Format : `Canal_N : -100XXXXXXXXXX # NomDuCanal`
- 108 canaux configures
- `CHANNEL_NUM_MAP` : mapping canal → numero

### SL par signal

- Un seul prix SL calcule (via `_cap_sl` avec entry du MK)
- Applique identiquement a MK, L1, L2
- Risque total < 3×MAX_SL_USD car L1/L2 entrent plus proche du SL
- `LIMIT_OFFSET_1 = 3.0$`, `LIMIT_OFFSET_2 = 6.0$`

### Deleted message tracker

- Module-level `_signal_tracker` dict → `signal_tracker.json`
- Verification toutes les **1 minute** (60s)
- Max 3 jours retention
- Pas de logs — tableau dans PDF quotidien uniquement
- Colonnes : CH, Canal, S. Reçu, S. Supprimé, Message

---

## Architecture de l'App Android

### Stack
- **Langage** : Kotlin
- **UI** : XML layouts, Material3
- **HTTP** : OkHttp + Gson
- **Build** : GitHub Actions → APK

### Fichiers principaux

- `MainActivity.kt` — activite principale, dashboard, performance, positions, config, logs
- `ApiClient.kt` — client HTTP pour l'API du bot
- `ApiModels.kt` — data classes (Trade, Position, Dashboard, etc.)
- `DateRangePickerDialog.kt` — picker de dates custom
- `PositionAdapter.kt` — adapter RecyclerView pour les positions
- `activity_main.xml` — layout principal
- `item_position.xml` — layout d'une carte position

### Panels de l'app

1. **Dashboard (Overview)** — P&L quotidien, floating, balance, equity, winrate
2. **Performance** — 3 tableaux :
   - Par Canal (CH) — avec expandable detail (PF, RR, MD)
   - Par Signal (ZN, PU, MP, QA, AL)
   - Par Session (heure UTC 00h-23h)
3. **Positions** — positions ouvertes avec badges CH/signal/ordre, swipe-to-close
4. **Config** — edition des variables .env
5. **Logs** — affichage des logs du bot

### Design
- Dark theme : `--bg-primary: #0F0F1A`, `--bg-secondary: #1A1A2E`
- Accent : `#6C63FF`
- Font : Inter
- Material Icons Round
- Pas d'emojis dans l'UI (sauf icone daily limit)

### Positions UI
- Cartes compactes avec badges CH/signal/ordre
- Swipe-to-close (gauche) avec confirmation AlertDialog
- Bouton "TOUT FERMER" fixe en bas avec confirmation
- panelPositions est un FrameLayout (pas NestedScrollView)

---

## Build APK (GitHub Actions)

### Workflow

1. **Modifier le code** Kotlin/XML localement
2. **Commit + Push** → declenche le build automatique
3. **Attendre** ~2-3 minutes
4. **Telecharger** l'APK
5. **Envoyer** a l'utilisateur

### Commandes completes

```bash
# 1. Aller dans le dossier
cd /data/data/com.termux/files/home/CopyTrading

# 2. Modifier les fichiers...

# 3. Commit + Push
git add -A
git commit -m "feat: description du changement"
git push origin main

# 4. Attendre le build (recuperer le dernier run)
gh run list --repo slh04ninja-cmyk/CopyTrading --limit 1

# 5. Attendre la fin
gh run watch <RUN_ID> --exit-status

# 6. Telecharger l'APK
rm -f apk_download/app-debug.apk
gh run download <RUN_ID> --repo slh04ninja-cmyk/CopyTrading -n copytrading-debug -D apk_download
cp apk_download/app-debug.apk /storage/emulated/0/Download/CopyTrading.apk

# 7. Envoyer a l'utilisateur
# MEDIA:/storage/emulated/0/Download/CopyTrading.apk
```

### Points importants

- Le build prend ~2-3 minutes
- L'artifact s'appelle `copytrading-debug` (pas CopyTrading-apk)
- Toujours `rm -f` l'ancien APK avant de telecharger (conflit de zip)
- L'APK est un debug build (pas signe release)
- Pas besoin de rebuild si seul le bot (bot_api.py) est modifie — utiliser `/api/file` + `/api/restart`
- L'installation via `pm install` ne marche pas depuis Termux (permission Xiaomi) — envoyer via Telegram

---

## Modification du bot (cote serveur)

### Modifier bot_api.py

```bash
# 1. Modifier le fichier localement
# 2. Lire le contenu
cat /data/data/com.termux/files/home/CopyTrading/bot_api.py

# 3. Upload via API
curl -X POST -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" \
  -d '{"path": "bot_api.py", "content": "<contenu>"}' \
  http://38.247.138.124:8000/api/file

# 4. Redemarrer uvicorn
curl -X POST -H "Authorization: Bearer <TOKEN>" http://38.247.138.124:8000/api/restart

# 5. Verifier
curl -s -H "Authorization: Bearer <TOKEN>" http://38.247.138.124:8000/api/status
```

### Modifier telegram_listener_v17_1.py

```bash
# 1. Modifier le fichier localement
# 2. Upload via API (chemin relatif)
curl -X POST -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" \
  -d '{"path": "telegram_listener_v17_1.py", "content": "<contenu>"}' \
  http://38.247.138.124:8000/api/file

# 3. Redemarrer le bot (pas uvicorn)
curl -X POST -H "Authorization: Bearer <TOKEN>" http://38.247.138.124:8000/api/bot/stop
sleep 2
curl -X POST -H "Authorization: Bearer <TOKEN>" http://38.247.138.124:8000/api/bot/start

# 4. Verifier
curl -s -H "Authorization: Bearer <TOKEN>" http://38.247.138.124:8000/api/status
```

### Modifier signal_parser_v15.py ou bot_messages_v15.py

Meme workflow que telegram_listener — upload + restart bot.

---

## Commandes Git utiles

```bash
# Voir les fichiers modifies
git status

# Voir l'historique
git log --oneline -10

# Voir les differences
git diff

# Voir les differences avec origin
git diff origin/main

# Stash des changements temporaires
git stash
git stash pop

# Pull les changements d'un autre agent
git fetch origin
git pull --rebase origin main

# Voir les GitHub Actions recents
gh run list --repo slh04ninja-cmyk/CopyTrading --limit 5

# Voir les details d'un build
gh run view <RUN_ID> --repo slh04ninja-cmyk/CopyTrading
```

---

## Contraintes

- **Pas de SSH** sur le serveur Windows
- **Pas de adb** — travail sur telephone Android
- **Sous-agents** casses (DaemonThreadPoolExecutor) — modifications manuelles
- **Variables .env** non reloaded dynamiquement — redemarrage necessaire
- **P&L quotidien** reset a 5h UTC (TRADING_START_HOUR dans bot_api.py)
- **Dashboard** reset a zero hors plage trading (5h-19h UTC)
- **Sous-agents max 3** en parallele
- **Reponses courtes** — minimiser le temps de reponse
- **Francais** — langue principale
- **Pas d'emojis** dans l'UI Android
- **read_file** : toujours stripper les numeros de ligne avant d'ecrire dans des fichiers source

- **Fichiers a ne pas supprimer** : bias_filters.py, bot_messages.py, bot_documentation_v16.html, telegram_listener_v15.py, telegram_listener_v16.py

---

## Donnees MT5

- **Compte** : 262342460
- **Serveur** : Exness-MT5Trial16
- **Leverage** : 500
- **Données deals** : 2650 trades sur 30 jours
- **Channels.txt** : 108 canaux

---

## Rapport quotidien

Genere par `_shutdown_end_of_day()` :
- `report_daily_full()` → texte
- `_generate_daily_report_pdf()` → PDF avec tableau suppressions
- `_export_daily_xlsx()` → XLSX

### Tableau suppressions PDF
- Colonnes : CH(12mm), Canal(40mm), S.Reçu(22mm), S.Supprimé(22mm), Message(restant)
- Preview 80 chars, format HH:MM
- Tri par channel_num croissant
- Filtre trading day start (3h UTC → 3h UTC)
