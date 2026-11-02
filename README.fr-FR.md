# KeyBind Bundles v1.4.2

KeyBind Bundles permet de regrouper vos raccourcis clavier dans des menus radiaux interactifs pour Minecraft NeoForge 1.21.1.

## Nouveautés v1.4.2
- **Sélecteur visuel d'items (`ItemPickerScreen`)** : Parcourez et filtrez visuellement tous les items du jeu dans une grille complète avec recherche instantanée. La sélection est immédiatement appliquée à l'écran d'édition sans réinitialisation des champs.
- **Collage d'images personnalisées (`ImageClipboardHelper` & `CustomImageManager`)** :
  - Collez n'importe quelle image (PNG, JPG, JPEG, SVG) directement depuis le presse-papier système (capture d'écran, image copiée dans le navigateur/Paint), d'un fichier d'image copié dans l'explorateur Windows, d'un chemin de fichier local ou d'une URL web.
  - Décodage asynchrone sécurisé via ImageIcon et fallback sur NativeImage (stb_image).
  - Enregistrement automatique et persistant au format 16x16 dans le dossier `config/keybindbundles/custom_icons/`.
- **Rendu dynamique unifié** : Affiche aussi bien les items classiques de Minecraft que les textures d'images importées sur les menus radiaux.

## Compilation
Exécuter le script PowerShell :
```powershell
./build.ps1
```
