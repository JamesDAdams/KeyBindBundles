# KeyBind Bundles v1.4.2

KeyBind Bundles allows you to bundle keybinds into interactive radial menus for Minecraft NeoForge 1.21.1.

## Features & Improvements in v1.4.2
- **Visual Item Selector (`ItemPickerScreen`)**: Browse and filter all in-game items visually in a full grid with instantaneous search. Persists chosen item back to EditKeyScreen seamlessly without losing form state.
- **Custom Image Pasting (`ImageClipboardHelper` & `CustomImageManager`)**:
  - Paste any image (PNG, JPG, JPEG, SVG) straight from your system clipboard (Snipping Tool, Discord, Chrome, etc.) or copied file from Windows Explorer or web URLs.
  - Automatically loads ToolkitImage asynchronously using synchronous ImageIcon decoding and fallback to NativeImage (stb_image).
  - Caches and registers 16x16 icon textures cleanly into `config/keybindbundles/custom_icons/`.
- **Dynamic Radial Rendering**: Renders both standard Minecraft item stacks and custom imported textures seamlessly on radial dials.

## Build
Run PowerShell build script:
```powershell
./build.ps1
```
