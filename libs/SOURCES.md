# libs/ — compile-only third-party jars

`build.gradle` resolves these jars through `flatDir { dirs 'libs' }`, so Gradle looks them up by the
literal file name `<name>-<version>.jar`. **Do not rename them** — a different file name simply will
not be found.

`libs/` 里的 jar 只用于**编译期**（`compileOnly`），运行期不需要安装；文件名必须与下表完全一致，
否则 `flatDir` 找不到依赖。以下 sha512 都是对本仓库实际提交的文件算出来的。

Total: 7 jars, 74.33 MB.

| File | modId / version |
|---|---|
| `tacz-neoforge-1.21.1-1.1.8-hotfix-r7.jar` | `tacz` 1.1.8-hotfix-r7 |
| `pointblank-neoforge-1.21-1.11.1.jar` | `pointblank` 1.11.1 |
| `musketmod-1.21.1-neoforge-1.5.4.jar` | `musketmod` 1.5.4 |
| `Enhanced-Celestials-NeoForge-1.21.1-6.0.2.6.jar` | `enhancedcelestials` 6.0.2.6 |
| `Enhanced-Celestials-2-Core-NeoForge-1.21.1-2.0.3.3.jar` | `enhancedcelestials2core` 2.0.3.3 |
| `data-anchor-neoforge-1.21.1-2.0.0.17.jar` | `dataanchor` 2.0.0.17 |
| `geckolib-neoforge-1.21.1-4.9.3.jar` | `geckolib` 4.9.3 |

---

## tacz-neoforge-1.21.1-1.1.8-hotfix-r7.jar

- **mod**: `tacz` (Timeless and Classics Zero) 1.1.8-hotfix-r7, loader `javafml`
- **source**: community **NeoForge 1.21.1 port** supplied with this port project. The official TaCZ
  Modrinth project publishes no NeoForge 1.21.1 build, so this file has no public upstream URL —
  it is recorded here by checksum only.
- **sha512**: `7700ed99a080729c3aa21f9619e018fbe9e2303cfbcaa463175ee5170553a000b692bc4cfa2e8ce0778747eed34dcc0197fb5e4344bb258cff1ba90adc842642`

## pointblank-neoforge-1.21-1.11.1.jar

- **mod**: `pointblank` (Vic's Point Blank) 1.11.1, loader `javafml`
- **source**: Modrinth `vics-point-blank`, version `E7JIkQhi` (release)
  <https://cdn.modrinth.com/data/og4KPYmA/versions/E7JIkQhi/pointblank-neoforge-1.21-1.11.1.jar>
- **sha512**: `3f2175757533f5db36d2c58e2059d1b41a146563e483ec495787b169bf254f6381766c3a594e6841d8d81e8627dad5a5b781c863b43506c01405cf2c8a54a186`

## musketmod-1.21.1-neoforge-1.5.4.jar

- **mod**: `musketmod` (Musket Mod) 1.5.4, loader `javafml`
- **source**: Modrinth `musket-mod`, version `hro6S6DK` (release)
  <https://cdn.modrinth.com/data/967vM4zw/versions/hro6S6DK/musketmod-1.21.1-neoforge-1.5.4.jar>
- **sha512**: `a1a544293057232305c75ffb288a33592115cda83471dd508b4a8fc27ff5332be0c57c0961bc57a34077f437c3ddd3bb6c9f3bf84dc9c813be7d8919a79fcf6c`

## Enhanced-Celestials-NeoForge-1.21.1-6.0.2.6.jar

- **mod**: `enhancedcelestials` (Enhanced Celestials) 6.0.2.6, loader `javafml`
- **source**: Modrinth `enhanced-celestials`, version `bPhEKzZO` (release)
  <https://cdn.modrinth.com/data/2rL16t1O/versions/bPhEKzZO/Enhanced-Celestials-NeoForge-1.21.1-6.0.2.6.jar>
- **sha512**: `5119e1a0026d705001e41abb7099fabe9379ad44ce624d3ec57b7cda01fc763c23dff0ebe058adedfd2de20d1a7d6245956574e4d9cfa7f199186115ca2d7811`

## Enhanced-Celestials-2-Core-NeoForge-1.21.1-2.0.3.3.jar

- **mod**: `enhancedcelestials2core` (Enhanced Celestials 2: Core) 2.0.3.3, loader `javafml`
- **source**: Modrinth `enhanced-celestials-2-core`, version `CWaaEX5L` (release)
  <https://cdn.modrinth.com/data/ugCWUHV9/versions/CWaaEX5L/Enhanced-Celestials-2-Core-NeoForge-1.21.1-2.0.3.3.jar>
- **sha512**: `e23e093260ae2fca326ab102325d4a2273da9c5f4d15b00c9e42b3a9d47b3b655920cc3e012350ed1e500284762ed8ca99b840fd2a88f254f9c74f62e96c2a64`

## data-anchor-neoforge-1.21.1-2.0.0.17.jar

- **mod**: `dataanchor` (Data Anchor) 2.0.0.17, loader `javafml`
- **source**: Modrinth `data-anchor`, version `Q3J2iejk` (release). Upstream file name is
  `Data_Anchor-neoforge-1.21.1-2.0.0.17.jar`; it is renamed here to the `flatDir` convention.
  <https://cdn.modrinth.com/data/z2XEADmE/versions/Q3J2iejk/Data_Anchor-neoforge-1.21.1-2.0.0.17.jar>
- **sha512**: `f760e127bc3a4701548da6d80012027aaad8b65c6895362b3f42c224bdf3d0083e0a8b94ec42d01c3f8d03a0e1d8123632f364f819882aa00913a2eee7907a5e`

## geckolib-neoforge-1.21.1-4.9.3.jar

- **mod**: `geckolib` (GeckoLib) 4.9.3, loader `javafml`
- **source**: Modrinth `geckolib`, version `Grwn5rUB` (release)
  <https://cdn.modrinth.com/data/8BmcQJ2H/versions/Grwn5rUB/geckolib-neoforge-1.21.1-4.9.3.jar>
- **sha512**: `8dfa5f8c57224a5e34e5098367ea9819ca6dc9c11357cd3062c16da3455c8bd1a483098af7383debc4b9ce8e4d7ab3fdc799b415644eb829da83eb72a7335dfd`

---

## Licensing

These jars remain the property of their respective authors and are redistributed here only as
compile-time references, under each project's own license. They are **not** required at runtime, and
this project does not modify them.

`libs/` 内各 jar 版权归各自作者，此处仅作编译期引用，遵循各自项目许可，未做任何修改。
