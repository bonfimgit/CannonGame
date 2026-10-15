# Cannon Game – Capítulo 6

Implementação completa do app **Cannon Game** do Capítulo 6 do livro *Android 6 for Programmers: An App-Driven Approach* (3ª edição, Deitel), feita em Java no Android Studio.

> **Integrantes do grupo:** _(preencher)_

## O jogo

O objetivo é destruir os **9 alvos** antes que acabem os **10 segundos**. Toque na tela para mirar e disparar o canhão. Só pode haver uma bala na tela por vez.

| Evento | Efeito |
|---|---|
| Acertar um alvo | +3 s (`HIT_REWARD`), som de vidro quebrando, o alvo some |
| Acertar o bloqueador | −2 s (`MISS_PENALTY`), som de batida, a bala volta |
| Disparar | Som de tiro de canhão |
| Fim de jogo | `AlertDialog` com vitória/derrota, nº de disparos e tempo total, mais o botão **Reset Game** |

Os alvos e o bloqueador se movem verticalmente em velocidades diferentes e invertem a direção ao bater no topo ou na base da tela. O app roda só em paisagem, em tela cheia e em modo imersivo.

## Como os objetivos do trabalho foram atendidos

| Objetivo específico | Onde está no código | Seção do livro |
|---|---|---|
| 1. Animações e gráficos de baixo nível | `Canvas.drawRect/drawCircle/drawLine/drawText` com `Paint` em `CannonView.drawGameElements`, `Cannon.draw`, `Cannonball.draw` e `GameElement.draw` | 6.8, 6.11.4, 6.12.4, 6.13.10 |
| 2. Threads e controle de tempo | `CannonView.CannonThread`: loop do jogo baseado no tempo decorrido entre quadros (`elapsedTimeMS`), com o mesmo ritmo em qualquer aparelho | 6.13.7, 6.13.15 |
| 3. Eventos de toque | `CannonView.onTouchEvent` (`ACTION_DOWN`/`ACTION_MOVE`) → `alignAndFireCannonball` | 6.3.3, 6.13.8, 6.13.14 |
| 4. Sons no jogo | `SoundPool` + `AudioAttributes` (`USAGE_GAME`) + `SparseIntArray`; `setVolumeControlStream(STREAM_MUSIC)`; arquivos em `res/raw` | 6.3.4, 6.4.5, 6.13.3 |
| 5. `SurfaceView` e `SurfaceHolder` | `CannonView extends SurfaceView implements SurfaceHolder.Callback`; `lockCanvas`/`unlockCanvasAndPost` dentro de `synchronized(surfaceHolder)` | 6.3.5, 6.13.13, 6.13.15 |
| 6. Organização profissional | 8 classes com responsabilidades separadas (herança de `GameElement`), constantes proporcionais à tela, recursos externos (strings, cores, sons), ciclo de vida do Fragment (`onPause`/`onDestroy`) | 6.5 |

Colisões simples: `Cannonball.collidesWith` (`Rect.intersects`) e `CannonView.testForCollisions` (Seções 6.3.6 e 6.13.11).

## Estrutura

```
app/src/main/
├── AndroidManifest.xml              # orientação landscape (6.4.1)
├── java/com/deitel/cannongame/
│   ├── MainActivity.java            # Fig. 6.5
│   ├── MainActivityFragment.java    # Fig. 6.6
│   ├── GameElement.java             # Fig. 6.7
│   ├── Blocker.java                 # Fig. 6.8
│   ├── Target.java                  # Fig. 6.9
│   ├── Cannon.java                  # Figs. 6.10–6.14
│   ├── Cannonball.java              # Figs. 6.15–6.18
│   └── CannonView.java              # Figs. 6.19–6.34
└── res/
    ├── layout/activity_main.xml     # <fragment> (6.4.7)
    ├── layout/fragment_main.xml     # FrameLayout + CannonView (6.4.8)
    ├── raw/                         # cannon_fire.wav, target_hit.wav, blocker_hit.wav (6.4.5)
    ├── values/strings.xml           # Fig. 6.4
    ├── values/colors.xml            # dark/light (6.4.4)
    └── values/styles.xml            # tema sem título e em tela cheia (6.4.2)
```

## Como executar

1. Abra a pasta do projeto no **Android Studio** (*File → Open*).
2. Aguarde a sincronização do Gradle.
3. Execute em um aparelho ou emulador (*Run ▶*). O livro recomenda um aparelho físico, porque no emulador lento a bala às vezes parece atravessar os alvos.

Pela linha de comando: `./gradlew assembleDebug` (o APK fica em `app/build/outputs/apk/debug/`).

A cada push, o GitHub Actions (`.github/workflows/android.yml`) compila o projeto e disponibiliza o APK na aba **Actions**.

## Adaptações em relação ao código do livro

O código segue as figuras do capítulo. As mudanças abaixo foram necessárias para compilar e rodar nas versões atuais do Android:

- **Support Library → AndroidX.** `android.support.v7.app.AppCompatActivity` e `android.support.v4.app.Fragment` foram descontinuadas. Usamos os equivalentes `androidx.appcompat.app.AppCompatActivity` e `androidx.fragment.app.Fragment`.
- **Diálogo de fim de jogo.** O livro cria o `DialogFragment` como classe anônima. A partir do Android 8.0, o `FragmentManager` lança `IllegalStateException` ("must be a public static class") ao exibir um fragment anônimo, e o app fecharia no fim da partida. Por isso o diálogo virou a classe estática `CannonView.GameResultDialogFragment`, que recebe os dados por `Bundle`. O conteúdo (título, mensagem e botão **Reset Game**) é o mesmo da Fig. 6.27.
- **Proteções contra falhas:** verificação de `canvas == null` no loop do jogo, de `cannonThread == null` em `surfaceDestroyed` e de `soundPool == null` em `playSound`/`releaseResources`. A flag `threadIsRunning` agora é `volatile`, já que é escrita por uma thread e lida por outra.
- **Sons.** Os arquivos `.wav` dos exemplos do livro não vêm junto com o texto, então os três sons foram sintetizados para este projeto, com os mesmos nomes usados no capítulo.
- **Tradução.** Os textos originais (em inglês) estão em `values/strings.xml`. Há uma tradução opcional em `values-pt-rBR/`, usada automaticamente em aparelhos configurados em português.
