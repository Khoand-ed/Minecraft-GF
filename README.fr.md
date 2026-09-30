# MCGF AI Companion

> [English](README.md) | [Tiếng Việt](README.vi.md) | [日本語](README.ja.md) | Français

Mod compagnon pour **Minecraft 1.21.1 Java Edition (Fabric)**. Invoquez un compagnon humanoïde : il vous suit, combat à vos côtés, mine, cultive, discute avec une IA et possède son propre inventaire.

> Repo : https://github.com/Khoand-ed/Minecraft-GF
> MC `1.21.1` | Fabric Loader `0.16.14` | Fabric API `0.102.0+1.21.1` | Java `21`

## Prérequis

- Minecraft Java Edition **1.21.1**
- Fabric Loader **0.16.14** + Fabric API `0.102.0+1.21.1`
- JDK **21** (seulement pour compiler depuis les sources)

## Installation

1. Téléchargez `mcgf-ai-companion-<version>.jar` depuis
   https://github.com/Khoand-ed/Minecraft-GF/releases (dernière version).
2. Placez-le dans `.minecraft/mods/` (profil Fabric 1.21.1 avec Fabric API).
3. Lancez le jeu, ouvrez un monde avec les cheats activés (ou OP sur serveur).

## Démarrage rapide (en jeu)

```
/gf spawn    # invoquer votre compagnon
/gf follow   # il vous suit et vous protège
@gf bonjour  # discuter (vietnamien/anglais/japonais/français, sans clé)
/gf lang fr  # changer la langue de l'IA : vi | en | ja | fr
```

## Commandes

### Compagnon

| Commande | Effet |
|---|---|
| `/gf spawn` | Invoque votre compagnon (apprivoisé, nommé, persiste après reconnexion) |
| `/gf follow` | Vous suit (comportement par défaut) |
| `/gf stay` | Reste immobile (pose accroupie) |
| `/gf here` | Le téléporte à côté de vous |
| `/gf goto <x> <y> <z>` | L'envoie aux coordonnées, puis reste immobile |
| `/gf dismiss` | Le renvoie |
| `/gf name <nom>` | Le renomme (plaque au-dessus de la tête incluse) |
| `/gf help` | Liste complète des commandes, par catégorie |

S'il se perd trop loin (au-delà de `teleportDistance`, 24 blocs par défaut), il se téléporte tout seul.

### Combat & survie

| Commande | Effet |
|---|---|
| `/gf attack` | Attaque le mob hostile le plus proche (16 blocs) |
| `/gf stop` | Arrête de combattre / annule la tâche en cours |
| `/gf mine` | Regardez un bloc (à moins de 6 blocs), il le mine pour vous — drops réels |
| `/gf collect` | Aspire objets au sol + XP (10 blocs) dans votre inventaire |
| `/gf feed` | Le nourrit avec la viande de votre inventaire pour le soigner |

Hors combat, il régénère lentement sa vie. La viande cuite soigne plus que la crue.

### Travaux auto (bloc par bloc, ~1 bloc / 4 ticks)

| Commande | Effet |
|---|---|
| `/gf minevein` | Mine tout un filon proche (max 32 blocs, rayon 24) |
| `/gf chop` | Abat un arbre entier proche |
| `/gf farm` | Récolte les cultures mûres + replante |
| `/gf bag` | Voir son inventaire privé de 9 slots |
| `/gf give` | Récupérer son contenu (le trop-plein tombe à vos pieds) |
| `/gf deposit` | Range son inventaire dans le coffre/tonneau le plus proche (8 blocs) |

Les drops tombent au sol comme en vraie survie, puis il les ramasse dans son inventaire. Les anciens compagnons-loups se convertissent automatiquement et gardent leur inventaire.

### Équipement (stats réelles, sauvegardées avec le compagnon)

| Commande | Effet |
|---|---|
| `/gf equip` | Équipe la meilleure arme + armure (son sac d'abord, puis votre inventaire) |
| `/gf gear` | Affiche l'équipement, les dégâts et l'armure |
| `/gf unequip` | Tout retire, retour dans votre inventaire |

Dégâts et protection calculés pour de vrai (système vanilla), et visibles sur son corps : arme en main droite, armure pièce par pièce.

### Chat IA

Parlez avec le préfixe (défaut `@gf`) n'importe où dans le chat :

```
@gf c'est quoi un creeper ?
@gf où suis-je ?
```

| Commande | Effet |
|---|---|
| `/gf ask <question>` | Comme le chat avec préfixe |
| `/gf forget` | Efface la mémoire de conversation |
| `/gf ai on\|off` | Active/coupe l'IA en ligne (OP requis) |
| `/gf apikey <clé>` | Enregistre une clé Gemini gratuite (OP requis, masquée) |
| `/gf lang vi\|en\|ja\|fr` | Langue de l'IA (vietnamien / anglais / japonais / français) |

Avec une clé, il appelle Gemini (asynchrone, sans lag) en connaissant votre position, vie, faim et la conversation récente. Sans clé (ou en cas d'erreur réseau), il répond hors-ligne. La langue suit `/gf lang` pour les deux modes. Mémoire sauvegardée dans `config/mcgf_history.json` à l'arrêt du serveur, rechargée au démarrage — une mémoire par joueur. Clé gratuite : https://aistudio.google.com/apikey.

### Divers

| Commande | Effet |
|---|---|
| `/gf say <text>` | Le fait répéter |
| `/gf hello` | Salutation |
| `/gf config` | Affiche la configuration |
| `/gf prefix <p>` | Change le préfixe de chat (OP requis) |
| `/gf version` | Affiche la version |

## Skins

Votre compagnon ressemble à un joueur (bras classiques). Priorité, sans recompiler :

1. `config/mcgf_skin.png` — déposez un skin PNG 64x64 dans le dossier config, reconnectez.
2. `skinUrl` dans `config/mcgf.json` — lien PNG direct, utilisé sans fichier.
3. Skin intégré par défaut (hoodie bleu + jean).

## Config (`config/mcgf.json`)

```json
{
  "companionName": "GF",
  "chatPrefix": "@gf",
  "followDistance": 3.0,
  "teleportDistance": 24.0,
  "replyInVietnamese": true,
  "language": "vi",
  "aiEnabled": true,
  "maxHistory": 8,
  "geminiApiKey": "",
  "geminiModel": "gemini-2.0-flash",
  "skinFile": "mcgf_skin.png",
  "skinUrl": ""
}
```

`config/mcgf.json` et `config/mcgf_history.json` sont git-ignorés — votre clé API ne quitte jamais votre machine.

## Multijoueur

- Fonctionne sur **serveur Fabric 1.21.1** : mod + Fabric API dans `mods` du serveur.
- **Chaque joueur doit installer** Fabric + le mod (entité custom, les clients vanilla ne l'affichent pas).
- **Impossible** sur Realms ou serveurs Vanilla/Paper/Spigot.
- Réservé OP : `apikey`, `ai`, `prefix`. Chacun son compagnon et sa mémoire IA.

## Compiler depuis les sources

Chaque push sur `main` lance le workflow `Build mod` (Gradle 8.10.2 + JDK 21) ; le `.jar` est dans Artifacts. Ou en local :

```powershell
cd D:\MCGF
.\gradlew.bat build
# build/libs/mcgf-ai-companion-<version>.jar
```

## Checklist de test complète

```
/gf version → version actuelle
/gf spawn → /gf follow → /gf attack → /gf stop → /gf mine → /gf collect → /gf feed
/gf minevein → attendre → /gf bag → /gf give → /gf deposit (coffre près du compagnon)
/gf chop → /gf farm (cultures mûres requises près du compagnon)
/gf equip → /gf gear (dégâts/armure corrects) → arme + armure visibles
/gf apikey <clé> → @gf où suis-je ? (il connaît votre position)
/gf prefix @bot → "@bot bonjour"
/gf lang fr → @gf bonjour (répond en français)
/gf config → affiche la config dont le skin
PNG 64x64 dans config/mcgf_skin.png → reconnexion → nouveau skin
Redémarrage serveur → mémoire IA + inventaire intacts
```

## Licence

MIT — voir `LICENSE`.
