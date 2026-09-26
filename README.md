# 📈 CopyTrading — Bot Manager

Application Android pour gérer le bot de trading CopyTrading déployé sur un serveur Windows distant.

## ✨ Fonctionnalités

- **Dashboard live** : P&L quotidien, flottant, total
- **Statistiques** : Trades, wins, losses, winrate
- **Positions ouvertes** : Liste en temps réel avec P&L par position
- **Start/Stop** du bot à distance
- **Fermeture** de positions individuelles ou toutes
- **Éditeur de config** (.env) à distance
- **Logs** en temps réel
- **Thème sombre** Material Design 3
- **Auto-refresh** toutes les 5 secondes

## 🏗️ Architecture

```
┌─────────────────┐     HTTP/REST      ┌─────────────────────┐
│  Android App    │ ◄──────────────────► │  bot_api.py (FastAPI)│
│  (Kotlin)       │                      │  Port 8000           │
└─────────────────┘                      └──────────┬──────────┘
                                                    │
                                         ┌──────────▼──────────┐
                                         │  telegram_listener   │
                                         │  _v17_1.py (Bot)     │
                                         │  + MetaTrader 5      │
                                         └─────────────────────┘
```

## 📱 Application Android

### Fichiers sources

```
app/src/main/java/com/copytrading/
├── SetupActivity.kt       # Configuration connexion serveur
├── MainActivity.kt        # Dashboard principal
├── api/
│   └── ApiClient.kt       # Client HTTP (OkHttp)
├── model/
│   └── ApiModels.kt       # Modèles de données
└── ui/
    └── PositionAdapter.kt # Liste des positions
```

### Compilation

```bash
# Cloner
git clone https://github.com/slh04ninja-cmyk/CopyTrading.git
cd CopyTrading

# Compiler l'APK debug
./gradlew assembleDebug

# L'APK sera dans app/build/outputs/apk/debug/
```

### Build automatique (GitHub Actions)

À chaque push sur `main`, le workflow :
1. Compile l'APK release (signé)
2. Compile l'APK debug
3. Upload les deux en artifacts

**Secrets requis** dans GitHub :
- `KEYSTORE_JKS` : keystore en base64
- `KEYSTORE_PASSWORD` : mot de passe du keystore
- `KEY_ALIAS` : alias de la clé
- `KEY_PASSWORD` : mot de passe de la clé

## 🖥️ Serveur API (bot_api.py)

### Installation

```bash
pip install fastapi uvicorn python-dotenv MetaTrader5
```

### Lancement

```bash
python bot_api.py
```

L'API démarre sur `http://0.0.0.0:8000`.

### Variables d'environnement

| Variable | Défaut | Description |
|---|---|---|
| `BOT_SCRIPT` | `telegram_listener_v17_1.py` | Script du bot |
| `BOT_WORKDIR` | (répertoire courant) | Répertoire de travail |
| `API_PORT` | `8000` | Port de l'API |
| `API_HOST` | `0.0.0.0` | Host de l'API |
| `API_TOKEN` | (vide) | Token d'authentification |

### Endpoints

| Méthode | Path | Description |
|---|---|---|
| GET | `/api/status` | Status du bot + MT5 |
| GET | `/api/dashboard` | P&L, stats, positions |
| GET | `/api/positions` | Positions ouvertes |
| GET | `/api/trades?days=7` | Historique trades |
| POST | `/api/bot/start` | Démarrer le bot |
| POST | `/api/bot/stop` | Arrêter le bot |
| GET | `/api/config` | Lire la config (.env) |
| PUT | `/api/config` | Modifier la config |
| GET | `/api/logs?lines=100` | Lire les logs |
| WS | `/ws/logs` | Logs en temps réel |
| POST | `/api/positions/{ticket}/close` | Fermer une position |
| POST | `/api/positions/close-all` | Fermer toutes les positions |

## 🔐 Sécurité

1. **Firewall Windows** : Ouvrir le port 8000 uniquement pour votre IP
2. **API_TOKEN** : Définir un token dans `.env` et dans l'app
3. **HTTPS** : Utiliser un reverse proxy (nginx) avec SSL en production
4. **VPN** : Recommandé pour un accès sécurisé

### Exemple nginx (HTTPS)

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://127.0.0.1:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 📦 Déploiement sur le serveur Windows

1. Copier `bot_api.py` dans le même dossier que le bot
2. Installer les dépendances : `pip install fastapi uvicorn`
3. Lancer : `python bot_api.py`
4. (Optionnel) Créer un service Windows avec NSSM :
   ```
   nssm install CopyTradingAPI python bot_api.py
   nssm start CopyTradingAPI
   ```

## 📝 Licence

Projet privé — Usage personnel.
