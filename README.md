# Curious Mobs

Curious Mobs（万物皆饰）是一个 Minecraft 1.20.1 Forge 模组：让所有生物获得与玩家一致的饰品栏，并用「魔术师之手」打开任意生物的饰品栏进行查看、取放与诅咒转移。

## 功能

- **万物皆饰**：所有生物都拥有与玩家相同的 Curios 饰品栏（head / necklace / back / body / bracelet / hands / ring / belt / charm / curio）。
- **魔术师之手**：对任意生物右键，打开它的背包与饰品栏，可自由查看、取放饰品，大容量时支持翻页。
- **替死稻草人**：将玩家身上的诅咒饰品（及诅咒）转移到目标生物身上，稻草人朝向玩家放置时面朝的方向生成。
- **诅咒图腾**：将身上诅咒饰品的诅咒转移到目标生物，或永久清除自身诅咒效果（保留饰品增益）。
- **魔鬼契约**：对已驯服/受控生物缔结契约，烙上诡厄巫法可识别的仆从印记，成为永久仆从。
- **万物皆驯兼容**：受控生物佩戴灵魂石的 AOE 伤害/负面效果不会波及友善生物与控制者。
- 带诅咒的饰品默认不可转移（反转诅咒饰品可逆转）。

## 依赖

| 模组 | 类型 |
| ---- | ---- |
| [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios) | 必需 |
| [Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config) | 可选（仅配置界面按钮） |

- Minecraft 1.20.1 / Forge 47+

## 构建

```bash
gradlew build -x test
```

构建产物位于 `build/libs/curious_mobs-1.0.0.jar`。

## 许可

MIT License，见 [LICENSE](LICENSE)。
