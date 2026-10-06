# AscendantCore

All-in-one SMP optimization + rules plugin for **Paper 1.21.11** (Java 21).

Open the control panel with `/ascendantcore:gui`, `/ascdcore:gui`, or `/ascendantcore gui`.
Left click toggles a module, right click configures it. Numbers are typed in an anvil (paper -> type value -> click result).

## Build
    mvn clean package
The jar is `target/AscendantCore.jar`. Drop it in `plugins/`.

## Commands
- `/ascendantcore [gui|reload|revive <player>|stacker <add|remove> <item>|restart [cancel]]` (alias `/ascdcore`, `/acore`) - `ascendantcore.admin`
- `/string` - turns cobwebs in hand into string (module must be on)
- `/withdrawheart [amount]` - lifesteal hearts to items

## Notes
- Item stacker is capped at 99 per ground stack (Minecraft cannot save larger item entities).
- Entity tracking range has no live API: the GUI button writes it to spigot.yml (backup created) and it applies on restart.
- Limiters apply on anvil / enchanting table / crafting; they do not rewrite items that already exist.
- Infinite restock raises each trade's max uses, so villagers keep that high limit if you turn the module off.
